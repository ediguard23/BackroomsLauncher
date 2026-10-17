package net.backrooms.evento.objetos;

import net.backrooms.evento.red.AccionJugador;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * La linterna y la camara. Se encienden solo con su tecla (F y C por defecto, se
 * cambian en Controles) desde cualquier hueco del inventario: el clic derecho no hace nada
 * (se encendian sin querer al abrir puertas o coger cosas).
 */
public class Herramienta extends Item {
	private final int accion;

	public Herramienta(int accion, Item.Properties propiedades) {
		super(propiedades);
		this.accion = accion;
	}

	@Override
	public InteractionResult use(Level nivel, Player jugador, InteractionHand mano) {
		return InteractionResult.PASS;
	}

	public int accion() {
		return this.accion;
	}
}
