package net.backrooms.evento.cliente.cinematica;

import com.mojang.math.Transformation;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fiesta.Anfitrion;
import net.backrooms.evento.fiesta.BloquesFiesta;
import net.backrooms.evento.fiesta.PlanoFiesta;
import net.backrooms.evento.fiesta.Torno;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * La salida de la Fiesta (la empieza SalidaFiesta al meterse en el ascensor del salon final). Como
 * la del tobogan: el mundo de verdad desde una camara que va sola, sin HUD, con franjas de cine
 * (PantallaSalidaFiesta) y sin los demas jugadores. El Anfitrion, la puerta morada que revienta y
 * las puertas del ascensor son solo de este cliente (los demas no ven nada). Guion (segundos):
 *
 *   0   - 0.9   en el ascensor; golpes lejanos: se da la vuelta hacia el salon
 *   0.9 - 2.2   la camara se acerca deprisa por el salon hacia el pasillo de la puerta morada
 *   2.2 - 4.0   golpes: la puerta tiembla cada vez mas
 *   4.0 - 4.5   la puerta revienta y cae hacia dentro, al suelo del pasillo (polvo)
 *   4.6 - 6.0   el Anfitrion entra por el hueco, despacio, y se para
 *   5.4         «¿Ya te vas? La fiesta todavia no ha terminado.»
 *   7.2 - 7.8   la camara vuelve de golpe al ascensor
 *   7.8 - 8.8   las puertas del ascensor se cierran; por la rendija, el Anfitrion viene corriendo
 *   9.0 - 10.5  el grito y los golpes contra las puertas (la cabina tiembla)
 *   10.8, 11.1  corta los cables: la cabina da un tiron y cae
 *   11.1 - 12.9 cae (todo tiembla, la luz parpadea)
 *   12.9        el golpe: un destello y negro
 *   13.1        el servidor le lleva a la Estacion; alli, negro un momento, se abre y va mareado un rato
 *
 * El sonido va en dos archivos (tools/sonidos/salida-fiesta.js): el de la Fiesta y el de despues
 * (el pitido en los oidos al llegar), que suena ya en la Estacion.
 */
public final class SalidaFiestaCliente {
	private static final Identifier BANDA = BackroomsEvento.id("cinematica.salida_fiesta");
	private static final Identifier BANDA_FIN = BackroomsEvento.id("cinematica.salida_fiesta.despues");
	private static final double OJOS = PlanoFiesta.SUELO + 1 + 1.62;
	/** El centro del ascensor (y del pasillo de la puerta morada: estan en la misma x). */
	private static final double X = PlanoFiesta.FIN_PUERTA_X + 0.5;
	/** Donde se queda la camara dentro de la cabina, y la boca de la cabina (la pared del salon). */
	private static final double Z_CABINA = PlanoFiesta.FIN_Z0 - 3.0;
	private static final double Z_BOCA = PlanoFiesta.FIN_Z0 - 0.4;
	/** Hasta donde se acerca la camara por el salon (mirando la puerta, al fondo del pasillo). */
	private static final double Z_CERCA = PlanoFiesta.FIN_Z1 - 11.0;
	private static final int PUERTA_Z = PlanoFiesta.FIN_PUERTA_Z;
	/** Los golpes en la puerta morada (el ultimo la revienta) y en las del ascensor. */
	private static final float[] GOLPES = {0.5F, 1.4F, 2.4F, 3.0F, 3.5F, 4.0F};
	private static final float[] GOLPES_ASCENSOR = {9.3F, 9.7F, 10.1F, 10.4F};
	private static final float ROMPE = 4.0F;
	private static final float CHOQUE = 12.9F;
	/** Al llegar a la Estacion: negro un momento, se abre, y mareado un rato. */
	private static final float NEGRO_LLEGADA = 0.6F;
	private static final float ABRE = 1.6F;
	private static final float MAREO = 6.0F;

	private static int ticks = -1;
	private static float llegada = -1.0F;
	private static @Nullable Marker camara;
	private static @Nullable Object nivel;
	private static boolean guiAntes;
	private static float giro0;
	private static float cabeceo0;
	private static @Nullable SoundInstance banda;
	private static @Nullable Anfitrion anfitrion;
	private static final Display.BlockDisplay[] puerta = new Display.BlockDisplay[2];
	private static final Display.BlockDisplay[] hojas = new Display.BlockDisplay[2];
	private static boolean polvo;
	/** Ids de las entidades de la escena (negativos: no chocan con las del servidor). */
	private static int siguienteId = -7000;

	private SalidaFiestaCliente() {
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(SalidaFiestaCliente::tick);
	}

	public static boolean activa() {
		return ticks >= 0;
	}

	public static float segundos(float parcial) {
		return ticks < 0 ? -1.0F : (ticks + parcial) / 20.0F;
	}

	public static void empezar() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		if (activa()) {
			terminar();
		}
		ticks = 0;
		llegada = -1.0F;
		polvo = false;
		nivel = mc.level;
		giro0 = mc.player.getYRot();
		cabeceo0 = mc.player.getXRot();
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, 0.0F, true);
		mc.setCameraEntity(m);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(0.0F);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(0.0F);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		montar(mc);
		banda = sonar(mc, BANDA);
		mc.setScreen(new PantallaSalidaFiesta());
	}

	/* --------------------------------------------------------------- el decorado */

	/** Lo de la escena, solo en este cliente: la puerta que se cae, las hojas del ascensor y el Anfitrion. */
	private static void montar(Minecraft mc) {
		// la puerta de verdad se quita (solo aqui) y en su sitio va una hoja que puede temblar y caerse
		BlockState base = BloquesFiesta.PUERTA.defaultBlockState().setValue(Torno.FACING, Direction.SOUTH).setValue(Torno.COSTE, 0)
			.setValue(Torno.TIPO, 2);
		for (int k = 0; k < 2; k++) {
			BlockPos p = new BlockPos(PlanoFiesta.FIN_PUERTA_X, PlanoFiesta.SUELO + 1 + k, PUERTA_Z);
			mc.level.setBlock(p, Blocks.AIR.defaultBlockState(), 0);
			Display.BlockDisplay d = pieza(mc, base.setValue(Torno.PARTE, k), PlanoFiesta.FIN_PUERTA_X, PlanoFiesta.SUELO + 1, PUERTA_Z);
			puerta[k] = d;
		}
		// las dos hojas del ascensor, abiertas (metidas en la pared de cada lado)
		for (int k = 0; k < 2; k++) {
			hojas[k] = pieza(mc, net.backrooms.evento.bloques.Bloques.ACERO.defaultBlockState(), PlanoFiesta.FINAL_TOB_X0, PlanoFiesta.SUELO + 1,
				PlanoFiesta.FIN_Z0 - 1);
		}
		// el Anfitrion, fuera, detras de la puerta (en lo oscuro)
		Anfitrion a = net.backrooms.evento.mision.Entidades.ANFITRION.create(mc.level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
		if (a != null) {
			a.setId(-7100);
			a.noPhysics = true;
			a.snapTo(X, PlanoFiesta.SUELO + 1, PUERTA_Z + 2.5, 180.0F, 0.0F);
			a.setYHeadRot(180.0F);
			mc.level.addEntity(a);
			anfitrion = a;
		}
		decorado(0.0F);
	}

	private static Display.BlockDisplay pieza(Minecraft mc, BlockState estado, int x, int y, int z) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, mc.level);
		d.setId(siguienteId--);
		d.setBlockState(estado);
		d.setPos(x, y, z);
		d.setViewRange(4.0F);
		d.setTransformationInterpolationDuration(2);
		mc.level.addEntity(d);
		return d;
	}

	/** La puerta, las hojas del ascensor y el Anfitrion en el segundo t. */
	private static void decorado(float t) {
		// la puerta morada: delgada (una hoja de 1/4), tiembla con cada golpe y al final cae hacia el pasillo
		float temblor = 0.0F;
		for (int i = 0; i < GOLPES.length - 1; i++) {
			float d = t - GOLPES[i];
			if (d >= 0 && d < 0.35F) {
				temblor = Math.max(temblor, (1.0F - d / 0.35F) * (0.4F + i * 0.15F));
			}
		}
		float caida = Mth.clamp((t - ROMPE) / 0.5F, 0.0F, 1.0F);
		// cae acelerando (como un peso) y rebota un pelo al llegar
		float angulo = -90.0F * caida * caida + (caida >= 1.0F ? (float) (Math.sin((t - ROMPE - 0.5F) * 30.0) * Math.exp(-(t - ROMPE - 0.5F) * 12.0) * 3.0) : 0.0F);
		if (t < ROMPE) {
			angulo = -temblor * 4.0F * (float) Math.sin(t * 90.0);
		}
		Quaternionf giro = new Quaternionf().rotationX((float) Math.toRadians(angulo));
		Vector3f pivote = new Vector3f(0.0F, 0.0F, 0.375F);
		float empuje = t < ROMPE ? (float) Math.sin(t * 70.0) * 0.04F * temblor : 0.0F;
		for (int k = 0; k < 2; k++) {
			if (puerta[k] == null) {
				continue;
			}
			Vector3f t0 = giro.transform(new Vector3f(0.0F, k, 0.0F)).add(pivote).add(empuje, 0.0F, 0.0F);
			puerta[k].setTransformation(new Transformation(t0, new Quaternionf(giro), new Vector3f(1.0F, 1.0F, 0.25F), new Quaternionf()));
		}
		// las hojas del ascensor: abiertas hasta 7.8; se cierran en 1 s; tiemblan con los golpes y la caida
		float cierre = suave((t - 7.8F) / 1.0F);
		float golpeA = 0.0F;
		for (float g : GOLPES_ASCENSOR) {
			float d = t - g;
			if (d >= 0 && d < 0.3F) {
				golpeA = Math.max(golpeA, 1.0F - d / 0.3F);
			}
		}
		float ancho = 1.5F;
		for (int k = 0; k < 2; k++) {
			if (hojas[k] == null) {
				continue;
			}
			// abierta: metida en la pared de su lado; cerrada: tocando a la otra en el medio
			float x = k == 0 ? Mth.lerp(cierre, -ancho, 0.0F) : Mth.lerp(cierre, 3.0F, ancho);
			float bollo = golpeA * 0.06F * (float) Math.sin(t * 80.0 + k);
			hojas[k].setTransformation(new Transformation(new Vector3f(x, 0.0F, 0.8F + bollo), new Quaternionf(),
				new Vector3f(ancho, 3.0F, 0.12F), new Quaternionf()));
		}
		// el Anfitrion: fuera hasta que cae la puerta; entra despacio, se para y habla; luego viene corriendo
		Anfitrion a = anfitrion;
		if (a != null) {
			double z;
			float velocidad;
			if (t < 4.6F) {
				z = PUERTA_Z + 2.5;
				velocidad = 0.0F;
			} else if (t < 6.0F) {
				z = Mth.lerp(suave((t - 4.6F) / 1.4F), PUERTA_Z + 2.5, PUERTA_Z - 2.0);
				velocidad = 0.35F;
			} else if (t < 7.6F) {
				z = PUERTA_Z - 2.0;
				velocidad = 0.0F;
			} else {
				z = PUERTA_Z - 2.0 - (t - 7.6F) * 9.0;
				velocidad = 1.0F;
			}
			a.xo = a.getX();
			a.yo = a.getY();
			a.zo = a.getZ();
			a.setPos(X, PlanoFiesta.SUELO + 1, z);
			a.setYRot(180.0F);
			a.yRotO = 180.0F;
			a.setYHeadRot(180.0F);
			a.yHeadRotO = 180.0F;
			a.setYBodyRot(180.0F);
			a.yBodyRotO = 180.0F;
			// habla: mueve la cabeza; corriendo, la pose de caza
			float cabeza = t > 5.4F && t < 7.4F ? (float) Math.sin((t - 5.4F) * 7.0) * 8.0F : 0.0F;
			a.setXRot(cabeza);
			a.xRotO = cabeza;
			a.cazandoCliente(t >= 7.6F);
			a.walkAnimation.update(velocidad, 0.4F, 1.0F);
		}
	}

	/* ----------------------------------------------------------------- la camara */

	private static SoundInstance sonar(Minecraft mc, Identifier id) {
		SoundInstance s = new SimpleSoundInstance(id, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
			SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
		mc.getSoundManager().play(s);
		return s;
	}

	private static void tick(Minecraft mc) {
		if (!activa()) {
			return;
		}
		if (mc.level == null || mc.player == null) {
			olvidar();
			return;
		}
		ticks++;
		float t = ticks / 20.0F;
		if (llegada < 0 && mc.level != nivel) {
			// ya en la Estacion: la camara vuelve al jugador; lo de la escena era del mundo de antes
			llegada = t;
			soltarCamara(mc);
			anfitrion = null;
			java.util.Arrays.fill(puerta, null);
			java.util.Arrays.fill(hojas, null);
			banda = sonar(mc, BANDA_FIN);
		}
		if (llegada < 0) {
			decorado(t);
			if (camara != null) {
				colocar(camara, t, false);
			}
			if (t >= ROMPE + 0.45F && !polvo) {
				polvo = true;
				nubeDePolvo(mc);
			}
		} else {
			// mareado: la vista se va un poco hacia los lados, cada vez menos
			float m = t - llegada;
			if (m < MAREO) {
				float k = 1.0F - m / MAREO;
				mc.player.setYRot(mc.player.getYRot() + (float) Math.sin(m * 2.3) * 0.9F * k);
				mc.player.setXRot(Mth.clamp(mc.player.getXRot() + (float) Math.sin(m * 1.7 + 1.0) * 0.6F * k, -90.0F, 90.0F));
			}
		}
		if (llegada >= 0 && t > llegada + Math.max(NEGRO_LLEGADA + ABRE, MAREO) || t > 30.0F) {
			terminar();
		}
	}

	private static void nubeDePolvo(Minecraft mc) {
		RandomSource r = RandomSource.create();
		BlockParticleOption trozos = new BlockParticleOption(ParticleTypes.BLOCK, BloquesFiesta.PUERTA.defaultBlockState());
		for (int i = 0; i < 40; i++) {
			double x = X - 1.2 + r.nextDouble() * 2.4;
			double z = PUERTA_Z - 2.0 + r.nextDouble() * 2.5;
			double y = PlanoFiesta.SUELO + 1.05 + r.nextDouble() * 0.6;
			mc.level.addParticle(i % 3 == 0 ? trozos : ParticleTypes.POOF, x, y, z, (r.nextDouble() - 0.5) * 0.08, 0.03 + r.nextDouble() * 0.05,
				(r.nextDouble() - 0.5) * 0.08);
		}
	}

	private static void colocar(Marker m, float t, boolean tambienAntes) {
		double[] antes = pose(Math.max(0.0F, t - 0.05F));
		double[] ahora = pose(t);
		if (tambienAntes) {
			antes = ahora;
		}
		m.setPos(ahora[0], ahora[1], ahora[2]);
		m.xo = antes[0];
		m.yo = antes[1];
		m.zo = antes[2];
		m.xOld = antes[0];
		m.yOld = antes[1];
		m.zOld = antes[2];
		m.setYRot((float) ahora[3]);
		m.yRotO = (float) antes[3];
		m.setXRot((float) ahora[4]);
		m.xRotO = (float) antes[4];
	}

	/** {x, y, z, giro, cabeceo} en el segundo t. Giro 0 = mirando al sur (al salon, a la puerta morada). */
	private static double[] pose(float t) {
		double x = X;
		double y = OJOS;
		double z;
		double giro;
		double cabeceo;
		double sacude = 0.0;
		float giroIni = giro0 + Mth.wrapDegrees(0.0F - giro0);
		if (t < 0.9F) {
			// se da la vuelta hacia el salon (lo que oye)
			double k = suave(t / 0.9F);
			z = Mth.lerp(k, Z_CABINA + 1.0, Z_BOCA);
			giro = Mth.lerp(k, giro0, giroIni);
			cabeceo = Mth.lerp(k, cabeceo0, 2.0);
		} else if (t < 2.2F) {
			// se acerca deprisa por el salon (como un zoom)
			double k = suave((t - 0.9F) / 1.3F);
			z = Mth.lerp(k, Z_BOCA, Z_CERCA);
			giro = giroIni;
			cabeceo = 2.0 - k * 2.0;
			sacude = 0.15;
		} else if (t < 7.2F) {
			// mira la puerta; tras la frase, se acerca un poco mas
			z = Z_CERCA + suave((t - 4.6F) / 2.6F) * 3.0;
			giro = giroIni;
			cabeceo = 0.0;
			sacude = t >= ROMPE && t < ROMPE + 0.6F ? 0.8 : 0.25;
		} else if (t < 7.8F) {
			// vuelta de golpe al ascensor
			double k = suave((t - 7.2F) / 0.6F);
			z = Mth.lerp(k, Z_CERCA + 3.0, Z_CABINA);
			giro = giroIni;
			cabeceo = 0.0;
			sacude = 0.4;
		} else {
			// dentro de la cabina, mirando a las puertas
			z = Z_CABINA;
			giro = giroIni;
			cabeceo = 3.0;
			for (float g : GOLPES_ASCENSOR) {
				float d = t - g;
				if (d >= 0 && d < 0.35F) {
					sacude = Math.max(sacude, 2.5 * (1.0 - d / 0.35F));
				}
			}
			if (t >= 10.8F && t < 11.3F) {
				// el tiron de los cables
				y -= 0.25 * suave((t - 10.8F) / 0.15F);
				sacude = Math.max(sacude, 4.0);
			} else if (t >= 11.3F) {
				// cayendo: cada vez mas
				y -= 0.25;
				sacude = Math.max(sacude, 1.5 + 2.5 * Mth.clamp((t - 11.3F) / 1.6F, 0.0F, 1.0F));
				cabeceo += 6.0 * Mth.clamp((t - 11.3F) / 1.6F, 0.0F, 1.0F);
			}
			if (t >= CHOQUE) {
				y -= 0.6;
				sacude = 6.0 * Math.exp(-(t - CHOQUE) * 4.0);
				cabeceo += 20.0;
			}
		}
		double r1 = Math.sin(t * 37.0) * 0.6 + Math.sin(t * 59.0) * 0.4;
		double r2 = Math.sin(t * 43.0 + 1.3) * 0.6 + Math.sin(t * 67.0) * 0.4;
		return new double[] {x + r1 * 0.012 * sacude, y + r2 * 0.012 * sacude, z, giro + r2 * 0.5 * sacude, cabeceo + r1 * 0.5 * sacude};
	}

	private static float suave(float k) {
		k = Mth.clamp(k, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private static double suave(double k) {
		return suave((float) k);
	}

	/* ------------------------------------------------------------------ pantalla */

	/** 0..1 de negro encima de todo (tras el golpe, mientras viaja y al abrirse en la Estacion). */
	public static float negro(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		if (llegada >= 0) {
			return 1.0F - Mth.clamp((t - llegada - NEGRO_LLEGADA) / ABRE, 0.0F, 1.0F);
		}
		return t >= CHOQUE + 0.08F ? 1.0F : 0.0F;
	}

	/**
	 * Cuanto se cierra el angulo (1 = normal): mientras mira la puerta morada, como un teleobjetivo
	 * (desde la otra punta del salon se ve la puerta de cerca); al volver al ascensor, normal.
	 */
	public static float zoom(float parcial) {
		if (!activa() || llegada >= 0) {
			return 1.0F;
		}
		float t = segundos(parcial);
		float cierra = suave((t - 0.9F) / 1.3F);
		float abre = suave((t - 7.2F) / 0.6F);
		return Mth.lerp(cierra * (1.0F - abre), 1.0F, 0.3F);
	}

	/** 0..1 del destello blanco del golpe. */
	public static float destello(float parcial) {
		if (!activa() || llegada >= 0) {
			return 0.0F;
		}
		float t = segundos(parcial);
		return t >= CHOQUE && t < CHOQUE + 0.08F ? 1.0F : 0.0F;
	}

	/** 0..1 de las franjas de cine. */
	public static float franjas(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		float entra = Mth.clamp(t / 0.6F, 0.0F, 1.0F);
		if (llegada >= 0) {
			return entra * (1.0F - Mth.clamp((t - llegada - NEGRO_LLEGADA) / ABRE, 0.0F, 1.0F));
		}
		return entra;
	}

	private static void soltarCamara(Minecraft mc) {
		if (mc.player != null) {
			mc.setCameraEntity(mc.player);
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(mc.player.getEyeHeight());
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(mc.player.getEyeHeight());
		}
		camara = null;
	}

	private static void quitarDecorado(Minecraft mc) {
		if (mc.level != null && mc.level == nivel) {
			for (Entity e : new Entity[] {anfitrion, puerta[0], puerta[1], hojas[0], hojas[1]}) {
				if (e != null) {
					mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
				}
			}
		}
		anfitrion = null;
		java.util.Arrays.fill(puerta, null);
		java.util.Arrays.fill(hojas, null);
	}

	static void terminar() {
		Minecraft mc = Minecraft.getInstance();
		if (camara != null) {
			soltarCamara(mc);
		}
		quitarDecorado(mc);
		ticks = -1;
		llegada = -1.0F;
		nivel = null;
		mc.options.hideGui = guiAntes;
		if (banda != null) {
			mc.getSoundManager().stop(banda);
			banda = null;
		}
		if (mc.screen instanceof PantallaSalidaFiesta) {
			mc.setScreen(null);
		}
	}

	/** Al desconectarse no queda nada a medias. */
	public static void olvidar() {
		if (activa()) {
			ticks = -1;
			llegada = -1.0F;
			camara = null;
			nivel = null;
			banda = null;
			anfitrion = null;
			java.util.Arrays.fill(puerta, null);
			java.util.Arrays.fill(hojas, null);
			Minecraft.getInstance().options.hideGui = guiAntes;
		}
	}
}
