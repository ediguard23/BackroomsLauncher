package net.backrooms.evento.estacion;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.bloques.EnPared;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Cuando alguien corta con la cizalla la cadena de la puerta del final (JuegoEstacion.persianaCerrada): el
 * chasquido, el candado que salta con el arco partido y cae dando vueltas, la cadena que se descuelga por
 * los lados y cae a plomo, el monton que queda en el suelo y, al final, la persiana que sube (eso lo hace
 * JuegoEstacion a los PUERTA ticks). Los trozos son entidades de bloque que se mueven solas (la
 * interpolacion de las display entities); el sonido, uno solo con todo a su tiempo (estacion.corte).
 */
final class CorteCadena {
	/** Cuando sube la persiana. */
	static final int PUERTA = 36;
	private static final int FIN = 32;

	private final List<Display.BlockDisplay> piezas = new ArrayList<>();
	private final Display.BlockDisplay candado;
	private final Display.BlockDisplay izq;
	private final Display.BlockDisplay medio;
	private final Display.BlockDisplay der;
	private int edad;

	private CorteCadena(ServerLevel nivel) {
		int[] c = PlanoEstacion.CADENA;
		this.izq = this.pieza(nivel, c[0], c[1], c[2], 0);
		this.medio = this.pieza(nivel, c[0] + 1, c[1], c[2], 3);
		this.der = this.pieza(nivel, c[0] + 2, c[1], c[2], 2);
		this.candado = this.pieza(nivel, c[0] + 1, c[1], c[2], 4);
	}

	/** Empieza (quita los bloques de la cadena y pone los trozos que se van a caer). */
	static CorteCadena empezar(ServerLevel nivel) {
		int[] c = PlanoEstacion.CADENA;
		CorteCadena corte = new CorteCadena(nivel);
		for (int k = 0; k < 3; k++) {
			nivel.setBlock(new BlockPos(c[0] + k, c[1], c[2]), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		nivel.playSound(null, c[0] + 1.5, c[1] + 0.5, c[2] + 0.5, Sonidos.ESTACION_CORTE, SoundSource.BLOCKS, 2.5F, 1.0F);
		nivel.sendParticles(ParticleTypes.CRIT, c[0] + 1.5, c[1] + 0.4, c[2] + 0.8, 10, 0.1, 0.1, 0.05, 0.25);
		return corte;
	}

	static BlockState estado(int variante) {
		return BloquesEstacion.CADENA.defaultBlockState().setValue(EnPared.FACING, Direction.SOUTH).setValue(Variante.VARIANTE, variante);
	}

	private Display.BlockDisplay pieza(ServerLevel nivel, int x, int y, int z, int variante) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, nivel) {
			@Override
			public boolean shouldBeSaved() {
				return false;
			}
		};
		d.setBlockState(estado(variante));
		d.setPos(x, y, z);
		d.setViewRange(1.5F);
		nivel.addFreshEntity(d);
		this.piezas.add(d);
		return d;
	}

	/** Mueve una pieza a (traslacion, giro alrededor de un pivote del bloque) en `ticks`. */
	private static void mover(Display.BlockDisplay d, int ticks, Vector3f desp, Quaternionf giro, Vector3f pivote) {
		Vector3f t = new Vector3f(pivote).sub(giro.transform(new Vector3f(pivote))).add(desp);
		d.setTransformationInterpolationDelay(0);
		d.setTransformationInterpolationDuration(ticks);
		d.setTransformation(new Transformation(t, new Quaternionf(giro), null, null));
	}

	private static Quaternionf rx(float grados) {
		return new Quaternionf().rotationX((float) Math.toRadians(grados));
	}

	/** Un tick; true cuando ha acabado (los trozos ya no estan y el monton esta en el suelo). */
	boolean tick(ServerLevel nivel) {
		this.edad++;
		int[] c = PlanoEstacion.CADENA;
		Vector3f centro = new Vector3f(0.5F, 0.3F, 0.88F);
		switch (this.edad) {
			// el candado salta (se le va el arco) y cae dando vueltas hasta el suelo
			case 2 -> mover(this.candado, 6, new Vector3f(0.05F, -0.55F, 0.2F), rx(-80.0F), centro);
			case 8 -> mover(this.candado, 4, new Vector3f(0.1F, -1.02F, 0.35F), rx(-180.0F), centro);
			case 12 -> mover(this.candado, 3, new Vector3f(0.12F, -0.98F, 0.38F), rx(-170.0F), centro);
			// el tramo del medio se suelta y cae a plomo
			case 6 -> mover(this.medio, 10, new Vector3f(0.0F, -1.0F, 0.15F), rx(-85.0F), new Vector3f(0.5F, 0.4F, 0.9F));
			// los de los lados se descuelgan desde el marco (como una bisagra) y luego caen
			case 9 -> {
				mover(this.izq, 8, new Vector3f(), new Quaternionf().rotationZ((float) Math.toRadians(-62.0F)), new Vector3f(0.0F, 0.5F, 0.9F));
				mover(this.der, 8, new Vector3f(), new Quaternionf().rotationZ((float) Math.toRadians(62.0F)), new Vector3f(1.0F, 0.5F, 0.9F));
			}
			case 18 -> {
				mover(this.izq, 7, new Vector3f(0.3F, -1.0F, 0.1F), new Quaternionf().rotationZ((float) Math.toRadians(-90.0F)).mul(rx(-80.0F)), new Vector3f(0.0F, 0.5F, 0.9F));
				mover(this.der, 7, new Vector3f(-0.3F, -1.0F, 0.1F), new Quaternionf().rotationZ((float) Math.toRadians(90.0F)).mul(rx(-80.0F)), new Vector3f(1.0F, 0.5F, 0.9F));
			}
			case 26 -> nivel.sendParticles(ParticleTypes.POOF, c[0] + 1.5, c[1] - 0.8, c[2] + 0.6, 8, 0.6, 0.05, 0.2, 0.01);
			default -> {
			}
		}
		if (this.edad >= FIN) {
			this.quitar();
			BlockPos monton = new BlockPos(c[0] + 1, c[1] - 1, c[2]);
			if (nivel.getBlockState(monton).isAir()) {
				nivel.setBlock(monton, estado(5), Block.UPDATE_CLIENTS);
			}
			return true;
		}
		return false;
	}

	void quitar() {
		this.piezas.forEach(Display.BlockDisplay::discard);
		this.piezas.clear();
	}
}
