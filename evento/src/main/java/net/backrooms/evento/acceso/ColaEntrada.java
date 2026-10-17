package net.backrooms.evento.acceso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mixin.ServerLoginAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

/**
 * La cola para entrar al servidor: entra un jugador cada CADA ticks (3 s), por orden de
 * llegada, para que 100 personas pulsando JUGAR a la vez no tumben el servidor. Mientras
 * espera, el inicio de sesion se queda parado (ServerLoginConnectionEvents, LoginSynchronizer)
 * y cada segundo se le dice su puesto por el canal CANAL; el mod lo ensena en la pantalla de
 * conexion (ColaCliente). Para que Minecraft no le eche por tardar en entrar (30 s), su
 * contador se pone a cero mientras espera (ColaLoginMixin). El staff (la lista de staff de
 * config/backrooms-acceso.json y los OP) se salta la cola.
 */
public final class ColaEntrada {
	public static final Identifier CANAL = BackroomsEvento.id("cola");
	/** Uno cada 3 s. */
	private static final int CADA = 60;

	private static final class Turno {
		final ServerLoginPacketListenerImpl conexion;
		final LoginPacketSender envio;
		final CompletableFuture<Void> listo = new CompletableFuture<>();

		Turno(ServerLoginPacketListenerImpl conexion, LoginPacketSender envio) {
			this.conexion = conexion;
			this.envio = envio;
		}
	}

	private static final List<Turno> COLA = new ArrayList<>();
	private static long ticks;
	private static long ultimo = -CADA;

	private ColaEntrada() {
	}

	public static void registrar() {
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sincronizador) -> {
			String nick = ((ServerLoginAccessor) handler).backrooms$nick();
			if (salta(server, nick)) {
				return;
			}
			Turno t = new Turno(handler, sender);
			COLA.add(t);
			sincronizador.waitFor(t.listo);
			avisar(t, COLA.size());
		});
		ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> COLA.removeIf(t -> t.conexion == handler));
		// el cliente contesta a cada aviso: no hay nada que hacer con la respuesta
		ServerLoginNetworking.registerGlobalReceiver(CANAL, (server, handler, entendido, buf, sincronizador, respuesta) -> {
		});
		ServerTickEvents.END_SERVER_TICK.register(ColaEntrada::tick);
	}

	/** Si esa conexion esta esperando turno (para no echarla por tardar). */
	public static boolean esperando(ServerLoginPacketListenerImpl conexion) {
		for (Turno t : COLA) {
			if (t.conexion == conexion) {
				return true;
			}
		}
		return false;
	}

	private static boolean salta(MinecraftServer servidor, String nick) {
		if (nick == null) {
			return false;
		}
		if (Acceso.config().staff.stream().anyMatch(s -> s.equalsIgnoreCase(nick))) {
			return true;
		}
		for (String op : servidor.getPlayerList().getOpNames()) {
			if (op.equalsIgnoreCase(nick)) {
				return true;
			}
		}
		return false;
	}

	private static void tick(MinecraftServer servidor) {
		ticks++;
		if (!COLA.isEmpty() && ticks - ultimo >= CADA) {
			Turno t = COLA.remove(0);
			ultimo = ticks;
			t.listo.complete(null);
		}
		if (ticks % 20 == 0) {
			for (int i = 0; i < COLA.size(); i++) {
				avisar(COLA.get(i), i + 1);
			}
		}
	}

	/** Le dice su puesto, cuantos hay y los segundos que le quedan mas o menos. */
	private static void avisar(Turno t, int puesto) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(puesto);
		buf.writeVarInt(COLA.size());
		buf.writeVarInt(Math.max(1, (int) Math.ceil(((puesto - 1) * CADA + Math.max(0, CADA - (ticks - ultimo))) / 20.0)));
		try {
			t.envio.sendPacket(CANAL, buf);
		} catch (Exception e) {
			// la conexion ya se cerro: la quita DISCONNECT
		}
	}
}
