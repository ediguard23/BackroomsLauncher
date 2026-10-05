package net.backrooms.evento.cliente.efectos;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.entidad.Bacteria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * El miedo: cuando una Bacteria te esta cazando cerca, el corazon se dispara
 * (latidos cada vez mas seguidos) y la imagen late con el (ver efectos.fsh):
 * los bordes se cierran, los colores se separan y la vista se encoge un poco
 * con cada latido. Con una Bacteria cerca que aun no te ha visto, solo un
 * poco.
 */
public final class Miedo {
	private static final double ALCANCE = 32.0;
	/** A partir de aqui se oye el corazon. */
	private static final float UMBRAL = 0.15F;

	private static float miedo;
	private static float miedoAntes;
	private static int siguiente;
	private static long latidoEn = Long.MIN_VALUE / 2;

	private Miedo() {
	}

	public static void tick(Minecraft mc) {
		miedoAntes = miedo;
		float objetivo = 0.0F;
		if (mc.player != null && mc.level != null && !mc.player.isSpectator() && !mc.player.isCreative()) {
			for (Bacteria b : mc.level.getEntitiesOfClass(Bacteria.class, mc.player.getBoundingBox().inflate(ALCANCE))) {
				float cerca = (float) Mth.clamp((ALCANCE - b.distanceTo(mc.player)) / (ALCANCE - 6.0), 0.0, 1.0);
				cerca = cerca * cerca * (3.0F - 2.0F * cerca);
				objetivo = Math.max(objetivo, cerca * (0.3F + 0.7F * b.caza(1.0F)));
			}
		}
		// sube de golpe y se calma despacio
		miedo += (objetivo - miedo) * (objetivo > miedo ? 0.08F : 0.02F);
		if (miedo > UMBRAL) {
			if (--siguiente <= 0) {
				siguiente = Math.round(Mth.lerp(miedo, 24.0F, 8.0F));
				latidoEn = Util.getMillis();
				mc.getSoundManager().play(SimpleSoundInstance.forUI(Sonidos.LATIDO, 1.0F + 0.1F * miedo, 0.35F + 0.65F * miedo));
			}
		} else {
			siguiente = 0;
		}
	}

	/** 0..1 interpolado entre ticks. */
	public static float valor(float parcial) {
		return Mth.lerp(parcial, miedoAntes, miedo);
	}

	/** true mientras suena el corazon del miedo (el de la cordura baja se calla). */
	public static boolean latiendo() {
		return miedo > UMBRAL;
	}

	/** 0..1: el golpe del ultimo latido, en dos tiempos como el sonido (latido.ogg). */
	public static float latido() {
		float t = (Util.getMillis() - latidoEn) / 1000.0F;
		if (t < 0.0F || t > 0.8F) {
			return 0.0F;
		}
		float uno = (float) Math.exp(-t / 0.09F);
		float dos = t >= 0.22F ? 0.7F * (float) Math.exp(-(t - 0.22F) / 0.09F) : 0.0F;
		return Math.min(1.0F, uno + dos);
	}

	public static void olvidar() {
		miedo = 0.0F;
		miedoAntes = 0.0F;
		siguiente = 0;
	}
}
