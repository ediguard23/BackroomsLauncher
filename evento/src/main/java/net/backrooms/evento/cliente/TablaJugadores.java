package net.backrooms.evento.cliente;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.backrooms.menu.Evento;
import net.backrooms.menu.render.Logos;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

/**
 * La lista del TAB del evento (sustituye a la de Minecraft, ver
 * TablaJugadoresMixin): el logo arriba, todos los jugadores en orden
 * alfabetico como «[+] nick» (corchetes grises, el + amarillo palido y el
 * nick blanco) y abajo el Discord, la tienda y el credito de
 * PeakMC Studio. Los eliminados (en espectador) salen apagados.
 *
 * Con 200 jugadores no caben en una columna: se reparten en tantas como
 * quepan a lo ancho, y si aun asi no caben, la letra baja de tamano.
 */
public final class TablaJugadores {
	private static final int AMARILLO = 0xFFF2E6A0;
	private static final int BLANCO = 0xFFFFFFFF;
	private static final int GRIS = 0xFF8C8C8C;
	private static final int FONDO = 0xD80E0C07;
	private static final int BORDE = 0x50F2E6A0;
	private static final String MARCA = "[+] ";

	private static Evento evento;
	private static long eventoLeido;

	private TablaJugadores() {
	}

	/** Los enlaces vienen de config/backrooms-event.json (lo escribe el launcher); se releen cada 10 s. */
	private static Evento evento() {
		long ahora = System.currentTimeMillis();
		if (evento == null || ahora - eventoLeido > 10_000) {
			evento = Evento.cargar();
			eventoLeido = ahora;
		}
		return evento;
	}

	private static String sinEsquema(String url) {
		return url.replaceFirst("^https?://", "").replaceFirst("/$", "");
	}

	private static int alfa(int color, float a) {
		return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (color & 0xFFFFFF);
	}

	/** Solo para pruebas (orden "tab <n>"): anade n jugadores ficticios para ver la lista llena. */
	public static int ficticios;

	private record Fila(String nombre, boolean fuera) {
	}

	private static int cacheClave;
	private static float cacheAncho;

	/** Ancho del nick mas largo con la letra a 12 (el ancho escala lineal con el tamano). */
	private static float nickMasLargo(List<Fila> filas) {
		int clave = filas.size() * 31 + filas.hashCode();
		if (clave != cacheClave) {
			float max = 0;
			for (Fila f : filas) {
				max = Math.max(max, Texto.anchoHud(f.nombre(), 12, 0.0F));
			}
			cacheAncho = max;
			cacheClave = clave;
		}
		return cacheAncho;
	}

	private static List<Fila> filas() {
		List<Fila> l = new ArrayList<>();
		ClientPacketListener red = Minecraft.getInstance().getConnection();
		if (red != null) {
			for (PlayerInfo p : red.getListedOnlinePlayers()) {
				l.add(new Fila(p.getProfile().name(), p.getGameMode() == GameType.SPECTATOR));
			}
		}
		if (ficticios > 0) {
			java.util.Random r = new java.util.Random(7);
			String[] trozos = {"Shadow", "xX", "Xx", "_", "Pro", "Noob", "Dark", "Lobo", "Gato", "MC", "2009", "YT", "Craft", "Luna", "Nico", "Sombra", "Rey", "Toxic", "Pixel", "Zz"};
			for (int i = 0; i < ficticios; i++) {
				StringBuilder b = new StringBuilder();
				while (b.length() < 3 + r.nextInt(12)) {
					b.append(trozos[r.nextInt(trozos.length)]);
				}
				String n = b.length() > 16 ? b.substring(0, 16) : b.toString();
				l.add(new Fila(n, r.nextInt(5) == 0));
			}
		}
		l.sort(Comparator.comparing(f -> f.nombre().toLowerCase(Locale.ROOT)));
		return l;
	}

	public static void dibujar(GuiGraphics g, int ancho) {
		if (Minecraft.getInstance().getConnection() == null) {
			return;
		}
		List<Fila> jugadores = filas();
		int n = jugadores.size();
		int altoPantalla = g.guiHeight();

		Evento ev = evento();
		String discord = sinEsquema(ev.discord);
		String tienda = sinEsquema(ev.tienda);
		String pie = (discord.isEmpty() ? "" : "DISCORD  " + discord) + (discord.isEmpty() || tienda.isEmpty() ? "" : "   ·   ")
			+ (tienda.isEmpty() ? "" : "TIENDA  " + tienda);

		// Cabecera y pie se encogen en pantallas bajas para dejar sitio a la lista
		int altoLogo = altoPantalla < 300 ? 24 : 34;
		int cabecera = 10 + altoLogo + 18;
		int altoPie = 44;
		int y0 = 6;
		int libreAlto = altoPantalla - y0 * 2 - cabecera - altoPie - 6;
		int libreAncho = ancho - 28;

		// La letra mas grande con la que caben todos: tantas columnas como entren a
		// lo ancho, sin pasar de 20 filas mientras sobre sitio.
		float base = nickMasLargo(jugadores);
		float tam = 12.0F;
		int fila = 0;
		int columna = 0;
		int columnas = 1;
		int filas = 1;
		int visibles = n;
		boolean cabe = false;
		for (; tam >= 5.0F; tam -= 0.5F) {
			fila = (int) Math.ceil(tam * 1.12F) + 1;
			float marca = Texto.anchoHud(MARCA, tam, 0.0F);
			columna = (int) Math.ceil(marca + base * tam / 12.0F + tam * 1.4F);
			int maxColumnas = Math.max(1, libreAncho / columna);
			int maxFilas = Math.max(1, libreAlto / fila);
			int porColumna = Math.min(maxFilas, 20);
			columnas = Math.max(1, (n + porColumna - 1) / porColumna);
			if (columnas > maxColumnas) {
				columnas = Math.max(1, (n + maxFilas - 1) / maxFilas);
			}
			if (columnas <= maxColumnas) {
				filas = Math.max(1, (n + columnas - 1) / columnas);
				cabe = true;
				break;
			}
		}
		if (!cabe) {
			// ni con la letra minima: los que quepan y un "+N mas"
			tam = 5.0F;
			fila = (int) Math.ceil(tam * 1.12F) + 1;
			columna = (int) Math.ceil(Texto.anchoHud(MARCA, tam, 0.0F) + base * tam / 12.0F + tam * 1.4F);
			columnas = Math.max(1, libreAncho / columna);
			filas = Math.max(1, libreAlto / fila);
			visibles = columnas * filas - 1;
		}

		int anchoRejilla = columnas * columna;
		int anchoPanel = Math.min(ancho - 8, Math.max(Math.max(anchoRejilla, (int) Texto.anchoHud(pie, 10, 0.04F)), 220) + 24);
		int x0 = (ancho - anchoPanel) / 2;
		int altoPanel = cabecera + filas * fila + 10 + 32;
		g.fill(x0, y0, x0 + anchoPanel, y0 + altoPanel, FONDO);
		g.renderOutline(x0, y0, anchoPanel, altoPanel, BORDE);

		// cabecera: el logo del evento y cuantos quedan
		float pixeles = Logos.escalaGui();
		int anchoLogo = Math.round(altoLogo * Logos.BACKROOMS.proporcion());
		Logos.BACKROOMS.dibujar(g, (ancho - anchoLogo) / 2, y0 + 8, altoLogo, pixeles, BLANCO);
		long vivos = jugadores.stream().filter(f -> !f.fuera()).count();
		String cuenta = vivos + (vivos == 1 ? " EXPLORADOR" : " EXPLORADORES") + "   ·   " + n + (n == 1 ? " CONECTADO" : " CONECTADOS");
		Texto.hud(g, cuenta, (ancho - Texto.anchoHud(cuenta, 10, 0.08F)) / 2.0F, y0 + 10 + altoLogo, 10, 0.08F, alfa(AMARILLO, 0.75F));

		// jugadores, por columnas
		int xr = (ancho - anchoRejilla) / 2;
		int yr = y0 + cabecera;
		float anchoMarca = Texto.anchoHud(MARCA, tam, 0.0F);
		for (int i = 0; i < Math.min(n, visibles); i++) {
			Fila f = jugadores.get(i);
			int cx = xr + (i / filas) * columna + 4;
			int cy = yr + (i % filas) * fila;
			float a = f.fuera() ? 0.4F : 1.0F;
			// «[+]»: corchetes grises y solo el + en amarillo palido
			float x = cx;
			x += Texto.hud(g, "[", x, cy, tam, 0.0F, alfa(GRIS, a));
			x += Texto.hud(g, "+", x, cy, tam, 0.0F, alfa(AMARILLO, a));
			Texto.hud(g, "]", x, cy, tam, 0.0F, alfa(GRIS, a));
			Texto.hud(g, f.nombre(), cx + anchoMarca, cy, tam, 0.0F, alfa(BLANCO, a));
		}
		if (visibles < n) {
			int i = visibles;
			Texto.hud(g, "+" + (n - visibles) + " MÁS", xr + (i / filas) * columna + 4, yr + (i % filas) * fila, tam, 0.06F, alfa(AMARILLO, 0.8F));
		}

		// pie: Discord y tienda, y el credito con el logo de PeakMC Studio
		int yp = yr + filas * fila + 8;
		g.fill(x0 + 10, yp - 3, x0 + anchoPanel - 10, yp - 2, BORDE);
		if (!pie.isEmpty()) {
			Texto.hud(g, pie, (ancho - Texto.anchoHud(pie, 10, 0.04F)) / 2.0F, yp, 10, 0.04F, BLANCO);
		}
		String credito = "DESARROLLADO POR";
		int altoPeak = 11;
		int anchoPeak = Math.round(altoPeak * Logos.PEAKMC_STUDIO.proporcion());
		float anchoCredito = Texto.anchoHud(credito, 8, 0.1F) + 5 + anchoPeak;
		float xc = (ancho - anchoCredito) / 2.0F;
		Texto.hud(g, credito, xc, yp + 15, 8, 0.1F, alfa(AMARILLO, 0.7F));
		Logos.PEAKMC_STUDIO.dibujar(g, Math.round(xc + Texto.anchoHud(credito, 8, 0.1F) + 5), yp + 14, altoPeak, pixeles, BLANCO);
	}
}
