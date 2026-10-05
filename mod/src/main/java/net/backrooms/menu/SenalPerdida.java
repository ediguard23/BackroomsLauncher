package net.backrooms.menu;

import java.util.List;
import net.backrooms.menu.render.Piscinas;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

/**
 * Sustituye a la pantalla de desconexion de Minecraft (que ofrecia volver a la
 * lista de servidores): muestra el motivo con el estilo del evento y deja
 * reintentar o volver al menu.
 */
public class SenalPerdida extends Screen {
	private final Component motivo;
	private final Evento evento = Evento.cargar();
	private BotonInvisible reintentar;
	private BotonInvisible volver;
	private float escala;
	private float ox;
	private float oy;

	public SenalPerdida(Component motivo) {
		super(Component.literal("Señal perdida"));
		this.motivo = motivo;
	}

	@Override
	protected void init() {
		this.escala = Math.min(this.width / 1180.0F, this.height / 720.0F);
		this.ox = (this.width - 1180.0F * this.escala) / 2.0F;
		this.oy = (this.height - 720.0F * this.escala) / 2.0F;
		this.reintentar = this.addRenderableWidget(new BotonInvisible(Component.literal("Reintentar"),
			() -> MenuBackrooms.conectar(new MenuBackrooms(), this.minecraft, this.evento)));
		this.volver = this.addRenderableWidget(new BotonInvisible(Component.literal("Volver"), () -> this.minecraft.setScreen(new MenuBackrooms())));
		this.reintentar.active = this.evento.tieneServidor();
		this.colocar(this.reintentar, 380, 500, 200, 42);
		this.colocar(this.volver, 600, 500, 200, 42);
	}

	private void colocar(BotonInvisible b, float x, float y, float w, float h) {
		b.colocar(Math.round(this.ox + x * this.escala), Math.round(this.oy + y * this.escala), Math.round(w * this.escala), Math.round(h * this.escala));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		Piscinas.dibujar(g, this.width, this.height);
		g.fill(0, 0, this.width, this.height, 0xB8041A20);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		for (BotonInvisible b : List.of(this.reintentar, this.volver)) {
			boolean encima = b.isHovered() && b.active;
			if (encima && !b.encimaAntes) {
				Sonidos.ui(Sonidos.GOTA, 0.45F);
			}
			b.encimaAntes = encima;
		}
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(this.ox, this.oy);
		p.scale(this.escala, this.escala);

		boolean punto = (System.currentTimeMillis() / 550) % 2 == 0;
		if (punto) {
			g.fill(30, 17, 43, 30, 0xFFFF4A3D);
		}
		Texto.hud(g, "REC", 52, 13, 24, 0.06F, 0xFFEAFCFF);
		Texto.hud(g, "SIN SEÑAL", 1148 - Texto.anchoHud("SIN SEÑAL", 24, 0.06F), 13, 24, 0.06F, 0xFFFF6A5C);

		String titulo = "SEÑAL PERDIDA";
		float ancho = Texto.anchoHud(titulo, 72, 0.12F);
		float glitch = (System.currentTimeMillis() % 3000) > 2850 ? 4 : 0;
		Texto.hud(g, titulo, 590 - ancho / 2 + 3 + glitch, 220, 72, 0.12F, 0x8CFF1E46);
		Texto.hud(g, titulo, 590 - ancho / 2 - 3 - glitch, 220, 72, 0.12F, 0x7300D2FF);
		Texto.hud(g, titulo, 590 - ancho / 2, 220, 72, 0.12F, 0xFFEAFCFF);
		String sub = "El agua se lo ha llevado. Esto es lo último que llegó:";
		Texto.maquina(g, sub, 590 - Texto.anchoMaquina(sub, 15, 0.02F) / 2, 318, 15, 0.02F, 0xBFEAFCFF);

		// el motivo con la fuente normal: puede traer colores del servidor
		float k = 1.6F;
		List<FormattedCharSequence> lineas = this.font.split(this.motivo, (int) (760 / k));
		float y = 360;
		for (FormattedCharSequence l : lineas.subList(0, Math.min(5, lineas.size()))) {
			p.pushMatrix();
			p.translate(590 - this.font.width(l) * k / 2, y);
			p.scale(k, k);
			g.drawString(this.font, l, 0, 0, 0xFFFFFFFF, true);
			p.popMatrix();
			y += 11 * k;
		}

		MenuBackrooms.botonHud(g, this.reintentar, 380, 500, 200, 42, "[ REINTENTAR ]");
		MenuBackrooms.botonHud(g, this.volver, 600, 500, 200, 42, "[ VOLVER ]");
		p.popMatrix();
		super.render(g, mx, my, parcial);
	}
}
