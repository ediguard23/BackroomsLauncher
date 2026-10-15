package net.backrooms.evento.cliente.cinematica;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.InputStream;
import java.io.Reader;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * El lienzo de la cinematica del /start: una imagen de 512 x 512 que el shader
 * lee como Sampler1 (cinematica.fsh, con texelFetch). Se pinta al empezar cada
 * cinematica porque lleva el nombre del jugador:
 *
 *  - la hoja del consentimiento informado (320 x 456, arriba a la izquierda),
 *    escrita con la letra de Minecraft (las mismas imagenes de la fuente que
 *    el jugador tiene cargadas, tildes incluidas);
 *  - la firma: una mascara con el orden en que la punta del boligrafo pasa
 *    por cada punto de tinta (asi el shader la va descubriendo trazo a trazo)
 *    y el recorrido de la punta, muestra a muestra;
 *  - los carteles de la sala y del pasillo, la pizarra y lo que va impreso en
 *    el boligrafo.
 *
 * Las posiciones de todo esto estan repetidas en cinematica.fsh (seccion
 * "lienzo"): si se mueve algo aqui hay que moverlo alli.
 */
public final class LienzoCinematica {
	public static final int LADO = 512;
	static final int HOJA_W = 320;
	static final int HOJA_H = 456;
	/** Fila de la hoja donde va la linea de la firma. */
	static final int LINEA_FIRMA = 404;
	/** Recuadro de la firma en la hoja; su mascara va en (MASCARA_X, MASCARA_Y). */
	static final int FIRMA_X = 22;
	static final int FIRMA_Y = 348;
	static final int FIRMA_W = 184;
	static final int FIRMA_H = 60;
	static final int MASCARA_X = 324;
	static final int MASCARA_Y = 0;
	/** El recorrido de la punta: la muestra i va en (i % 512, RECORRIDO_Y + i / 512). */
	static final int RECORRIDO_Y = 504;
	static final int MAX_MUESTRAS = 1024;
	// carteles: {x, y, ancho, alto}
	static final int[] PLACA = {324, 64, 128, 32};
	static final int[] PIZARRA = {324, 100, 176, 88};
	static final int[] ARCHIVO = {324, 192, 88, 14};
	static final int[] LABORATORIO = {416, 192, 88, 14};
	static final int[] SALA = {324, 210, 88, 14};
	static final int[] ASCENSOR = {416, 210, 88, 14};
	static final int[] BOLI = {324, 228, 40, 8};

	private static final int PAPEL = 0xFFF3F0E7;
	private static final int TINTA = 0xFF26272D;
	private static final int GRIS = 0xFF726D65;
	private static final int TEAL = 0xFF2E6E66;

	private static DynamicTexture textura;
	private static NativeImage img;
	private static final Map<Integer, Glifo> GLIFOS = new HashMap<>();

	private record Glifo(NativeImage img, int x, int y, int w, int h, float escala, int ascent, int ancho) {
	}

	private LienzoCinematica() {
	}

	/** Vuelve a pintar el lienzo con el nombre de este jugador (al empezar la cinematica). */
	public static void preparar(String nick) {
		cargarFuente();
		if (textura == null) {
			img = new NativeImage(LADO, LADO, true);
			textura = new DynamicTexture(() -> "backrooms_evento cinematica lienzo", img);
		}
		img.fillRect(0, 0, LADO, LADO, 0);
		try {
			hoja(nick);
			firma(nick);
			carteles();
		} catch (Exception e) {
			BackroomsEvento.LOG.warn("Cinematica: no se pudo pintar el contrato", e);
		}
		textura.upload();
	}

	public static GpuTextureView vista() {
		if (textura == null) {
			preparar("Participante");
		}
		return textura.getTextureView();
	}

	public static GpuSampler muestreo() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
	}

	/* --------------------------------------------------------------- la hoja */

	private static void hoja(String nick) {
		Random r = new Random(nick.hashCode());
		// el papel, con algo de fibra y el borde un poco mas oscuro
		for (int y = 0; y < HOJA_H; y++) {
			for (int x = 0; x < HOJA_W; x++) {
				int v = r.nextInt(7) - 3;
				int c = sumar(PAPEL, v, v, v - 1);
				if (x == 0 || y == 0 || x == HOJA_W - 1 || y == HOJA_H - 1) {
					c = 0xFFE0DACB;
				}
				img.setPixel(x, y, c);
			}
		}
		String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT));
		String exp = String.format(Locale.ROOT, "S-%04d", Math.abs(nick.hashCode() % 10000));
		// membrete: el logo (un umbral, una puerta abierta), el nombre y la referencia
		rect(22, 9, 13, 16, TEAL);
		rect(24, 11, 9, 14, PAPEL);
		rect(26, 13, 5, 12, 0xFF9CC9C2);
		rect(20, 24, 17, 2, TEAL);
		texto("PROYECTO UMBRAL", 41, 18, TEAL, 1, true);
		texto("Instituto de Estudios de Percepción", 41, 29, GRIS, 1, false);
		derecha("Ref. PU-0047/" + exp.substring(2), 298, 18, GRIS, false);
		derecha("Hoja 1 de 1", 298, 29, GRIS, false);
		rect(22, 35, 276, 1, TEAL);
		rect(22, 37, 276, 1, 0xFFCFD8D4);
		// titulo
		centrado("CONSENTIMIENTO", 59, TINTA, 2, true);
		centrado("INFORMADO", 77, TINTA, 2, true);
		// datos
		int x = 22 + texto("Participante: ", 22, 95, TINTA, 1, false);
		texto(nick, x, 95, TINTA, 1, true);
		derecha("Expediente: " + exp, 298, 95, TINTA, false);
		texto("Fecha: " + fecha, 22, 106, TINTA, 1, false);
		derecha("Sala de reuniones 3", 298, 106, TINTA, false);
		rect(22, 111, 276, 1, 0xFFD6D0C2);
		// las clausulas
		int y = 125;
		y = seccion("1. Objeto del estudio", "El participante acepta formar parte de un estudio experimental sobre nuevas "
			+ "tecnologías de realidad y percepción, dirigido por el Proyecto Umbral.", y);
		y = seccion("2. Duración", "El estudio termina cuando el participante alcanza la salida designada. "
			+ "No existe otra forma de abandonarlo.", y);
		y = seccion("3. Registro", "El participante será observado y grabado en todo momento, también cuando "
			+ "crea estar solo.", y);
		// la 14.c, subrayada con rotulador por alguien
		String c14 = "El participante acepta cualquier consecuencia física, psicológica o perceptiva derivada "
			+ "del experimento. Al firmar, deja de ser considerado ciudadano a efectos del estudio y pasa a ser "
			+ "considerado sujeto.";
		List<String> lineas = partir(c14, 270);
		int alto = 11 + lineas.size() * 10;
		for (int i = 0; i < alto + 2; i++) {
			int x0 = 20 + r.nextInt(2);
			int x1 = 300 - r.nextInt(3);
			for (int xx = x0; xx < x1; xx++) {
				multiplicar(xx, y - 9 + i, 0xFFF6E37A, 0.8F);
			}
		}
		rect(18, y - 9, 2, alto + 2, 0xFFB79A2C);
		texto("Cláusula 14.c", 25, y, TINTA, 1, true);
		y += 11;
		for (String l : lineas) {
			texto(l, 25, y, TINTA, 1, false);
			y += 10;
		}
		y += 8;
		texto("Lugar y fecha: Sala de reuniones 3, " + fecha, 22, y, TINTA, 1, false);
		// la firma (la del participante va en firma(); la del investigador ya esta)
		texto("X", 24, LINEA_FIRMA - 2, GRIS, 1, false);
		rect(22, LINEA_FIRMA, 172, 1, TINTA);
		texto("Firma del participante", 22, LINEA_FIRMA + 10, GRIS, 1, false);
		rect(212, LINEA_FIRMA, 86, 1, TINTA);
		texto("Por el Proyecto Umbral", 212, LINEA_FIRMA + 10, GRIS, 1, false);
		garabato(r, 216, LINEA_FIRMA - 6);
		sello(268, LINEA_FIRMA - 20);
		texto("Documento confidencial. No se entregan copias", 22, 432, GRIS, 1, false);
		texto("al participante.", 22, 442, GRIS, 1, false);
		derecha("PU-CI 3.1", 298, 442, GRIS, false);
	}

	private static int seccion(String titulo, String cuerpo, int y) {
		texto(titulo, 22, y, TINTA, 1, true);
		y += 11;
		for (String l : partir(cuerpo, 276)) {
			texto(l, 22, y, TINTA, 1, false);
			y += 10;
		}
		return y + 6;
	}

	/** La firma del investigador: unos lazos rapidos con boligrafo negro. */
	private static void garabato(Random r, int x0, int base) {
		float px = x0;
		float py = base;
		for (int i = 0; i <= 140; i++) {
			float u = i / 140.0F;
			float x = x0 + u * 72 + (float) Math.sin(u * 31.0) * 3.5F;
			float y = base - (float) Math.abs(Math.sin(u * 15.5)) * (9 - u * 5) + u * 3;
			linea(px, py, x, y, 0xFF1C1C22);
			px = x;
			py = y;
		}
		linea(x0 - 2, base + 4, x0 + 78, base + 1, 0xFF1C1C22);
	}

	/** El sello de "aprobado": dos aros de tinta morada, medio gastados. */
	private static void sello(int cx, int cy) {
		Random r = new Random(7);
		for (int y = -21; y <= 21; y++) {
			for (int x = -21; x <= 21; x++) {
				double d = Math.sqrt(x * x + y * y);
				boolean aro = (d > 18.5 && d < 20.5) || (d > 14.5 && d < 15.6);
				if (aro && r.nextFloat() > 0.18F) {
					mezclar(cx + x, cy + y, 0xFF5B4E9A, 0.75F);
				}
			}
		}
		int w = ancho("UMBRAL", 1, true);
		texto("UMBRAL", cx - w / 2, cy + 3, 0xC05B4E9A, 1, true);
	}

	/* --------------------------------------------------------------- la firma */

	/**
	 * El nombre del jugador escrito a mano: las letras de la fuente, mas grandes e
	 * inclinadas, recorridas columna a columna (arriba y abajo, como un boligrafo),
	 * y una rubrica debajo. Cada muestra del recorrido deja un punto de tinta; la
	 * mascara guarda que muestra lo pinto primero.
	 */
	private static void firma(String nick) {
		List<float[]> rec = new ArrayList<>();
		int[][] orden = new int[FIRMA_H][FIRMA_W];
		float[][] fuerza = new float[FIRMA_H][FIRMA_W];
		for (int[] fila : orden) {
			Arrays.fill(fila, -1);
		}
		int ancho1 = 0;
		for (int cp : nick.codePoints().toArray()) {
			ancho1 += avance(cp, false);
		}
		float s = Math.max(1.0F, Math.min(2.0F, 150.0F / Math.max(1, ancho1)));
		float base = LINEA_FIRMA - 7;
		float x0 = FIRMA_X + 12;
		float cursor = x0;
		int col = 0;
		for (int cp : nick.codePoints().toArray()) {
			Glifo g = glifo(cp);
			if (g != null) {
				for (int cx = 0; cx < g.w; cx++) {
					List<Integer> filas = new ArrayList<>();
					for (int cy = 0; cy < g.h; cy++) {
						if ((g.img.getPixel(g.x + cx, g.y + cy) >>> 24) > 127) {
							filas.add(cy);
						}
					}
					if (filas.isEmpty()) {
						continue;
					}
					if ((col++ & 1) == 1) {
						java.util.Collections.reverse(filas);
					}
					for (int cy : filas) {
						float fx = cursor + (cx * g.escala + 0.5F) * s;
						float fy = base - g.ascent * s + (cy * g.escala + 0.5F) * s;
						fx += (base - fy) * 0.3F;
						punto(rec, orden, fuerza, fx, fy, s, 2.4F * s + 0.6F);
					}
				}
			}
			cursor += avance(cp, false) * s;
		}
		// la rubrica: una linea que sale por debajo del nombre y vuelve con un gancho
		float ux0 = x0 - 6;
		float ux1 = Math.min(FIRMA_X + FIRMA_W - 8, cursor + 10);
		for (int i = 0; i <= 90; i++) {
			float u = i / 90.0F;
			float x = ux0 + (ux1 - ux0) * u;
			float y = base + 4.5F - (float) Math.sin(u * Math.PI) * 2.5F - u * 2.0F;
			punto(rec, orden, fuerza, x, y, 1.1F, 3.0F);
		}
		for (int i = 1; i <= 14; i++) {
			float u = i / 14.0F;
			punto(rec, orden, fuerza, ux1 - u * 7, base + 0.5F - 1.5F - u * 6, 1.1F, 3.0F);
		}
		int n = Math.min(rec.size(), MAX_MUESTRAS);
		for (int i = 0; i < n; i++) {
			float[] m = rec.get(i);
			int xq = Math.round(m[0] * 8);
			int yq = Math.round(m[1] * 8);
			int rr = (xq >> 8) | (m[2] > 0 ? 0x80 : 0);
			img.setPixel(i % LADO, RECORRIDO_Y + i / LADO, (yq & 255) << 24 | rr << 16 | (xq & 255) << 8 | (yq >> 8));
		}
		img.setPixel(0, LADO - 1, 0xFF000000 | (n >> 8) << 16 | (n & 255) << 8);
		for (int y = 0; y < FIRMA_H; y++) {
			for (int x = 0; x < FIRMA_W; x++) {
				int o = orden[y][x];
				if (o >= 0 && o < n) {
					int b = Math.round(Math.min(1.0F, fuerza[y][x]) * 255);
					img.setPixel(MASCARA_X + x, MASCARA_Y + y, 0xFF000000 | (o >> 8) << 16 | (o & 255) << 8 | b);
				}
			}
		}
	}

	/** Una muestra del recorrido en (x, y) de la hoja y su punto de tinta. */
	private static void punto(List<float[]> rec, int[][] orden, float[][] fuerza, float x, float y, float grosor, float salto) {
		boolean levanta = false;
		if (!rec.isEmpty()) {
			float[] u = rec.get(rec.size() - 1);
			levanta = Math.hypot(x - u[0], y - u[1]) > salto;
		}
		int i = rec.size();
		rec.add(new float[] {x, y, levanta ? 1 : 0});
		float rad = grosor * 0.62F + 0.35F;
		for (int py = (int) Math.floor(y - rad - 1); py <= Math.ceil(y + rad + 1); py++) {
			for (int px = (int) Math.floor(x - rad - 1); px <= Math.ceil(x + rad + 1); px++) {
				int mx = px - FIRMA_X;
				int my = py - FIRMA_Y;
				if (mx < 0 || my < 0 || mx >= FIRMA_W || my >= FIRMA_H) {
					continue;
				}
				double d = Math.hypot(px + 0.5 - x, py + 0.5 - y);
				if (d > rad) {
					continue;
				}
				if (orden[my][mx] < 0) {
					orden[my][mx] = i;
				}
				fuerza[my][mx] = Math.max(fuerza[my][mx], (float) (1.15 - d / rad * 0.45));
			}
		}
	}

	/* ------------------------------------------------------------- carteles */

	private static void carteles() {
		// la placa de la sala
		int[] p = PLACA;
		rect(p[0], p[1], p[2], p[3], 0xFF8FBDB6);
		rect(p[0] + 1, p[1] + 1, p[2] - 2, p[3] - 2, 0xFF2B3536);
		centradoEn("PROYECTO UMBRAL", p, p[1] + 14, 0xFFE8F2EF, true);
		centradoEn("SALA DE REUNIONES 3", p, p[1] + 26, 0xFF8FBDB6, false);
		pizarra();
		cartel(ARCHIVO, "ARCHIVO");
		cartel(LABORATORIO, "LABORATORIO 2");
		cartel(SALA, "SALA 3");
		cartel(ASCENSOR, "ASCENSOR");
		int[] b = BOLI;
		rect(b[0], b[1], b[2], b[3], 0xFFF2F2EF);
		centradoEn("UMBRAL", b, b[1] + 7, 0xFF23379A, false);
	}

	private static void cartel(int[] c, String s) {
		rect(c[0], c[1], c[2], c[3], 0xFF1E2324);
		rect(c[0] + 1, c[1] + 1, c[2] - 2, c[3] - 2, 0xFF353C3D);
		centradoEn(s, c, c[1] + 10, 0xFFEDEDE6, false);
	}

	/** La pizarra de la sala: el plan del estudio, a rotulador. */
	private static void pizarra() {
		int[] p = PIZARRA;
		int x0 = p[0];
		int y0 = p[1];
		Random r = new Random(3);
		for (int y = 0; y < p[3]; y++) {
			for (int x = 0; x < p[2]; x++) {
				int v = r.nextInt(4);
				img.setPixel(x0 + x, y0 + y, sumar(0xFFF1F3F0, -v, -v, -v));
			}
		}
		// restos de algo que se borro
		for (int i = 0; i < 9; i++) {
			int cx = x0 + 10 + r.nextInt(150);
			int cy = y0 + 10 + r.nextInt(70);
			for (int k = 0; k < 40; k++) {
				multiplicar(cx + r.nextInt(16), cy + r.nextInt(5), 0xFFDADDE0, 0.6F);
			}
		}
		int azul = 0xFF2546A8;
		int negro = 0xFF22252B;
		int rojo = 0xFFC0362C;
		int amarillo = 0xFFE3CB55;
		trazo(texto("FASE 1: OBSERVACIÓN", x0 + 6, y0 + 12, azul, 1, true), x0 + 6, y0 + 14, azul);
		// el Nivel 0: salas amarillas conectadas
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 4; i++) {
				int sx = x0 + 8 + i * 21;
				int sy = y0 + 22 + j * 19;
				rect(sx, sy, 17, 15, amarillo);
				marco(sx, sy, 17, 15, negro);
				if (i < 3) {
					rect(sx + 17, sy + 6, 4, 2, negro);
				}
				if (j == 0 && i != 1) {
					rect(sx + 7, sy + 15, 2, 4, negro);
				}
			}
		}
		texto("NIVEL 0", x0 + 30, y0 + 72, negro, 1, false);
		// la flecha hacia una salida que nadie ha visto
		for (int k = 0; k < 2; k++) {
			linea(x0 + 94, y0 + 40 + k, x0 + 126, y0 + 40 + k, rojo);
			linea(x0 + 126, y0 + 40 + k, x0 + 120, y0 + 34 + k, rojo);
			linea(x0 + 126, y0 + 40 + k, x0 + 120, y0 + 46 + k, rojo);
		}
		texto("SALIDA?", x0 + 130, y0 + 44, rojo, 1, true);
		marco(x0 + 127, y0 + 32, 46, 16, rojo);
		texto("SUJETOS: 12/??", x0 + 104, y0 + 72, negro, 1, false);
		trazo(texto("GRABARLO TODO", x0 + 98, y0 + 84, rojo, 1, true), x0 + 98, y0 + 86, rojo);
	}

	/** Subraya lo que se acaba de escribir (ancho w desde x). */
	private static void trazo(int w, int x, int y, int c) {
		rect(x, y, w - 1, 1, c);
	}

	/* --------------------------------------------------------------- letras */

	private static void cargarFuente() {
		if (!GLIFOS.isEmpty()) {
			return;
		}
		ResourceManager rm = Minecraft.getInstance().getResourceManager();
		try (Reader rd = rm.openAsReader(Identifier.withDefaultNamespace("font/include/default.json"))) {
			JsonObject raiz = JsonParser.parseReader(rd).getAsJsonObject();
			for (JsonElement e : raiz.getAsJsonArray("providers")) {
				JsonObject p = e.getAsJsonObject();
				String archivo = p.has("file") ? p.get("file").getAsString() : "";
				if (!"bitmap".equals(p.get("type").getAsString())
					|| !(archivo.endsWith("ascii.png") || archivo.endsWith("accented.png") || archivo.endsWith("nonlatin_european.png"))) {
					continue;
				}
				int altura = p.has("height") ? p.get("height").getAsInt() : 8;
				int ascent = p.get("ascent").getAsInt();
				JsonArray filas = p.getAsJsonArray("chars");
				NativeImage im;
				try (InputStream in = rm.open(Identifier.parse(archivo).withPrefix("textures/"))) {
					im = NativeImage.read(in);
				}
				int columnas = filas.get(0).getAsString().codePointCount(0, filas.get(0).getAsString().length());
				int cw = im.getWidth() / columnas;
				int ch = im.getHeight() / filas.size();
				for (int f = 0; f < filas.size(); f++) {
					int[] cps = filas.get(f).getAsString().codePoints().toArray();
					for (int c = 0; c < cps.length; c++) {
						if (cps[c] == 0 || GLIFOS.containsKey(cps[c])) {
							continue;
						}
						int ancho = 0;
						for (int x = cw - 1; x >= 0 && ancho == 0; x--) {
							for (int y = 0; y < ch; y++) {
								if ((im.getPixel(c * cw + x, f * ch + y) >>> 24) > 0) {
									ancho = x + 1;
									break;
								}
							}
						}
						GLIFOS.put(cps[c], new Glifo(im, c * cw, f * ch, cw, ch, (float) altura / ch, ascent, ancho));
					}
				}
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.warn("Cinematica: no se pudo leer la fuente", e);
		}
	}

	private static Glifo glifo(int cp) {
		Glifo g = GLIFOS.get(cp);
		if (g == null && cp > 127) {
			// sin ese caracter: la letra sin tilde
			String s = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
			if (!s.isEmpty()) {
				g = GLIFOS.get(s.codePointAt(0));
			}
		}
		return g;
	}

	private static int avance(int cp, boolean negrita) {
		if (cp == ' ') {
			return 4;
		}
		Glifo g = glifo(cp);
		if (g == null) {
			return 6;
		}
		return Math.round(g.ancho * g.escala) + 1 + (negrita ? 1 : 0);
	}

	private static int ancho(String s, int k, boolean negrita) {
		int w = 0;
		for (int cp : s.codePoints().toArray()) {
			w += avance(cp, negrita) * k;
		}
		return w;
	}

	/** Escribe s con la base en y; devuelve el ancho. k: pixeles por pixel de letra. */
	private static int texto(String s, int x, int y, int color, int k, boolean negrita) {
		int x0 = x;
		for (int cp : s.codePoints().toArray()) {
			Glifo g = glifo(cp);
			if (g != null && cp != ' ') {
				for (int cy = 0; cy < g.h; cy++) {
					for (int cx = 0; cx < g.w; cx++) {
						if ((g.img.getPixel(g.x + cx, g.y + cy) >>> 24) <= 127) {
							continue;
						}
						int fx = x + Math.round(cx * g.escala) * k;
						int fy = y - g.ascent * k + Math.round(cy * g.escala) * k;
						for (int b = 0; b <= (negrita ? k : 0); b += Math.max(1, k)) {
							for (int dy = 0; dy < k; dy++) {
								for (int dx = 0; dx < k; dx++) {
									mezclar(fx + dx + b, fy + dy, color, (color >>> 24) / 255.0F);
								}
							}
						}
					}
				}
			}
			x += avance(cp, negrita) * k;
		}
		return x - x0;
	}

	private static void derecha(String s, int x, int y, int color, boolean negrita) {
		texto(s, x - ancho(s, 1, negrita), y, color, 1, negrita);
	}

	private static void centrado(String s, int y, int color, int k, boolean negrita) {
		texto(s, HOJA_W / 2 - ancho(s, k, negrita) / 2, y, color, k, negrita);
	}

	private static void centradoEn(String s, int[] c, int y, int color, boolean negrita) {
		texto(s, c[0] + c[2] / 2 - ancho(s, 1, negrita) / 2, y, color, 1, negrita);
	}

	/** Parte el texto en lineas de como mucho `max` pixeles. */
	private static List<String> partir(String s, int max) {
		List<String> lineas = new ArrayList<>();
		StringBuilder l = new StringBuilder();
		for (String p : s.split(" ")) {
			String prueba = l.isEmpty() ? p : l + " " + p;
			if (ancho(prueba, 1, false) > max && !l.isEmpty()) {
				lineas.add(l.toString());
				l = new StringBuilder(p);
			} else {
				l = new StringBuilder(prueba);
			}
		}
		if (!l.isEmpty()) {
			lineas.add(l.toString());
		}
		return lineas;
	}

	/* -------------------------------------------------------------- pixeles */

	private static void rect(int x, int y, int w, int h, int c) {
		for (int j = y; j < y + h; j++) {
			for (int i = x; i < x + w; i++) {
				mezclar(i, j, c, (c >>> 24) / 255.0F);
			}
		}
	}

	private static void marco(int x, int y, int w, int h, int c) {
		rect(x, y, w, 1, c);
		rect(x, y + h - 1, w, 1, c);
		rect(x, y, 1, h, c);
		rect(x + w - 1, y, 1, h, c);
	}

	private static void linea(float x0, float y0, float x1, float y1, int c) {
		int n = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))) + 1;
		for (int i = 0; i <= n; i++) {
			float u = (float) i / n;
			mezclar(Math.round(x0 + (x1 - x0) * u), Math.round(y0 + (y1 - y0) * u), c, (c >>> 24) / 255.0F);
		}
	}

	private static void mezclar(int x, int y, int c, float a) {
		if (x < 0 || y < 0 || x >= LADO || y >= LADO) {
			return;
		}
		int o = img.getPixel(x, y);
		int r = Math.round(((o >> 16) & 255) * (1 - a) + ((c >> 16) & 255) * a);
		int g = Math.round(((o >> 8) & 255) * (1 - a) + ((c >> 8) & 255) * a);
		int b = Math.round((o & 255) * (1 - a) + (c & 255) * a);
		img.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
	}

	/** Como un rotulador: oscurece multiplicando por c. */
	private static void multiplicar(int x, int y, int c, float a) {
		if (x < 0 || y < 0 || x >= LADO || y >= LADO) {
			return;
		}
		int o = img.getPixel(x, y);
		int r = Math.round(((o >> 16) & 255) * (1 - a + a * ((c >> 16) & 255) / 255.0F));
		int g = Math.round(((o >> 8) & 255) * (1 - a + a * ((c >> 8) & 255) / 255.0F));
		int b = Math.round((o & 255) * (1 - a + a * (c & 255) / 255.0F));
		img.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
	}

	private static int sumar(int c, int dr, int dg, int db) {
		int r = Math.max(0, Math.min(255, ((c >> 16) & 255) + dr));
		int g = Math.max(0, Math.min(255, ((c >> 8) & 255) + dg));
		int b = Math.max(0, Math.min(255, (c & 255) + db));
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}
