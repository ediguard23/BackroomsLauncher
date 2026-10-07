package net.backrooms.evento.mixin.cliente;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.backrooms.evento.cliente.HerramientasCliente;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Con la camara levantada la rueda del raton no cambia de casilla (las teclas 1-9 las
 * para HerramientasCliente). En los menus la rueda sigue igual.
 */
@Mixin(MouseHandler.class)
public abstract class RuedaCamaraMixin {
	@WrapWithCondition(
		method = "onScroll",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V")
	)
	private boolean backrooms$sinCambiarConCamara(Inventory inventario, int hueco) {
		return !HerramientasCliente.camara();
	}
}
