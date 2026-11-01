package net.backrooms.evento.mixin.cliente;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Cuantas pulsaciones de una tecla quedan por atender, sin gastarlas (ControlesTactiles). */
@Mixin(KeyMapping.class)
public interface TeclaAccessor {
	@Accessor("clickCount")
	int backrooms$pulsaciones();
}
