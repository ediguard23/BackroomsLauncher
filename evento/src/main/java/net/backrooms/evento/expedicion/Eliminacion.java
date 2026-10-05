package net.backrooms.evento.expedicion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.red.Eliminado;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

/**
 * Quien cae en el Nivel 0 pierde: todos ven la animacion de su eliminacion
 * (con su nick, su cara y cuantos quedan) y, a los pocos segundos, se le
 * banea con el /ban de Minecraft de siempre (queda en banned-players.json y
 * al volver a entrar ve el motivo).
 *
 * Los OP no se banean: son el staff probando. Al acabar el evento (o tras una
 * prueba) `/backrooms perdonar` quita el ban a todos los eliminados, y solo a
 * ellos: se reconocen por el motivo del ban.
 */
public final class Eliminacion {
	public static final String MOTIVO = "Has caído en los Backrooms. Tu expedición ha terminado.";
	/** Lo que tarda el ban: lo justo para que vea su muerte y todos la animacion. */
	private static final int RETRASO_TICKS = 20 * 4;

	private static final Eliminacion INSTANCIA = new Eliminacion();

	private final Map<UUID, Pendiente> pendientes = new HashMap<>();
	private MinecraftServer servidor;
	private long ticks;
	/** Mientras se reenvia el mensaje de muerte ya formateado (para no interceptarlo otra vez). */
	private boolean reenviando;

	private record Pendiente(String nombre, long cuando) {
	}

	private Eliminacion() {
	}

	public static Eliminacion get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerMessageEvents.ALLOW_GAME_MESSAGE.register((s, mensaje, encima) -> INSTANCIA.mensajeMuerte(s, mensaje, encima));
	}

	/** Llamado al morir alguien que estaba en la expedicion (ya eliminado de las misiones). */
	public void eliminado(ServerPlayer j) {
		if (this.servidor == null) {
			return;
		}
		String nombre = j.getGameProfile().name();
		int quedan = 0;
		for (ServerPlayer otro : this.servidor.getPlayerList().getPlayers()) {
			if (otro != j && !otro.isSpectator() && Misiones.get().enExpedicion(otro)) {
				quedan++;
			}
		}
		Eliminado aviso = new Eliminado(nombre, j.getUUID(), quedan);
		for (ServerPlayer otro : this.servidor.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(otro, Eliminado.TYPE)) {
				ServerPlayNetworking.send(otro, aviso);
			}
		}
		if (this.servidor.getPlayerList().isOp(j.nameAndId())) {
			BackroomsEvento.LOG.info("{} ha caido, pero es OP: no se le banea", nombre);
			return;
		}
		this.pendientes.put(j.getUUID(), new Pendiente(nombre, this.ticks + RETRASO_TICKS));
	}

	/**
	 * El mensaje de muerte de Minecraft («x ha muerto», «x ha sido victima de La
	 * Bacteria»...) se cambia por una linea con el estilo del evento: [✖], la
	 * causa (traducida en cada cliente) y, si estaba en la expedicion, si queda
	 * eliminado y cuantos quedan.
	 */
	private boolean mensajeMuerte(MinecraftServer s, Component mensaje, boolean encima) {
		if (encima || this.reenviando || !(mensaje.getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith("death.")) {
			return true;
		}
		Object[] args = tc.getArgs();
		ServerPlayer victima = args.length > 0 && args[0] instanceof Component c ? s.getPlayerList().getPlayerByName(c.getString()) : null;
		MutableComponent linea = Component.empty()
			.append(Component.literal("[").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("✖").withColor(0xFF4A3D))
			.append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
			.append(mensaje.copy().withColor(0xE8E0D0));
		if (victima != null && net.backrooms.evento.fase.Fase.de(victima.level()) != null && Misiones.get().enExpedicion(victima)) {
			linea.append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY));
			if (net.backrooms.evento.vestibulo.Vestibulo.get().muerteElimina()) {
				int quedan = 0;
				for (ServerPlayer otro : s.getPlayerList().getPlayers()) {
					if (otro != victima && !otro.isSpectator() && Misiones.get().enExpedicion(otro)) {
						quedan++;
					}
				}
				linea.append(Component.literal("ELIMINADO").withColor(0xFF4A3D).withStyle(ChatFormatting.BOLD))
					.append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
					.append(Component.literal("quedan " + quedan + (quedan == 1 ? " explorador" : " exploradores")).withColor(0xF2E6A0));
			} else {
				linea.append(Component.literal("vuelve a la fase").withStyle(ChatFormatting.GRAY));
			}
		}
		this.reenviando = true;
		try {
			s.getPlayerList().broadcastSystemMessage(linea, false);
		} finally {
			this.reenviando = false;
		}
		return false;
	}

	private void tick() {
		this.ticks++;
		if (this.pendientes.isEmpty() || this.servidor == null) {
			return;
		}
		List<UUID> hechos = new ArrayList<>();
		for (Map.Entry<UUID, Pendiente> e : this.pendientes.entrySet()) {
			if (this.ticks >= e.getValue().cuando()) {
				this.comando("ban " + e.getValue().nombre() + " " + MOTIVO);
				BackroomsEvento.LOG.info("{} eliminado: baneado", e.getValue().nombre());
				hechos.add(e.getKey());
			}
		}
		hechos.forEach(this.pendientes::remove);
	}

	/** Quita el ban a todos los que cayeron en el evento. Devuelve cuantos. */
	public int perdonar() {
		if (this.servidor == null) {
			return 0;
		}
		List<String> nombres = new ArrayList<>();
		for (UserBanListEntry b : this.servidor.getPlayerList().getBans().getEntries()) {
			NameAndId u = b.getUser();
			if (u != null && MOTIVO.equals(b.getReason())) {
				nombres.add(u.name());
			}
		}
		nombres.forEach(n -> this.comando("pardon " + n));
		this.pendientes.clear();
		return nombres.size();
	}

	private void comando(String c) {
		CommandSourceStack fuente = this.servidor.createCommandSourceStack()
			.withSuppressedOutput()
			.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
		this.servidor.getCommands().performPrefixedCommand(fuente, c);
	}
}
