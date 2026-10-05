package net.backrooms.menu.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * La primera linea del F3 era "Minecraft 1.21.11 (BACKROOMS/fabric)": se le
 * quita la marca del loader. Solo cambia lo que se ve en pantalla; la marca
 * que el cliente envia al servidor sigue siendo la de siempre.
 */
@Mixin(targets = "net.minecraft.client.gui.components.debug.DebugEntryVersion")
public abstract class DebugEntryVersionMixin {
	@ModifyArg(
		method = "display",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;addPriorityLine(Ljava/lang/String;)V")
	)
	private String backrooms$sinMarca(String linea) {
		return linea.replace("/" + ClientBrandRetriever.getClientModName() + ")", ")");
	}
}
