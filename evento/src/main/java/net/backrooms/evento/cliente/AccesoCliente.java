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
 * como conseguir la entrada.
 */
public final class AccesoCliente {
	private AccesoCliente() {
	}

	public static void registrar() {
		ClientLoginNetworking.registerGlobalReceiver(Acceso.CANAL, (cliente, handler, buf, callbacks) -> {
			byte[] reto = buf.readByteArray(64);
			return CompletableFuture.completedFuture(responder(cliente, reto));
		});
	}

	private static FriendlyByteBuf responder(Minecraft cliente, byte[] reto) {
		FriendlyByteBuf r = PacketByteBufs.create();
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
