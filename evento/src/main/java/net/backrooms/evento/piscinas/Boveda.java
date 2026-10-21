package net.backrooms.evento.piscinas;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * La puerta de la boveda de la salida de las Piscinas, la que se ve: una entidad de bloque
 * (BlockDisplay) con el modelo de PUERTA_BOVEDA (una hoja redonda de acero de 3x3 con su
 * volante y sus bisagras). Los bloques de la puerta (PuertaSalida) no se ven: solo chocan.
 *
 * Cerrada, la hoja tapa el hueco; al meter la tercera tarjeta gira sobre sus bisagras hacia
 * dentro del pasillo (ABRE ticks, la interpolacion de las entidades de Minecraft la hace suave en
 * cada cliente) y se queda pegada a la pared. La entidad solo existe con alguien cerca (y no se
 * guarda con el mundo): al volver, sale ya en su sitio.
 *
 * El modelo va de -16 a 32 en x e y (tres bloques) y de 0 a 16 en z (el grosor de la pared, con la
 * cara del volante en z = 16, hacia las piscinas). La entidad esta en la bisagra: el borde de la
 * puerta del lado a = 0, a ras del suelo y en la cara de la pared del lado del pasillo.
 */
public final class Boveda {
	private static final Boveda INSTANCIA = new Boveda();
	/** Lo que tarda en abrirse (ticks) y cuanto gira (grados). */
	private static final int ABRE = 70;
	private static final float GIRO = 92.0F;
	private static final double CERCA = 64.0;

	private @Nullable Hoja hoja;
	private boolean abierta;

	private Boveda() {
	}

	public static Boveda get() {
		return INSTANCIA;
	}

	/** Cada medio segundo (Tarjetas): pone o quita la hoja y la abre cuando toca. */
	public void tick(ServerLevel nivel, PlanoPiscinas.Salida s, boolean abrir) {
		if (this.hoja != null && (this.hoja.isRemoved() || this.hoja.level() != nivel)) {
			this.hoja = null;
		}
		double[] c = s.mundo(2.0, 1.0);
		boolean cerca = nivel.getNearestPlayer(c[0], PlanoPiscinas.PISO + 1.5, c[1], CERCA, false) != null;
		if (this.hoja == null) {
			double[] b = s.mundo(0.5, 1.5);
			BlockPos donde = BlockPos.containing(c[0], PlanoPiscinas.PISO + 1, c[1]);
			if (!cerca || !nivel.isPositionEntityTicking(donde)) {
				return;
			}
			Hoja h = new Hoja(nivel);
			h.setBlockState(BloquesPiscinas.PUERTA_BOVEDA.defaultBlockState());
			h.setPos(b[0], PlanoPiscinas.PISO, b[1]);
			h.setViewRange(1.5F);
			h.setWidth(8.0F);
			h.setHeight(4.0F);
			h.setTransformation(pose(s, abrir ? GIRO : 0.0F));
			nivel.addFreshEntity(h);
			this.hoja = h;
			this.abierta = abrir;
		} else if (!cerca) {
			this.hoja.discard();
			this.hoja = null;
			return;
		}
		// la luz de la sala de delante (dentro de la pared no llega)
		int[] f = s.columna(2);
		BlockPos delante = new BlockPos(f[0], PlanoPiscinas.PISO + 1, f[1]);
		this.hoja.setBrightnessOverride(new net.minecraft.util.Brightness(nivel.getBrightness(LightLayer.BLOCK, delante), nivel.getBrightness(LightLayer.SKY, delante)));
		if (abrir != this.abierta) {
			this.abierta = abrir;
			this.hoja.setTransformationInterpolationDelay(0);
			this.hoja.setTransformationInterpolationDuration(abrir ? ABRE : 20);
			this.hoja.setTransformation(pose(s, abrir ? GIRO : 0.0F));
		}
	}

	/**
	 * La hoja girada `grados` sobre la bisagra: primero el giro de la salida (el +x del modelo a lo
	 * largo de la pared, +a) y luego el de la hoja, alrededor de la bisagra (-1, -1, 0) del modelo.
	 */
	private static Transformation pose(PlanoPiscinas.Salida s, float grados) {
		int[] a = s.ejeA();
		float salida = (float) Math.atan2(-a[1], a[0]);
		Quaternionf giro = new Quaternionf().rotationY(salida).rotateY((float) Math.toRadians(grados));
		Vector3f bisagra = new Vector3f(-1.0F, -1.0F, 0.0F);
		Vector3f t = giro.transform(new Vector3f(bisagra)).negate();
		return new Transformation(t, giro, new Vector3f(1.0F, 1.0F, 1.0F), new Quaternionf());
	}

	/** Al parar el servidor no queda nada colgando. */
	public void olvidar() {
		this.hoja = null;
	}

	/** La entidad de la hoja: un BlockDisplay que no se guarda con el mundo. */
	private static final class Hoja extends Display.BlockDisplay {
		Hoja(Level nivel) {
			super(EntityType.BLOCK_DISPLAY, nivel);
		}

		@Override
		public boolean shouldBeSaved() {
			return false;
		}
	}
}
