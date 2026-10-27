package net.backrooms.evento.expedicion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.red.NumerosParticipantes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * El numero de participante de cada jugador (como en un concurso: 001, 002...), por orden de
 * llegada al servidor. Se guarda con el mundo (backrooms/participantes.json), asi que no cambia
 * aunque salga y vuelva a entrar. Se manda a todos para pintarlo en la espalda del traje.
 */
public final class Participantes {
	private static final Gson GSON = new Gson();
	private static final Map<UUID, Integer> NUMEROS = new LinkedHashMap<>();
	private static MinecraftServer servidor;
	/** A quien falta mandarle la lista. */
	private static final java.util.Set<UUID> PENDIENTES = new java.util.HashSet<>();

	private Participantes() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			servidor = s;
			cargar();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			servidor = null;
			NUMEROS.clear();
		});
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> {
			if (!NUMEROS.containsKey(h.player.getUUID())) {
				NUMEROS.put(h.player.getUUID(), NUMEROS.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
				guardar();
			}
			// a todos (el nuevo recibe la lista entera; los demas, tambien el numero del nuevo). Se manda en
			// los ticks siguientes: al entrar, el cliente aun no ha dicho que canales tiene
			for (ServerPlayer j : s.getPlayerList().getPlayers()) {
				PENDIENTES.add(j.getUUID());
			}
			// (el que entra puede no estar aun en la lista)
			PENDIENTES.add(h.player.getUUID());
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (PENDIENTES.isEmpty() || s.getTickCount() % 10 != 0) {
				return;
			}
			NumerosParticipantes p = new NumerosParticipantes(new HashMap<>(NUMEROS));
			PENDIENTES.removeIf(id -> {
				ServerPlayer j = s.getPlayerList().getPlayer(id);
				if (j == null) {
					return true;
				}
				if (!ServerPlayNetworking.canSend(j, NumerosParticipantes.TYPE)) {
					return false;
				}
				ServerPlayNetworking.send(j, p);
				return true;
			});
		});
	}

	/** El numero de un jugador, o 0 si aun no tiene. */
	public static int numero(UUID id) {
		return NUMEROS.getOrDefault(id, 0);
	}

	private static Path archivo() {
		return servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("participantes.json");
	}

	private static void cargar() {
		NUMEROS.clear();
		try {
			Path f = archivo();
			if (Files.exists(f)) {
				Map<String, Integer> m = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<Map<String, Integer>>() { }.getType());
				if (m != null) {
					m.forEach((k, v) -> NUMEROS.put(UUID.fromString(k), v));
				}
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo leer participantes.json", e);
		}
	}

	private static void guardar() {
		try {
			Path f = archivo();
			Files.createDirectories(f.getParent());
			Map<String, Integer> m = new LinkedHashMap<>();
			NUMEROS.forEach((k, v) -> m.put(k.toString(), v));
			Files.writeString(f, GSON.toJson(m), StandardCharsets.UTF_8);
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo guardar participantes.json", e);
		}
	}
}
