package net.backrooms.evento.cliente;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.red.Eliminado;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

/**
 * La animacion que ven todos cuando alguien cae: un corte de cinta (estatica
 * roja y lineas rotas) y una banda con la cara y el nick del eliminado y
 * cuantos exploradores quedan. Si caen varios a la vez, salen uno detras de
 * otro.
 */
public final class Eliminaciones {
	private static final float DURA = 5.0F;
	private static final int ROJO = 0xFFFF4A3D;
	private static final int AMARILLO = 0xFFF2E6A0;
	private static final int BLANCO = 0xFFFFFFFF;

	private record Aviso(String nombre, int quedan, @Nullable Component cara) {
	}

	private static final Deque<Aviso> COLA = new ArrayDeque<>();
	private static final Random AZAR = new Random();
	private static @Nullable Aviso actual;
	private static long desde;
	/** Hasta cuando este jugador cuenta como recien eliminado (para su pantalla de muerte). */
	private static long yoHasta;

	private Eliminaciones() {
	}

	/** true si al jugador local lo acaban de eliminar: su pantalla de muerte no ofrece reaparecer. */
	public static boolean yoEliminado() {
		return System.currentTimeMillis() < yoHasta;
	}

	public static void recibir(Eliminado e) {
		var yo = Minecraft.getInstance().player;
		if (yo != null && e.id().equals(yo.getUUID())) {
			yoHasta = System.currentTimeMillis() + 60_000;
		}
		Component cara = null;
		var red = Minecraft.getInstance().getConnection();
		PlayerInfo info = red == null ? null : red.getPlayerInfo(e.id());
		if (info != null) {
			cara = Component.object(new PlayerSprite(ResolvableProfile.createResolved(info.getProfile()), true));
		}
		COLA.add(new Aviso(e.nombre(), e.quedan(), cara));
	}

	private static float suave(float a, float b, float x) {
		float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
		return t * t * (3.0F - 2.0F * t);
	}

	private static int alfa(int color, float a) {
		return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (color & 0xFFFFFF);
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		long ahora = System.currentTimeMillis();
		if (actual == null || ahora - desde > DURA * 1000) {
			actual = COLA.poll();
			if (actual == null) {
				return;
			}
			desde = ahora;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sonidos.ELIMINADO, 1.0F, 0.9F));
		}
		float t = (ahora - desde) / 1000.0F;
		int w = g.guiWidth();
		int h = g.guiHeight();

		// 1) el corte: estatica roja y lineas de cinta rotas por toda la pantalla
		if (t < 0.45F) {
			float f = 1.0F - t / 0.45F;
			g.fill(0, 0, w, h, alfa(0x5A0A06, 0.35F * f));
			for (int i = 0; i < 46; i++) {
				int y = AZAR.nextInt(h);
				int alto = 1 + AZAR.nextInt(4);
				int color = AZAR.nextInt(4) == 0 ? BLANCO : ROJO;
				g.fill(0, y, w, y + alto, alfa(color, f * (0.15F + AZAR.nextFloat() * 0.35F)));
			}
		}

		// 2) la banda con el eliminado
		float a = suave(0.12F, 0.42F, t) * (1.0F - suave(DURA - 0.8F, DURA, t));
		if (a <= 0.0F) {
			return;
		}
		int cy = Math.round(h * 0.24F);
		int mitad = 30;
		// temblor de la imagen mientras entra
		int dx = t < 1.1F ? Math.round((AZAR.nextFloat() - 0.5F) * 6.0F * (1.1F - t)) : 0;
		g.fill(0, cy - mitad, w, cy + mitad, alfa(0x0A0806, 0.82F * a));
		g.fill(0, cy - mitad, w, cy - mitad + 1, alfa(ROJO, 0.9F * a));
		g.fill(0, cy + mitad - 1, w, cy + mitad, alfa(ROJO, 0.9F * a));
		for (int y = cy - mitad; y < cy + mitad; y += 2) {
			g.fill(0, y, w, y + 1, alfa(0x000000, 0.22F * a));
		}

		Aviso av = actual;
		String titulo = "[ SEÑAL PERDIDA ]";
		Texto.hud(g, titulo, (w - Texto.anchoHud(titulo, 9, 0.3F)) / 2.0F + dx, cy - mitad + 5, 9, 0.3F, alfa(ROJO, a));

		String nick = av.nombre().toUpperCase();
		float tam = 22.0F;
		float anchoNick = Texto.anchoHud(nick, tam, 0.08F);
		int lado = 22;
		float total = anchoNick + (av.cara() != null ? lado + 8 : 0);
		float x = (w - total) / 2.0F + dx;
		float yNick = cy - 11;
		if (av.cara() != null) {
			Matrix3x2fStack p = g.pose();
			p.pushMatrix();
			p.translate(x, yNick);
			p.scale(lado / 8.0F, lado / 8.0F);
			g.drawString(Minecraft.getInstance().font, av.cara(), 0, 0, alfa(BLANCO, a), false);
			p.popMatrix();
			x += lado + 8;
		}
		// corrimiento de color de la cinta en el nick
		float corre = t < 1.4F ? 2.0F + AZAR.nextFloat() * 2.0F : 1.0F;
		Texto.hud(g, nick, x + corre, yNick, tam, 0.08F, alfa(0xFF1E46, 0.55F * a));
		Texto.hud(g, nick, x - corre, yNick, tam, 0.08F, alfa(0x00D2FF, 0.45F * a));
		Texto.hud(g, nick, x, yNick, tam, 0.08F, alfa(BLANCO, a));

		String sub = "HA CAÍDO EN EL NIVEL 0   ·   QUEDAN " + av.quedan() + (av.quedan() == 1 ? " EXPLORADOR" : " EXPLORADORES");
		Texto.hud(g, sub, (w - Texto.anchoHud(sub, 9, 0.12F)) / 2.0F + dx, cy + mitad - 14, 9, 0.12F, alfa(AMARILLO, 0.85F * a));
	}
}
