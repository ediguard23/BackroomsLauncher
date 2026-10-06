package net.backrooms.evento.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.escondite.Arrastre;
import net.backrooms.evento.escondite.Escondites;
import net.backrooms.evento.red.AccionJugador;
import net.backrooms.evento.red.EstadoArrastre;
import net.backrooms.menu.render.Texto;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

/**
 * Arrastrarse en el cliente (ver escondite/Arrastre):
 *  - Z (configurable) tumba o levanta.
 *  - Agacharse (Mayus) delante de un hueco de la pared tumba solo, y al salir del
 *    hueco y soltar Mayus se vuelve a levantar.
 * Abajo en el centro: el aviso de que hay un hueco delante y, dentro, ESCONDIDO.
 */
public final class ArrastreCliente {
	public static final KeyMapping ARRASTRARSE = KeyBindingHelper.registerKeyBinding(
		new KeyMapping("key.backrooms_evento.arrastrarse", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, HerramientasCliente.CATEGORIA));

	/** Tumbado con la tecla (se queda hasta volver a pulsarla). */
	private static boolean manual;
	/** Tumbado solo por un hueco (se levanta al salir). */
	private static boolean solo;
	private static boolean enviado;
	private static float aviso;
	private static float avisoAntes;

	private ArrastreCliente() {
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(EstadoArrastre.TYPE, (p, ctx) -> {
			LocalPlayer yo = ctx.client().player;
			if (yo != null && yo.getUUID().equals(p.jugador())) {
				// el servidor me ha levantado (he muerto, cambiado de mundo o me han agarrado)
				if (!p.si()) {
					manual = false;
					solo = false;
					enviado = false;
				}
			}
			Arrastre.cliente(p.jugador(), p.si());
		});
		ClientTickEvents.END_CLIENT_TICK.register(ArrastreCliente::tick);
	}

	private static boolean hueco(Level nivel, BlockPos p) {
		return nivel.getBlockState(p).is(Bloques.HUECO);
	}

	/** Hay un hueco justo delante (o se esta dentro). */
	private static boolean huecoCerca(LocalPlayer j) {
		BlockPos pies = j.blockPosition();
		if (hueco(j.level(), pies)) {
			return true;
		}
		Direction d = j.getDirection();
		return hueco(j.level(), pies.relative(d)) && j.position().distanceToSqr(pies.relative(d).getBottomCenter()) < 2.0;
	}

	private static void tick(Minecraft mc) {
		LocalPlayer j = mc.player;
		avisoAntes = aviso;
		if (j == null) {
			while (ARRASTRARSE.consumeClick()) {
				// nada
			}
			return;
		}
		while (ARRASTRARSE.consumeClick()) {
			manual = !manual;
			solo = false;
		}
		boolean cerca = !j.isPassenger() && !j.isSpectator() && huecoCerca(j);
		if (cerca && j.isShiftKeyDown()) {
			solo = true;
		} else if (solo && !hueco(j.level(), j.blockPosition()) && !j.isShiftKeyDown()) {
			solo = false;
		}
		boolean quiero = (manual || solo) && !j.isPassenger() && !j.isSpectator();
		if (quiero != enviado) {
			enviado = quiero;
			Arrastre.cliente(j.getUUID(), quiero);
			ClientPlayNetworking.send(new AccionJugador(AccionJugador.ARRASTRARSE, quiero));
		}
		aviso += ((cerca && !Escondites.escondido(j) ? 1.0F : 0.0F) - aviso) * 0.25F;
	}

	/** El aviso del hueco y el ESCONDIDO, abajo en el centro encima de la barra. */
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
			int a = Math.round(pulso * 255);
			String s = "ESCONDIDO";
			Texto.hud(g, s, w / 2.0F - Texto.anchoHud(s, 12, 0.3F) / 2.0F, y, 12, 0.3F, (a << 24) | 0xE8D9A0);
			return;
		}
		float a = Mth.lerp(tiempo.getGameTimeDeltaPartialTick(false), avisoAntes, aviso);
		if (a > 0.02F) {
			String s = Arrastre.arrastrandose(j) ? "ENTRA EN EL HUECO" : "MAYÚS · METERSE EN EL HUECO";
			int alfa = Math.round(a * 220);
			Texto.hud(g, s, w / 2.0F - Texto.anchoHud(s, 10, 0.2F) / 2.0F, y + 2, 10, 0.2F, (alfa << 24) | 0xF2EEE4);
		}
	}

	public static void olvidar() {
		manual = false;
		solo = false;
		enviado = false;
		aviso = 0;
		avisoAntes = 0;
		Arrastre.olvidarCliente();
	}
}
