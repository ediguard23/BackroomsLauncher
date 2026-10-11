package net.backrooms.evento.piscinas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.backrooms.evento.entidad.Acechadores;
import net.backrooms.evento.entidad.Serpiente;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * Quien vive en las Piscinas: las Serpientes. Una por cada 10 jugadores de la expedicion
 * que esten alli (minimo una, como la Bacteria en la fase 1). Aparecen lejos de todos (a
 * 45-75 bloques de alguien y a mas de 30 de cualquiera) y se van si se quedan sin nadie a
 * menos de 140. Con /backrooms acechadores false no aparece ninguna (como las del Nivel 0).
 */
public final class Habitantes {
	private static final int MAX = 20;
	private static MinecraftServer servidor;
	private static long ticks;
	private static final Random azar = new Random();

	private Habitantes() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> servidor = null);
		ServerTickEvents.END_SERVER_TICK.register(s -> tick());
	}

	private static void tick() {
		if (servidor == null || ++ticks % 20 != 0) {
			return;
		}
		ServerLevel nivel = servidor.getLevel(Piscinas.DIMENSION);
		if (nivel == null) {
			return;
		}
		List<Serpiente> vivas = new ArrayList<>();
		for (var e : nivel.getAllEntities()) {
			if (e instanceof Serpiente s && s.isAlive()) {
				vivas.add(s);
			}
		}
		for (Serpiente s : vivas) {
			if (nivel.getNearestPlayer(s, 140) == null) {
				s.discard();
			}
		}
		vivas.removeIf(Serpiente::isRemoved);
		List<ServerPlayer> jugando = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (!j.isSpectator() && !j.isCreative() && Misiones.get().enExpedicion(j) && !Fases.get().viajando(j)) {
				jugando.add(j);
			}
		}
		if (!Acechadores.get().activos() || jugando.isEmpty()) {
			return;
		}
		int quiero = Math.min(MAX, Math.max(1, (int) Math.round(jugando.size() / 10.0)));
		if (vivas.size() >= quiero) {
			return;
		}
		BlockPos p = lejos(nivel, jugando.get(azar.nextInt(jugando.size())));
		if (p != null) {
			poner(nivel, p);
		}
	}

	public static @Nullable Serpiente poner(ServerLevel nivel, BlockPos p) {
		Serpiente s = Entidades.SERPIENTE.create(nivel, EntitySpawnReason.EVENT);
		if (s == null) {
			return null;
		}
		s.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, azar.nextFloat() * 360.0F, 0.0F);
		nivel.addFreshEntity(s);
		return s;
	}

	/** Un sitio libre a 45-75 bloques de j y a mas de 30 de todos; null si no lo encuentra. */
	private static @Nullable BlockPos lejos(ServerLevel nivel, ServerPlayer j) {
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas gen)) {
			return null;
		}
		for (int intento = 0; intento < 12; intento++) {
			double ang = azar.nextDouble() * Math.PI * 2;
			double d = 45 + azar.nextDouble() * 30;
			BlockPos p = gen.puntoLibre(nivel.getChunkSource().randomState(), (int) Math.round(j.getX() + Math.cos(ang) * d),
				(int) Math.round(j.getZ() + Math.sin(ang) * d));
			if (nivel.isLoaded(p) && nivel.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 30, false) == null) {
				return p;
			}
		}
		return null;
	}
}
