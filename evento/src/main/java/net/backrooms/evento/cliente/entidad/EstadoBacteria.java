package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo de la Bacteria necesita saber en cada frame. */
public class EstadoBacteria extends LivingEntityRenderState {
	/** 0..1: cuanto esta en modo caza (suavizado en el cliente). */
	public float caza;
}
