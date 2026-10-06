package net.backrooms.evento.escondite;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.bloques.Bloques;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * Los huecos de las paredes como escondite: quien esta dentro (tumbado, con el
 * centro en el bloque del hueco) no lo ve ni lo oye ninguna Bacteria, y la que
 * le estaba persiguiendo se va lejos (Bacteria#retirarse). Al meterse o salir
 * cruje el pladur y caen cascotes.
 */
public final class Escondites {
	private static final Escondites INSTANCIA = new Escondites();

	private final Set<UUID> dentro = new HashSet<>();
	private MinecraftServer servidor;
	private long ticks;

	private Escondites() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> INSTANCIA.dentro.remove(h.player.getUUID()));
	}

	/** Esta metido en un hueco de la pared (y por tanto escondido). */
	public static boolean escondido(Player j) {
		return j.isAlive() && j.getPose() == Pose.SWIMMING && j.level().getBlockState(j.blockPosition()).is(Bloques.HUECO);
	}

	private void tick() {
		if (this.servidor == null || ++this.ticks % 2 != 0) {
			return;
		}
		for (ServerPlayer j : this.servidor.getPlayerList().getPlayers()) {
			boolean ahora = escondido(j);
			boolean antes = ahora ? !this.dentro.add(j.getUUID()) : !this.dentro.remove(j.getUUID());
			if (ahora != antes) {
				this.crujir(j, ahora);
			}
		}
	}

	private void crujir(ServerPlayer j, boolean entra) {
		ServerLevel nivel = j.level();
		BlockPos p = j.blockPosition();
		nivel.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, Sonidos.HUECO, SoundSource.BLOCKS,
			entra ? 0.9F : 0.6F, (entra ? 0.95F : 1.15F) + nivel.getRandom().nextFloat() * 0.1F);
		nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Bloques.PAPEL_PINTADO.defaultBlockState()),
			p.getX() + 0.5, p.getY() + 0.75, p.getZ() + 0.5, entra ? 14 : 8, 0.35, 0.15, 0.35, 0.05);
	}
}
