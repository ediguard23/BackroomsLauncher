package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo del Anfitrion necesita en este frame. */
public class EstadoAnfitrion extends LivingEntityRenderState {
	/** 0..1: cuanto esta cazando. */
	public float caza;
	public boolean abrazando;
}
