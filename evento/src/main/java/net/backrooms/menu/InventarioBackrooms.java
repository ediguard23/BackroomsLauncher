package net.backrooms.menu;

import java.util.List;
import net.backrooms.evento.cliente.MisionesCliente;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix3x2fStack;

/**
 * Inventario (E) con el aspecto del evento: el equipo y la mochila en un
 * recuadro del HUD de la camara, sin mesa de crafteo ni libro de recetas, y
 * al lado la hoja de MISIONES con el detector de casetes.
 *
 * Usa el mismo InventoryMenu que el de Minecraft (los huecos estan donde el
 * servidor los espera); solo deja de dibujar y de atender los 5 del crafteo.
 * En creativo se sigue usando el de Minecraft (MenuBackrooms.sustituir).
 */
public class InventarioBackrooms extends AbstractContainerScreen<InventoryMenu> {
	private static final int PANEL = 176;
	private static final int SEPARACION = 6;

	private final Tema tema = Tema.actual();
	private final Player jugador;
	private float hojaEscala;
	private float xMouse;
	private float yMouse;

	public InventarioBackrooms(Player jugador) {
		super(jugador.inventoryMenu, jugador.getInventory(), Component.literal("Equipo"));
		this.jugador = jugador;
	}

	@Override
	protected void init() {
		// la hoja tan grande como deje la pantalla (se lee mejor), centrada con el panel
		float k = Math.min(Math.min(0.6F, (this.height - 16) / (float) HojaMisiones.ALTO), (this.width - PANEL - SEPARACION - 8) / (float) HojaMisiones.ANCHO);
		this.hojaEscala = k >= 0.25F ? k : 0;
		this.imageWidth = PANEL + (this.hojaEscala > 0 ? SEPARACION + (int) Math.ceil(HojaMisiones.ANCHO * this.hojaEscala) : 0);
		super.init();
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (this.jugador.hasInfiniteMaterials()) {
			this.minecraft.setScreen(new InventoryScreen(this.jugador));
		}
	}

	/** La hoja puede salir por arriba y por abajo del panel: pinchar en ella no es tirar el objeto. */
	@Override
	protected boolean hasClickedOutside(double mx, double my, int izq, int arriba) {
		if (this.hojaEscala > 0) {
			float alto = HojaMisiones.ALTO * this.hojaEscala;
			float hy = arriba + (this.imageHeight - alto) / 2.0F;
			if (mx >= izq + PANEL && mx < izq + this.imageWidth && my >= hy && my < hy + alto) {
				return false;
			}
		}
		return super.hasClickedOutside(mx, my, izq, arriba);
	}

	/** Los huecos del crafteo (resultado y 2x2) no existen en este inventario. */
	private static boolean oculto(Slot s) {
		return s.index >= InventoryMenu.RESULT_SLOT && s.index < InventoryMenu.CRAFT_SLOT_END;
	}

	@Override
	protected void renderSlots(GuiGraphics g, int mx, int my) {
		for (Slot s : this.menu.slots) {
			if (!oculto(s) && s.isActive()) {
				this.renderSlot(g, s, mx, my);
			}
		}
	}

	@Override
	protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
		if (w == 16 && h == 16) {
			for (Slot s : this.menu.slots) {
				if (oculto(s) && s.x == x && s.y == y) {
					return false;
				}
			}
		}
		return super.isHovering(x, y, w, h, mx, my);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		this.renderTooltip(g, mx, my);
		this.xMouse = mx;
		this.yMouse = my;
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		// los rotulos van en renderBg, con las fuentes del evento
	}

	@Override
	protected void renderBg(GuiGraphics g, float parcial, int mx, int my) {
		int x = this.leftPos;
		int y = this.topPos;
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		this.panel(g);
		p.popMatrix();
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.jugador);
		if (this.hojaEscala > 0) {
			float alto = HojaMisiones.ALTO * this.hojaEscala;
			p.pushMatrix();
			p.translate(x + PANEL + SEPARACION, y + (this.imageHeight - alto) / 2.0F);
			p.scale(this.hojaEscala, this.hojaEscala);
			HojaMisiones.dibujar(g, this.tema, this.jugador.getYRot());
			p.popMatrix();
		}
	}

	/** Recuadro del HUD con el equipo, la mochila y el estado del traje. */
	private void panel(GuiGraphics g) {
		int w = PANEL;
		int h = this.imageHeight;
		g.fill(0, 0, w, h, MenuBackrooms.alfa(this.tema.caja, 0.9F));
		for (int yy = 1; yy < h; yy += 2) {
			g.fill(0, yy, w, yy + 1, 0x12000000);
		}
		g.renderOutline(0, 0, w, h, MenuBackrooms.alfa(this.tema.tubo, 0.35F));
		// esquinas de visor
		int c = MenuBackrooms.alfa(this.tema.tubo, 0.8F);
		for (int[] e : new int[][] {{2, 2, 1, 1}, {w - 3, 2, -1, 1}, {2, h - 3, 1, -1}, {w - 3, h - 3, -1, -1}}) {
			g.fill(Math.min(e[0], e[0] + e[2] * 6), e[1], Math.max(e[0], e[0] + e[2] * 6) + 1, e[1] + 1, c);
			g.fill(e[0], Math.min(e[1], e[1] + e[3] * 6), e[0] + 1, Math.max(e[1], e[1] + e[3] * 6) + 1, c);
		}

		// retrato del explorador
		g.fill(25, 7, 76, 79, 0xAA000000);
		g.renderOutline(25, 7, 51, 72, MenuBackrooms.alfa(this.tema.tubo, 0.3F));

		for (Slot s : this.menu.slots) {
			if (!oculto(s)) {
				g.fill(s.x - 1, s.y - 1, s.x + 17, s.y + 17, 0x7A000000);
				g.renderOutline(s.x - 1, s.y - 1, 18, 18, MenuBackrooms.alfa(this.tema.tubo, 0.16F));
			}
		}
		// separacion entre la mochila y la barra rapida
		for (int xx = 8; xx < w - 8; xx += 4) {
			g.fill(xx, 137, xx + 2, 138, MenuBackrooms.alfa(this.tema.tubo, 0.2F));
		}
		this.estadoTraje(g);
	}

	/** Donde Minecraft pone el crafteo: el estado del traje y de la mision. */
	private void estadoTraje(GuiGraphics g) {
		int x = 98;
		Texto.hud(g, "TRAJE ANTI-RAD", x, 7, 10, 0.06F, this.tema.acento);
		boolean on = (System.currentTimeMillis() / 600) % 2 == 0;
		g.fill(x, 21, x + 4, 25, on ? 0xFF7BD66B : 0xFF2F5A29);
		Texto.hud(g, "SELLADO", x + 7, 18, 10, 0.08F, this.tema.tubo);
		for (int xx = x; xx < 170; xx += 3) {
			g.fill(xx, 32, xx + 1, 33, MenuBackrooms.alfa(this.tema.tubo, 0.25F));
		}
		List<TipoMision> misiones = MisionesCliente.misiones();
		int actual = MisionesCliente.actual();
		if (misiones.isEmpty()) {
			Texto.hud(g, "SIN EXPEDICIÓN", x, 37, 9, 0.06F, MenuBackrooms.alfa(this.tema.tubo, 0.55F));
			return;
		}
		if (actual >= misiones.size()) {
			Texto.hud(g, "MISIONES", x, 37, 9, 0.06F, MenuBackrooms.alfa(this.tema.tubo, 0.7F));
			Texto.hud(g, "COMPLETAS", x, 48, 13, 0.06F, this.tema.acento);
			return;
		}
		Texto.hud(g, "MISIÓN " + (actual + 1) + "/" + misiones.size(), x, 37, 9, 0.06F, MenuBackrooms.alfa(this.tema.tubo, 0.7F));
		if (misiones.get(actual) == TipoMision.CASETES) {
			Texto.hud(g, "CASETES " + MisionesCliente.casetes() + "/" + MisionesCliente.necesarios(), x, 48, 13, 0.04F, this.tema.tubo);
		} else {
			Texto.hud(g, "GRABAR", x, 48, 13, 0.06F, this.tema.tubo);
		}
	}
}
