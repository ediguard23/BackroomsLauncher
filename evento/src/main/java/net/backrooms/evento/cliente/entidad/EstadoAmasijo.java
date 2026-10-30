package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Lo que el modelo del Amasijo necesita en este frame. */
public class EstadoAmasijo extends LivingEntityRenderState {
	/** 0..1: cuanto esta cazando. */
	public float caza;
	public boolean agarrando;
	/** Solo en las cinematicas (SalidaEstacionCliente), 0..1: el salto sobre la presa, comiendo y alerta (ha oido algo). */
	public float embiste;
	public float come;
	public float alerta;
}
