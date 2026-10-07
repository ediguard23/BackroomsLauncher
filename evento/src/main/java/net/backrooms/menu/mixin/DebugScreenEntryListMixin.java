package net.backrooms.menu.mixin;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El F3 del evento solo ensena la version del juego, los FPS, las entidades y las
 * coordenadas: nada de bloque o entidad que se mira, bioma, luz, memoria ni equipo.
 * Da igual lo que se active en F3+F6 o en debug-profile.json; los atajos de F3
 * (cajas de las entidades, bordes de chunk...) tampoco se ven.
 */
@Mixin(DebugScreenEntryList.class)
public abstract class DebugScreenEntryListMixin {
	private static final List<Identifier> BACKROOMS$PERMITIDAS = List.of(
		DebugScreenEntries.GAME_VERSION, DebugScreenEntries.FPS, DebugScreenEntries.ENTITY_RENDER_STATS, DebugScreenEntries.PLAYER_POSITION);

	@Shadow
	@Final
	private List<Identifier> currentlyEnabled;
	@Shadow
	private boolean isOverlayVisible;
	@Shadow
	private long currentlyEnabledVersion;

	@Inject(method = "rebuildCurrentList", at = @At("HEAD"), cancellable = true)
	private void backrooms$soloLoBasico(CallbackInfo ci) {
		this.currentlyEnabled.clear();
		if (this.isOverlayVisible) {
			boolean reducida = Minecraft.getInstance().showOnlyReducedInfo();
			for (Identifier id : BACKROOMS$PERMITIDAS) {
				DebugScreenEntry entrada = DebugScreenEntries.getEntry(id);
				if (entrada != null && entrada.isAllowed(reducida)) {
					this.currentlyEnabled.add(id);
				}
			}
		}
		this.currentlyEnabledVersion++;
		ci.cancel();
	}
}
