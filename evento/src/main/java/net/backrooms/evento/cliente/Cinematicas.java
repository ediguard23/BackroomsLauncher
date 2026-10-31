package net.backrooms.evento.cliente;

import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.cliente.cinematica.SalidaEstacionCliente;
import net.backrooms.evento.cliente.cinematica.SalidaFiestaCliente;
import net.backrooms.evento.cliente.cinematica.ToboganCliente;
import net.minecraft.resources.Identifier;

/**
 * Las cinematicas, juntas: si hay alguna en marcha, si un sonido es de su banda sonora y cuanto se oye el
 * resto (el mundo y el chat de voz). Mientras se ve una cinematica solo se oye ella: ni el nivel, ni la
 * megafonia, ni las voces de los demas; al despertar vuelve poco a poco (CinematicaSonidoMixin, EcoVoz).
 */
public final class Cinematicas {
	private Cinematicas() {
	}

	public static boolean alguna() {
		return CinematicaCliente.activa() || ToboganCliente.activa() || SalidaFiestaCliente.activa() || SalidaEstacionCliente.activa();
	}

	/** Si ese sonido es de la cinematica en curso (no se silencia). */
	public static boolean esBanda(Identifier id) {
		return CinematicaCliente.activa() && CinematicaCliente.esBanda(id)
			|| ToboganCliente.activa() && ToboganCliente.esBanda(id)
			|| SalidaFiestaCliente.activa() && SalidaFiestaCliente.esBanda(id)
			|| SalidaEstacionCliente.activa() && SalidaEstacionCliente.esBanda(id);
	}

	/** 0..1: cuanto se oye todo lo que no es la cinematica (1 si no hay ninguna). */
	public static float volumenMundo() {
		return CinematicaCliente.volumenMundo() * ToboganCliente.volumenMundo() * SalidaFiestaCliente.volumenMundo()
			* SalidaEstacionCliente.volumenMundo();
	}
}
