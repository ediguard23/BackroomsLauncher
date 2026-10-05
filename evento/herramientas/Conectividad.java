import java.util.ArrayDeque;
import net.backrooms.evento.mundo.Plano;

/**
 * Mide que parte del suelo de un cuadrado de lado L alrededor del spawn se
 * puede alcanzar andando desde (1, 1): lo que no, son bolsas cerradas donde
 * alguien podria aparecer atrapado.
 *
 *   java -cp build/mapa Conectividad <semilla> <lado>
 */
public class Conectividad {
	public static void main(String[] args) {
		long semilla = Long.parseLong(args[0]);
		int lado = Integer.parseInt(args[1]);
		Plano p = new Plano(semilla);
		int m = lado / 2;
		boolean[] pared = new boolean[lado * lado];
		boolean[] visto = new boolean[lado * lado];
		int libres = 0;
		for (int i = 0; i < lado; i++) {
			for (int j = 0; j < lado; j++) {
				pared[i * lado + j] = p.pared(i - m, j - m);
				if (!pared[i * lado + j]) {
					libres++;
				}
			}
		}
		ArrayDeque<int[]> cola = new ArrayDeque<>();
		cola.add(new int[] {m + 1, m + 1});
		visto[(m + 1) * lado + m + 1] = true;
		int alcanzados = 0;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!cola.isEmpty()) {
			int[] c = cola.poll();
			alcanzados++;
			for (int[] d : dirs) {
				int x = c[0] + d[0];
				int z = c[1] + d[1];
				if (x < 0 || z < 0 || x >= lado || z >= lado) {
					continue;
				}
				int k = x * lado + z;
				if (!pared[k] && !visto[k]) {
					visto[k] = true;
					cola.add(new int[] {x, z});
				}
			}
		}
		System.out.printf("Lado %d: %d de %d bloques de suelo alcanzables (%.2f %%), %d en bolsas cerradas%n",
			lado, alcanzados, libres, alcanzados * 100.0 / libres, libres - alcanzados);
	}
}
