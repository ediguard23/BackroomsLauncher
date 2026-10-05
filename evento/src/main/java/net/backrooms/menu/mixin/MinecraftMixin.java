package net.backrooms.menu.mixin;

import net.backrooms.menu.Evento;
import net.backrooms.menu.MenuBackrooms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.Music;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public @Nullable ClientLevel level;

	/**
	 * Toda pantalla que lleve al menu de inicio, a un jugador, a multijugador
	 * o a Realms se cambia por el menu del evento. Asi no hay atajo ni boton
	 * de otra pantalla que deje elegir otro servidor.
	 */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private @Nullable Screen backrooms$sustituirPantalla(@Nullable Screen pantalla) {
		return MenuBackrooms.sustituir(pantalla, this.level != null);
	}

	/** La barra de titulo de la ventana dice el nombre del evento, no "Minecraft* 1.21.11". */
	@Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
	private void backrooms$titulo(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(Evento.titulo());
	}

	/** Sin musica de menu de Minecraft: en los menus suenan los bucles del nivel (Tema). */
	@Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
	private void backrooms$sinMusicaDeMenu(CallbackInfoReturnable<@Nullable Music> cir) {
		if (this.level == null) {
			cir.setReturnValue(null);
		}
	}
}
