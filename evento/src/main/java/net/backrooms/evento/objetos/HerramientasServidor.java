package net.backrooms.evento.objetos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.red.AccionJugador;
import net.backrooms.evento.red.Herramientas;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Linternas y camaras de los jugadores, del lado del servidor.
 *
 * El servidor es quien manda: guarda si cada uno lleva la linterna encendida
 * y la camara levantada, las apaga si no tiene el objeto y reparte a cada
 * cliente quien tiene la linterna encendida cerca (para dibujar sus focos en
 * el apagon). Con la camara levantada se anda mas despacio.
 *
 * Ni la linterna ni la camara se pueden tirar: si alguien las suelta,
 * vuelven a su inventario.
 */
public final class HerramientasServidor {
	private static final HerramientasServidor INSTANCIA = new HerramientasServidor();
	/** Hasta donde se ven los focos de las linternas de los demas. */
	private static final double ALCANCE = 64;
	private static final Identifier LENTO = BackroomsEvento.id("camara_levantada");

	public static HerramientasServidor get() {
		return INSTANCIA;
	}

	private final Map<UUID, boolean[]> estados = new HashMap<>();
	private final Map<UUID, String> enviado = new HashMap<>();
	private MinecraftServer servidor;
	private long ticks;

	private HerramientasServidor() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayNetworking.registerGlobalReceiver(AccionJugador.TYPE, (p, ctx) -> INSTANCIA.accion(ctx.player(), p.accion(), p.valor()));
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			INSTANCIA.estados.remove(h.player.getUUID());
			INSTANCIA.enviado.remove(h.player.getUUID());
		});
		ServerEntityEvents.ENTITY_LOAD.register((entidad, nivel) -> INSTANCIA.devolverSiSeTira(entidad));
	}

	private boolean[] estado(ServerPlayer j) {
		return this.estados.computeIfAbsent(j.getUUID(), k -> new boolean[2]);
	}

	public boolean linterna(ServerPlayer j) {
		return this.estado(j)[AccionJugador.LINTERNA];
	}

	public boolean camara(ServerPlayer j) {
		return this.estado(j)[AccionJugador.CAMARA];
	}

	private static Item objeto(int accion) {
		return accion == AccionJugador.LINTERNA ? Objetos.LINTERNA : Objetos.CAMARA;
	}

	public static boolean lleva(ServerPlayer j, Item objeto) {
		return j.getInventory().contains(s -> s.is(objeto));
	}

	/** Enciende o apaga (si lleva el objeto). */
	public void accion(ServerPlayer j, int accion, boolean valor) {
		if (accion == AccionJugador.ARRASTRARSE) {
			net.backrooms.evento.escondite.Arrastre.pedir(j, valor);
			return;
		}
		if (accion != AccionJugador.LINTERNA && accion != AccionJugador.CAMARA) {
			return;
		}
		boolean[] e = this.estado(j);
		boolean nuevo = valor && !j.isSpectator() && lleva(j, objeto(accion));
		if (e[accion] == nuevo) {
			this.enviado.remove(j.getUUID()); // que le llegue el estado bueno
			return;
		}
		e[accion] = nuevo;
		if (accion == AccionJugador.LINTERNA) {
			this.marcarLinterna(j, nuevo);
			j.level().playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.LINTERNA, SoundSource.PLAYERS, 0.5F, nuevo ? 1.1F : 0.85F);
		} else {
			this.lento(j, nuevo);
			j.level().playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.CAMARA, SoundSource.PLAYERS, 0.45F, nuevo ? 1.0F : 0.8F);
		}
		this.enviado.remove(j.getUUID());
	}

	/** El dibujo de la linterna cambia (lente encendida) en el inventario y en la mano. */
	private void marcarLinterna(ServerPlayer j, boolean encendida) {
		for (int i = 0; i < j.getInventory().getContainerSize(); i++) {
			ItemStack s = j.getInventory().getItem(i);
			if (s.is(Objetos.LINTERNA)) {
				s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(encendida), List.of(), List.of()));
			}
		}
	}

	private void lento(ServerPlayer j, boolean si) {
		AttributeInstance a = j.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a == null) {
			return;
		}
		a.removeModifier(LENTO);
		if (si) {
			a.addTransientModifier(new AttributeModifier(LENTO, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private void tick() {
		if (this.servidor == null || ++this.ticks % 4 != 0) {
			return;
		}
		List<ServerPlayer> todos = this.servidor.getPlayerList().getPlayers();
		for (ServerPlayer j : todos) {
			boolean[] e = this.estado(j);
			// si ha perdido el objeto (o esta de espectador) se apaga solo
			for (int a = 0; a < 2; a++) {
				if (e[a] && (j.isSpectator() || !lleva(j, objeto(a)))) {
					this.accion(j, a, false);
				}
			}
		}
		for (ServerPlayer j : todos) {
			List<Integer> cerca = new ArrayList<>();
			for (ServerPlayer o : j.level().players()) {
				if (o != j && this.linterna(o) && o.distanceToSqr(j) < ALCANCE * ALCANCE) {
					cerca.add(o.getId());
				}
			}
			int[] ids = cerca.stream().mapToInt(Integer::intValue).sorted().toArray();
			boolean[] e = this.estado(j);
			String firma = e[0] + "," + e[1] + Arrays.toString(ids);
			if (!firma.equals(this.enviado.get(j.getUUID())) && ServerPlayNetworking.canSend(j, Herramientas.TYPE)) {
				ServerPlayNetworking.send(j, new Herramientas(e[0], e[1], ids));
				this.enviado.put(j.getUUID(), firma);
			}
		}
	}

	/** La linterna o la camara tiradas al suelo vuelven a quien las tiro. */
	private void devolverSiSeTira(Entity entidad) {
		if (!(entidad instanceof ItemEntity item) || this.servidor == null) {
			return;
		}
		ItemStack s = item.getItem();
		if (!s.is(Objetos.LINTERNA) && !s.is(Objetos.CAMARA)) {
			return;
		}
		Entity dueno = item.getOwner();
		if (dueno instanceof ServerPlayer j && !j.isCreative()) {
			ItemStack copia = s.copy();
			this.servidor.execute(() -> {
				item.discard();
				if (!lleva(j, copia.getItem())) {
					j.getInventory().add(copia);
				}
			});
		}
	}
}
