package net.backrooms.menu;

import java.util.List;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Menu de pausa con el aspecto del evento: la camara en PAUSA sobre la
 * partida, el boton de tubo para volver y los de HUD para lo demas. No tiene
 * "abrir a LAN", ni enlaces de Mojang, ni pantalla de denuncias.
 *
 * Hereda de PauseScreen para que el juego lo siga tratando como la pausa
 * (comprobaciones de instanceof, F3+Esc sin menu, etc.).
 */
public class MenuPausa extends PauseScreen {
	private static final float ANCHO = 1180.0F;
	private static final float ALTO = 720.0F;
	private static final int ROJO = 0xFFFF4A3D;

	private final Evento evento = Evento.cargar();
	private final Tema tema = Tema.actual();
	private final long abierto = System.currentTimeMillis();
	private float escala;
	private float ox;
	private float oy;

	private BotonInvisible volver;
	private BotonInvisible configuracion;
	private BotonInvisible logros;
	private BotonInvisible estadisticas;
	private BotonInvisible discord;
	private BotonInvisible tienda;
	private BotonInvisible desconectar;
	private List<BotonInvisible> botones = List.of();

	public MenuPausa(boolean conMenu) {
		super(conMenu);
	}

	@Override
	protected void init() {
		if (!this.showsPauseMenu()) {
			super.init(); // F3+Esc: pausa sin menu, como en Minecraft
			return;
		}
		this.escala = Math.min(this.width / ANCHO, this.height / ALTO);
		this.ox = (this.width - ANCHO * this.escala) / 2.0F;
		this.oy = (this.height - ALTO * this.escala) / 2.0F;

		this.volver = this.boton("Volver a la partida", () -> this.minecraft.setScreen(null), 44, 300, 452, 86);
		this.configuracion = this.boton("Configuración", () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)), 44, 404, 221, 40);
		this.logros = this.boton("Logros",
			() -> this.minecraft.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this)), 275, 404, 221, 40);
		this.estadisticas = this.boton("Estadísticas",
			() -> this.minecraft.setScreen(new StatsScreen(this, this.minecraft.player.getStats())), 44, 454, 221, 40);
		this.discord = this.boton("Discord", () -> this.abrir(this.evento.discord), 275, 454, 106, 40);
		this.tienda = this.boton("Tienda", () -> this.abrir(this.evento.tienda), 390, 454, 106, 40);
		this.desconectar = this.boton("Desconectar", this::salir, 44, 524, 452, 40);
		this.discord.active = !this.evento.discord.isBlank();
		this.tienda.active = !this.evento.tienda.isBlank();
		this.botones = List.of(this.volver, this.configuracion, this.logros, this.estadisticas, this.discord, this.tienda, this.desconectar);
	}

	private BotonInvisible boton(String nombre, Runnable accion, float x, float y, float w, float h) {
		BotonInvisible b = this.addRenderableWidget(new BotonInvisible(Component.literal(nombre), accion));
		b.colocar(Math.round(this.ox + x * this.escala), Math.round(this.oy + y * this.escala), Math.round(w * this.escala), Math.round(h * this.escala));
		return b;
	}

	private void abrir(String url) {
		if (url != null && url.startsWith("https://")) {
			ConfirmLinkScreen.confirmLinkNow(this, url);
		}
	}

	/** Lo mismo que el boton de desconectar de Minecraft (incluido el aviso de denuncia a medias). */
	private void salir() {
		this.desconectar.active = false;
		this.minecraft.getReportingContext()
			.draftReportHandled(this.minecraft, this, () -> this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
	}

	/* ----------------------------------------------------------- dibujo */

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		if (!this.showsPauseMenu()) {
			return;
		}
		super.renderBackground(g, mx, my, parcial);
		int w = this.width;
		int h = this.height;
		g.fill(0, 0, w, h, MenuBackrooms.alfa(this.tema.velo, 0.55F));
		g.fillGradient(0, 0, w, h / 6, MenuBackrooms.alfa(this.tema.velo, 0.6F), MenuBackrooms.alfa(this.tema.velo, 0));
		g.fillGradient(0, h - h / 5, w, h, MenuBackrooms.alfa(this.tema.velo, 0), MenuBackrooms.alfa(this.tema.velo, 0.69F));
		for (int y = 0; y < h; y += 2) {
			g.fill(0, y, w, y + 1, 0x1A000000);
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		if (!this.showsPauseMenu()) {
			super.render(g, mx, my, parcial);
			return;
		}
		for (BotonInvisible b : this.botones) {
			boolean encima = b.isHovered() && b.active;
			if (encima && !b.encimaAntes) {
				Sonidos.ui(this.tema.encima, 0.45F);
				b.encimaDesde = System.currentTimeMillis();
			}
			b.encimaAntes = encima;
		}

		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(this.ox, this.oy);
		p.scale(this.escala, this.escala);
		this.hud(g);
		int alto = MenuBackrooms.logo(g, 44, 74, 200, this.escala);
		int tx = 44 + Math.round(alto * net.backrooms.menu.render.Logos.BACKROOMS.proporcion()) + 22;
		Texto.hud(g, "PAUSA", tx, 136, 64, 0.12F, this.tema.tubo);
		Texto.parrafo(g, "La cinta sigue grabando. Ellos no se paran.", tx, 204, 14, 496 - tx, 1.45F, MenuBackrooms.alfa(this.tema.tubo, 0.75F));

		MenuBackrooms.botonTubo(g, this.volver, 44, 300, 496, 386, "VOLVER", "VOLVER");
		MenuBackrooms.botonHud(g, this.configuracion, 44, 404, 221, 40, "CONFIGURACIÓN");
		MenuBackrooms.botonHud(g, this.logros, 275, 404, 221, 40, "LOGROS");
		MenuBackrooms.botonHud(g, this.estadisticas, 44, 454, 221, 40, "ESTADÍSTICAS");
		MenuBackrooms.botonHud(g, this.discord, 275, 454, 106, 40, "DISCORD");
		MenuBackrooms.botonHud(g, this.tienda, 390, 454, 106, 40, "TIENDA");
		this.botonSalir(g);
		this.estado(g);
		this.notaMisiones(g);
		MenuBackrooms.fecha(g, MenuBackrooms.alfa(this.tema.tubo, 0.9F));
		MenuBackrooms.firma(g, 643, 678, this.escala, this.tema.tubo);
		p.popMatrix();

		super.render(g, mx, my, parcial);
	}

	/** Las misiones van en el inventario: aqui solo un recordatorio clavado. */
	private void notaMisiones(GuiGraphics g) {
		int x0 = 790;
		int y0 = 272;
		int x1 = 1140;
		int y1 = 352;
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.rotateAbout((float) Math.toRadians(-0.8), (x0 + x1) / 2.0F, y0);
		for (int i = 1; i <= 3; i++) {
			g.fill(x0 + i, y0 + i * 3, x1 + i, y1 + i * 3, 0x1E000000);
		}
		g.fill(x0, y0, x1, y1, this.tema.papel);
		g.fill(x0 + 34, y0, x0 + 36, y1, 0x59BE3228);
		Texto.maquina(g, "MISIONES DEL EXPLORADOR", x0 + 46, y0 + 14, 13, 0.18F, this.tema.papelTinta);
		String tecla = this.minecraft.options.keyInventory.getTranslatedKeyMessage().getString().toUpperCase();
		Texto.parrafo(g, "Están en tu inventario: pulsa " + tecla + ".", x0 + 46, y0 + 42, 14, x1 - x0 - 62, 1.4F, this.tema.papelTexto);
		p.popMatrix();
	}

	/** Desconectar va en rojo al pasar por encima: es la salida. */
	private void botonSalir(GuiGraphics g) {
		BotonInvisible b = this.desconectar;
		boolean encima = b.active && b.isHoveredOrFocused();
		int x = 44;
		int y = 524;
		int w = 452;
		int h = 40;
		g.fill(x, y, x + w, y + h, encima ? 0xFFB3241B : MenuBackrooms.alfa(this.tema.caja, 0.55F));
		g.renderOutline(x, y, w, h, encima ? 0xFFFF6A5C : MenuBackrooms.alfa(0xFF6A5C, b.active ? 0.6F : 0.25F));
		String texto = b.active ? "DESCONECTAR" : "SALIENDO...";
		float ancho = Texto.anchoHud(texto, 24, 0.1F);
		Texto.hud(g, texto, x + w / 2.0F - ancho / 2.0F, y + h / 2.0F - 9, 24, 0.1F, encima ? this.tema.tubo : MenuBackrooms.alfa(0xFF8A7E, b.active ? 1.0F : 0.5F));
	}

	/** HUD de camara en pausa: las dos barras parpadeando y el tiempo parado. */
	private void hud(GuiGraphics g) {
		boolean on = (System.currentTimeMillis() / 550) % 2 == 0;
		if (on) {
			g.fill(30, 16, 36, 32, this.tema.tubo);
			g.fill(40, 16, 46, 32, this.tema.tubo);
		}
		Texto.hud(g, "PAUSE", 56, 13, 24, 0.06F, this.tema.tubo);
		long s = (System.currentTimeMillis() - this.abierto) / 1000;
		Texto.hud(g, MenuBackrooms.dos(s / 60) + ":" + MenuBackrooms.dos(s % 60), 124, 13, 24, 0.06F, MenuBackrooms.alfa(this.tema.tubo, 0.85F));
		Texto.hud(g, "SP", 1076, 13, 24, 0.06F, MenuBackrooms.alfa(this.tema.tubo, 0.8F));
		g.renderOutline(1106, 16, 30, 15, this.tema.tubo);
		g.fill(1136, 20, 1139, 27, this.tema.tubo);
		for (int i = 0; i < 3; i++) {
			if (i < 2 || on) {
				g.fill(1109 + i * 9, 19, 1115 + i * 9, 28, this.tema.tubo);
			}
		}
	}

	/** Recuadro de la derecha: quien sigue dentro y como va la senal. */
	private void estado(GuiGraphics g) {
		int x0 = 790;
		int y0 = 62;
		int x1 = 1140;
		int y1 = 250;
		g.fill(x0, y0, x1, y1, MenuBackrooms.alfa(this.tema.caja, 0.62F));
		g.renderOutline(x0, y0, x1 - x0, y1 - y0, MenuBackrooms.alfa(this.tema.tubo, 0.22F));
		Texto.hud(g, "EXPEDICIÓN EN CURSO", x0 + 18, y0 + 14, 19, 0.22F, this.tema.acento);
		Texto.hud(g, this.evento.nombre.toUpperCase(), x0 + 18, y0 + 38, 50, 0.05F, this.tema.tubo);
		for (int x = x0 + 18; x < x1 - 18; x += 6) {
			g.fill(x, y0 + 100, x + 3, y0 + 101, MenuBackrooms.alfa(this.tema.tubo, 0.2F));
		}

		int errantes = 0;
		int latencia = -1;
		if (this.minecraft.getConnection() != null) {
			errantes = this.minecraft.getConnection().getOnlinePlayers().size();
			PlayerInfo yo = this.minecraft.getConnection().getPlayerInfo(this.minecraft.getUser().getProfileId());
			if (yo != null) {
				latencia = yo.getLatency();
			}
		}
		int nivel = latencia < 0 ? 0 : latencia < 60 ? 4 : latencia < 120 ? 3 : latencia < 220 ? 2 : 1;
		for (int i = 0; i < 4; i++) {
			int a = 7 + i * 6;
			g.fill(x0 + 18 + i * 10, y0 + 140 - a, x0 + 25 + i * 10, y0 + 140, i < nivel ? this.tema.tubo : MenuBackrooms.alfa(this.tema.tubo, 0.16F));
		}
		Texto.hud(g, latencia < 0 ? "SEÑAL --" : "SEÑAL · " + latencia + " MS", x0 + 70, y0 + 112, 20, 0.05F, this.tema.tubo);
		Texto.hud(g, "ERRANTES DENTRO " + errantes, x0 + 70, y0 + 132, 20, 0.05F, this.tema.tubo);
		if ((System.currentTimeMillis() / 800) % 2 == 0) {
			g.fill(x0 + 18, y0 + 160, x0 + 28, y0 + 170, ROJO);
		}
		Texto.hud(g, "LA CINTA SIGUE GRABANDO", x0 + 36, y0 + 156, 19, 0.12F, MenuBackrooms.alfa(this.tema.tubo, 0.85F));
	}
}
