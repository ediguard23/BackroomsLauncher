package net.backrooms.evento.piscinas;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.backrooms.evento.mision.Entidades;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pone los cuerpos de las Piscinas (PlanoPiscinas#cadaverEnCelda) cuando alguien pasa cerca y
 * los quita cuando ya no queda nadie, como la comida: nunca hay cientos cargados. Son
 * siempre los mismos y en el mismo sitio (salen del plano).
 */
public final class Cadaveres {
	private static final int RADIO = 56;
	private static final int QUITAR = 80;
	private static MinecraftServer servidor;
	private static long ticks;
	private static final Map<Long, CadaverEntidad> vivos = new HashMap<>();

	private Cadaveres() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			servidor = null;
			vivos.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> tick());
	}

	private static void tick() {
		if (servidor == null || ++ticks % 20 != 0) {
			return;
		}
		ServerLevel nivel = servidor.getLevel(Piscinas.DIMENSION);
		PlanoPiscinas p = nivel == null ? null : Valvulas.plano(nivel);
		if (p == null) {
			return;
		}
		// tambien para los espectadores (el staff mirando)
		for (ServerPlayer j : nivel.players()) {
			int cx0 = Math.floorDiv(j.getBlockX() - RADIO, PlanoPiscinas.C);
			int cx1 = Math.floorDiv(j.getBlockX() + RADIO, PlanoPiscinas.C);
			int cz0 = Math.floorDiv(j.getBlockZ() - RADIO, PlanoPiscinas.C);
			int cz1 = Math.floorDiv(j.getBlockZ() + RADIO, PlanoPiscinas.C);
			for (int cx = cx0; cx <= cx1; cx++) {
				for (int cz = cz0; cz <= cz1; cz++) {
					PlanoPiscinas.Cadaver c = p.cadaverEnCelda(cx, cz);
					if (c == null) {
						continue;
					}
					long k = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
					CadaverEntidad vivo = vivos.get(k);
					if (vivo != null && !vivo.isRemoved() && nivel.getEntity(vivo.getId()) == vivo) {
						continue;
					}
					if (!nivel.isPositionEntityTicking(BlockPos.containing(c.x(), PlanoPiscinas.PISO, c.z()))) {
						continue;
					}
					if (vivo != null) {
						vivo.discard();
					}
					CadaverEntidad nuevo = new CadaverEntidad(Entidades.CADAVER, nivel);
					nuevo.preparar(c);
					nivel.addFreshEntity(nuevo);
					vivos.put(k, nuevo);
				}
			}
		}
		Iterator<CadaverEntidad> it = vivos.values().iterator();
		while (it.hasNext()) {
			CadaverEntidad c = it.next();
			if (c.isRemoved()) {
				it.remove();
			} else if (nivel.players().stream().noneMatch(j -> j.distanceToSqr(c.getX(), PlanoPiscinas.PISO, c.getZ()) < (double) QUITAR * QUITAR)) {
				c.discard();
				it.remove();
			}
		}
	}
}
