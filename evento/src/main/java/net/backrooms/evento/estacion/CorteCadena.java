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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Cuando alguien corta con la cizalla el candado de la cadena de la puerta del final (JuegoEstacion.persianaCerrada):
 * el chasquido, el candado que salta con el arco partido y cae al suelo dando media vuelta, y la cadena, que se
 * parte por el medio: cada mitad sigue sujeta a su lado del marco y cae por su peso, se balancea como un
 * pendulo y se queda colgando. Cada mitad es una cuerda de eslabones (Verlet: gravedad y la distancia entre
 * eslabones), y cada eslabon una entidad de bloque que se mueve sola (la interpolacion de las display
 * entities). Al acabar se quitan y queda el bloque de la cadena cortada (variante 5, con la misma pose). La
 * persiana sube a los PUERTA ticks (eso lo hace JuegoEstacion); el sonido, uno solo con todo a su tiempo
 * (estacion.corte).
 */
final class CorteCadena {
	/** Cuando sube la persiana. */
	static final int PUERTA = 36;
	/** Cuando se acaba: las mitades ya casi quietas se dejan como el bloque de la cadena cortada. */
	private static final int FIN = 100;
	/** Eslabones por mitad y lo que mide cada uno (bloques). */
	private static final int ESLABONES = 6;
	/** Donde se sujeta cada mitad: a 2,5 px del borde del marco (como el bloque de la cadena cortada). */
	private static final float ANCLA = 2.5F / 16.0F;
	private static final float LARGO = (1.5F - ANCLA) / ESLABONES;
	/** Altura de la cadena dentro de su bloque y su z (la del modelo, girado al sur). */
	private static final float ALTO = 0.5F;
	private static final Vector3f CENTRO_ESLABON = new Vector3f(0.5F, 0.5F, 1.5F / 16.0F);
	/** El cuerpo del candado (modelo de la variante 4, girado al sur), para girarlo sobre si mismo. */
	private static final Vector3f CENTRO_CANDADO = new Vector3f(8.0F / 16.0F, 3.5F / 16.0F, 1.9F / 16.0F);
	/** Cuanto baja y avanza el centro del candado hasta quedar tumbado en el suelo (como en la variante 5). */
	private static final float CAE = 18.6F / 16.0F;
	private static final float AVANZA = 2.6F / 16.0F;
	private static final float GRAVEDAD = 0.045F;
	private static final int PASOS = 4;

	private final List<Display.BlockDisplay> piezas = new ArrayList<>();
	private final Mitad izq;
	private final Mitad der;
	private final Display.BlockDisplay candado;
	/** El candado: lo que ha bajado su centro, a que velocidad, y lo que ha girado (grados). */
	private float caidaCandado;
	private float velCandado = -0.06F;
	private float giroCandado;
	private boolean enSuelo;
	private int edad;

	/** Una mitad de la cadena: puntos (x, y) del ancla a la punta cortada, en bloques desde la esquina de la cadena. */
	private final class Mitad {
		final float[] x = new float[ESLABONES + 1];
		final float[] y = new float[ESLABONES + 1];
		final float[] xa = new float[ESLABONES + 1];
		final float[] ya = new float[ESLABONES + 1];
		final Display.BlockDisplay[] eslabones = new Display.BlockDisplay[ESLABONES];
		final float anclaX;

		Mitad(ServerLevel nivel, int[] c, boolean izquierda) {
			this.anclaX = izquierda ? ANCLA : 3.0F - ANCLA;
			float hacia = izquierda ? 1.0F : -1.0F;
			for (int i = 0; i <= ESLABONES; i++) {
				this.x[i] = this.anclaX + hacia * LARGO * i;
				this.y[i] = ALTO;
				this.xa[i] = this.x[i];
				this.ya[i] = this.y[i];
			}
			// al cortarse se suelta la tension: la punta da un tiron hacia su lado y un poco hacia arriba
			for (int i = 1; i <= ESLABONES; i++) {
				float k = (float) i / ESLABONES;
				this.xa[i] += hacia * 0.05F * k;
				this.ya[i] -= 0.02F * k;
			}
			for (int i = 0; i < ESLABONES; i++) {
				this.eslabones[i] = CorteCadena.this.pieza(nivel, c, i % 2 == 0 ? 6 : 7);
			}
		}

		/** Un tick de fisica (en PASOS pasos): inercia con rozamiento, gravedad y el largo de los eslabones. */
		void mover(float rozamiento) {
			float g = GRAVEDAD / (PASOS * PASOS);
			for (int paso = 0; paso < PASOS; paso++) {
				for (int i = 1; i <= ESLABONES; i++) {
					float vx = (this.x[i] - this.xa[i]) * rozamiento;
					float vy = (this.y[i] - this.ya[i]) * rozamiento;
					this.xa[i] = this.x[i];
					this.ya[i] = this.y[i];
					this.x[i] += vx;
					this.y[i] += vy - g;
				}
				for (int it = 0; it < 10; it++) {
					this.x[0] = this.anclaX;
					this.y[0] = ALTO;
					for (int i = 0; i < ESLABONES; i++) {
						float dx = this.x[i + 1] - this.x[i];
						float dy = this.y[i + 1] - this.y[i];
						float d = Math.max(1.0E-5F, (float) Math.sqrt(dx * dx + dy * dy));
						float f = (d - LARGO) / d;
						if (i == 0) {
							this.x[1] -= dx * f;
							this.y[1] -= dy * f;
						} else {
							this.x[i] += dx * f * 0.5F;
							this.y[i] += dy * f * 0.5F;
							this.x[i + 1] -= dx * f * 0.5F;
							this.y[i + 1] -= dy * f * 0.5F;
						}
					}
				}
			}
		}

		/** Los ultimos ticks: se lleva cada punto a la pose del bloque de la cadena cortada (colgando recta). */
		void asentar(float k) {
			for (int i = 1; i <= ESLABONES; i++) {
				float x = Mth.lerp(k, this.x[i], this.anclaX);
				float y = Mth.lerp(k, this.y[i], ALTO - LARGO * i);
				this.xa[i] += x - this.x[i];
				this.ya[i] += y - this.y[i];
				this.x[i] = x;
				this.y[i] = y;
			}
		}

		/** Cada eslabon, entre sus dos puntos y girado como ellos. */
		void dibujar() {
			for (int i = 0; i < ESLABONES; i++) {
				float mx = (this.x[i] + this.x[i + 1]) * 0.5F;
				float my = (this.y[i] + this.y[i + 1]) * 0.5F;
				float ang = (float) Math.atan2(this.y[i + 1] - this.y[i], this.x[i + 1] - this.x[i]);
				Quaternionf q = new Quaternionf().rotationZ(ang);
				Vector3f t = new Vector3f(mx, my, CENTRO_ESLABON.z).sub(q.transform(new Vector3f(CENTRO_ESLABON)));
				poner(this.eslabones[i], 1, t, q);
			}
		}
	}

	private CorteCadena(ServerLevel nivel) {
		int[] c = PlanoEstacion.CADENA;
		this.izq = new Mitad(nivel, c, true);
		this.der = new Mitad(nivel, c, false);
		this.candado = this.pieza(nivel, c, 4);
		// el candado era el del bloque del medio: la entidad esta en la esquina de la cadena, se corre un bloque
		poner(this.candado, 0, new Vector3f(1.0F, 0.0F, 0.0F), new Quaternionf());
		this.izq.dibujar();
		this.der.dibujar();
	}

	/** Empieza (quita los bloques de la cadena y pone los trozos que se van a caer). */
	static CorteCadena empezar(ServerLevel nivel) {
		int[] c = PlanoEstacion.CADENA;
		CorteCadena corte = new CorteCadena(nivel);
		for (int k = 0; k < 3; k++) {
			nivel.setBlock(new BlockPos(c[0] + k, c[1], c[2]), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		nivel.playSound(null, c[0] + 1.5, c[1] + 0.5, c[2] + 0.5, Sonidos.ESTACION_CORTE, SoundSource.BLOCKS, 2.5F, 1.0F);
		nivel.sendParticles(ParticleTypes.CRIT, c[0] + 1.5, c[1] + 0.5, c[2] + 0.2, 10, 0.1, 0.1, 0.05, 0.25);
		return corte;
	}

	static BlockState estado(int variante) {
		return BloquesEstacion.CADENA.defaultBlockState().setValue(EnPared.FACING, Direction.SOUTH).setValue(Variante.VARIANTE, variante);
	}

	/** Una pieza en la esquina de la cadena (todas en el mismo sitio: lo que las mueve es su transformacion). */
	private Display.BlockDisplay pieza(ServerLevel nivel, int[] c, int variante) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, nivel) {
			@Override
			public boolean shouldBeSaved() {
				return false;
			}
		};
		d.setBlockState(estado(variante));
		d.setPos(c[0], c[1], c[2]);
		d.setViewRange(1.5F);
		nivel.addFreshEntity(d);
		this.piezas.add(d);
		return d;
	}

	private static void poner(Display.BlockDisplay d, int ticks, Vector3f t, Quaternionf giro) {
		d.setTransformationInterpolationDelay(0);
		d.setTransformationInterpolationDuration(ticks);
		d.setTransformation(new Transformation(t, giro, null, null));
	}

	/** El candado: salta un poco, cae dando media vuelta hacia fuera, bota y se queda tumbado. */
	private void candado(ServerLevel nivel, int[] c) {
		if (!this.enSuelo || Math.abs(this.velCandado) > 0.004F || this.caidaCandado > -CAE) {
			this.velCandado -= GRAVEDAD;
			this.caidaCandado += this.velCandado;
			if (this.caidaCandado <= -CAE) {
				this.caidaCandado = -CAE;
				if (!this.enSuelo) {
					nivel.sendParticles(ParticleTypes.POOF, c[0] + 1.5, c[1] - 0.95, c[2] + 0.4, 4, 0.15, 0.02, 0.1, 0.01);
				}
				this.enSuelo = true;
				this.velCandado = Math.abs(this.velCandado) > 0.05F ? -this.velCandado * 0.3F : 0.0F;
			}
		}
		// gira mientras cae; en el suelo se asienta tumbado (90 grados)
		float objetivo = this.enSuelo ? 90.0F : 115.0F * Mth.clamp(-this.caidaCandado / CAE, 0.0F, 1.0F);
		this.giroCandado += (objetivo - this.giroCandado) * (this.enSuelo ? 0.45F : 0.6F);
		float avanza = AVANZA * Mth.clamp(-this.caidaCandado / CAE, 0.0F, 1.0F);
		Quaternionf q = new Quaternionf().rotationX((float) Math.toRadians(this.giroCandado));
		Vector3f t = new Vector3f(CENTRO_CANDADO).sub(q.transform(new Vector3f(CENTRO_CANDADO)))
			.add(1.0F, this.caidaCandado, avanza);
		poner(this.candado, 1, t, q);
	}

	/** Un tick; true cuando ha acabado (los trozos ya no estan y queda el bloque de la cadena cortada). */
	boolean tick(ServerLevel nivel) {
		this.edad++;
		int[] c = PlanoEstacion.CADENA;
		// el rozamiento del aire y de los eslabones; al final, mas, para que se quede quieta
		float rozamiento = this.edad < 50 ? 0.996F : 0.975F;
		for (Mitad m : new Mitad[] {this.izq, this.der}) {
			m.mover(rozamiento);
			if (this.edad > FIN - 15) {
				m.asentar((this.edad - (FIN - 15)) / 15.0F);
			}
			m.dibujar();
		}
		this.candado(nivel, c);
		if (this.edad >= FIN) {
			this.quitar();
			BlockPos medio = new BlockPos(c[0] + 1, c[1], c[2]);
			if (nivel.getBlockState(medio).isAir()) {
				nivel.setBlock(medio, estado(5), Block.UPDATE_CLIENTS);
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
