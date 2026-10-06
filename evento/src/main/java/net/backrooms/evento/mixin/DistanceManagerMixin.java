package net.backrooms.evento.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El servidor de pruebas se cayo tres veces el 2026-10-05 con el mismo error: al
 * sacar a un jugador de una dimension (al viajar al Nivel 0 o al desconectarse),
 * Minecraft lo busca en el chunk donde cree que esta apuntado y alli no hay
 * nadie (NullPointerException en removePlayer: "$$4" is null). Todo el servidor
 * se para con 200 personas dentro.
 *
 * Si pasa, en vez de reventar se le busca en el chunk donde de verdad esta
 * apuntado y se le quita de ahi (con los tickets de ese chunk), y se deja en el
 * log para seguirle la pista.
 */
@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin {
	@Shadow
	@Final
	Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk;

	@Shadow
	public abstract void removePlayer(SectionPos seccion, ServerPlayer jugador);

	@Inject(method = "removePlayer", at = @At("HEAD"), cancellable = true)
	private void backrooms$sinChunk(SectionPos seccion, ServerPlayer jugador, CallbackInfo ci) {
		long clave = seccion.chunk().toLong();
		if (this.playersPerChunk.get(clave) != null) {
			return;
		}
		ci.cancel();
		for (Long2ObjectMap.Entry<ObjectSet<ServerPlayer>> e : this.playersPerChunk.long2ObjectEntrySet()) {
			if (e.getValue().contains(jugador)) {
				long real = e.getLongKey();
				BackroomsEvento.LOG.warn("{} no estaba apuntado en el chunk {} sino en {}: se le quita de ahi",
					jugador.getGameProfile().name(), seccion.chunk(), new ChunkPos(real));
				this.removePlayer(SectionPos.of(ChunkPos.getX(real), seccion.y(), ChunkPos.getZ(real)), jugador);
				return;
			}
		}
		BackroomsEvento.LOG.warn("{} no estaba apuntado en ningun chunk al salir de {}: nada que quitar",
			jugador.getGameProfile().name(), seccion.chunk());
	}
}
