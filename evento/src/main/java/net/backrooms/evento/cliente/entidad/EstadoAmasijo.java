package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo del Amasijo necesita en este frame. */
public class EstadoAmasijo extends LivingEntityRenderState {
	/** 0..1: cuanto esta cazando. */
	public float caza;
	public boolean agarrando;
}
