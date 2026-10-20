package net.backrooms.evento.fiesta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.backrooms.evento.BackroomsEvento;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Lo que se puede ajustar de la Fiesta sin tocar el mod: config/backrooms-fiesta.json (se crea
 * con estos valores la primera vez; `/backrooms fiesta recargar` lo vuelve a leer).
 */
public final class ConfigFiesta {
	/** Tickets que abren la puerta del final (y con ella el tobogan de salida). */
	public int objetivoTickets = 1500;

	/* las vueltas: el bote del tobogan del castillo baja con cada minuto de vuelta */
	public int jackpotInicial = 600;
	public int jackpotMinimo = 200;
	public int jackpotPerdidaMinuto = 40;
	/** Monedas que cuesta el localizador de salida de las salas seguras. */
	public int localizadorCoste = 2;

	/* el semaforo del castillo */
	/** Tickets que se pierden si te pillan moviendote con la luz roja (y vuelves a la salida). */
	public int semaforoCastigo = 25;
	/** Segundos de luz verde y de luz roja (minimo y maximo). */
	public float[] semaforoVerde = {2.5F, 6.0F};
	public float[] semaforoRoja = {2.0F, 4.0F};
	/** Segundos que tienes para pararte al ponerse en rojo. */
	public float semaforoMargen = 0.45F;

	/* la memoria de colores y los regalos */
	public int memoriaPremio = 250;
	public int memoriaPremioRepetido = 100;
	public int regalosPremio = 200;
	public int regalosPremioRepetido = 80;
	/** Si se acaba el tiempo de los regalos: tickets por cada uno encontrado. */
	public int regalosPorRegalo = 10;

	/* los apagones y el Anfitrion */
	/** Minutos entre apagones (con alguien en la Fiesta). */
	public float[] apagonCada = {5.0F, 9.0F};
	/** Segundos que dura cada apagon (lo que dura la persecucion). */
	public int[] apagonDura = {35, 50};
	/** Un Anfitrion por cada tantos jugadores en la Fiesta (minimo uno). */
	public int anfitrionPorJugadores = 5;
	/** Bloques a los que te ve el Anfitrion en un apagon (nunca a traves de paredes). */
	public double anfitrionDetecta = 8.0;
	/** Bloques por segundo persiguiendo (como la Serpiente en seco); merodeando, la mitad. */
	public double anfitrionVelocidad = 4.4;
	/** En los apagones va este tanto mas rapido (1,2 = un 20 %). */
	public double anfitrionApagon = 1.2;
	/** Segundos sin verte hasta que te deja. */
	public float anfitrionOlvida = 4.0F;

	private static ConfigFiesta actual = new ConfigFiesta();

	public static ConfigFiesta get() {
		return actual;
	}

	private static Path archivo() {
		return FabricLoader.getInstance().getConfigDir().resolve("backrooms-fiesta.json");
	}

	public static void cargar() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Path f = archivo();
		try {
			if (Files.exists(f)) {
				ConfigFiesta c = gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), ConfigFiesta.class);
				if (c != null) {
					actual = c;
				}
			}
			Files.createDirectories(f.getParent());
			Files.writeString(f, gson.toJson(actual), StandardCharsets.UTF_8);
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo leer {}: se usan los valores de siempre", f, e);
			actual = new ConfigFiesta();
		}
	}
}
