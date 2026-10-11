package net.backrooms.evento.cliente.piscinas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Sintetiza un susurro que dice un nombre: el de quien lo va a oir (de 200 nombres no hay
 * grabacion posible, asi que se hace aqui, en su cliente).
 *
 * Un susurro es aire sin voz: ruido que pasa por las tres resonancias de la boca de cada
 * vocal (formantes), un siseo para la s, un golpecito para la p, la t y la k... Se lee como
 * en espanol, que se pronuncia casi como se escribe; de un nick raro sale algo que parece un
 * nombre, que es justo lo que tiene que parecer. Primero una inspiracion, el nombre, un
 * silencio y el nombre otra vez, mas flojo.
 */
final class Susurrador {
	static final int SR = 22050;

	private enum Clase { VOCAL, LIQUIDA, FRICATIVA, OCLUSIVA, NASAL, SILENCIO, ALIENTO }

	/** Un trozo: su clase, cuanto dura, formantes o filtro del ruido, y cuanto suena. */
	private record Trozo(Clase clase, double dur, double f1, double f2, double f3, double fruido, double q, double a) {
	}

	private static final double[][] VOCALES = {
		{750, 1200, 2500}, // a
		{520, 1850, 2550}, // e
		{320, 2250, 3000}, // i
		{560, 900, 2450},  // o
		{360, 850, 2300}   // u
	};

	private Susurrador() {
	}

	/** Las muestras (16 bits, mono, SR) del susurro con ese nombre. */
	static short[] susurrar(String nombre, long semilla) {
		List<Trozo> l = new ArrayList<>();
		l.add(new Trozo(Clase.ALIENTO, 0.28, 0, 0, 0, 1300, 0.7, 0.18));
		List<Trozo> dicho = leer(nombre);
		l.addAll(dicho);
		l.add(new Trozo(Clase.SILENCIO, 0.42, 0, 0, 0, 0, 0, 0));
		for (Trozo t : dicho) {
			l.add(new Trozo(t.clase, t.dur * 1.12, t.f1, t.f2, t.f3, t.fruido, t.q, t.a * 0.62));
		}
		l.add(new Trozo(Clase.SILENCIO, 0.3, 0, 0, 0, 0, 0, 0));
		return sintetizar(l, new Random(semilla));
	}

	/* ------------------------------------------------------- de letras a sonidos */

	private static boolean esVocal(char c) {
		return "aeiou".indexOf(c) >= 0;
	}

	private static Trozo vocal(char c, boolean ultima) {
		double[] f = VOCALES["aeiou".indexOf(c)];
		return new Trozo(Clase.VOCAL, ultima ? 0.2 : 0.13, f[0], f[1], f[2], 0, 0, c == 'i' || c == 'u' ? 0.8 : 1.0);
	}

	private static Trozo fricativa(double f, double q, double a, double dur) {
		return new Trozo(Clase.FRICATIVA, dur, 0, 0, 0, f, q, a);
	}

	private static Trozo oclusiva(double f, double q, double a) {
		return new Trozo(Clase.OCLUSIVA, 0.075, 0, 0, 0, f, q, a);
	}

	/** El nombre como lo leeria alguien de habla hispana (sin numeros ni simbolos). */
	static List<Trozo> leer(String nombre) {
		String s = nombre.toLowerCase(Locale.ROOT)
			.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
			.replaceAll("[^a-zñ]", "");
		if (s.length() > 10) {
			s = s.substring(0, 10);
		}
		if (s.isEmpty() || s.chars().noneMatch(c -> esVocal((char) c) || c == 'y')) {
			s = "tu";
		}
		int ultimaVocal = -1;
		for (int i = 0; i < s.length(); i++) {
			if (esVocal(s.charAt(i))) {
				ultimaVocal = i;
			}
		}
		List<Trozo> l = new ArrayList<>();
		char antes = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			char sig = i + 1 < s.length() ? s.charAt(i + 1) : 0;
			if (c == antes && c != 'r' && c != 'l') {
				continue; // letras dobles: una
			}
			antes = c;
			switch (c) {
				case 'a', 'e', 'i', 'o', 'u' -> l.add(vocal(c, i == ultimaVocal));
				case 's', 'z' -> l.add(fricativa(6000, 1.0, 0.5, 0.11));
				case 'x' -> {
					l.add(oclusiva(1900, 1.6, 0.5));
					l.add(fricativa(6000, 1.0, 0.45, 0.09));
				}
				case 'f' -> l.add(fricativa(4000, 0.6, 0.25, 0.1));
				case 'j' -> l.add(fricativa(1600, 1.5, 0.45, 0.1));
				case 'h' -> {
					// muda; salvo "ch"
				}
				case 'c' -> {
					if (sig == 'h') {
						l.add(new Trozo(Clase.SILENCIO, 0.035, 0, 0, 0, 0, 0, 0));
						l.add(fricativa(3000, 1.4, 0.55, 0.09));
						i++;
					} else if (sig == 'e' || sig == 'i') {
						l.add(fricativa(6000, 1.0, 0.5, 0.11));
					} else {
						l.add(oclusiva(1900, 1.6, 0.55));
					}
				}
				case 'q', 'k' -> {
					l.add(oclusiva(1900, 1.6, 0.55));
					if (c == 'q' && sig == 'u') {
						i++;
					}
				}
				case 'g' -> {
					if (sig == 'e' || sig == 'i') {
						l.add(fricativa(1600, 1.5, 0.45, 0.1));
					} else {
						l.add(oclusiva(1700, 1.4, 0.32));
						if (sig == 'u' && i + 2 < s.length() && (s.charAt(i + 2) == 'e' || s.charAt(i + 2) == 'i')) {
							i++;
						}
					}
				}
				case 'p' -> l.add(oclusiva(900, 0.8, 0.5));
				case 'b', 'v' -> l.add(oclusiva(800, 0.8, 0.3));
				case 't' -> l.add(oclusiva(4200, 1.2, 0.5));
				case 'd' -> l.add(oclusiva(3800, 1.2, 0.3));
				case 'm', 'n' -> l.add(new Trozo(Clase.NASAL, 0.075, 300, 1100, 0, 0, 0, 0.22));
				case 'ñ' -> {
					l.add(new Trozo(Clase.NASAL, 0.07, 300, 1100, 0, 0, 0, 0.22));
					l.add(new Trozo(Clase.LIQUIDA, 0.05, 300, 2300, 3000, 0, 0, 0.45));
				}
				case 'l' -> {
					if (sig == 'l') {
						l.add(new Trozo(Clase.LIQUIDA, 0.07, 300, 2300, 3000, 0, 0, 0.5));
						i++;
					} else {
						l.add(new Trozo(Clase.LIQUIDA, 0.065, 360, 1150, 2700, 0, 0, 0.45));
					}
				}
				case 'r' -> l.add(new Trozo(Clase.LIQUIDA, 0.035, 420, 1300, 2400, 0, 0, 0.25));
				case 'y' -> {
					if (esVocal(sig)) {
						l.add(new Trozo(Clase.LIQUIDA, 0.06, 300, 2300, 3000, 0, 0, 0.5));
					} else {
						l.add(vocal('i', i == s.length() - 1));
					}
				}
				case 'w' -> l.add(new Trozo(Clase.LIQUIDA, 0.06, 320, 800, 2300, 0, 0, 0.5));
				default -> {
				}
			}
		}
		return l;
	}

	/* --------------------------------------------------------------- sintesis */

	/** Filtro paso banda (RBJ, ganancia 0 dB en el centro) que se puede reajustar. */
	private static final class Banda {
		double b0, b2, a1, a2, x1, x2, y1, y2;

		void ajustar(double f, double q) {
			double w = 2 * Math.PI * Math.min(f, SR * 0.45) / SR;
			double alfa = Math.sin(w) / (2 * q);
			double a0 = 1 + alfa;
			this.b0 = alfa / a0;
			this.b2 = -alfa / a0;
			this.a1 = -2 * Math.cos(w) / a0;
			this.a2 = (1 - alfa) / a0;
		}

		double paso(double x) {
			double y = this.b0 * x + this.b2 * this.x2 - this.a1 * this.y1 - this.a2 * this.y2;
			this.x2 = this.x1;
			this.x1 = x;
			this.y2 = this.y1;
			this.y1 = y;
			return y;
		}
	}

	private static short[] sintetizar(List<Trozo> trozos, Random azar) {
		double total = 0;
		for (Trozo t : trozos) {
			total += t.dur;
		}
		int n = (int) (total * SR) + SR / 10;
		double[] out = new double[n];
		Banda[] boca = {new Banda(), new Banda(), new Banda()};
		Banda ruido = new Banda();
		Banda nasal = new Banda();
		Banda aliento = new Banda();
		nasal.ajustar(320, 2);
		double[] f = {500, 1500, 2500};
		int i = 0;
		for (int k = 0; k < trozos.size(); k++) {
			Trozo t = trozos.get(k);
			int muestras = (int) (t.dur * SR);
			if (t.clase == Clase.FRICATIVA || t.clase == Clase.OCLUSIVA) {
				ruido.ajustar(t.fruido, t.q);
			}
			if (t.clase == Clase.ALIENTO) {
				aliento.ajustar(t.fruido, t.q);
			}
			for (int m = 0; m < muestras && i < n; m++, i++) {
				double u = m / (double) muestras;
				double blanco = azar.nextDouble() * 2 - 1;
				// la boca se mueve hacia los formantes del trozo (si los tiene): coarticulacion
				if (t.f1 > 0) {
					f[0] += (t.f1 - f[0]) * 0.004;
					f[1] += (t.f2 - f[1]) * 0.004;
					if (t.f3 > 0) {
						f[2] += (t.f3 - f[2]) * 0.004;
					}
				}
				if ((m & 15) == 0) {
					boca[0].ajustar(f[0], 5);
					boca[1].ajustar(f[1], 7);
					boca[2].ajustar(f[2], 9);
				}
				double envolvente = Math.min(1, Math.min(m / (0.018 * SR), (muestras - m) / (0.025 * SR)));
				double v = 0;
				switch (t.clase) {
					case VOCAL, LIQUIDA -> {
						v = (boca[0].paso(blanco) + boca[1].paso(blanco) * 0.7 + boca[2].paso(blanco) * 0.4) * 2.2 * t.a;
						if (t.clase == Clase.LIQUIDA && t.dur < 0.04) {
							v *= 0.4 + 0.6 * Math.abs(u - 0.5) * 2; // la r: un toque, se hunde en medio
						}
					}
					case FRICATIVA -> v = ruido.paso(blanco) * t.a * 1.8;
					case OCLUSIVA -> {
						// cierre (nada), golpe y un poco de aire
						double tt = m / (double) SR;
						if (tt > 0.045) {
							double d = tt - 0.045;
							v = ruido.paso(blanco) * t.a * 2.5 * Math.exp(-d / 0.012)
								+ (boca[0].paso(blanco) + boca[1].paso(blanco) * 0.5) * 0.35 * t.a * Math.exp(-d / 0.03);
						}
						envolvente = 1;
					}
					case NASAL -> v = (nasal.paso(blanco) + boca[1].paso(blanco) * 0.25) * t.a * 1.4;
					case ALIENTO -> v = aliento.paso(blanco) * t.a * Math.sin(Math.PI * u);
					default -> v = 0;
				}
				out[i] += v * envolvente;
			}
		}
		// dos reflejos tempranos: se oye en una sala, no dentro de la cabeza
		double[] sala = out.clone();
		int r1 = (int) (0.021 * SR);
		int r2 = (int) (0.037 * SR);
		for (int k = 0; k < n; k++) {
			if (k >= r1) {
				sala[k] += out[k - r1] * 0.22;
			}
			if (k >= r2) {
				sala[k] += out[k - r2] * 0.12;
			}
		}
		double max = 1e-9;
		for (double v : sala) {
			max = Math.max(max, Math.abs(v));
		}
		short[] pcm = new short[n];
		for (int k = 0; k < n; k++) {
			pcm[k] = (short) Math.round(sala[k] / max * 0.6 * Short.MAX_VALUE);
		}
		return pcm;
	}
}
