package net.backrooms.evento.mixin;

import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Donde aparece un jugador (primera entrada o reaparicion sin cama) en el
 * Nivel 0.
 *
 * En dimensiones sin cielo Minecraft ajusta la altura del spawn sin cargar el
 * chunk: lo ve todo como aire, baja hasta el fondo del mundo y el jugador cae
 * al vacio. Aqui la posicion sale del plano, sin depender de que el chunk
 * este cargado, y nunca dentro de una pared.
 */
@Mixin(PlayerSpawnFinder.class)
public abstract class PlayerSpawnFinderMixin {
	@Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
	private static void backrooms$nivel0(ServerLevel nivel, BlockPos sugerido, CallbackInfoReturnable<CompletableFuture<Vec3>> cir) {
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 generador) {
			BlockPos pos = generador.puntoLibre(nivel.getChunkSource().randomState(), sugerido.getX(), sugerido.getZ());
			cir.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(pos)));
		}
	}
}
