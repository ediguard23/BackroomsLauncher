package net.backrooms.evento.cliente;

import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.acceso.ColaEntrada;
import net.backrooms.menu.Tema;
import net.backrooms.menu.render.Texto;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;

/**
 * La cola para entrar (ColaEntrada): el servidor manda cada segundo el puesto, cuantos hay y
 * los segundos que faltan; mientras tanto la pantalla de conexion ensena la sala de espera
 * en lugar de «Iniciando sesion...» (ColaConexionMixin).
 */
public final class ColaCliente {
	private static int puesto;
	private static int total;
	private static int segundos;
	private static long recibido;

	private ColaCliente() {
	}

	public static void registrar() {
		ClientLoginNetworking.registerGlobalReceiver(ColaEntrada.CANAL, (cliente, handler, buf, callbacks) -> {
			puesto = buf.readVarInt();
			total = buf.readVarInt();
			segundos = buf.readVarInt();
			recibido = System.currentTimeMillis();
			return CompletableFuture.completedFuture(PacketByteBufs.empty());
		});
		ClientPlayConnectionEvents.JOIN.register((h, s, c) -> puesto = 0);
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> puesto = 0);
	}

	/** Si esta esperando en la cola (con un aviso de hace menos de 3 s). */
	public static boolean activa() {
		return puesto > 0 && System.currentTimeMillis() - recibido < 3000;
	}

	/** La sala de espera, encima de la pantalla de conexion. */
	public static void dibujar(GuiGraphics g, int ancho, int alto) {
		if (!activa()) {
			return;
		}
		Tema tema = Tema.actual();
		g.fill(0, 0, ancho, alto, 0xE80C0B08);
		float tam = alto / 12.0F;
		String titulo = "SALA DE ESPERA";
		Texto.hud(g, titulo, (ancho - Texto.anchoHud(titulo, tam, 0.14F)) / 2.0F, alto * 0.24F, tam, 0.14F, 0xFFE8D9A0);
		String sub = "Hay mucha gente entrando: se pasa de uno en uno.";
		float ts = tam * 0.32F;
		Texto.maquina(g, sub, (ancho - Texto.anchoMaquina(sub, ts, 0.0F)) / 2.0F, alto * 0.24F + tam * 1.2F, ts, 0.0F, 0xFFC8BC90);
		String pos = "PUESTO " + puesto + " DE " + total;
		float tp = tam * 0.8F;
		Texto.hud(g, pos, (ancho - Texto.anchoHud(pos, tp, 0.1F)) / 2.0F, alto * 0.5F, tp, 0.1F, 0xFFF2EEE4);
		// la cuenta atras sigue bajando entre aviso y aviso
		int quedan = Math.max(1, segundos - (int) ((System.currentTimeMillis() - recibido) / 1000));
		String s = quedan <= 3 ? "Entras ahora..." : "Entras en unos " + quedan + " s";
		float tq = tam * 0.38F;
		Texto.maquina(g, s, (ancho - Texto.anchoMaquina(s, tq, 0.0F)) / 2.0F, alto * 0.5F + tp * 1.3F, tq, 0.0F, 0xFFC8BC90);
		// unos puntos que laten, para que se vea que no esta colgado
		int n = (int) (System.currentTimeMillis() / 400 % 4);
		for (int i = 0; i < 3; i++) {
			int c = i < n ? 0xFFE8D9A0 : 0x40E8D9A0;
			int x = ancho / 2 - 18 + i * 14;
			g.fill(x, (int) (alto * 0.72F), x + 6, (int) (alto * 0.72F) + 6, c);
		}
		if (tema == null) {
			return;
		}
	}
}
