package net.backrooms.menu;

import java.util.List;
import net.backrooms.evento.cliente.Eliminaciones;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

/**
 * La pantalla de muerte del evento, en vez de la de Minecraft («¡Has muerto!»,
 * puntuacion y reaparecer). Si el servidor te ha eliminado (te llega tu propio
 * aviso de Eliminado), no hay reaparecer: tu expedicion ha terminado y en unos
 * segundos llega el ban. Si el evento esta en modo «reaparecer», el boton de
 * tubo te devuelve a una zona nueva de la fase.
 *
 * Hereda de DeathScreen para que el juego la siga tratando como la pantalla de
 * muerte. La franja de arriba queda libre para la animacion de la eliminacion.
 */
public class PantallaMuerte extends DeathScreen {
	private static final float ANCHO = 1180.0F;
	private static final float ALTO = 720.0F;
	private static final int ROJO = 0xFFFF4A3D;
	private static final int AMARILLO = 0xFFF2E6A0;

	private final @Nullable Component causa;
	private final LocalPlayer jugador;
	private final long abierta = System.currentTimeMillis();
	private float escala;
	private float ox;
	private float oy;
	private BotonInvisible reaparecer;
	private BotonInvisible salir;
	private boolean pulsado;

	public PantallaMuerte(@Nullable Component causa, boolean hardcore, LocalPlayer jugador) {
		super(causa, hardcore, jugador);
		this.causa = causa;
		this.jugador = jugador;
	}

	@Override
	protected void init() {
		this.escala = Math.min(this.width / ANCHO, this.height / ALTO);
		this.ox = (this.width - ANCHO * this.escala) / 2.0F;
		this.oy = (this.height - ALTO * this.escala) / 2.0F;
		this.reaparecer = this.addRenderableWidget(new BotonInvisible(Component.literal("Reaparecer"), () -> {
			this.pulsado = true;
			this.jugador.respawn();
		}));
		this.salir = this.addRenderableWidget(new BotonInvisible(Component.literal("Salir del evento"), () -> {
			this.pulsado = true;
			this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
		}));
		this.colocar(this.reaparecer, 390, 470, 400, 76);
		this.colocar(this.salir, 440, 566, 300, 40);
		this.reaparecer.active = false;
		this.salir.active = false;
	}

	private void colocar(BotonInvisible b, float x, float y, float w, float h) {
		b.colocar(Math.round(this.ox + x * this.escala), Math.round(this.oy + y * this.escala), Math.round(w * this.escala), Math.round(h * this.escala));
	}

	@Override
	public void tick() {
		super.tick();
		// un segundo sin botones, como en Minecraft, para no pulsar sin querer
		boolean listo = System.currentTimeMillis() - this.abierta > 1000 && !this.pulsado;
		boolean eliminado = Eliminaciones.yoEliminado();
		this.reaparecer.visible = !eliminado;
		this.reaparecer.active = listo && !eliminado;
		this.salir.active = listo;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		int w = this.width;
		int h = this.height;
		g.fillGradient(0, 0, w, h, 0xC0160404, 0xE0080202);
		for (int y = 0; y < h; y += 2) {
			g.fill(0, y, w, y + 1, 0x22000000);
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		boolean eliminado = Eliminaciones.yoEliminado();
		for (BotonInvisible b : List.of(this.reaparecer, this.salir)) {
			boolean encima = b.isHovered() && b.active;
			if (encima && !b.encimaAntes) {
				Sonidos.ui(Tema.actual().encima, 0.45F);
				b.encimaDesde = System.currentTimeMillis();
			}
			b.encimaAntes = encima;
		}
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(this.ox, this.oy);
		p.scale(this.escala, this.escala);

		// HUD de la camara: la cinta parada
		g.fill(30, 17, 43, 30, ROJO);
		Texto.hud(g, "STOP", 52, 13, 24, 0.06F, ROJO);
		long s = (System.currentTimeMillis() - this.abierta) / 1000;
		Texto.hud(g, MenuBackrooms.dos(s / 60) + ":" + MenuBackrooms.dos(s % 60), 112, 13, 24, 0.06F, MenuBackrooms.alfa(0xFFEAD9A0, 0.8F));
		String sin = "SIN SEÑAL";
		Texto.hud(g, sin, 1148 - Texto.anchoHud(sin, 24, 0.06F), 13, 24, 0.06F, (System.currentTimeMillis() / 600) % 2 == 0 ? ROJO : MenuBackrooms.alfa(ROJO, 0.4F));

		// titulo con el corrimiento de color de la cinta
		String titulo = "HAS CAÍDO";
		float tam = 84;
		float ancho = Texto.anchoHud(titulo, tam, 0.12F);
		float x = 590 - ancho / 2.0F;
		float tiron = (System.currentTimeMillis() % 2600) > 2450 ? 6 : 0;
		Texto.hud(g, titulo, x + 4 + tiron, 270, tam, 0.12F, 0x8CFF1E46);
		Texto.hud(g, titulo, x - 4 - tiron, 270, tam, 0.12F, 0x7300D2FF);
		Texto.hud(g, titulo, x, 270, tam, 0.12F, 0xFFFFE8E0);

		// la causa, con la letra normal del juego (trae los nombres del servidor)
		if (this.causa != null) {
			float k = 1.8F;
			List<FormattedCharSequence> lineas = this.font.split(this.causa, (int) (760 / k));
			float y = 380;
			for (FormattedCharSequence l : lineas.subList(0, Math.min(3, lineas.size()))) {
				p.pushMatrix();
				p.translate(590 - this.font.width(l) * k / 2, y);
				p.scale(k, k);
				g.drawString(this.font, l, 0, 0, 0xFFE8D8D0, true);
				p.popMatrix();
				y += 11 * k;
			}
		}

		if (eliminado) {
			String fin = "TU EXPEDICIÓN HA TERMINADO";
			Texto.hud(g, fin, 590 - Texto.anchoHud(fin, 26, 0.12F) / 2.0F, 448, 26, 0.12F, AMARILLO);
			String nota = "La señal se cortará en unos segundos. Gracias por bajar al Nivel 0.";
			Texto.maquina(g, nota, 590 - Texto.anchoMaquina(nota, 14, 0.02F) / 2.0F, 498, 14, 0.02F, MenuBackrooms.alfa(0xFFEAD9A0, 0.75F));
		} else {
			MenuBackrooms.botonTubo(g, this.reaparecer, 390, 470, 790, 546, "REAPARECER", "...");
		}
		MenuBackrooms.botonHud(g, this.salir, 440, 566, 300, 40, "SALIR DEL EVENTO");
		MenuBackrooms.fecha(g, MenuBackrooms.alfa(0xFFEAD9A0, 0.8F));
		p.popMatrix();

		// solo los botones: el render de DeathScreen pintaria su titulo y la puntuacion
		this.reaparecer.render(g, mx, my, parcial);
		this.salir.render(g, mx, my, parcial);
	}
}
