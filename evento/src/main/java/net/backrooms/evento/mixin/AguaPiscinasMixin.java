package net.backrooms.evento.mixin;

import net.backrooms.evento.piscinas.Piscinas;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * En el agua de las Piscinas los jugadores van un 10 % mas lentos (andando con el agua por
 * los tobillos o nadando): se rebaja el empuje de cada tick, y la velocidad a la que se
 * llega es proporcional a el. Solo jugadores y solo en esa dimension: el agua del resto del
 * evento (y la de la Serpiente, que va aparte) no cambia. El movimiento del jugador lo
 * calcula su cliente, por eso va en los dos lados.
 */
@Mixin(LivingEntity.class)
public abstract class AguaPiscinasMixin {
	@ModifyArg(method = "travelInWater", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"), index = 0)
	private float backrooms$aguaPesada(float empuje) {
		LivingEntity e = (LivingEntity) (Object) this;
		return e instanceof Player && Piscinas.es(e.level()) ? empuje * 0.9F : empuje;
	}
}
