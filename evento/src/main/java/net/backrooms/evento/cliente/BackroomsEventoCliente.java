package net.backrooms.evento.cliente;

import net.backrooms.evento.bloques.Bloques;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Parte de cliente del mod del evento. Por ahora: los bloques con huecos
 * transparentes (dibujos, aspas, senales, venas) se dibujan recortados; sin
 * esto los pixeles transparentes salen negros.
 */
public class BackroomsEventoCliente implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
			Bloques.DIBUJO, Bloques.VENTILADOR, Bloques.VENTILADOR_GRANDE, Bloques.CAPA_BACILO,
			Bloques.SENAL_ALTO, Bloques.SENAL_PELIGRO, Bloques.SENAL_SIGA, Bloques.SENAL_ALTO_REVES, Bloques.SENAL_SIGA_REVES);
	}
}
