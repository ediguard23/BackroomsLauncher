package net.backrooms.evento.cliente;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.acceso.Acceso;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Responde a la pregunta de acceso del servidor (ver Acceso): manda el pase
 * que guardo el launcher en backrooms-pase.json y firma el reto con la clave
 * de este dispositivo. Si no hay pase, responde vacio y el servidor explica
 * como conseguir la entrada; entonces, en vez de la pantalla de desconexion,
 * sale PantallaCodigo para canjear el codigo ahi mismo (quien juega sin el
 * launcher: Pojav en el movil, un Minecraft montado a mano).
 */
public final class AccesoCliente {
	/** El servidor al que se intento entrar (para volver a conectar tras canjear el codigo). */
	private static net.minecraft.client.multiplayer.@org.jspecify.annotations.Nullable ServerData servidor;
	/** Se respondio al reto y aun no se ha entrado: si echan por la entrada, se pide el codigo. */
	private static boolean intento;

	private AccesoCliente() {
	}

	/** Cada tick: una vez dentro del servidor, ya no hace falta pedir el codigo. */
	public static void tick(Minecraft mc) {
		if (intento && mc.level != null) {
			intento = false;
		}
	}

	/**
	 * Al echarle del servidor (MenuBackrooms, en lugar de SenalPerdida): si fue por no tener entrada (el
	 * mensaje de Acceso lleva la tienda), la pantalla para canjear el codigo; si no, null.
	 */
	public static net.minecraft.client.gui.screens.@org.jspecify.annotations.Nullable Screen pedirCodigo(net.minecraft.network.chat.Component razon) {
		String texto = razon.getString();
		if (!intento || !texto.contains("tienda.peakmc.lat")) {
			return null;
		}
		intento = false;
		// el mensaje es "BACKROOMS", el por que y donde comprar, separados por lineas en blanco: se ensena el por que
		String[] partes = texto.split("\n\n");
		String motivo = partes.length > 1 ? partes[1].trim() : "";
		return new PantallaCodigo(servidor, net.minecraft.network.chat.Component.literal(motivo));
	}

	public static void registrar() {
		ClientLoginNetworking.registerGlobalReceiver(Acceso.CANAL, (cliente, handler, buf, callbacks) -> {
			byte[] reto = buf.readByteArray(64);
			servidor = ((net.backrooms.evento.mixin.cliente.InicioSesionAccessor) handler).backrooms$servidor();
			return CompletableFuture.completedFuture(responder(cliente, reto));
		});
	}

	private static FriendlyByteBuf responder(Minecraft cliente, byte[] reto) {
		FriendlyByteBuf r = PacketByteBufs.create();
		intento = true;
		try {
			Path f = cliente.gameDirectory.toPath().resolve("backrooms-pase.json");
			JsonObject j = new Gson().fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
			PrivateKey clave = KeyFactory.getInstance("Ed25519")
				.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(j.get("clave").getAsString())));
			Signature s = Signature.getInstance("Ed25519");
			s.initSign(clave);
			s.update(reto);
			r.writeUtf(j.get("pase").getAsString(), 4096);
			r.writeByteArray(s.sign());
		} catch (Exception e) {
			BackroomsEvento.LOG.info("Sin pase de entrada ({})", e.getClass().getSimpleName());
			r.writeUtf("", 4096);
			r.writeByteArray(new byte[0]);
		}
		return r;
	}
}
