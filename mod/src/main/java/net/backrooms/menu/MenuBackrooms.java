package net.backrooms.menu;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import net.backrooms.menu.render.Degradado;
import net.backrooms.menu.render.Logos;
import net.backrooms.menu.render.Piscinas;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix3x2fStack;

/**
 * El menu de inicio del evento. Es el mismo diseno que el launcher (HUD de
 * videocamara, pase de explorador, boton de tubo fluorescente, registro de
 * expedicion), pero en otro nivel: las Piscinas.
 *
 * Todo se dibuja en un lienzo de diseno de 1180x720, el tamano de la ventana
 * del launcher, escalado y centrado sobre la pantalla; asi las medidas son las
 * mismas que en su CSS. Los botones reales (BotonInvisible) solo aportan la
 * zona de clic, el teclado y la narracion.
 */
public class MenuBackrooms extends Screen {
	private static final float ANCHO = 1180.0F;
	private static final float ALTO = 720.0F;
	private static final String[] MESES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};

	// paleta de las Piscinas
	private static final int TUBO = 0xFFEAFCFF;
	private static final int AGUA = 0xFF8FE3EA;
	private static final int NEGRO = 0xFF06161A;
	private static final int ROJO = 0xFFFF4A3D;

	private final Evento evento = Evento.cargar();
	private final long abierto = System.currentTimeMillis();
	private final Random azar = new Random();

	private float escala;
	private float ox;
	private float oy;

	private BotonInvisible jugar;
	private BotonInvisible configuracion;
	private BotonInvisible salir;
	private BotonInvisible discord;
	private BotonInvisible tienda;

	private String aviso = "";
	private long avisoHasta;
	private long proximoDestello = System.currentTimeMillis() + 9000;
	private long destelloDesde;

	public MenuBackrooms() {
		super(Component.literal("Backrooms"));
	}

	/** Pantallas que el jugador no debe ver: todas llevan a este menu. */
	public static Screen sustituir(Screen pantalla, boolean hayMundo) {
		if (pantalla == null) {
			return hayMundo ? null : new MenuBackrooms();
		}
		if (pantalla instanceof net.minecraft.client.gui.screens.DisconnectedScreen d) {
			return new SenalPerdida(((net.backrooms.menu.mixin.DisconnectedScreenAccessor) d).backrooms$detalles().reason());
		}
		if (pantalla.getClass() == net.minecraft.client.gui.screens.PauseScreen.class) {
			return new MenuPausa(((net.minecraft.client.gui.screens.PauseScreen) pantalla).showsPauseMenu());
		}
		String n = pantalla.getClass().getName();
		if (pantalla instanceof net.minecraft.client.gui.screens.TitleScreen
			|| pantalla instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
			|| pantalla instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
			|| pantalla instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
			|| pantalla instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
			|| n.startsWith("com.mojang.realmsclient.")) {
			return new MenuBackrooms();
		}
		return pantalla;
	}

	/* ------------------------------------------------------------ ciclo */

	@Override
	protected void init() {
		this.calcular();
		this.jugar = this.addRenderableWidget(new BotonInvisible(Component.literal("Jugar"), this::entrar));
		this.configuracion = this.addRenderableWidget(new BotonInvisible(Component.literal("Configuración"),
			() -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))));
		this.salir = this.addRenderableWidget(new BotonInvisible(Component.literal("Salir"), this.minecraft::stop));
		this.discord = this.addRenderableWidget(new BotonInvisible(Component.literal("Discord"), () -> this.abrir(this.evento.discord)));
		this.tienda = this.addRenderableWidget(new BotonInvisible(Component.literal("Tienda"), () -> this.abrir(this.evento.tienda)));
		this.discord.active = !this.evento.discord.isBlank();
		this.tienda.active = !this.evento.tienda.isBlank();
		this.jugar.active = this.evento.tieneServidor();

		this.colocar(this.jugar, 44, 512, 452, 86);
		this.colocar(this.configuracion, 44, 610, 221, 40);
		this.colocar(this.salir, 275, 610, 221, 40);
		this.colocar(this.discord, 790, 630, 170, 40);
		this.colocar(this.tienda, 970, 630, 170, 40);

		this.minecraft.getMusicManager().stopPlaying();
		if (!this.evento.tieneServidor()) {
			this.avisar("Abre el juego desde el launcher del evento.", 0);
		}
	}

	private void calcular() {
		this.escala = Math.min(this.width / ANCHO, this.height / ALTO);
		this.ox = (this.width - ANCHO * this.escala) / 2.0F;
		this.oy = (this.height - ALTO * this.escala) / 2.0F;
	}

	private void colocar(BotonInvisible b, float x, float y, float w, float h) {
		b.colocar(Math.round(this.ox + x * this.escala), Math.round(this.oy + y * this.escala), Math.round(w * this.escala), Math.round(h * this.escala));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private void entrar() {
		conectar(this, this.minecraft, this.evento);
	}

	/** Conecta al servidor fijo del evento; `padre` es a donde vuelve si se cancela. */
	static void conectar(Screen padre, net.minecraft.client.Minecraft mc, Evento evento) {
		if (!evento.tieneServidor()) {
			return;
		}
		Sonidos.ui(Sonidos.INMERSION, 0.9F);
		String dir = evento.host + ":" + evento.puerto;
		ServerData datos = new ServerData(evento.nombre, dir, ServerData.Type.OTHER);
		// el resource pack del servidor se acepta solo: es parte del evento
		datos.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
		ConnectScreen.startConnecting(padre, mc, ServerAddress.parseString(dir), datos, false, null);
	}

	private void abrir(String url) {
		if (url != null && url.startsWith("https://")) {
			ConfirmLinkScreen.confirmLinkNow(this, url);
		}
	}

	private void avisar(String texto, long ms) {
		this.aviso = texto;
		this.avisoHasta = ms > 0 ? System.currentTimeMillis() + ms : Long.MAX_VALUE;
	}

	/* ----------------------------------------------------------- dibujo */

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		this.destellos();
		Piscinas.dibujar(g, this.width, this.height);
		// velos: el lado del texto mas oscuro para que se lea sobre el agua
		int w = this.width;
		int h = this.height;
		Degradado.horizontal(g, 0, 0, w * 0.52F, h, 0xD2041A20, 0x00041A20);
		Degradado.horizontal(g, w * 0.6F, 0, w, h, 0x00041A20, 0xA8041A20);
		g.fillGradient(0, 0, w, (int) (h * 0.14F), 0x99041A20, 0x00041A20);
		g.fillGradient(0, (int) (h * 0.84F), w, h, 0x00041A20, 0xB0041A20);
	}

	/** Cada 15-35 s entra un golpe de luz por los arcos y suena un chapoteo lejano. */
	private void destellos() {
		long ahora = System.currentTimeMillis();
		if (ahora >= this.proximoDestello) {
			this.destelloDesde = ahora;
			this.proximoDestello = ahora + 15000 + this.azar.nextInt(20000);
			Sonidos.ui(Sonidos.ECO, 0.6F);
		}
		float d = (ahora - this.destelloDesde) / 1000.0F;
		Piscinas.luz = d < 1.6F ? 1.0F + 0.22F * (float) Math.sin(Math.PI * d / 1.6F) : 1.0F;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		for (BotonInvisible b : List.of(this.jugar, this.configuracion, this.salir, this.discord, this.tienda)) {
			boolean encima = b.isHovered() && b.active;
			if (encima && !b.encimaAntes) {
				Sonidos.ui(Sonidos.GOTA, 0.45F);
				b.encimaDesde = System.currentTimeMillis();
			}
			b.encimaAntes = encima;
		}

		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(this.ox, this.oy);
		p.scale(this.escala, this.escala);
		this.hud(g);
		this.titulo(g);
		this.pase(g);
		this.botonJugar(g);
		this.botonHud(g, this.configuracion, 44, 610, 221, 40, "CONFIGURACIÓN");
		this.botonHud(g, this.salir, 275, 610, 221, 40, "SALIR");
		this.estado(g);
		this.registro(g);
		this.botonHud(g, this.discord, 790, 630, 170, 40, "[ DISCORD ]");
		this.botonHud(g, this.tienda, 970, 630, 170, 40, "[ TIENDA ]");
		this.pie(g);
		p.popMatrix();

		super.render(g, mx, my, parcial);
	}

	static int alfa(int color, float a) {
		return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (color & 0xFFFFFF);
	}

	static String dos(long n) {
		return n < 10 ? "0" + n : Long.toString(n);
	}

	private void hud(GuiGraphics g) {
		long s = (System.currentTimeMillis() - this.abierto) / 1000;
		boolean punto = (System.currentTimeMillis() / 550) % 2 == 0;
		if (punto) {
			g.fill(30, 17, 43, 30, ROJO);
		}
		Texto.hud(g, "REC", 52, 13, 24, 0.06F, TUBO);
		Texto.hud(g, dos(s / 3600) + ":" + dos(s / 60 % 60) + ":" + dos(s % 60), 104, 13, 24, 0.06F, alfa(TUBO, 0.85F));
		Texto.hud(g, "SP", 1076, 13, 24, 0.06F, alfa(TUBO, 0.8F));
		// bateria
		g.renderOutline(1106, 16, 30, 15, TUBO);
		g.fill(1136, 20, 1139, 27, TUBO);
		for (int i = 0; i < 3; i++) {
			if (i < 2 || punto) {
				g.fill(1109 + i * 9, 19, 1115 + i * 9, 28, TUBO);
			}
		}
	}

	private void titulo(GuiGraphics g) {
		int alto = logo(g, 44, 66, 200, this.escala);
		int tx = 44 + Math.round(alto * Logos.BACKROOMS.proporcion()) + 22;
		Texto.hud(g, "NIVEL 37", tx, 150, 26, 0.5F, AGUA);
		Texto.parrafo(g, "El agua está templada. No recuerdas haber entrado.", tx, 184, 14, 496 - tx, 1.45F, alfa(TUBO, 0.75F));
	}

	/**
	 * El logo del evento con el corrimiento de color de la cinta (y un tiron
	 * mas fuerte cada 7 s, como el glitch del titulo del launcher).
	 */
	static int logo(GuiGraphics g, int x, int y, int alto, float escala) {
		float px = escala * Logos.escalaGui();
		float t = Piscinas.segundos() % 7.0F;
		int glitch = t > 6.5F ? Math.round((float) Math.sin(t * 90) * 5) : 0;
		Logos.BACKROOMS.dibujar(g, x + 2 + glitch, y, alto, px, 0x59FF1E46);
		Logos.BACKROOMS.dibujar(g, x - 2 - glitch, y, alto, px, 0x4D00D2FF);
		Logos.BACKROOMS.dibujar(g, x, y, alto, px, 0xFFFFFFFF);
		return alto;
	}

	/** "DESARROLLADO POR" y el logo de PeakMC Studio, centrados en `cx`. */
	static void firma(GuiGraphics g, float cx, int y, float escala, int color) {
		int alto = 34;
		int ancho = Math.round(alto * Logos.PEAKMC_STUDIO.proporcion());
		String por = "DESARROLLADO POR";
		float texto = Texto.anchoHud(por, 17, 0.22F);
		float x = cx - (texto + 10 + ancho) / 2.0F;
		Texto.hud(g, por, x, y + alto / 2.0F - 8, 17, 0.22F, alfa(color, 0.85F));
		Logos.PEAKMC_STUDIO.dibujar(g, Math.round(x + texto + 10), y, alto, escala * Logos.escalaGui(), 0xFFFFFFFF);
	}

	private void pase(GuiGraphics g) {
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.rotateAbout((float) Math.toRadians(-1.2), 270, 292);
		int x0 = 44;
		int y0 = 292;
		int x1 = 496;
		int y1 = 492;
		// sombra
		for (int i = 1; i <= 4; i++) {
			g.fill(x0 + i * 2, y0 + i * 4, x1 + i * 2, y1 + i * 4, 0x1E000000);
		}
		g.fill(x0, y0, x1, y1, 0xFFEEF5F3);
		g.fillGradient(x0, y0, x1, y0 + 56, 0xFFA6DFE3, 0xFF6CBCC4);
		g.fill(x0, y0 + 56, x1, y0 + 58, 0x59000000);
		g.fill(243, y0 + 9, 297, y0 + 18, 0xFF15292C);
		Texto.hud(g, "PASE DE EXPLORADOR", x0 + 18, y0 + 30, 22, 0.14F, 0xFF0F2629);
		String nombre = this.minecraft.getUser().getName();
		boolean premium = this.minecraft.getUser().getAccessToken().length() > 32;
		String num = "N.º " + numeroPase(premium ? this.minecraft.getUser().getProfileId().toString() : nombre);
		Texto.hud(g, num, x1 - 18 - Texto.anchoHud(num, 18, 0.14F), y0 + 33, 18, 0.14F, 0xBF0F2629);

		// foto: la cabeza del jugador con su skin
		g.fill(x0 + 18, y0 + 74, x0 + 114, y0 + 186, 0xFF8FA6A3);
		g.fill(x0 + 20, y0 + 76, x0 + 112, y0 + 184, 0xFFC9D6D3);
		Component cara = Component.object(new PlayerSprite(ResolvableProfile.createResolved(this.minecraft.getGameProfile()), true));
		p.pushMatrix();
		p.translate(x0 + 30, y0 + 84);
		p.scale(9.0F, 9.0F);
		g.drawString(this.font, cara, 0, 0, 0xFFFFFFFF, false);
		p.popMatrix();
		Texto.hud(g, "FOTO", x0 + 46, y0 + 162, 15, 0.3F, 0x8C0F2629);

		Texto.hud(g, "NOMBRE", x0 + 132, y0 + 74, 16, 0.3F, 0x990F2629);
		Texto.hud(g, nombre, x0 + 132, y0 + 92, 38, 0.0F, 0xFF0F2629);
		g.fill(x0 + 132, y0 + 132, x1 - 18, y0 + 134, 0xFF0F2629);
		// estado de la cuenta, como el interruptor del launcher
		int pista = premium ? 0xFF3D7A6A : 0xFF8FA09E;
		g.fill(x0 + 132, y0 + 152, x0 + 176, y0 + 174, pista);
		int bx = premium ? x0 + 157 : x0 + 135;
		g.fill(bx, y0 + 155, bx + 16, y0 + 171, 0xFFF6FBFA);
		Texto.hud(g, premium ? "CUENTA PREMIUM" : "CUENTA SIN VERIFICAR", x0 + 186, y0 + 152, 19, 0.08F, 0xFF0F2629);

		if (premium) {
			p.pushMatrix();
			p.rotateAbout((float) Math.toRadians(-12), x1 - 90, y1 - 34);
			g.renderOutline(x1 - 162, y1 - 52, 150, 38, 0xD1B0281D);
			g.renderOutline(x1 - 159, y1 - 49, 144, 32, 0xD1B0281D);
			Texto.hud(g, "VERIFICADO", x1 - 150, y1 - 47, 30, 0.14F, 0xD1B0281D);
			p.popMatrix();
		}
		p.popMatrix();
	}

	private static String numeroPase(String texto) {
		long h = 7;
		for (char c : texto.toCharArray()) {
			h = (h * 31 + c) & 0xFFFFFFFFL;
		}
		String n = Long.toString(h % 10000);
		return "0000".substring(n.length()) + n;
	}

	private void botonJugar(GuiGraphics g) {
		botonTubo(g, this.jugar, 44, 512, 496, 598, "JUGAR", "SIN SERVIDOR");
	}

	/** El boton grande de tubo fluorescente del launcher (JUGAR, VOLVER...). */
	static void botonTubo(GuiGraphics g, BotonInvisible boton, int x0, int y0, int x1, int y1, String texto, String inactivo) {
		boolean activo = boton.active;
		boolean encima = activo && boton.isHoveredOrFocused();
		if (activo) {
			// halo del tubo
			int capas = encima ? 10 : 8;
			for (int i = capas; i >= 1; i--) {
				g.fill(x0 - i * 5, y0 - i * 5, x1 + i * 5, y1 + i * 5, alfa(0xDDF8FF, encima ? 0.085F : 0.06F));
			}
		}
		g.fill(x0, y0, x1, y1, 0xFF233A3D);
		if (activo) {
			// parpadeo al pasar por encima, como en el launcher
			float t = (System.currentTimeMillis() % 4000) / 4000.0F;
			float vida = 1.0F;
			if (encima) {
				vida = parpadeoEntrada((System.currentTimeMillis() - boton.encimaDesde) / 900.0F);
			} else if (t > 0.47F && t < 0.49F) {
				vida = 0.93F;
			}
			g.fillGradient(x0 + 7, y0 + 7, x1 - 7, y1 - 7, alfa(0xF8FFFF, vida), alfa(0xC4EEF2, vida));
			for (int x = x0 + 9; x < x1 - 7; x += 18) {
				g.fill(x, y0 + 7, x + 2, y1 - 7, 0x0F000000);
			}
		} else {
			g.fill(x0 + 7, y0 + 7, x1 - 7, y1 - 7, 0xFF2A3A3C);
		}
		String txt = activo ? texto : inactivo;
		float tam = activo ? 58 : 40;
		float esp = activo ? 0.32F : 0.18F;
		float ancho = Texto.anchoHud(txt, tam, esp);
		Texto.hud(g, txt, (x0 + x1) / 2.0F - ancho / 2.0F, activo ? y0 + 17 : y0 + 26, tam, esp, activo ? 0xFF0F2E2F : 0xFF6D8285);
	}

	/** Mismo parpadeo que el launcher al pasar por encima (keyframes de parpadeo-tubo). */
	private static float parpadeoEntrada(float f) {
		if (f >= 0.26F) {
			return 1.0F;
		}
		if (f < 0.08F) {
			return 1.0F - f / 0.08F * 0.8F;
		}
		if (f < 0.14F) {
			return 0.2F + (f - 0.08F) / 0.06F * 0.8F;
		}
		if (f < 0.22F) {
			return 1.0F - (f - 0.14F) / 0.08F * 0.65F;
		}
		return 0.35F + (f - 0.22F) / 0.04F * 0.65F;
	}

	static void botonHud(GuiGraphics g, BotonInvisible b, int x, int y, int w, int h, String texto) {
		boolean encima = b.active && b.isHoveredOrFocused();
		float a = b.active ? 1.0F : 0.35F;
		g.fill(x, y, x + w, y + h, encima ? TUBO : alfa(0x04141A, 0.55F * a));
		g.renderOutline(x, y, w, h, alfa(TUBO, 0.4F * a));
		float tam = 24;
		float ancho = Texto.anchoHud(texto, tam, 0.1F);
		Texto.hud(g, texto, x + w / 2.0F - ancho / 2.0F, y + h / 2.0F - 9, tam, 0.1F, encima ? NEGRO : alfa(TUBO, a));
	}

	private void estado(GuiGraphics g) {
		int x0 = 790;
		int y0 = 62;
		int x1 = 1140;
		int y1 = 226;
		g.fill(x0, y0, x1, y1, 0x9E04141A);
		g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0x38EAFCFF);

		long falta = this.evento.apertura - System.currentTimeMillis();
		if (this.evento.apertura == 0 || falta <= 0) {
			Texto.hud(g, "LA PUERTA ESTÁ ABIERTA", x0 + 18, y0 + 14, 19, 0.22F, AGUA);
			if ((System.currentTimeMillis() / 800) % 2 == 0) {
				Texto.hud(g, "EN DIRECTO", x0 + 18, y0 + 38, 50, 0.05F, ROJO);
			}
		} else {
			long s = falta / 1000;
			long d = s / 86400;
			Texto.hud(g, "LA PUERTA SE ABRE EN", x0 + 18, y0 + 14, 19, 0.22F, AGUA);
			Texto.hud(g, (d > 0 ? d + "D " : "") + dos(s / 3600 % 24) + ":" + dos(s / 60 % 60) + ":" + dos(s % 60), x0 + 18, y0 + 38, 50, 0.05F, TUBO);
		}
		for (int x = x0 + 18; x < x1 - 18; x += 6) {
			g.fill(x, y0 + 100, x + 3, y0 + 101, 0x33EAFCFF);
		}

		Ping.Estado st = Ping.estado(this.evento);
		int nivel = 0;
		if (st != null && st.online()) {
			nivel = st.latencia() < 60 ? 4 : st.latencia() < 120 ? 3 : st.latencia() < 220 ? 2 : 1;
		}
		for (int i = 0; i < 4; i++) {
			int alto = 7 + i * 6;
			int col = st != null && !st.online() ? 0x59FF5A4D : i < nivel ? TUBO : 0x29EAFCFF;
			g.fill(x0 + 18 + i * 10, y0 + 140 - alto, x0 + 25 + i * 10, y0 + 140, col);
		}
		String linea1;
		String linea2;
		if (st == null) {
			linea1 = this.evento.tieneServidor() ? "BUSCANDO SEÑAL" : "SIN SERVIDOR";
			linea2 = "ERRANTES --";
		} else if (!st.online()) {
			linea1 = "SERVIDOR SIN SEÑAL";
			linea2 = "ERRANTES --";
		} else {
			linea1 = "SERVIDOR EN LÍNEA · " + st.latencia() + " MS";
			linea2 = "ERRANTES " + st.jugadores() + "/" + st.maximo();
		}
		Texto.hud(g, linea1, x0 + 70, y0 + 112, 20, 0.05F, TUBO);
		Texto.hud(g, linea2, x0 + 70, y0 + 132, 20, 0.05F, TUBO);
	}

	private void registro(GuiGraphics g) {
		int x0 = 790;
		int y0 = 242;
		int x1 = 1140;
		int y1 = 616;
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.rotateAbout((float) Math.toRadians(0.7), (x0 + x1) / 2.0F, y0);
		for (int i = 1; i <= 4; i++) {
			g.fill(x0 + i, y0 + i * 4, x1 + i, y1 + i * 4, 0x1E000000);
		}
		g.fill(x0, y0, x1, y1, 0xFFF2F5EF);
		for (int y = y0 + 26; y < y1; y += 26) {
			g.fill(x0, y, x1, y + 1, 0x2E3C6EA0);
		}
		g.fill(x0 + 34, y0, x0 + 36, y1, 0x59BE3228);
		Texto.maquina(g, "REGISTRO DE EXPEDICIÓN", x0 + 46, y0 + 12, 13, 0.18F, 0xFF1F2A2B);
		g.fill(x0, y0 + 34, x1, y0 + 35, 0x401F2A2B);
		// clip
		g.renderOutline(x1 - 40, y0 - 14, 16, 44, 0xFF8D9093);
		g.renderOutline(x1 - 36, y0 - 8, 8, 30, 0xFF8D9093);

		g.enableScissor(x0, y0 + 36, x1, y1 - 6);
		float y = y0 + 46;
		if (this.evento.noticias.isEmpty()) {
			Texto.maquina(g, "Sin entradas todavía.", x0 + 46, y, 13, 0.0F, 0xFF6D7A78);
		}
		for (Evento.Noticia n : this.evento.noticias) {
			String f = fecha(n.fecha());
			if (!f.isEmpty()) {
				Texto.maquina(g, f, x0 + 46, y, 11, 0.14F, 0xFF2A6F7A);
				y += 16;
			}
			y += Texto.parrafo(g, n.titulo(), x0 + 46, y, 15, x1 - x0 - 62, 1.3F, 0xFF1F2A2B);
			if (!n.texto().isBlank()) {
				for (String parte : n.texto().split("\n")) {
					y += Texto.parrafo(g, parte, x0 + 46, y + 2, 12.5F, x1 - x0 - 62, 1.45F, 0xFF4A5553);
				}
			}
			y += 10;
			for (int x = x0 + 46; x < x1 - 16; x += 6) {
				g.fill(x, (int) y, x + 3, (int) y + 1, 0x381F2A2B);
			}
			y += 12;
		}
		g.disableScissor();
		p.popMatrix();
	}

	private static String fecha(String iso) {
		try {
			LocalDate d = iso.length() == 10 ? LocalDate.parse(iso) : LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault()).toLocalDate();
			return dos(d.getDayOfMonth()) + " " + MESES[d.getMonthValue() - 1] + " " + d.getYear();
		} catch (Exception e) {
			return "";
		}
	}

	private void pie(GuiGraphics g) {
		fecha(g, alfa(TUBO, 0.9F));
		firma(g, 643, 678, this.escala, TUBO);
		if (!this.aviso.isEmpty() && System.currentTimeMillis() < this.avisoHasta) {
			Texto.hud(g, this.aviso, 32, 686, 22, 0.05F, TUBO);
		}
	}

	/** Fecha y hora de la camara, abajo a la derecha. */
	static void fecha(GuiGraphics g, int color) {
		LocalDateTime d = LocalDateTime.now();
		String f = MESES[d.getMonthValue() - 1] + ". " + dos(d.getDayOfMonth()) + " " + d.getYear() + "   " + dos(d.getHour()) + ":" + dos(d.getMinute());
		Texto.hud(g, f, 1148 - Texto.anchoHud(f, 22, 0.05F), 686, 22, 0.05F, color);
	}
}
