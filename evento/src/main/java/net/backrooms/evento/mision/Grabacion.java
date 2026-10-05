package net.backrooms.evento.mision;

import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.entidad.Bacteria;
import net.backrooms.evento.entidad.Smiler;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.objetos.HerramientasServidor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Las misiones de grabar, con la camara (C) levantada. Cada cuarto de
 * segundo mira que tiene delante cada jugador que graba:
 *  - alarmas: basta con grabar mientras los tubos estan en rojo;
 *  - la Bacteria (o la Bacteria con alarma): tenerla en el encuadre (25
 *    grados del centro), a menos de 28 bloques y sin paredes en medio;
 *  - un Smiler: lo mismo, a menos de 22 (por el visor no hace flash).
 * Lo grabado se acumula en Misiones.grabar, que completa la mision.
 */
public final class Grabacion {
	private static final float CADA = 0.25F;
	private static final double ENCUADRE = Math.cos(Math.toRadians(25));
	private static MinecraftServer servidor;
	private static long ticks;

	private Grabacion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> tick());
	}

	private static void tick() {
		if (servidor == null || ++ticks % 5 != 0) {
			return;
		}
		for (ServerPlayer j : servidor.getPlayerList().getPlayers()) {
			if (!HerramientasServidor.get().camara(j) || Fase.de(j.level()) == null) {
				continue;
			}
			TipoMision m = Misiones.get().grabacionEnCurso(j);
			if (m == null) {
				continue;
			}
			boolean bien = switch (m) {
				case LUCES_ROJAS -> Ambiente.get().alarma(j.level());
				case ENTIDAD -> encuadra(j, Bacteria.class, 28);
				case ENTIDAD_ALARMA -> Ambiente.get().alarma(j.level()) && encuadra(j, Bacteria.class, 28);
				case SMILER -> encuadra(j, Smiler.class, 22);
				default -> false;
			};
			if (bien) {
				Misiones.get().grabar(j, m, CADA);
			}
		}
	}

	private static boolean encuadra(ServerPlayer j, Class<? extends Entity> clase, double alcance) {
		Vec3 ojo = j.getEyePosition();
		Vec3 mira = j.getViewVector(1.0F);
		for (Entity e : j.level().getEntitiesOfClass(clase, j.getBoundingBox().inflate(alcance))) {
			Vec3 hacia = e.getBoundingBox().getCenter().subtract(ojo);
			double d = hacia.length();
			if (d < alcance && d > 0.01 && mira.dot(hacia.scale(1.0 / d)) > ENCUADRE && j.hasLineOfSight(e)) {
				return true;
			}
		}
		return false;
	}
}
