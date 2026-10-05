package net.backrooms.menu.render;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Letras de 5x7 pixeles, como las del OSD de una videocamara, dibujadas con
 * rectangulos. Las usa la pantalla de carga: alli todavia no hay ninguna
 * fuente cargada (ni la de Minecraft ni las TTF del mod).
 */
public final class Osd {
	private static final Map<Character, String[]> GLIFOS = new HashMap<>();

	static {
		g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
		g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
		g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
		g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
		g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
		g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
		g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
		g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
		g('I', ".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.");
		g('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
		g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
		g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
		g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
		g('N', "#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#");
		g('Ñ', ".##.#", "#.##.", "#...#", "##..#", "#.#.#", "#..##", "#...#");
		g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
		g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
		g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
		g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
		g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
		g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
		g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
		g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
		g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#.");
		g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
		g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
		g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
		g('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
		g('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
		g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
		g('3', "#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###.");
		g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
		g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
		g('6', "..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###.");
		g('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
		g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
		g('9', ".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##..");
		g('.', ".....", ".....", ".....", ".....", ".....", ".##..", ".##..");
		g(',', ".....", ".....", ".....", ".....", ".##..", "..#..", ".#...");
		g(':', ".....", ".##..", ".##..", ".....", ".##..", ".##..", ".....");
		g('%', "##...", "##..#", "...#.", "..#..", ".#...", "#..##", "...##");
		g('/', ".....", "....#", "...#.", "..#..", ".#...", "#....", ".....");
		g('-', ".....", ".....", ".....", "#####", ".....", ".....", ".....");
		g('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
		g('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
		g('>', "#....", "##...", "###..", "####.", "###..", "##...", "#....");
	}

	private Osd() {
	}

	private static void g(char c, String... filas) {
		GLIFOS.put(c, filas);
	}

	/** Mayusculas y sin tildes (salvo la Ñ), que es lo que tiene la fuente. */
	public static String normalizar(String s) {
		StringBuilder out = new StringBuilder();
		for (char c : s.toUpperCase().toCharArray()) {
			if (c == 'Ñ' || GLIFOS.containsKey(c) || c == ' ') {
				out.append(c);
			} else {
				String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
				out.append(base.isEmpty() ? ' ' : base.charAt(0));
			}
		}
		return out.toString();
	}

	/** Ancho en pixeles de `s` con pixeles de tamano `p`. */
	public static int ancho(String s, int p) {
		return Math.max(0, s.length() * 6 * p - p);
	}

	public static int alto(int p) {
		return 7 * p;
	}

	/** Escribe `s` (ya normalizado) con pixeles de `p` x `p`. */
	public static void texto(GuiGraphics g, String s, int x, int y, int p, int color) {
		int cx = x;
		for (int i = 0; i < s.length(); i++) {
			String[] glifo = GLIFOS.get(s.charAt(i));
			if (glifo != null) {
				for (int fy = 0; fy < 7; fy++) {
					String fila = glifo[fy];
					// tramos seguidos de una fila en un solo rectangulo
					int desde = -1;
					for (int fx = 0; fx <= 5; fx++) {
						boolean on = fx < 5 && fila.charAt(fx) == '#';
						if (on && desde < 0) {
							desde = fx;
						} else if (!on && desde >= 0) {
							g.fill(cx + desde * p, y + fy * p, cx + fx * p, y + (fy + 1) * p, color);
							desde = -1;
						}
					}
				}
			}
			cx += 6 * p;
		}
	}

	/** Como texto(), con el corrimiento rojo/cian de una cinta gastada. */
	public static void textoCinta(GuiGraphics g, String s, int x, int y, int p, int alfa) {
		int d = Math.max(1, p / 2);
		texto(g, s, x + d, y, p, (Math.round(alfa * 0.6F) << 24) | 0xFF003C);
		texto(g, s, x - d, y, p, (Math.round(alfa * 0.5F) << 24) | 0x00C8FF);
		texto(g, s, x, y, p, (alfa << 24) | 0xFFFFFF);
	}
}
