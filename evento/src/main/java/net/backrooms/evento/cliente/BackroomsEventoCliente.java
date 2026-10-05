package net.backrooms.evento.cliente;

import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.red.SyncMisiones;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * Parte de cliente del mod del evento: capas de los bloques con huecos
 * transparentes (si no, salen negros), el dibujo de los casetes y el estado
 * de las misiones que manda el servidor.
 */
public class BackroomsEventoCliente implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
			Bloques.DIBUJO, Bloques.VENTILADOR, Bloques.VENTILADOR_GRANDE, Bloques.CAPA_BACILO,
			Bloques.SENAL_ALTO, Bloques.SENAL_PELIGRO, Bloques.SENAL_SIGA, Bloques.SENAL_ALTO_REVES, Bloques.SENAL_SIGA_REVES);
		EntityRenderers.register(Entidades.CASETE, CaseteRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(SyncMisiones.TYPE, (s, ctx) -> MisionesCliente.recibir(s));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> MisionesCliente.olvidar());
		OrdenesPrueba.registrar();
	}
}
