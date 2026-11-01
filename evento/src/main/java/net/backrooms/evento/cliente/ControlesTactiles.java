package net.backrooms.evento.cliente;

import java.nio.file.Files;
import net.backrooms.evento.mixin.cliente.TeclaAccessor;
import net.backrooms.evento.objetos.Herramienta;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * Para quien juega sin teclado (Pojav en el movil): el evento se pensó para PC (F linterna, C camara, M
 * misiones, mantener el clic derecho en las valvulas), y en el movil no hay esas teclas.
 *
 *   - La tecla del inventario (en el movil, su boton de la mochila; en PC la E) abre y cierra el papel de las
 *     misiones, para todos: el inventario del evento no se abre (solo la barra).
 *   - En modo tactil, mantener pulsada la pantalla (golpear, el clic izquierdo) con la linterna o la camara en la
 *     mano la enciende o la apaga, como la F o la C. Tocar (usar) sigue siendo abrir puertas y coger monedas. Una
 *     vez encendida se puede cambiar de casilla: sigue encendida.
 *   - En modo tactil, un toque en una valvula la sigue girando sola (como mantener el clic) hasta que se toca otra
 *     vez, se deja de mirarla o pasan 6 s.
 *
 * El modo tactil se activa si el juego no se abrio desde el launcher del evento (no hay backrooms-event.json),
 * si es Android, o con la variable BACKROOMS_TACTIL (pruebas).
 */
public final class ControlesTactiles {
	private static final int MAX_GIRO = 20 * 6;

	private static boolean tactil;
	private static int comprobado = -1;
	private static @Nullable BlockPos valvula;
	private static int girando;

	private ControlesTactiles() {
	}

	public static void registrar() {
		// antes de que el juego mire sus teclas (Minecraft#handleKeybinds va despues)
		ClientTickEvents.START_CLIENT_TICK.register(ControlesTactiles::tick);
	}

	public static boolean tactil() {
		return tactil;
	}

	private static void tick(Minecraft mc) {
		if (comprobado < 0 || ++comprobado > 200) {
			comprobado = 0;
			tactil = System.getenv("BACKROOMS_TACTIL") != null || System.getenv("ANDROID_ROOT") != null
				|| System.getenv("POJAV_NATIVEDIR") != null
				|| FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
					&& !Files.exists(FabricLoader.getInstance().getConfigDir().resolve("backrooms-event.json"));
		}
		LocalPlayer j = mc.player;
		if (j == null || mc.screen != null) {
			soltarValvula(mc);
			return;
		}
		// el inventario no se abre en el evento: su tecla (o su boton en el movil) es la de las misiones
		if (!j.hasInfiniteMaterials()) {
			while (mc.options.keyInventory.consumeClick()) {
				PapelMisiones.alternar(mc);
			}
		}
		if (!tactil) {
			return;
		}
		// con la linterna o la camara en la mano, golpear = su tecla (golpear no sirve de nada en el evento)
		if (j.getMainHandItem().getItem() instanceof Herramienta h) {
			while (mc.options.keyAttack.consumeClick()) {
				HerramientasCliente.pulsar(h.accion());
			}
			mc.options.keyAttack.setDown(false);
		}
		TeclaAccessor usar = (TeclaAccessor) mc.options.keyUse;
		// la valvula que se esta girando sola
		if (valvula != null) {
			boolean otroToque = usar.backrooms$pulsaciones() > 0;
			if (otroToque || ++girando > MAX_GIRO || !mirando(mc, valvula)) {
				if (otroToque) {
					while (mc.options.keyUse.consumeClick()) {
						// ese toque solo era para parar
					}
				}
				soltarValvula(mc);
			} else {
				mc.options.keyUse.setDown(true);
				return;
			}
		}
		if (usar.backrooms$pulsaciones() <= 0) {
			return;
		}
		// un toque en una valvula: se sigue girando (el toque lo atiende el juego como siempre)
		if (mc.hitResult instanceof BlockHitResult b && mc.hitResult.getType() == HitResult.Type.BLOCK
			&& mc.level != null && mc.level.getBlockState(b.getBlockPos()).getBlock() instanceof net.backrooms.evento.piscinas.BloqueValvula) {
			valvula = b.getBlockPos().immutable();
			girando = 0;
		}
	}

	private static boolean mirando(Minecraft mc, BlockPos p) {
		return mc.hitResult instanceof BlockHitResult b && mc.hitResult.getType() == HitResult.Type.BLOCK && b.getBlockPos().equals(p);
	}

	private static void soltarValvula(Minecraft mc) {
		if (valvula != null) {
			valvula = null;
			mc.options.keyUse.setDown(false);
		}
	}
}
