package net.backrooms.menu.mixin;

import com.mojang.blaze3d.platform.IconSet;
import net.backrooms.menu.BackroomsMenu;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El icono de la ventana (y de la barra de tareas) es la puerta del logo del
 * evento en vez del bloque de hierba. Los PNG van en el jar del mod, en los
 * mismos cinco tamanos que pide Minecraft.
 */
@Mixin(IconSet.class)
public abstract class IconSetMixin {
	@Inject(method = "getStandardIcons", at = @At("HEAD"), cancellable = true)
	private void backrooms$iconos(PackResources pack, CallbackInfoReturnable<List<IoSupplier<InputStream>>> cir) {
		List<IoSupplier<InputStream>> iconos = new ArrayList<>();
		for (int lado : new int[] {16, 32, 48, 128, 256}) {
			String ruta = "/assets/backrooms/icons/icon_" + lado + "x" + lado + ".png";
			if (BackroomsMenu.class.getResource(ruta) == null) {
				return; // sin los iconos, los de Minecraft
			}
			iconos.add(() -> {
				InputStream in = BackroomsMenu.class.getResourceAsStream(ruta);
				if (in == null) {
					throw new FileNotFoundException(ruta);
				}
				return in;
			});
		}
		cir.setReturnValue(iconos);
	}
}
