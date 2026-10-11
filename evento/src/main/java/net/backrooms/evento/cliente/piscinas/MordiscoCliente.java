package net.backrooms.evento.cliente.piscinas;

import java.util.Random;
import net.backrooms.evento.cliente.efectos.Miedo;
import net.backrooms.evento.entidad.Serpiente;
import net.backrooms.evento.red.Mordisco;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que ve a quien ataca la Serpiente (Serpiente#atacar): la vista se le va al craneo (puede
 * forcejear un poco con el raton, pero vuelve), que se echa atras y se le lanza encima; en el
 * mordisco, un tiron, la pantalla en rojo y todo tiembla; luego se funde a negro y muere. No
 * puede pegar, usar ni abrir nada. Los tiempos son los de Serpiente (AT_*), y la cara la
 * misma que anima ModeloSerpiente.
 */
public final class MordiscoCliente {
	private static final Random AZAR = new Random();
	private static int serpiente = -1;
	private static int ticks = -1;

	private MordiscoCliente() {
	}

	public static void empezar(Mordisco p) {
		serpiente = p.serpiente();
		ticks = 0;
	}

	public static boolean activo() {
		return ticks >= 0;
	}

	public static void olvidar() {
		serpiente = -1;
		ticks = -1;
	}

	public static void tick(Minecraft mc) {
		if (ticks < 0) {
			return;
		}
		LocalPlayer j = mc.player;
		Entity e = mc.level == null ? null : mc.level.getEntity(serpiente);
		if (j == null || !j.isAlive() || !(e instanceof Serpiente s) || ticks > Serpiente.AT_FIN + 20) {
			olvidar();
			return;
		}
		ticks++;
		while (mc.options.keyAttack.consumeClick()) {
			// nada
		}
		while (mc.options.keyUse.consumeClick()) {
			// nada
		}
		while (mc.options.keyInventory.consumeClick()) {
			// nada
		}
		while (mc.options.keyDrop.consumeClick()) {
			// nada
		}
		Miedo.forzar(1.0F);
		// la vista al craneo: encima de su caja, y en el lanzamiento, mas cerca y mas bajo
		float lanza = lanzamiento(ticks);
		float giro = s.getYRot() * Mth.DEG_TO_RAD;
		Vec3 delante = new Vec3(-Mth.sin(giro), 0, Mth.cos(giro));
		Vec3 craneo = s.position().add(delante.scale(0.4 + 1.3 * lanza)).add(0, 2.9 - 0.9 * lanza, 0);
		Vec3 d = craneo.subtract(j.getEyePosition());
		float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
		float muerde = mordisco(ticks);
		float temblor = 0.8F + 3.0F * Mth.clamp(ticks / (float) Serpiente.AT_MUERDE, 0.0F, 1.0F) + muerde * 9.0F;
		yaw += (AZAR.nextFloat() - 0.5F) * temblor;
		pitch += (AZAR.nextFloat() - 0.5F) * temblor + muerde * 8.0F;
		j.setYRot(j.getYRot() + Mth.wrapDegrees(yaw - j.getYRot()) * 0.5F);
		j.setXRot(Mth.clamp(j.getXRot() + (pitch - j.getXRot()) * 0.5F, -90.0F, 90.0F));
		j.yHeadRot = j.getYRot();
	}

	/** 0..1: cuanto se ha lanzado (se echa atras, se lanza y en el mordisco esta encima). */
	public static float lanzamiento(float t) {
		if (t < Serpiente.AT_ATRAS) {
			return -0.3F * Mth.sin(t / Serpiente.AT_ATRAS * Mth.HALF_PI);
		}
		if (t < Serpiente.AT_MUERDE) {
			float u = (t - Serpiente.AT_ATRAS) / (Serpiente.AT_MUERDE - Serpiente.AT_ATRAS);
			return -0.3F + 1.3F * u * u;
		}
		if (t < Serpiente.AT_MATA + 2) {
			return 1.0F;
		}
		return Math.max(0.0F, 1.0F - (t - Serpiente.AT_MATA - 2) / (float) (Serpiente.AT_FIN - Serpiente.AT_MATA - 2));
	}

	/** 0..1: el golpe del mordisco (sube de golpe y cae en 6 ticks). */
	public static float mordisco(float t) {
		float u = t - Serpiente.AT_MUERDE;
		if (u < 0 || u >= 8) {
			return 0.0F;
		}
		return u < 1 ? u : 1.0F - (u - 1) / 7.0F;
	}

	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		if (ticks < 0) {
			return;
		}
		float t = ticks + tiempo.getGameTimeDeltaPartialTick(false);
		int w = g.guiWidth();
		int h = g.guiHeight();
		// vineta azul oscura que se cierra, y rojo con el mordisco
		float base = 0.15F + 0.3F * Mth.clamp(t / Serpiente.AT_MUERDE, 0.0F, 1.0F);
		float muerde = mordisco(t);
		int bandas = 10;
		for (int i = 0; i < bandas; i++) {
			float k = 1.0F - i / (float) bandas;
			int a = Math.round(Mth.clamp((base + muerde * 0.4F) * k * k, 0.0F, 1.0F) * 255);
			int mx = Math.round(w * 0.22F * i / bandas);
			int my = Math.round(h * 0.22F * i / bandas);
			int col = (a << 24) | (muerde > 0.05F ? 0x5A0804 : 0x06141C);
			int gy = Math.max(1, Math.round(h * 0.022F));
			int gx = Math.max(1, Math.round(w * 0.022F));
			g.fill(mx, my, w - mx, my + gy, col);
			g.fill(mx, h - my - gy, w - mx, h - my, col);
			g.fill(mx, my, mx + gx, h - my, col);
			g.fill(w - mx - gx, my, w - mx, h - my, col);
		}
		if (muerde > 0.0F) {
			g.fill(0, 0, w, h, (Math.round(muerde * 140) << 24) | 0x7A0A06);
		}
		float negro = Mth.clamp((t - (Serpiente.AT_MATA - 6)) / 5.0F, 0.0F, 1.0F);
		if (negro > 0.0F) {
			g.fill(0, 0, w, h, Math.round(negro * 255) << 24);
		}
	}
}
