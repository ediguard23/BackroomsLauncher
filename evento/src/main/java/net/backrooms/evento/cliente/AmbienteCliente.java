package net.backrooms.evento.cliente;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.red.EstadoAmbiente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * La luz del Nivel 0 en el cliente: apagones y alarmas que manda el servidor.
 *
 * Al irse la luz los tubos parpadean un instante y se apagan con un golpe de
 * rele; el zumbido de fondo se corta (SonidoAmbienteMixin) y la pantalla se
 * queda en negro salvo lo que alumbren las linternas (EfectosMundo). Al
 * volver, otra rafaga de parpadeos y el zumbido sube. Con la alarma los
 * tubos se tinen de rojo y suena la sirena en bucle.
 *
 * El color de los tubos va en el modelo (tintindex) y Minecraft lo hornea en
 * la malla de cada seccion: al cambiar se rehacen solo las secciones del
 * techo que hay a la vista.
 */
public final class AmbienteCliente {
	private static boolean apagon;
	private static boolean alarma;
	private static long cambioApagon = Long.MIN_VALUE / 2;
	private static long cambioAlarma = Long.MIN_VALUE / 2;
	private static boolean huboApagon;
	private static int colorTubos = 0xFFFFFF;
	private static Sirena sirena;

	private AmbienteCliente() {
	}

	private static long ahora() {
		return net.minecraft.util.Util.getMillis();
	}

	public static void recibir(EstadoAmbiente e) {
		Minecraft mc = Minecraft.getInstance();
		if (e.apagon() != apagon) {
			apagon = e.apagon();
			cambioApagon = ahora();
			huboApagon = true;
			mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(apagon ? Sonidos.APAGON : Sonidos.LUZ_VUELVE, 1.0F, 0.9F));
		}
		if (e.alarma() != alarma) {
			alarma = e.alarma();
			cambioAlarma = ahora();
			if (alarma && (sirena == null || sirena.isStopped())) {
				sirena = new Sirena();
				mc.getSoundManager().play(sirena);
			}
		}
	}

	public static void olvidar() {
		apagon = false;
		alarma = false;
		huboApagon = false;
		cambioApagon = Long.MIN_VALUE / 2;
		cambioAlarma = Long.MIN_VALUE / 2;
		colorTubos = 0xFFFFFF;
	}

	public static boolean apagon() {
		return apagon;
	}

	public static boolean alarma() {
		return alarma;
	}

	/**
	 * Cuanta oscuridad hay: 0 luz normal, 1 apagon total. Con los parpadeos
	 * del principio y del final.
	 */
	public static float oscuridad() {
		if (!huboApagon) {
			return 0.0F;
		}
		float t = (ahora() - cambioApagon) / 1000.0F;
		if (apagon) {
			// se va: chispazo, vuelve un instante, se va, un ultimo intento y negro
			if (t < 0.07F) {
				return 1.0F;
			}
			if (t < 0.16F) {
				return 0.15F;
			}
			if (t < 0.31F) {
				return 1.0F;
			}
			if (t < 0.36F) {
				return 0.55F;
			}
			return 1.0F;
		}
		// vuelve: el rele a los 0,5 s y los tubos arrancan a tirones
		if (t < 0.5F) {
			return 1.0F;
		}
		if (t < 0.56F) {
			return 0.1F;
		}
		if (t < 0.78F) {
			return 1.0F;
		}
		if (t < 0.84F) {
			return 0.25F;
		}
		if (t < 0.98F) {
			return 0.9F;
		}
		return Mth.clamp(1.0F - (t - 0.98F) / 0.25F, 0.0F, 1.0F) * 0.4F;
	}

	/** 0..1: cuanto rojo de alarma hay (entra y sale en medio segundo). */
	public static float rojo() {
		float t = (ahora() - cambioAlarma) / 500.0F;
		return alarma ? Mth.clamp(t, 0.0F, 1.0F) : Mth.clamp(1.0F - t, 0.0F, 1.0F);
	}

	/** Volumen del zumbido de los tubos (0 en el apagon). */
	public static float zumbido() {
		if (!huboApagon) {
			return 1.0F;
		}
		float t = (ahora() - cambioApagon) / 1000.0F;
		if (apagon) {
			return t < 0.36F ? 0.6F : 0.0F;
		}
		return Mth.clamp((t - 0.6F) / 2.5F, 0.0F, 1.0F);
	}

	/** Color de los tubos encendidos (lo usa el BlockColor de los fluorescentes). */
	public static int colorTubos() {
		return colorTubos;
	}

	/** Cada tick: tine los tubos cuando toca y rehace el techo a la vista. */
	public static void tick() {
		float t = (ahora() - cambioApagon) / 1000.0F;
		int quiero;
		if (apagon && t > 0.36F) {
			quiero = 0x2C2A26;
		} else if (!apagon && huboApagon && t < 0.98F) {
			quiero = 0x2C2A26;
		} else if (alarma) {
			quiero = 0xFF2A1C;
		} else {
			quiero = 0xFFFFFF;
		}
		if (quiero != colorTubos) {
			colorTubos = quiero;
			rehacerTecho();
		}
	}

	private static void rehacerTecho() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		int r = mc.options.getEffectiveRenderDistance() + 1;
		int cx = SectionPos.blockToSectionCoord(mc.player.getBlockX());
		int cz = SectionPos.blockToSectionCoord(mc.player.getBlockZ());
		int sy = SectionPos.blockToSectionCoord(net.backrooms.evento.mundo.GeneradorNivel0.TECHO_Y);
		mc.levelRenderer.setSectionRangeDirty(cx - r, sy, cz - r, cx + r, sy, cz + r);
	}

	/** La sirena de la alarma, en bucle mientras dure; se apaga con un fundido. */
	private static final class Sirena extends AbstractTickableSoundInstance {
		private int fuera;

		Sirena() {
			super(Sonidos.ALARMA, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.volume = 0.7F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			if (!alarma || Minecraft.getInstance().level == null) {
				this.fuera++;
				this.volume = Math.max(0.0F, 0.7F - this.fuera * 0.05F);
				if (this.volume <= 0.0F) {
					this.stop();
				}
			}
		}
	}
}
