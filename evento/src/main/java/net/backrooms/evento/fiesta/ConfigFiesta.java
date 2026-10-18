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
	/** Tickets para canjear el Gran Premio (y pasar al siguiente tramo). */
	public int objetivoTickets = 1500;

	/* el semaforo */
	public int semaforoPremio = 300;
	public int semaforoPremioRepetido = 150;
	/** Tickets que se pierden si te pillan moviendote con la luz roja (y vuelves a la salida). */
	public int semaforoCastigo = 25;
	/** Segundos de luz verde y de luz roja (minimo y maximo). */
	public float[] semaforoVerde = {2.5F, 6.0F};
	public float[] semaforoRoja = {2.0F, 4.0F};
	/** Segundos que tienes para pararte al ponerse en rojo. */
	public float semaforoMargen = 0.45F;

	/* los apagones y el Anfitrion */
	/** Minutos entre apagones (con alguien en la Fiesta). */
	public float[] apagonCada = {5.0F, 9.0F};
	/** Segundos que dura cada apagon (lo que dura la persecucion). */
	public int[] apagonDura = {35, 50};
	/** Bloques a los que te ve el Anfitrion (sin paredes en medio) y a los que te oye correr. */
	public double anfitrionVista = 22.0;
	public double anfitrionOido = 12.0;
	/** Bloques por segundo cazando y paseando. */
	public double anfitrionCaza = 5.2;
	public double anfitrionPaseo = 1.4;
	/** Segundos sin verte hasta que te deja. */
	public float anfitrionOlvida = 4.0F;
	/** Minutos entre apariciones sueltas (fuera de los apagones: te mira de lejos y se va). */
	public float[] aparicionCada = {2.0F, 4.0F};

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
