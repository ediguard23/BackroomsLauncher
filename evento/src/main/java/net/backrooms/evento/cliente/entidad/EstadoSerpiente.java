package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo de la Serpiente necesita para animarse en este frame. */
public class EstadoSerpiente extends LivingEntityRenderState {
	/** 0..1: cuanto esta cazando. */
	public float caza;
	/** Ticks desde que empezo a atacar, o -1. */
	public float ataque = -1.0F;
	/** Si esta en el agua (nada en vez de arrastrarse). */
	public boolean agua;
}
