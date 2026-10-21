package net.backrooms.evento.mixin.cliente;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** La altura de los ojos que la camara suaviza: la cinematica del tobogan la fija al cambiar de entidad. */
@Mixin(Camera.class)
public interface CamaraAccessor {
	@Accessor("eyeHeight")
	void backrooms$alturaOjos(float altura);

	@Accessor("eyeHeightOld")
	void backrooms$alturaOjosAntes(float altura);
}
