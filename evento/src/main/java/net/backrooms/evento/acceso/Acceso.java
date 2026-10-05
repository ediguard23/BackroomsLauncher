package net.backrooms.evento.acceso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mixin.ServerLoginAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jspecify.annotations.Nullable;

/**
 * Solo entra quien ha pagado. El servidor del evento esta en modo offline
 * (entran premium y no premium), asi que el nick no prueba nada por si solo:
 * al conectarse, durante el login, el servidor manda un reto aleatorio y el
 * cliente (este mod, puesto por el Backrooms Launcher) responde con:
 *  - el PASE que firmo la tienda al canjear el codigo de compra: dice para que
 *    nick y que evento vale, hasta cuando, y la clave publica del dispositivo;
 *  - la firma del reto hecha con la clave privada de ese dispositivo.
 * Se comprueba la firma de la tienda (Ed25519, con su clave publica), que el
 * nick sea el que se conecta, que no haya caducado y la firma del reto. Si
 * algo falla, fuera, con un mensaje que dice donde comprar la entrada.
 * Un cliente sin el mod ni siquiera entiende la pregunta: tambien fuera.
 *
 * Configuracion en config/backrooms-acceso.json (se crea sola):
 *  activo        false para un servidor de pruebas sin entradas
 *  clavePublica  la de la tienda (node tools/backrooms.js clave)
 *  evento        tiene que coincidir con el del pase (BACKROOMS_EVENTO de la tienda)
 *  staff         nicks que entran sin pase (para emergencias; mejor darles entrada)
 */
public final class Acceso {
	public static final Identifier CANAL = BackroomsEvento.id("pase");
	/** La de la tienda de PeakMC (tienda.peakmc.lat). Solo firma quien tiene la privada. */
	private static final String CLAVE_TIENDA = "MCowBQYDK2VwAyEAlpnR/GA1CYRcPb8PE1wPgksTl5cmBu3D8JePxd6MYKk=";
	private static final String COMPRAR = "Compra tu entrada en tienda.peakmc.lat y entra con el Backrooms Launcher.";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final SecureRandom AZAR = new SecureRandom();
	private static final Map<ServerLoginPacketListenerImpl, byte[]> RETOS = new ConcurrentHashMap<>();

	/** Lo que hay en config/backrooms-acceso.json. */
	public static final class Config {
		public boolean activo = true;
		public String clavePublica = CLAVE_TIENDA;
		public String evento = "backrooms-0";
		public List<String> staff = new ArrayList<>();
	}

	private static Config config = new Config();
	private static @Nullable PublicKey clave;

	private Acceso() {
	}

	public static void registrar() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(Acceso::cargar);
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sincronizador) -> {
			if (!config.activo || clave == null) {
				return;
			}
			byte[] reto = new byte[32];
			AZAR.nextBytes(reto);
			RETOS.put(handler, reto);
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeByteArray(reto);
			sender.sendPacket(CANAL, buf);
		});
		ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> RETOS.remove(handler));
		ServerLoginNetworking.registerGlobalReceiver(CANAL, (server, handler, entendido, buf, sincronizador, respuesta) -> {
			byte[] reto = RETOS.remove(handler);
			if (reto == null) {
				return;
			}
			String nick = ((ServerLoginAccessor) handler).backrooms$nick();
			String fallo = entendido ? comprobar(nick, buf, reto) : "Entra con el Backrooms Launcher.";
			if (fallo != null) {
				BackroomsEvento.LOG.info("Acceso denegado a {}: {}", nick, fallo);
				handler.disconnect(Component.literal("BACKROOMS\n\n").withStyle(ChatFormatting.GOLD)
					.append(Component.literal(fallo).withStyle(ChatFormatting.WHITE))
					.append(Component.literal("\n\n" + COMPRAR).withStyle(ChatFormatting.GRAY)));
			}
		});
	}

	private static void cargar(MinecraftServer servidor) {
		Path f = servidor.getServerDirectory().resolve("config").resolve("backrooms-acceso.json");
		try {
			if (Files.exists(f)) {
				Config c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Config.class);
				if (c != null) {
					config = c;
				}
			} else {
				Files.createDirectories(f.getParent());
				Files.writeString(f, GSON.toJson(config), StandardCharsets.UTF_8);
			}
			clave = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(config.clavePublica.trim())));
			BackroomsEvento.LOG.info("Acceso con entrada: {} (evento {})", config.activo ? "ACTIVO" : "apagado", config.evento);
		} catch (Exception e) {
			clave = null;
			BackroomsEvento.LOG.error("config/backrooms-acceso.json no vale: el servidor queda ABIERTO sin comprobar entradas", e);
		}
	}

	/** null si el pase vale; si no, el motivo para el jugador. */
	static @Nullable String comprobar(String nick, FriendlyByteBuf buf, byte[] reto) {
		if (config.staff.stream().anyMatch(s -> s.equalsIgnoreCase(nick))) {
			return null;
		}
		String pase;
		byte[] firmaReto;
		try {
			pase = buf.readUtf(4096);
			firmaReto = buf.readByteArray(256);
		} catch (Exception e) {
			return "Respuesta de acceso no válida.";
		}
		if (pase.isEmpty()) {
			return "No tienes entrada para este nick (" + nick + ").";
		}
		try {
			String[] partes = pase.split("\\.");
			if (partes.length != 2) {
				return "El pase de entrada está dañado.";
			}
			byte[] datos = Base64.getUrlDecoder().decode(partes[0]);
			byte[] firma = Base64.getUrlDecoder().decode(partes[1]);
			Signature s = Signature.getInstance("Ed25519");
			s.initVerify(clave);
			s.update(datos);
			if (!s.verify(firma)) {
				return "El pase de entrada no es auténtico.";
			}
			JsonObject j = GSON.fromJson(new String(datos, StandardCharsets.UTF_8), JsonObject.class);
			if (!config.evento.equals(j.get("evento").getAsString())) {
				return "Tu entrada es de otro evento.";
			}
			if (!nick.toLowerCase(Locale.ROOT).equals(j.get("nick").getAsString().toLowerCase(Locale.ROOT))) {
				return "Tu entrada es para otro nick.";
			}
			if (j.get("exp").getAsLong() * 1000L < System.currentTimeMillis()) {
				return "Tu pase ha caducado: vuelve a escribir tu código en el launcher.";
			}
			PublicKey dispositivo = KeyFactory.getInstance("Ed25519")
				.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(j.get("disp").getAsString())));
			Signature sd = Signature.getInstance("Ed25519");
			sd.initVerify(dispositivo);
			sd.update(reto);
			if (!sd.verify(firmaReto)) {
				return "Este pase es de otro PC: vuelve a escribir tu código en el launcher.";
			}
			return null;
		} catch (Exception e) {
			return "No se pudo comprobar tu entrada.";
		}
	}
}
