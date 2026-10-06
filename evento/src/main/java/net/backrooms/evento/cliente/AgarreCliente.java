package net.backrooms.evento.cliente;

import java.util.Random;
import net.backrooms.evento.cliente.efectos.Miedo;
import net.backrooms.evento.entidad.Bacteria;
import net.backrooms.evento.red.Agarrado;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que ve quien ha agarrado la Bacteria (ver Bacteria#agarrar): la vista se le
 * queda clavada en su cara (puede forcejear un poco con el raton, pero vuelve),
 * tiembla cada vez mas y da un tiron en cada mordisco; no puede pegar, usar ni
 * abrir nada; el corazon a tope; la pantalla se tine de rojo con cada mordisco y
 * al final se funde a negro justo antes de morir.
 */
public final class AgarreCliente {
	private static final Random AZAR = new Random();
	private static int bacteria = -1;
	private static int ticks = -1;

	private AgarreCliente() {
	}

	public static void empezar(Agarrado p) {
		bacteria = p.bacteria();
		ticks = 0;
		// al "montarte" en ella Minecraft pone «Pulsa Mayús para bajarte»: ni se puede ni pega
		net.minecraft.client.Minecraft.getInstance().gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
	}

	public static boolean activo() {
		return ticks >= 0;
	}

	public static void tick(Minecraft mc) {
		if (ticks < 0) {
			return;
		}
		LocalPlayer j = mc.player;
		Entity e = mc.level == null ? null : mc.level.getEntity(bacteria);
		// unos ticks de margen hasta que llega que va montado
		if (j == null || !j.isAlive() || !(e instanceof Bacteria b) || (ticks > 4 && j.getVehicle() != b) || ticks > Bacteria.AG_FIN + 40) {
			olvidar();
			return;
		}
		ticks++;
		if (ticks < 10) {
			// el «Pulsa Mayús para bajarte» puede llegar despues que el agarre (ver empezar)
			mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
		}
		// nada de pegar, usar, soltar ni abrir el inventario
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

		// la vista a su cara: un poco por delante de sus ojos (la cabeza va encorvada)
		float giro = b.getYRot() * Mth.DEG_TO_RAD;
		Vec3 delante = new Vec3(-Mth.sin(giro), 0, Mth.cos(giro));
		Vec3 cara = b.position().add(delante.scale(0.85)).add(0, 3.15, 0);
		Vec3 d = cara.subtract(j.getEyePosition());
		float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
		float muerde = Bacteria.mordisco(ticks);
		float temblor = 0.6F + 2.4F * Mth.clamp((ticks - Bacteria.AG_LEVANTA) / 30.0F, 0.0F, 1.0F) + muerde * 7.0F;
		yaw += (AZAR.nextFloat() - 0.5F) * temblor;
		pitch += (AZAR.nextFloat() - 0.5F) * temblor + muerde * 6.0F;
		j.setYRot(j.getYRot() + Mth.wrapDegrees(yaw - j.getYRot()) * 0.55F);
		j.setXRot(Mth.clamp(j.getXRot() + (pitch - j.getXRot()) * 0.55F, -90.0F, 90.0F));
		j.yHeadRot = j.getYRot();
	}

	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		if (ticks < 0) {
			return;
		}
		float t = ticks + tiempo.getGameTimeDeltaPartialTick(false);
		int w = g.guiWidth();
		int h = g.guiHeight();
		// vineta roja que se cierra segun avanza, y un fogonazo con cada mordisco
		float base = 0.12F + 0.28F * Mth.clamp((t - Bacteria.AG_LEVANTA) / (Bacteria.AG_COME - Bacteria.AG_LEVANTA), 0.0F, 1.0F);
		float muerde = Bacteria.mordisco(t);
		int bandas = 10;
		for (int i = 0; i < bandas; i++) {
			float k = 1.0F - i / (float) bandas;
			int a = Math.round(Mth.clamp((base + muerde * 0.35F) * k * k, 0.0F, 1.0F) * 255);
			int mx = Math.round(w * 0.22F * i / bandas);
			int my = Math.round(h * 0.22F * i / bandas);
			int col = (a << 24) | 0x5A0804;
			g.fill(mx, my, w - mx, my + Math.max(1, Math.round(h * 0.022F)), col);
			g.fill(mx, h - my - Math.max(1, Math.round(h * 0.022F)), w - mx, h - my, col);
			g.fill(mx, my, mx + Math.max(1, Math.round(w * 0.022F)), h - my, col);
			g.fill(w - mx - Math.max(1, Math.round(w * 0.022F)), my, w - mx, h - my, col);
		}
		if (muerde > 0.0F) {
			g.fill(0, 0, w, h, (Math.round(muerde * 120) << 24) | 0x7A0A06);
		}
		// se va todo a negro justo antes del final
		float negro = Mth.clamp((t - (Bacteria.AG_FIN - 7)) / 6.0F, 0.0F, 1.0F);
		if (negro > 0.0F) {
			g.fill(0, 0, w, h, Math.round(negro * 255) << 24);
		}
	}

	public static void olvidar() {
		bacteria = -1;
		ticks = -1;
	}
}
