package net.backrooms.evento.acceso;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.backrooms.evento.BackroomsEvento;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * La lista del evento (la "whitelist"). No es un archivo del servidor: son los
 * nicks con entrada en la tienda (quien la compró, con el nick con el que
 * entró a la tienda, o a quien el staff se la dio). Así hay una sola lista,
 * guardada en la tienda desde la primera venta, y se ve en su panel.
 *
 * El servidor la descarga al arrancar y cada minuto (y la guarda en
 * config/backrooms-whitelist.json por si un día la tienda no responde):
 *  - al entrar, además del pase, el nick tiene que estar en la lista;
 *  - si alguien sale de la lista (reembolso, /brwhitelist quitar), se le echa.
 *
 * Comando (OP nivel 3 o consola), escribe en la tienda:
 *   /brwhitelist agregar <nick> [nota]   le crea su entrada y enseña el código para dárselo
 *   /brwhitelist quitar <nick>           anula sus entradas (y si está dentro, fuera)
 *   /brwhitelist ver <nick>              sus entradas y en qué estado están
 *   /brwhitelist lista                   los nicks de la lista
 *   /brwhitelist recargar                vuelve a descargarla ya
 *
 * Necesita en config/backrooms-acceso.json "token" (BACKROOMS_SERVIDOR_TOKEN de
 * la tienda); "tienda" ya viene puesta. Sin token no hay lista y solo cuenta el
 * pase. Los OP no pasan por la lista (sí por el pase).
 */
public final class Whitelist {
	private static final Gson GSON = new Gson();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static final Pattern NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
	private static final int CADA_TICKS = 20 * 60;
	/** Si de golpe sobran más que estos, no se echa a nadie: más vale un fallo de la tienda que vaciar el evento. */
	private static final int MAX_EXPULSIONES = 5;

	/** En minúsculas, para comprobar. */
	private static volatile Set<String> nicks = Set.of();
	/** Tal cual vienen de la tienda, para enseñarlos. */
	private static volatile List<String> nombres = List.of();
	private static volatile boolean cargada;
	private static @Nullable MinecraftServer servidor;
	private static @Nullable Path cache;
	private static int ticks;
	private static volatile boolean pidiendo;
	private static boolean avisadoSinToken;
	private static boolean avisadoFallo;

	private Whitelist() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			servidor = s;
			cache = s.getServerDirectory().resolve("config").resolve("backrooms-whitelist.json");
			leerCache();
			actualizar(null);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> servidor = null);
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (++ticks % CADA_TICKS == 0) {
				actualizar(null);
			}
		});
		CommandRegistrationCallback.EVENT.register((d, registros, entorno) -> comando(d));
	}

	private static boolean activa() {
		Acceso.Config c = Acceso.config();
		return c.activo && c.token != null && !c.token.isBlank();
	}

	private static boolean esOp(String nick) {
		MinecraftServer s = servidor;
		return s != null && Arrays.stream(s.getPlayerList().getOpNames()).anyMatch(n -> n.equalsIgnoreCase(nick));
	}

	/**
	 * Al entrar (después de comprobar el pase): null si puede, o el motivo.
	 * Mientras no haya lista (sin token, la tienda sin responder y sin copia, o
	 * vacía) manda solo el pase: la lista sirve para echar a quien ya no vale.
	 */
	static @Nullable String comprobar(String nick) {
		if (!activa() || !cargada || nicks.isEmpty() || esOp(nick)) {
			return null;
		}
		return nicks.contains(nick.toLowerCase(Locale.ROOT)) ? null : "Tu nick (" + nick + ") no está en la lista del evento.";
	}

	/* ------------------------------------------------------------- la lista */

	private static void leerCache() {
		try {
			if (cache != null && Files.exists(cache)) {
				JsonObject j = GSON.fromJson(Files.readString(cache, StandardCharsets.UTF_8), JsonObject.class);
				poner(leer(j.getAsJsonArray("nicks")));
				cargada = true;
				BackroomsEvento.LOG.info("Lista del evento: {} nicks (copia guardada)", nicks.size());
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.warn("No se pudo leer config/backrooms-whitelist.json: {}", e.toString());
		}
	}

	private static List<String> leer(@Nullable JsonArray a) {
		List<String> l = new ArrayList<>();
		if (a != null) {
			for (JsonElement e : a) {
				l.add(e.getAsString());
			}
		}
		return l;
	}

	private static Set<String> minusculas(List<String> l) {
		Set<String> s = new HashSet<>();
		l.forEach(n -> s.add(n.toLowerCase(Locale.ROOT)));
		return Set.copyOf(s);
	}

	private static void poner(List<String> l) {
		nombres = l.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
		nicks = minusculas(l);
	}

	private static HttpRequest.Builder peticion(String ruta) {
		Acceso.Config c = Acceso.config();
		String base = c.tienda.replaceAll("/+$", "");
		return HttpRequest.newBuilder(URI.create(base + "/whitelist" + ruta))
			.timeout(Duration.ofSeconds(15))
			.header("Authorization", "Bearer " + c.token.trim())
			.header("Accept", "application/json");
	}

	/** Descarga la lista. `fin` (opcional) recibe el resultado en el hilo del servidor. */
	private static void actualizar(@Nullable Consumer<Component> fin) {
		if (!activa()) {
			if (!avisadoSinToken && Acceso.config().activo) {
				avisadoSinToken = true;
				BackroomsEvento.LOG.warn("Sin \"token\" en config/backrooms-acceso.json: no hay lista del evento, solo se comprueba el pase");
			}
			if (fin != null) {
				fin.accept(Component.literal("La lista está apagada: falta \"token\" en config/backrooms-acceso.json.").withStyle(ChatFormatting.RED));
			}
			return;
		}
		if (pidiendo) {
			return;
		}
		pidiendo = true;
		HTTP.sendAsync(peticion("").GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
			.whenComplete((r, error) -> {
				pidiendo = false;
				MinecraftServer s = servidor;
				if (s == null) {
					return;
				}
				if (error != null || r.statusCode() != 200) {
					String motivo = error != null ? error.toString() : "HTTP " + r.statusCode() + " " + r.body();
					if (!avisadoFallo) {
						avisadoFallo = true;
						BackroomsEvento.LOG.warn("No se pudo descargar la lista del evento ({}); sigo con la que tenía", motivo);
					}
					if (fin != null) {
						s.execute(() -> fin.accept(Component.literal("No se pudo descargar la lista: " + motivo).withStyle(ChatFormatting.RED)));
					}
					return;
				}
				avisadoFallo = false;
				List<String> nueva;
				try {
					nueva = leer(GSON.fromJson(r.body(), JsonObject.class).getAsJsonArray("nicks"));
				} catch (Exception e) {
					BackroomsEvento.LOG.warn("La lista del evento llegó mal: {}", e.toString());
					return;
				}
				s.execute(() -> {
					aplicar(s, nueva);
					if (fin != null) {
						fin.accept(Component.literal("Lista recargada: " + nueva.size() + " nicks.").withStyle(ChatFormatting.GREEN));
					}
				});
			});
	}

	private static void aplicar(MinecraftServer s, List<String> lista) {
		Set<String> nueva = minusculas(lista);
		boolean cambio = !nueva.equals(nicks) || !cargada;
		poner(lista);
		cargada = true;
		if (cambio && cache != null) {
			try {
				JsonObject j = new JsonObject();
				JsonArray a = new JsonArray();
				nombres.forEach(a::add);
				j.add("nicks", a);
				Files.createDirectories(cache.getParent());
				Files.writeString(cache, GSON.toJson(j), StandardCharsets.UTF_8);
			} catch (Exception e) {
				BackroomsEvento.LOG.warn("No se pudo guardar config/backrooms-whitelist.json: {}", e.toString());
			}
		}
		if (nueva.isEmpty()) {
			return;
		}
		// quien ya no está en la lista, fuera (salvo OP y staff de la config)
		List<ServerPlayer> sobran = new ArrayList<>();
		for (ServerPlayer j : s.getPlayerList().getPlayers()) {
			String nick = j.getGameProfile().name();
			if (!nueva.contains(nick.toLowerCase(Locale.ROOT)) && !esOp(nick)
				&& Acceso.config().staff.stream().noneMatch(n -> n.equalsIgnoreCase(nick))) {
				sobran.add(j);
			}
		}
		if (sobran.size() > MAX_EXPULSIONES) {
			BackroomsEvento.LOG.warn("La lista nueva dejaría fuera a {} jugadores de golpe: no echo a nadie (¿fallo de la tienda?)", sobran.size());
			return;
		}
		for (ServerPlayer j : sobran) {
			BackroomsEvento.LOG.info("{} ya no está en la lista del evento: fuera", j.getGameProfile().name());
			j.connection.disconnect(Component.literal("BACKROOMS\n\n").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Tu entrada ya no es válida.").withStyle(ChatFormatting.WHITE))
				.append(Component.literal("\n\nSi crees que es un error, habla con el staff en el Discord.").withStyle(ChatFormatting.GRAY)));
		}
	}

	/* ----------------------------------------------------------- el comando */

	private static void comando(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("brwhitelist")
			.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
			.then(Commands.literal("agregar")
				.then(Commands.argument("nick", StringArgumentType.word())
					.executes(c -> agregar(c, ""))
					.then(Commands.argument("nota", StringArgumentType.greedyString())
						.executes(c -> agregar(c, StringArgumentType.getString(c, "nota"))))))
			.then(Commands.literal("quitar")
				.then(Commands.argument("nick", StringArgumentType.word()).executes(Whitelist::quitar)))
			.then(Commands.literal("ver")
				.then(Commands.argument("nick", StringArgumentType.word()).executes(Whitelist::ver)))
			.then(Commands.literal("lista").executes(Whitelist::lista))
			.then(Commands.literal("recargar").executes(c -> {
				CommandSourceStack src = c.getSource();
				actualizar(m -> src.sendSuccess(() -> m, false));
				return 1;
			})));
	}

	private static @Nullable String nickDe(CommandContext<CommandSourceStack> c) {
		String nick = StringArgumentType.getString(c, "nick");
		if (!NICK.matcher(nick).matches()) {
			c.getSource().sendFailure(Component.literal("«" + nick + "» no es un nick válido (3-16 letras, números o _)."));
			return null;
		}
		if (!activa()) {
			c.getSource().sendFailure(Component.literal("La lista está apagada: falta \"token\" en config/backrooms-acceso.json."));
			return null;
		}
		return nick;
	}

	/** Manda la petición y pasa la respuesta (o el error) al hilo del servidor. */
	private static void enviar(CommandSourceStack src, HttpRequest req, Consumer<JsonObject> ok) {
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((r, error) -> {
			MinecraftServer s = servidor;
			if (s == null) {
				return;
			}
			s.execute(() -> {
				if (error != null) {
					src.sendFailure(Component.literal("La tienda no responde: " + error.getMessage()));
					return;
				}
				JsonObject j;
				try {
					j = GSON.fromJson(r.body(), JsonObject.class);
				} catch (Exception e) {
					j = null;
				}
				if (r.statusCode() != 200 || j == null) {
					String msg = j != null && j.has("error") ? j.get("error").getAsString() : "HTTP " + r.statusCode();
					src.sendFailure(Component.literal("La tienda dice: " + msg));
					return;
				}
				ok.accept(j);
			});
		});
	}

	private static int agregar(CommandContext<CommandSourceStack> c, String nota) {
		String nick = nickDe(c);
		if (nick == null) {
			return 0;
		}
		CommandSourceStack src = c.getSource();
		JsonObject cuerpo = new JsonObject();
		cuerpo.addProperty("nick", nick);
		cuerpo.addProperty("por", src.getTextName());
		if (!nota.isBlank()) {
			cuerpo.addProperty("nota", nota);
		}
		HttpRequest req = peticion("").header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(cuerpo), StandardCharsets.UTF_8)).build();
		enviar(src, req, j -> {
			String codigo = j.get("codigo").getAsString();
			boolean nuevo = j.get("nuevo").getAsBoolean();
			MutableComponent cod = Component.literal(codigo).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withUnderlined(true)
				.withClickEvent(new ClickEvent.CopyToClipboard(codigo))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Clic para copiar"))));
			src.sendSuccess(() -> Component.literal(nick + (nuevo ? " añadido a la lista del evento. " : " ya estaba en la lista. "))
				.withStyle(ChatFormatting.GREEN)
				.append(Component.literal("Su código: ").withStyle(ChatFormatting.GRAY))
				.append(cod)
				.append(Component.literal(" (pásaselo: lo escribe en el launcher con ese nick)").withStyle(ChatFormatting.GRAY)), true);
			actualizar(null);
		});
		return 1;
	}

	private static int quitar(CommandContext<CommandSourceStack> c) {
		String nick = nickDe(c);
		if (nick == null) {
			return 0;
		}
		CommandSourceStack src = c.getSource();
		enviar(src, peticion("/" + nick).DELETE().build(), j -> {
			int n = j.get("anuladas").getAsInt();
			src.sendSuccess(() -> Component.literal(n > 0
				? nick + " fuera de la lista (" + n + (n == 1 ? " entrada anulada)." : " entradas anuladas).")
				: nick + " no estaba en la lista.").withStyle(n > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
			actualizar(null);
		});
		return 1;
	}

	private static int ver(CommandContext<CommandSourceStack> c) {
		String nick = nickDe(c);
		if (nick == null) {
			return 0;
		}
		CommandSourceStack src = c.getSource();
		enviar(src, peticion("/" + nick).GET().build(), j -> {
			JsonArray entradas = j.getAsJsonArray("entradas");
			if (entradas == null || entradas.isEmpty()) {
				src.sendSuccess(() -> Component.literal(nick + " no tiene ninguna entrada.").withStyle(ChatFormatting.GRAY), false);
				return;
			}
			src.sendSuccess(() -> Component.literal("Entradas de " + nick + ":").withStyle(ChatFormatting.GOLD), false);
			for (JsonElement e : entradas) {
				JsonObject o = e.getAsJsonObject();
				String codigo = o.get("codigo").getAsString();
				boolean anulada = o.get("revocado").getAsBoolean();
				boolean canjeada = o.has("canjeado_en") && !o.get("canjeado_en").isJsonNull();
				String estado = anulada ? "anulada" : canjeada ? "canjeada" : "sin canjear";
				String origen = o.get("origen").getAsString();
				src.sendSuccess(() -> Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(codigo).withStyle(s -> s.withColor(anulada ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW)
						.withClickEvent(new ClickEvent.CopyToClipboard(codigo))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Clic para copiar")))))
					.append(Component.literal("  " + origen + " · " + estado).withStyle(ChatFormatting.GRAY)), false);
			}
		});
		return 1;
	}

	private static int lista(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		if (!activa()) {
			src.sendFailure(Component.literal("La lista está apagada: falta \"token\" en config/backrooms-acceso.json."));
			return 0;
		}
		List<String> todos = nombres;
		String muestra = String.join(", ", todos.subList(0, Math.min(60, todos.size())));
		String resto = todos.size() > 60 ? " … (+" + (todos.size() - 60) + ")" : "";
		src.sendSuccess(() -> Component.literal(todos.size() + " nicks en la lista" + (cargada ? "" : " (sin descargar aún)") + ": ")
			.withStyle(ChatFormatting.GOLD)
			.append(Component.literal(muestra + resto).withStyle(ChatFormatting.WHITE)), false);
		return 1;
	}
}
