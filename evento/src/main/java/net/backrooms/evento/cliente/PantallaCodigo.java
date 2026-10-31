package net.backrooms.evento.cliente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * El codigo de entrada pedido dentro del juego, para quien entra sin el Backrooms Launcher
 * (en el movil con Pojav, o con un Minecraft montado a mano): si el servidor le echa por no
 * tener pase (AccesoCliente), sale esta pantalla. El codigo se canjea en la tienda igual que en
 * el launcher (src/core/acceso.js: clave Ed25519 del dispositivo, POST /canjear), el pase se
 * guarda en backrooms-pase.json y se vuelve a conectar solo.
 */
public class PantallaCodigo extends Screen {
	/**
	 * La API de la tienda (la misma que lleva el manifest del pack en acceso.api). Solo para pruebas,
	 * BACKROOMS_TIENDA la cambia (tools/acceso/tienda-prueba.js).
	 */
	private static final String API = System.getenv("BACKROOMS_TIENDA") != null ? System.getenv("BACKROOMS_TIENDA")
		: "https://tienda.peakmc.lat/api/backrooms";
	private static final String ARCHIVO = "backrooms-pase.json";
	private static final int BLANCO = 0xFFFFFFFF;
	private static final int GRIS = 0xFFA8A8A8;
	private static final int ROJO = 0xFFFF6A5A;
	private static final int VERDE = 0xFF7BE07B;

	private final @Nullable ServerData servidor;
	private final Component motivo;
	private @Nullable EditBox caja;
	private @Nullable Button boton;
	private String escrito = "";
	private Component estado = Component.empty();
	private int colorEstado = GRIS;
	private boolean ocupado;

	public PantallaCodigo(@Nullable ServerData servidor, Component motivo) {
		super(Component.literal("Código de entrada"));
		this.servidor = servidor;
		this.motivo = motivo;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 10;
		if (this.caja != null) {
			this.escrito = this.caja.getValue();
		}
		this.caja = new EditBox(this.font, cx - 110, y, 220, 20, Component.literal("Código"));
		this.caja.setMaxLength(24);
		this.caja.setHint(Component.literal("BR-XXXX-XXXX-XXXX"));
		this.caja.setValue(this.escrito);
		this.addRenderableWidget(this.caja);
		this.setInitialFocus(this.caja);
		this.boton = this.addRenderableWidget(Button.builder(Component.literal("Canjear y entrar"), b -> this.canjear())
			.bounds(cx - 110, y + 30, 220, 20).build());
		this.boton.active = !this.ocupado;
		this.addRenderableWidget(Button.builder(Component.literal("Volver"), b -> this.onClose())
			.bounds(cx - 110, y + 56, 220, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		int cx = this.width / 2;
		int y = this.height / 2 - 10;
		g.drawCenteredString(this.font, Component.literal("BACKROOMS"), cx, y - 72, 0xFFE8C547);
		g.drawCenteredString(this.font, Component.literal("Escribe el código de tu entrada para entrar al evento."), cx, y - 52, BLANCO);
		g.drawCenteredString(this.font, Component.literal("Nick: " + this.minecraft.getUser().getName()), cx, y - 38, GRIS);
		g.drawCenteredString(this.font, this.motivo, cx, y - 24, GRIS);
		g.drawCenteredString(this.font, this.estado, cx, y + 84, this.colorEstado);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
		if ((e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && !this.ocupado) {
			this.canjear();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
	}

	/** Solo pruebas (OrdenesPrueba "canjear <codigo>"): lo escribe y lo canjea. */
	void probar(String codigo) {
		if (this.caja != null) {
			this.caja.setValue(codigo);
		}
		this.canjear();
	}

	private void canjear() {
		String codigo = limpiar(this.caja == null ? "" : this.caja.getValue());
		if (codigo == null) {
			this.aviso("El código tiene el formato BR-XXXX-XXXX-XXXX", ROJO);
			return;
		}
		String nick = this.minecraft.getUser().getName();
		Path dir = this.minecraft.gameDirectory.toPath();
		this.ocupado = true;
		if (this.boton != null) {
			this.boton.active = false;
		}
		this.aviso("Canjeando...", GRIS);
		CompletableFuture.supplyAsync(() -> {
			try {
				canjearEnTienda(dir, codigo, nick);
				return (String) null;
			} catch (Exception ex) {
				return ex.getMessage() != null ? ex.getMessage() : "No se pudo canjear el código.";
			}
		}).thenAccept(error -> this.minecraft.execute(() -> {
			this.ocupado = false;
			if (this.boton != null) {
				this.boton.active = true;
			}
			if (error != null) {
				this.aviso(error, ROJO);
				return;
			}
			if (this.servidor == null) {
				this.aviso("¡Listo! Vuelve a conectarte al servidor.", VERDE);
				return;
			}
			ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), this.minecraft,
				ServerAddress.parseString(this.servidor.ip), this.servidor, false, null);
		}));
	}

	private void aviso(String s, int color) {
		this.estado = Component.literal(s);
		this.colorEstado = color;
	}

	/** BR-XXXX-XXXX-XXXX (vale en minusculas, con espacios o sin guiones), o null. */
	static @Nullable String limpiar(String texto) {
		String s = texto.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "").replaceFirst("^BR", "");
		return s.length() == 12 ? "BR-" + s.substring(0, 4) + "-" + s.substring(4, 8) + "-" + s.substring(8, 12) : null;
	}

	/**
	 * Lo mismo que canjear() de src/core/acceso.js. La clave del dispositivo se guarda entera (la
	 * privada, como en el launcher, y aparte la publica) para reutilizarla si se vuelve a canjear:
	 * asi la tienda no lo cuenta como otro dispositivo.
	 */
	private static void canjearEnTienda(Path dir, String codigo, String nick) throws Exception {
		Path archivo = dir.resolve(ARCHIVO);
		Gson gson = new Gson();
		KeyFactory kf = KeyFactory.getInstance("Ed25519");
		KeyPair par = null;
		try {
			JsonObject previo = gson.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), JsonObject.class);
			if (previo.has("publica")) {
				par = new KeyPair(
					kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(previo.get("publica").getAsString()))),
					kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(previo.get("clave").getAsString()))));
			}
		} catch (Exception sinPrevio) {
			par = null;
		}
		if (par == null) {
			par = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		}
		JsonObject cuerpo = new JsonObject();
		cuerpo.addProperty("codigo", codigo);
		cuerpo.addProperty("nick", nick);
		cuerpo.addProperty("dispositivo", Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
		HttpResponse<String> r;
		try {
			r = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build().send(
				HttpRequest.newBuilder(URI.create(API + "/canjear")).timeout(Duration.ofSeconds(20))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8)).build(),
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (Exception red) {
			throw new Exception("No se pudo contactar con la tienda. Revisa tu conexión.");
		}
		JsonObject datos = null;
		try {
			datos = gson.fromJson(r.body(), JsonObject.class);
		} catch (Exception vacia) {
			// respuesta que no es JSON
		}
		if (datos == null) {
			datos = new JsonObject();
		}
		if (r.statusCode() / 100 != 2 || !datos.has("pase")) {
			throw new Exception(datos.has("error") ? datos.get("error").getAsString() : "La tienda respondió " + r.statusCode());
		}
		JsonObject guardar = new JsonObject();
		guardar.addProperty("nick", datos.has("nick") ? datos.get("nick").getAsString() : nick);
		guardar.addProperty("codigo", codigo);
		guardar.addProperty("pase", datos.get("pase").getAsString());
		guardar.addProperty("clave", Base64.getEncoder().encodeToString(par.getPrivate().getEncoded()));
		guardar.addProperty("publica", Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
		Files.writeString(archivo, new GsonBuilder().setPrettyPrinting().create().toJson(guardar), StandardCharsets.UTF_8);
		BackroomsEvento.LOG.info("Pase de entrada canjeado dentro del juego para {}", nick);
	}
}
