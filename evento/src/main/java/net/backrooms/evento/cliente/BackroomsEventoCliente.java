package net.backrooms.evento.cliente;

import java.util.List;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.bloques.Bloques;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.cliente.cinematica.PantallaCinematica;
import net.backrooms.evento.cliente.efectos.Cordura;
import net.backrooms.evento.cliente.entidad.BacteriaRenderer;
import net.backrooms.evento.cliente.entidad.MallaBacteria;
import net.backrooms.evento.cliente.entidad.MallaSmiler;
import net.backrooms.evento.cliente.entidad.SmilerRenderer;
import net.backrooms.evento.red.EstadoJugador;
import net.backrooms.evento.red.Susto;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.backrooms.evento.cliente.efectos.EfectosMundo;
import net.backrooms.evento.cliente.efectos.Flash;
import net.backrooms.evento.cliente.efectos.Miedo;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.red.EstadoAmbiente;
import net.backrooms.evento.red.IniciarCinematica;
import net.backrooms.evento.red.SyncMisiones;
import net.backrooms.evento.red.ViajeAscensor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.Identifier;

/**
 * Parte de cliente del mod del evento: capas de los bloques con huecos
 * transparentes (si no, salen negros), el dibujo de los casetes, el estado
 * de las misiones, la luz (apagones y alarmas), la linterna y la camara, el
 * ascensor de salida y los efectos de pantalla.
 */
public class BackroomsEventoCliente implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
			Bloques.DIBUJO, Bloques.VENTILADOR, Bloques.VENTILADOR_GRANDE, Bloques.CAPA_BACILO,
			Bloques.SENAL_ALTO, Bloques.SENAL_PELIGRO, Bloques.SENAL_SIGA, Bloques.SENAL_ALTO_REVES, Bloques.SENAL_SIGA_REVES,
			Bloques.PANEL_ASCENSOR, Bloques.HUECO);
		EntityRenderers.register(Entidades.CASETE, CaseteRenderer::new);
		EntityRenderers.register(Entidades.ASIENTO, NoopRenderer::new);
		EntityRenderers.register(Entidades.COMIDA, ComidaRenderer::new);
		EntityRenderers.register(Entidades.BACTERIA, BacteriaRenderer::new);
		EntityRenderers.register(Entidades.SMILER, SmilerRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(BacteriaRenderer.CAPA, MallaBacteria::crear);
		EntityModelLayerRegistry.registerModelLayer(SmilerRenderer.CAPA, MallaSmiler::crear);
		// los tubos encendidos se tinen: grises en el apagon, rojos con la alarma
		ColorProviderRegistry.BLOCK.register((estado, mundo, pos, tinte) -> AmbienteCliente.colorTubos(),
			Bloques.FLUORESCENTE, Bloques.FLUORESCENTE_PARPADEO);

		ClientPlayNetworking.registerGlobalReceiver(SyncMisiones.TYPE, (s, ctx) -> MisionesCliente.recibir(s));
		ClientPlayNetworking.registerGlobalReceiver(IniciarCinematica.TYPE, (s, ctx) -> CinematicaCliente.empezar());
		ClientPlayNetworking.registerGlobalReceiver(EstadoAmbiente.TYPE, (s, ctx) -> AmbienteCliente.recibir(s));
		ClientPlayNetworking.registerGlobalReceiver(ViajeAscensor.TYPE, (s, ctx) -> AscensorCliente.empezar(s));
		ClientPlayNetworking.registerGlobalReceiver(EstadoJugador.TYPE, (s, ctx) -> SupervivenciaCliente.recibir(s));
		ClientPlayNetworking.registerGlobalReceiver(Susto.TYPE, (s, ctx) -> SupervivenciaCliente.susto(s));
		ClientPlayNetworking.registerGlobalReceiver(net.backrooms.evento.red.Eliminado.TYPE, (s, ctx) -> Eliminaciones.recibir(s));
		ClientPlayNetworking.registerGlobalReceiver(net.backrooms.evento.red.Agarrado.TYPE, (s, ctx) -> AgarreCliente.empezar(s));
		HerramientasCliente.registrar();
		HuecoCliente.registrar();
		AccesoCliente.registrar();

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			AmbienteCliente.tick();
			Cordura.tick();
			SupervivenciaCliente.tick(mc);
			Balanceo.tick(mc);
			AgarreCliente.tick(mc);
			Miedo.tick(mc);
			net.backrooms.evento.cliente.cinematica.CinematicaCliente.tick(mc);
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, BackroomsEvento.id("supervivencia"), SupervivenciaCliente::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, BackroomsEvento.id("camara"), CamaraHud::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, BackroomsEvento.id("hueco"), HuecoCliente::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, BackroomsEvento.id("agarre"), AgarreCliente::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, BackroomsEvento.id("ascensor"), AscensorCliente::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, BackroomsEvento.id("eliminaciones"), Eliminaciones::render);
		// con la camara levantada no se ve nada del juego (mira, barra, vida, armadura, comida,
		// aire, experiencia): solo el visor
		for (Identifier elemento : List.of(VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.ARMOR_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
			VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP)) {
			HudElementRegistry.replaceElement(elemento, viejo -> (g, t) -> {
				if (HerramientasCliente.subida(1.0F) < 0.5F) {
					viejo.render(g, t);
				}
			});
		}

		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
			MisionesCliente.olvidar();
			CinematicaCliente.olvidar();
			AmbienteCliente.olvidar();
			HerramientasCliente.olvidar();
			AscensorCliente.olvidar();
			Cordura.olvidar();
			Flash.olvidar();
			SupervivenciaCliente.olvidar();
			Miedo.olvidar();
			AgarreCliente.olvidar();
			HuecoCliente.olvidar();
		});
		PantallaCinematica.cargar();
		EfectosMundo.cargar();
		OrdenesPrueba.registrar();
	}
}
