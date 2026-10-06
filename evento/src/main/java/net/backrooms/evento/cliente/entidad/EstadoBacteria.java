package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo de la Bacteria necesita saber en cada frame. */
public class EstadoBacteria extends LivingEntityRenderState {
	/** 0..1: cuanto esta en modo caza (suavizado en el cliente). */
	public float caza;
	/** Ticks desde que agarro a alguien (con fraccion), o -1. */
	public float agarre = -1.0F;
}
