package net.backrooms.evento.piscinas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.mision.Misiones;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * La mision de las valvulas de las Piscinas. Hay 25 en el mapa (PlanoPiscinas#valvulas) y a
 * cada uno le tocan 3 al azar entre las 7 mas cercanas a donde llega (Misiones#asignarPiscinas):
 * a unos 360 bloques unas de otras. Es individual: cada valvula se puede cerrar una vez por
 * cada jugador al que le toca; para los demas esta atascada.
 *
 * Se cierra manteniendo el clic derecho CERRAR ticks en total (Minecraft repite el uso cada 4
 * ticks mientras se mantiene; lo girado no se pierde al soltar): el volante gira suave
 * (ValvulaEntidad) con el chirrido del metal y, al
 * cerrar, suelta la presion (un siseo y vapor). Con las 3 cerradas, a lo lejos arranca una
 * maquinaria (solo la oye el, como el resto de la mision).
 */
public final class Valvulas {
	private static final Valvulas INSTANCIA = new Valvulas();
	/** Ticks de clic mantenido para cerrar una (3 s). */
	private static final int CERRAR = 60;
	/** Lo que cuenta cada uso (el clic mantenido se repite cada 4 ticks). */
	private static final int POR_USO = 4;
	/** Entre las cuantas mas cercanas se sortean las 3 de cada uno. */
	private static final int CERCANAS = 7;

	/** Lo que lleva girado cada uno de cada valvula (ticks): "uuid#id". No se pierde al soltar. */
	private final Map<String, Integer> girado = new HashMap<>();
	/** Quien acaba de cerrar las tres: cuando le suena la maquinaria. */
	private final Map<UUID, Long> maquinaria = new HashMap<>();
	private final Map<UUID, Long> avisado = new HashMap<>();
	private final Random azar = new Random();
	private MinecraftServer servidor;
	private long ticks;

	private Valvulas() {
	}

	public static Valvulas get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> INSTANCIA.servidor = null);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			INSTANCIA.maquinaria.remove(h.player.getUUID());
		});
	}

	private void tick() {
		this.ticks++;
		if (this.maquinaria.isEmpty() || this.servidor == null) {
			return;
		}
		this.maquinaria.entrySet().removeIf(en -> {
			if (en.getValue() > this.ticks) {
				return false;
			}
			ServerPlayer j = this.servidor.getPlayerList().getPlayer(en.getKey());
			if (j != null) {
				Sonidos.aJugador(j, Sonidos.VALVULA_MAQUINARIA, 1.0F);
				j.displayClientMessage(Component.literal("Algo se ha puesto en marcha en las Piscinas.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
			}
			return true;
		});
	}

	public static @Nullable PlanoPiscinas plano(ServerLevel nivel) {
		return nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen ? gen.plano(nivel.getChunkSource().randomState()) : null;
	}

	public List<PlanoPiscinas.Valvula> todas(ServerLevel nivel) {
		PlanoPiscinas p = plano(nivel);
		return p == null ? List.of() : p.valvulas();
	}

	private PlanoPiscinas.@Nullable Valvula en(ServerLevel nivel, BlockPos pos) {
		for (PlanoPiscinas.Valvula v : this.todas(nivel)) {
			if (v.x() == pos.getX() && v.y() == pos.getY() && v.z() == pos.getZ()) {
				return v;
			}
		}
		return null;
	}

	/** Las 3 de alguien que llega a `origen`: al azar entre las CERCANAS mas cercanas. */
	public List<Integer> elegir(ServerLevel nivel, BlockPos origen) {
		List<PlanoPiscinas.Valvula> l = new ArrayList<>(this.todas(nivel));
		l.sort(Comparator.comparingDouble(v -> origen.distToCenterSqr(v.x(), v.y(), v.z())));
		List<PlanoPiscinas.Valvula> cerca = new ArrayList<>(l.subList(0, Math.min(CERCANAS, l.size())));
		Collections.shuffle(cerca, this.azar);
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < Math.min(3, cerca.size()); i++) {
			ids.add(cerca.get(i).id());
		}
		return ids;
	}

	/** {x, z} de la valvula suya sin cerrar mas cercana, o null. */
	public int @Nullable [] cercana(ServerPlayer j, Misiones.Estado e) {
		int[] mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (PlanoPiscinas.Valvula v : this.todas(j.level())) {
			if (!e.valvulas.contains(v.id()) || e.valvulasHechas.contains(v.id())) {
				continue;
			}
			double d = j.distanceToSqr(v.x() + 0.5, v.y(), v.z() + 0.5);
			if (d < dMejor) {
				dMejor = d;
				mejor = new int[] {v.x(), v.z()};
			}
		}
		return mejor;
	}

	/** Lo llama BloqueValvula a cada uso (cada 4 ticks mientras se mantiene el clic). */
	public void girar(ServerPlayer j, BlockPos pos) {
		ServerLevel nivel = j.level();
		PlanoPiscinas.Valvula v = this.en(nivel, pos);
		if (v == null) {
			return;
		}
		Misiones.Estado e = Misiones.get().estado(j);
		if (e == null || !Misiones.get().enExpedicion(j) || !e.valvulas.contains(v.id())) {
			// no es suya: esta atascada
			this.avisar(j, "Esta válvula no es una de las tuyas: está atascada.");
			nivel.playSound(null, pos, Sonidos.VALVULA_GIRO, SoundSource.BLOCKS, 0.35F, 0.55F);
			return;
		}
		if (e.valvulasHechas.contains(v.id())) {
			this.avisar(j, "Esta ya la cerraste.");
			return;
		}
		String clave = j.getUUID() + "#" + v.id();
		int llevado = this.girado.merge(clave, POR_USO, Integer::sum);
		nivel.blockEvent(pos, BloquesPiscinas.VALVULA, 1, 0);
		if (llevado % (POR_USO * 2) == 0) {
			nivel.playSound(null, pos, Sonidos.VALVULA_GIRO, SoundSource.BLOCKS, 0.8F, 0.9F + j.getRandom().nextFloat() * 0.2F);
		}
		float hecho = Math.min(1.0F, llevado / (float) CERRAR);
		if (hecho < 1.0F) {
			int llenas = Math.round(hecho * 12);
			j.displayClientMessage(Component.literal("CERRANDO  " + "▮".repeat(llenas) + "▯".repeat(12 - llenas)).withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		this.girado.remove(clave);
		nivel.playSound(null, pos, Sonidos.VALVULA_CIERRE, SoundSource.BLOCKS, 1.6F, 1.0F);
		nivel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 18, 0.25, 0.3, 0.25, 0.04);
		Misiones.get().cerrarValvula(j, v.id());
		int cerradas = e.valvulasHechas.size();
		j.displayClientMessage(Component.literal("VÁLVULA CERRADA " + cerradas + "/" + e.valvulas.size()).withStyle(ChatFormatting.GOLD), true);
		if (cerradas >= e.valvulas.size()) {
			this.maquinaria.put(j.getUUID(), this.ticks + 40);
		}
	}

	/** Un aviso en la barra, como mucho uno por segundo (el clic se repite cada 4 ticks). */
	private void avisar(ServerPlayer j, String texto) {
		Long antes = this.avisado.get(j.getUUID());
		if (antes != null && this.ticks - antes < 20) {
			return;
		}
		this.avisado.put(j.getUUID(), this.ticks);
		j.displayClientMessage(Component.literal(texto).withStyle(ChatFormatting.GRAY), true);
	}
}
