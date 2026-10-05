package net.backrooms.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Datos del evento que escribe el launcher antes de abrir el juego en
 * config/backrooms-event.json: servidor fijo, enlaces, noticias y hora de
 * apertura. El jugador no tiene ninguna pantalla para cambiarlos.
 */
public final class Evento {
	public record Noticia(String fecha, String titulo, String texto) {
	}

	public String nombre = "BACKROOMS";
	public String host = "";
	public int puerto = 25565;
	public String discord = "";
	public String tienda = "";
	/** Milisegundos de la apertura; 0 si no hay cuenta atras. */
	public long apertura = 0;
	public final List<Noticia> noticias = new ArrayList<>();

	public static Evento cargar() {
		Evento e = new Evento();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("backrooms-event.json");
		try {
			JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			e.nombre = texto(o, "nombre", e.nombre);
			if (o.has("server")) {
				JsonObject s = o.getAsJsonObject("server");
				e.host = texto(s, "host", "");
				e.puerto = s.has("port") ? s.get("port").getAsInt() : 25565;
			}
			if (o.has("links")) {
				JsonObject l = o.getAsJsonObject("links");
				e.discord = texto(l, "discord", "");
				e.tienda = texto(l, "tienda", "");
			}
			String inicio = texto(o, "eventStart", "");
			if (!inicio.isEmpty()) {
				try {
					e.apertura = OffsetDateTime.parse(inicio).toInstant().toEpochMilli();
				} catch (Exception ex) {
					e.apertura = Instant.parse(inicio).toEpochMilli();
				}
			}
			if (o.has("news") && o.get("news").isJsonArray()) {
				JsonArray a = o.getAsJsonArray("news");
				for (JsonElement n : a) {
					JsonObject no = n.getAsJsonObject();
					e.noticias.add(new Noticia(texto(no, "date", ""), texto(no, "title", ""), texto(no, "text", "")));
				}
			}
		} catch (Exception ex) {
			BackroomsMenu.LOG.warn("No se pudo leer {}: {}", file, ex.toString());
		}
		return e;
	}

	private static String texto(JsonObject o, String clave, String porDefecto) {
		return o.has(clave) && !o.get(clave).isJsonNull() ? o.get(clave).getAsString() : porDefecto;
	}

	public boolean tieneServidor() {
		return !this.host.isBlank();
	}
}
