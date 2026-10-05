package net.backrooms.menu.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

/**
 * Texto con las fuentes del launcher: VT323 (HUD de camara) y Special Elite
 * (maquina de escribir). Los tamanos se dan en pixeles de diseno, igual que
 * el font-size del CSS del launcher, y se escala a mano porque Minecraft
 * rasteriza cada fuente TTF a un solo tamano.
 */
public final class Texto {
	public static final FontDescription HUD = new FontDescription.Resource(Identifier.fromNamespaceAndPath("backrooms", "hud"));
	public static final FontDescription MAQUINA = new FontDescription.Resource(Identifier.fromNamespaceAndPath("backrooms", "maquina"));
	/** Tamano ("size") con el que se rasteriza cada fuente en assets/backrooms/font/. */
	private static final float TAM_HUD = 20.0F;
	private static final float TAM_MAQUINA = 16.0F;
	/** VT323 se dibuja mas alto que su caja en CSS: se baja esta fraccion del tamano. */
	private static final float BAJADA_HUD = 0.2F;

	private Texto() {
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Component hud(String s) {
		return Component.literal(s).withStyle(st -> st.withFont(HUD));
	}

	public static Component maquina(String s) {
		return Component.literal(s).withStyle(st -> st.withFont(MAQUINA));
	}

	/**
	 * Escribe en VT323 a `tam` px de diseno, con `espaciado` en em entre letras
	 * (el letter-spacing del CSS). Devuelve el ancho dibujado.
	 */
	public static float hud(GuiGraphics g, String s, float x, float y, float tam, float espaciado, int color) {
		return letras(g, s, x, y + tam * BAJADA_HUD, tam / TAM_HUD, espaciado * TAM_HUD, color, true);
	}

	public static float anchoHud(String s, float tam, float espaciado) {
		return medir(s, tam / TAM_HUD, espaciado * TAM_HUD, true);
	}

	public static float maquina(GuiGraphics g, String s, float x, float y, float tam, float espaciado, int color) {
		return letras(g, s, x, y, tam / TAM_MAQUINA, espaciado * TAM_MAQUINA, color, false);
	}

	public static float anchoMaquina(String s, float tam, float espaciado) {
		return medir(s, tam / TAM_MAQUINA, espaciado * TAM_MAQUINA, false);
	}

	/** Parrafo en Special Elite ajustado a `ancho` px. Devuelve la altura usada. */
	public static float parrafo(GuiGraphics g, String s, float x, float y, float tam, float ancho, float interlineado, int color) {
		float k = tam / TAM_MAQUINA;
		List<FormattedCharSequence> lineas = font().split(maquina(s), (int) (ancho / k));
		float yy = y;
		for (FormattedCharSequence linea : lineas) {
			Matrix3x2fStack p = g.pose();
			p.pushMatrix();
			p.translate(x, yy);
			p.scale(k, k);
			g.drawString(font(), linea, 0, 0, color, false);
			p.popMatrix();
			yy += tam * interlineado;
		}
		return yy - y;
	}

	private static float letras(GuiGraphics g, String s, float x, float y, float k, float extra, int color, boolean hud) {
		Font f = font();
		Matrix3x2fStack p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(k, k);
		float cx = 0;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			String c = new String(Character.toChars(cp));
			Component comp = hud ? hud(c) : maquina(c);
			g.drawString(f, comp, Math.round(cx), 0, color, false);
			cx += f.width(comp) + extra;
			i += Character.charCount(cp);
		}
		p.popMatrix();
		return (cx - extra) * k;
	}

	private static float medir(String s, float k, float extra, boolean hud) {
		Font f = font();
		float cx = 0;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			String c = new String(Character.toChars(cp));
			cx += f.width(hud ? hud(c) : maquina(c)) + extra;
			i += Character.charCount(cp);
		}
		return (cx - extra) * k;
	}
}
