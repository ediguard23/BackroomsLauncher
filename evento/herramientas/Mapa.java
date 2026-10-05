import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.backrooms.evento.mundo.Plano;

/**
 * Dibuja el Nivel 0 visto desde arriba (1 pixel = 1 bloque) para revisar el
 * trazado sin abrir el juego:
 *
 *   javac -d build/mapa src/main/java/net/backrooms/evento/mundo/Plano.java herramientas/Mapa.java
 *   java -cp build/mapa Mapa <semilla> <lado> <salida.png> [x0 z0 [zoom]]
 *
 * Paredes en marron oscuro, moqueta en ocre (mas oscura si esta mojada),
 * tubos encendidos en blanco, apagados en gris y parpadeando en naranja; las
 * zonas a oscuras salen sombreadas.
 */
public class Mapa {
	public static void main(String[] args) throws Exception {
		long semilla = Long.parseLong(args[0]);
		int lado = Integer.parseInt(args[1]);
		File salida = new File(args[2]);
		int x0 = args.length > 4 ? Integer.parseInt(args[3]) : -lado / 2;
		int z0 = args.length > 4 ? Integer.parseInt(args[4]) : -lado / 2;
		int zoom = args.length > 5 ? Integer.parseInt(args[5]) : 1;
		Plano p = new Plano(semilla);
		BufferedImage img = new BufferedImage(lado * zoom, lado * zoom, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < lado; i++) {
			for (int j = 0; j < lado; j++) {
				int x = x0 + i;
				int z = z0 + j;
				int c;
				if (p.pared(x, z)) {
					c = 0x3A2F17;
				} else {
					int t = p.techo(x, z);
					if (t == Plano.TUBO) {
						c = 0xFFFBE0;
					} else if (t == Plano.TUBO_PARPADEO) {
						c = 0xFF9A3C;
					} else if (t == Plano.TUBO_APAGADO) {
						c = 0x77736A;
					} else {
						c = p.mojado(x, z) ? 0x7E6C34 : 0xB59D50;
					}
					int o = p.oscuridad(x, z);
					if (o > 0 && t == Plano.TECHO) {
						c = oscurecer(c, o == 2 ? 0.35 : 0.65);
					}
				}
				for (int a = 0; a < zoom; a++) {
					for (int b = 0; b < zoom; b++) {
						img.setRGB(i * zoom + a, j * zoom + b, c);
					}
				}
			}
		}
		ImageIO.write(img, "png", salida);
		System.out.println("Mapa " + lado + "x" + lado + " -> " + salida);
	}

	private static int oscurecer(int c, double f) {
		int r = (int) (((c >> 16) & 255) * f);
		int g = (int) (((c >> 8) & 255) * f);
		int b = (int) ((c & 255) * f);
		return (r << 16) | (g << 8) | b;
	}
}
