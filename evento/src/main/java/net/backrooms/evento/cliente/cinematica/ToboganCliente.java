package net.backrooms.evento.cliente.cinematica;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.piscinas.PlanoPiscinas;
import net.backrooms.evento.red.CinematicaTobogan;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import org.jspecify.annotations.Nullable;

/**
 * La cinematica del tobogan, de las Piscinas a la Fiesta (la empieza TransicionFiesta al entrar en
 * el pasillo de la boveda). Se ve el mundo de verdad desde una camara que va sola (una entidad
 * Marker que no esta en el mundo), sin HUD, con franjas negras arriba y abajo (PantallaTobogan) y
 * sin los jugadores (OcultarJugadoresMixin). Guion (segundos):
 *
 *   0   - 1.4   andas por el pasillo hacia el tobogan (hasta el medio); las luces parpadean (0.8)
 *   1.2         salta la alarma: todo en rojo, la sirena y la megafonia (fallo de contencion,
 *               inundacion total del nivel inminente)
 *   1.8 - 3.2   te das la vuelta y miras a la puerta de la boveda
 *   3.0 - 8.9   el agua entra por la puerta y sube (la superficie de la inundacion, solo en tu cliente)
 *   6.6 - 7.2   te vuelves hacia el tobogan
 *   7.2 - 8.2   corres por el pasillo
 *   8.2 - 8.9   te agachas y te metes por la boca
 *   8.9 - 10.2  caes por el pozo (y se va a negro desde 9.6)
 *   11          el servidor te lleva a la Fiesta; en negro hasta que carga y se abre poco a poco
 *
 * El sonido va en dos archivos (tools/sonidos/tobogan.js): el de las Piscinas, y el del final
 * (el tobogan y la caida), que suena al llegar (Minecraft corta todo al cambiar de mundo).
 */
public final class ToboganCliente {
	private static final Identifier BANDA = BackroomsEvento.id("cinematica.tobogan");
	private static final Identifier BANDA_FIN = BackroomsEvento.id("cinematica.tobogan.despues");
	private static final double OJOS = PlanoPiscinas.PISO + 1.62;
	/** Tras llegar a la Fiesta: negro un momento y se abre. */
	private static final float NEGRO_LLEGADA = 0.5F;
	private static final float ABRE = 0.9F;

	private static PlanoPiscinas.Salida salida;
	private static int ticks = -1;
	private static float llegada = -1.0F;
	private static @Nullable Marker camara;
	private static @Nullable Object nivel;
	private static boolean guiAntes;
	private static double a0;
	private static double d0;
	private static float giro0;
	private static float cabeceo0;
	private static @Nullable SoundInstance banda;

	private ToboganCliente() {
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(ToboganCliente::tick);
	}

	public static boolean activa() {
		return ticks >= 0;
	}

	/** Si la camara esta en el pasillo (aun en las Piscinas). */
	private static boolean enPiscinas() {
		return activa() && llegada < 0;
	}

	/** Segundos desde que empezo (con la fraccion del tick que se esta dibujando). */
	public static float segundos(float parcial) {
		return ticks < 0 ? -1.0F : (ticks + parcial) / 20.0F;
	}

	public static void empezar(CinematicaTobogan p) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		if (activa()) {
			terminar();
		}
		PlanoPiscinas.Salida s = new PlanoPiscinas.Salida(p.x(), p.z(), p.lado());
		salida = s;
		double[] l = s.local(mc.player.getX(), mc.player.getZ());
		a0 = Mth.clamp(l[0], 1.3, 2.7);
		d0 = Mth.clamp(l[1], 1.8, 4.0);
		giro0 = mc.player.getYRot();
		cabeceo0 = mc.player.getXRot();
		ticks = 0;
		llegada = -1.0F;
		nivel = mc.level;
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, 0.0F, true);
		mc.setCameraEntity(m);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(0.0F);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(0.0F);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		banda = sonar(mc, BANDA);
		mc.setScreen(new PantallaTobogan());
	}

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
			// ya en la Fiesta: la camara vuelve al jugador (la otra era del mundo de antes)
			llegada = t;
			soltarCamara(mc);
			banda = sonar(mc, BANDA_FIN);
		}
		if (llegada < 0 && camara != null) {
			colocar(camara, t, false);
		}
		if (llegada >= 0 && t > llegada + NEGRO_LLEGADA + ABRE || t > 25.0F) {
			terminar();
		}
	}

	/** La camara en el segundo t (y donde estaba en el tick anterior, para que Minecraft interpole). */
	private static void colocar(Marker m, float t, boolean tambienAntes) {
		double[] antes = pose(Math.max(0.0F, t - 0.05F));
		double[] ahora = pose(t);
		if (tambienAntes) {
			antes = ahora;
		}
		double[] wa = salida.mundo(antes[0], antes[1]);
		double[] wb = salida.mundo(ahora[0], ahora[1]);
		m.setPos(wb[0], ahora[2], wb[1]);
		m.xo = wa[0];
		m.yo = antes[2];
		m.zo = wa[1];
		m.xOld = wa[0];
		m.yOld = antes[2];
		m.zOld = wa[1];
		m.setYRot((float) ahora[3]);
		m.yRotO = (float) antes[3];
		m.setXRot((float) ahora[4]);
		m.xRotO = (float) antes[4];
	}

	/** {a, d, y, giro, cabeceo} en el segundo t, en las coordenadas de la salida. */
	private static double[] pose(float t) {
		float dentro = salida.lado() * 90.0F + 180.0F;
		float giroIni = giro0 + Mth.wrapDegrees(dentro - giro0);
		float detras = giroIni + 180.0F;
		double a;
		double d;
		double y = OJOS;
		double giro;
		double cabeceo;
		double bamboleo = 0.0;
		if (t < 1.4F) {
			// andando hacia el tobogan, mirando al frente
			double k = suave(t / 1.4F);
			a = Mth.lerp(k, a0, 2.0);
			d = Mth.lerp(k, d0, Math.max(d0, 6.2));
			giro = Mth.lerp(suave(Math.min(1.0F, t / 0.8F)), giro0, giroIni);
			cabeceo = Mth.lerp(suave(Math.min(1.0F, t / 0.8F)), cabeceo0, 0.0F);
			bamboleo = Math.sin(t * Math.PI * 4.0) * 0.035 * (1.0 - k);
		} else {
			a = 2.0;
			d = Math.max(d0, 6.2);
			giro = giroIni;
			cabeceo = 0.0;
			if (t < 1.8F) {
				// quieto: la alarma
				giro += Math.sin((t - 1.4F) * 30.0F) * 0.6;
			} else if (t < 3.2F) {
				// se da la vuelta (despacio al principio)
				double k = suave((t - 1.8F) / 1.4F);
				giro = Mth.lerp(k, giroIni, detras);
				cabeceo = 6.0 * Math.sin(k * Math.PI);
			} else if (t < 6.6F) {
				// mira la puerta: el agua entra; tiembla un poco
				giro = detras + Math.sin(t * 7.3) * 0.8;
				cabeceo = 9.0 + Math.sin(t * 5.1) * 0.6;
				d -= suave((t - 3.2F) / 3.4F) * 0.4;
			} else if (t < 7.2F) {
				double k = suave((t - 6.6F) / 0.6F);
				giro = Mth.lerp(k, detras, giroIni + 360.0F);
				cabeceo = Mth.lerp(k, 9.0F, 4.0F);
				d -= 0.4;
			} else if (t < 8.2F) {
				// corre
				double k = (t - 7.2F) / 1.0F;
				d = Mth.lerp(k * k * (3 - 2 * k), Math.max(d0, 6.2) - 0.4, 8.3);
				giro = giroIni + Math.sin(t * 9.0) * 1.2;
				cabeceo = 4.0 + 6.0 * k;
				y = OJOS - 0.5 * suave((float) Math.max(0.0, (k - 0.6) / 0.4));
				bamboleo = Math.abs(Math.sin(t * Math.PI * 5.5)) * 0.09 - 0.045;
			} else if (t < 8.9F) {
				// se agacha y entra por la boca
				double k = suave((t - 8.2F) / 0.7F);
				d = Mth.lerp(k, 8.3, 9.55);
				y = Mth.lerp(k, OJOS - 0.5, PlanoPiscinas.PISO + 0.8);
				giro = giroIni;
				cabeceo = Mth.lerp(k, 10.0, 40.0);
			} else {
				// cae por el pozo, cada vez mas deprisa
				double k = Math.min(1.0, (t - 8.9F) / 1.3F);
				d = Mth.lerp(Math.min(1.0, k * 3.0), 9.55, 10.0);
				y = Mth.lerp(k * k, PlanoPiscinas.PISO + 0.8, PlanoPiscinas.FONDO_POZO + 1.3);
				giro = giroIni + k * 25.0;
				cabeceo = Mth.lerp(Math.min(1.0, k * 2.0), 40.0, 78.0);
			}
		}
		return new double[] {a, d, y + bamboleo, giro, cabeceo};
	}

	private static float suave(float k) {
		k = Mth.clamp(k, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private static double suave(double k) {
		return suave((float) k);
	}

	/** 0..1 de negro encima de todo (al caer, mientras viaja y al abrirse en la Fiesta). */
	public static float negro(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		if (llegada >= 0) {
			return 1.0F - Mth.clamp((t - llegada - NEGRO_LLEGADA) / ABRE, 0.0F, 1.0F);
		}
		return Mth.clamp((t - 9.6F) / 0.6F, 0.0F, 1.0F);
	}

	/** 0..1 de las franjas de cine (entran al principio y se van al abrirse en la Fiesta). */
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

	/** El rojo de la alarma (como AmbienteCliente#rojo: late con la sirena, una vez por segundo). */
	public static float rojo() {
		if (!enPiscinas()) {
			return 0.0F;
		}
		float t = segundos(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		if (t < 1.2F) {
			return 0.0F;
		}
		float c = (t - 1.2F) % 1.0F;
		float latido = (float) Math.pow(c, 0.7) * (c < 0.85F ? 1.0F : 1.0F - (c - 0.85F) / 0.15F);
		return Mth.clamp((t - 1.2F) / 0.4F, 0.0F, 1.0F) * (0.6F + 0.4F * latido);
	}

	/** El parpadeo de las luces antes de la alarma (oscuridad 0..1). */
	public static float oscuridad() {
		if (!enPiscinas()) {
			return 0.0F;
		}
		float t = segundos(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		return t > 0.8F && t < 0.9F || t > 1.0F && t < 1.12F ? 0.85F : 0.0F;
	}

	/** La altura del agua que se ve, o -1 si no hay (sube por la puerta y se deja al meterse en la boca). */
	public static float agua() {
		if (!enPiscinas()) {
			return -1.0F;
		}
		float t = segundos(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		if (t < 3.0F || t > 8.9F) {
			return -1.0F;
		}
		float k = (t - 3.0F) / 5.9F;
		return (float) (PlanoPiscinas.PISO - 0.3 + 1.0 * Math.sqrt(k));
	}

	private static void soltarCamara(Minecraft mc) {
		if (mc.player != null) {
			mc.setCameraEntity(mc.player);
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(mc.player.getEyeHeight());
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(mc.player.getEyeHeight());
		}
		camara = null;
	}

	static void terminar() {
		Minecraft mc = Minecraft.getInstance();
		if (camara != null) {
			soltarCamara(mc);
		}
		ticks = -1;
		llegada = -1.0F;
		salida = null;
		nivel = null;
		mc.options.hideGui = guiAntes;
		if (banda != null) {
			mc.getSoundManager().stop(banda);
			banda = null;
		}
		if (mc.screen instanceof PantallaTobogan) {
			mc.setScreen(null);
		}
	}

	/** Al desconectarse no queda nada a medias. */
	public static void olvidar() {
		if (activa()) {
			ticks = -1;
			llegada = -1.0F;
			camara = null;
			salida = null;
			nivel = null;
			banda = null;
			Minecraft.getInstance().options.hideGui = guiAntes;
		}
	}
}
