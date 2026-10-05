package net.backrooms.evento.fase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.objetos.Equipo;
import net.backrooms.evento.red.ViajeAscensor;
import net.backrooms.evento.vestibulo.Vestibulo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/**
 * El paso de una fase a otra por los ascensores de salida.
 *
 * Quien ha completado sus misiones pulsa el panel de un ascensor: la puerta
 * se cierra (en su pantalla), baja unos segundos y aparece en un punto al
 * azar de la fase siguiente, con misiones nuevas mas dificiles. En la ultima
 * fase el ascensor saca de los Backrooms: ha escapado, se apunta su puesto
 * y vuelve al vestibulo.
 *
 * Los puestos se guardan en <mundo>/backrooms/escapados.json.
 */
public final class Fases {
	private static final Fases INSTANCIA = new Fases();
	/** Ticks de bajada: lo que dura el fundido del ascensor hasta llegar. */
	public static final int BAJADA = 20 * 6;

	public static Fases get() {
		return INSTANCIA;
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Map<UUID, Long> viajes = new HashMap<>();
	private final Map<ResourceKey<Level>, List<BlockPos>> ocupadas = new HashMap<>();
	private final Random azar = new Random();
	private List<String> escapados = new ArrayList<>();
	private MinecraftServer servidor;
	private long ticks;

	private Fases() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.cargar();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
	}

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("escapados.json");
	}

	private void cargar() {
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				List<String> l = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<List<String>>() { }.getType());
				if (l != null) {
					this.escapados = new ArrayList<>(l);
				}
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo leer escapados.json", e);
		}
	}

	private void guardar() {
		try {
			Path f = this.archivo();
			Files.createDirectories(f.getParent());
			Files.writeString(f, this.gson.toJson(this.escapados), StandardCharsets.UTF_8);
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo guardar escapados.json", e);
		}
	}

	public List<String> escapados() {
		return this.escapados;
	}

	public void olvidarEscapados() {
		this.escapados.clear();
		this.guardar();
	}

	/* ----------------------------------------------------- ascensor */

	/** Alguien pulsa el panel de un ascensor de salida. */
	public void pulsar(ServerPlayer j, BlockPos panel) {
		Fase f = Fase.de(j.level());
		if (f == null || this.viajes.containsKey(j.getUUID())) {
			return;
		}
		Misiones.Estado e = Misiones.get().estado(j);
		if (e == null || e.actual < e.misiones.size()) {
			j.level().playSound(null, panel, net.backrooms.evento.Sonidos.ASCENSOR_DENEGADO, SoundSource.BLOCKS, 0.9F, 1.0F);
			j.displayClientMessage(Component.literal("ACCESO DENEGADO · Completa tus misiones").withStyle(ChatFormatting.RED), true);
			return;
		}
		Fase sig = f.siguiente();
		ServerPlayNetworking.send(j, sig == null ? new ViajeAscensor(0, "LA SALIDA", "") : new ViajeAscensor(sig.numero(), sig.nombre(), sig.dificultad()));
		j.level().playSound(null, panel, net.backrooms.evento.Sonidos.ASCENSOR_PANEL, SoundSource.BLOCKS, 1.0F, 1.0F);
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, BAJADA + 20, 6, false, false, false));
		this.viajes.put(j.getUUID(), this.ticks + BAJADA);
	}

	/** Lo manda ya a la fase n (staff). */
	public void mandar(ServerPlayer j, Fase f) {
		this.llegar(j, f);
	}

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		if (this.viajes.isEmpty()) {
			return;
		}
		this.viajes.entrySet().removeIf(en -> {
			if (en.getValue() > this.ticks) {
				return false;
			}
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(en.getKey());
			if (j != null) {
				Fase f = Fase.de(j.level());
				Fase sig = f == null ? null : f.siguiente();
				if (f != null && sig == null) {
					this.escapar(j);
				} else if (sig != null) {
					this.llegar(j, sig);
				}
			}
			return true;
		});
	}

	private void llegar(ServerPlayer j, Fase f) {
		ServerLevel nivel = this.servidor.getLevel(f.dimension());
		if (nivel == null) {
			BackroomsEvento.LOG.error("No existe la dimension de la fase {} ({})", f.numero(), f.dimension().identifier());
			j.displayClientMessage(Component.literal("La fase " + f.numero() + " no esta disponible en este servidor.").withStyle(ChatFormatting.RED), false);
			return;
		}
		BlockPos p = this.zonaNueva(nivel, f);
		j.teleportTo(nivel, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), this.azar.nextFloat() * 360.0F - 180.0F, 0.0F, true);
		j.resetFallDistance();
		j.removeEffect(MobEffects.SLOWNESS);
		Equipo.vestir(j);
		Misiones.get().asignar(j, p, f);
		net.backrooms.evento.supervivencia.Comida.get().repartirAguas(nivel, p);
		BackroomsEvento.LOG.info("{} llega a la fase {} en {} {}", j.getGameProfile().name(), f.numero(), p.getX(), p.getZ());
	}

	private void escapar(ServerPlayer j) {
		String nombre = j.getGameProfile().name();
		if (!this.escapados.contains(nombre)) {
			this.escapados.add(nombre);
			this.guardar();
		}
		int puesto = this.escapados.indexOf(nombre) + 1;
		Misiones.get().escapado(j, puesto);
		j.removeEffect(MobEffects.SLOWNESS);
		Vestibulo.get().llevar(j);
		this.servidor.getPlayerList().broadcastSystemMessage(
			Component.literal(nombre + " ha escapado de los Backrooms (puesto #" + puesto + ")").withStyle(ChatFormatting.GOLD), false);
		for (ServerPlayer o : this.servidor.getPlayerList().getPlayers()) {
			net.backrooms.evento.Sonidos.aJugador(o, net.backrooms.evento.Sonidos.ESCAPADO, 0.9F);
		}
		BackroomsEvento.LOG.info("{} ha escapado (puesto {})", nombre, puesto);
	}

	/* ------------------------------------------------ puntos de llegada */

	/** Punto al azar de la fase, en suelo libre y separado de los ya repartidos en ella. */
	public BlockPos zonaNueva(ServerLevel nivel, Fase f) {
		List<BlockPos> usadas = this.ocupadas.computeIfAbsent(nivel.dimension(), k -> new ArrayList<>());
		int x = 0;
		int z = 0;
		for (int intento = 0; intento < 80; intento++) {
			x = this.azar.nextInt(f.radio() * 2 + 1) - f.radio();
			z = this.azar.nextInt(f.radio() * 2 + 1) - f.radio();
			boolean lejos = true;
			for (BlockPos o : usadas) {
				long dx = o.getX() - x;
				long dz = o.getZ() - z;
				if (dx * dx + dz * dz < (long) f.separacion() * f.separacion()) {
					lejos = false;
					break;
				}
			}
			if (lejos) {
				break;
			}
		}
		BlockPos p = new BlockPos(x, GeneradorNivel0.SUELO + 1, z);
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen) {
			p = gen.puntoLibre(nivel.getChunkSource().randomState(), x, z);
		}
		usadas.add(p);
		return p;
	}
}
