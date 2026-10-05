package net.backrooms.evento.objetos;

import net.backrooms.evento.red.AccionJugador;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * La linterna y la camara. Se encienden con su tecla (F y C por defecto, se
 * cambian en Controles) desde cualquier hueco del inventario, o con clic
 * derecho si se tienen en la mano.
 */
public class Herramienta extends Item {
	private final int accion;

	public Herramienta(int accion, Item.Properties propiedades) {
		super(propiedades);
		this.accion = accion;
	}

	@Override
	public InteractionResult use(Level nivel, Player jugador, InteractionHand mano) {
		if (jugador instanceof ServerPlayer sp) {
			HerramientasServidor h = HerramientasServidor.get();
			boolean ahora = this.accion == AccionJugador.LINTERNA ? h.linterna(sp) : h.camara(sp);
			h.accion(sp, this.accion, !ahora);
		}
		return InteractionResult.SUCCESS;
	}
}
