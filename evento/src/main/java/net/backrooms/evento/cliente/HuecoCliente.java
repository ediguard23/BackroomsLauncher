package net.backrooms.evento.cliente;

import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.escondite.Escondites;
import net.backrooms.evento.escondite.Hueco;
import net.backrooms.menu.render.Texto;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Los huecos de las paredes en el HUD, abajo en el centro: con uno delante, el
 * aviso de que hay que agacharse (Mayus) para meterse; dentro, ESCONDIDO.
 */
public final class HuecoCliente {
	private static float aviso;
	private static float avisoAntes;

	private HuecoCliente() {
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(HuecoCliente::tick);
	}

	/** Hay un boquete de pared hueca a menos de 1,5 bloques, a la altura de los pies. */
	private static boolean huecoCerca(LocalPlayer j) {
		BlockPos pies = j.blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(pies.offset(-1, 0, -1), pies.offset(1, 0, 1))) {
			var e = j.level().getBlockState(p);
			if (e.is(Bloques.HUECO) && Hueco.roto(e) && j.position().distanceToSqr(p.getBottomCenter()) < 2.25) {
				return true;
			}
		}
		return false;
	}

	private static void tick(Minecraft mc) {
		avisoAntes = aviso;
		LocalPlayer j = mc.player;
		boolean ver = j != null && !j.isSpectator() && !j.isPassenger() && !j.isShiftKeyDown() && !Escondites.escondido(j) && huecoCerca(j);
		aviso += ((ver ? 1.0F : 0.0F) - aviso) * 0.25F;
	}

	public static void render(GuiGraphics g, DeltaTracker tiempo) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer j = mc.player;
		if (j == null || mc.options.hideGui) {
			return;
		}
		int w = g.guiWidth();
		int y = g.guiHeight() - 62;
		if (Escondites.escondido(j)) {
			float pulso = 0.75F + 0.25F * Mth.sin(Util.getMillis() / 260.0F);
			String s = "ESCONDIDO";
			Texto.hud(g, s, w / 2.0F - Texto.anchoHud(s, 12, 0.3F) / 2.0F, y, 12, 0.3F, (Math.round(pulso * 255) << 24) | 0xE8D9A0);
			return;
		}
		float a = Mth.lerp(tiempo.getGameTimeDeltaPartialTick(false), avisoAntes, aviso);
		if (a > 0.02F) {
			String s = "MAYÚS · AGÁCHATE PARA ENTRAR";
			Texto.hud(g, s, w / 2.0F - Texto.anchoHud(s, 10, 0.2F) / 2.0F, y + 2, 10, 0.2F, (Math.round(a * 220) << 24) | 0xF2EEE4);
		}
	}

	public static void olvidar() {
		aviso = 0;
		avisoAntes = 0;
	}
}
