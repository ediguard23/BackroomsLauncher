package net.backrooms.evento.vestibulo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.expedicion.Expedicion;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.objetos.Equipo;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * El vestibulo ("Centro de Expediciones"): su propia dimension, generada por
 * GeneradorVestibulo. Aqui esta todo el mundo antes del /start, aqui vuelve
 * quien muere (queda fuera) y quien escapa.
 *
 * Al entrar al servidor, quien no esta jugando una fase va al vestibulo (el
 * staff en creativo o espectador se queda donde este). Los rotulos, el logo
 * de la pantalla y la vitrina del equipo son entidades de texto y de objeto
 * que se ponen la primera vez (o con /backrooms vestibulo rehacer).
 *
 * Al morir en una fase: si MUERTE_ELIMINA (por defecto), fuera de la
 * expedicion y al vestibulo; si no, reaparece en otro punto de la misma fase
 * con sus misiones.
 *
 * Los ascensores del fondo estan cerrados hasta el /start. Al abrirse, quien
 * entra en una cabina baja al Nivel 0 (Expedicion#subir): pasa al hueco de
 * espera y le llega la cinematica en cuanto le toca. Se cierran solos al
 * arrancar el servidor y con /backrooms ascensores cerrar.
 */
public final class Vestibulo {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, BackroomsEvento.id("vestibulo"));
	private static final String ETIQUETA = "backrooms_vestibulo";
	private static final Vestibulo INSTANCIA = new Vestibulo();

	public static Vestibulo get() {
		return INSTANCIA;
	}

	private MinecraftServer servidor;
	private boolean muerteElimina = true;
	private long ticks;
	/** Proximo aviso por megafonia en el vestibulo. */
	private long proximoAviso = 20 * 60;
	private boolean decorarPendiente;
	private long decorarLimite;
	private boolean ascensoresAbiertos;
	/** 3 s entre pulsaciones del panel de los ascensores, por jugador. */
	private static final int PANEL_ESPERA = 60;
	private final Map<UUID, Long> panelPulsado = new HashMap<>();

	private Vestibulo() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.decorarSiFalta();
			INSTANCIA.ascensores(false);
			// la barra de localizacion de 1.21 dice hacia donde esta cada jugador: en el evento cada
			// uno empieza solo (en la beta #1 se quito a mano)
			INSTANCIA.comando(s.overworld(), "gamerule locator_bar false");
		});
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.alEntrar(h.player));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> {
			INSTANCIA.megafonia();
			INSTANCIA.tickDecorar();
			INSTANCIA.tickAscensores();
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad instanceof ServerPlayer j) {
				INSTANCIA.alMorir(j);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> {
			if (!vivo) {
				INSTANCIA.alReaparecer(nuevo, viejo.level());
			}
		});
	}

	public @Nullable ServerLevel nivel() {
		return this.servidor == null ? null : this.servidor.getLevel(DIMENSION);
	}

	public boolean muerteElimina() {
		return this.muerteElimina;
	}

	public void muerteElimina(boolean si) {
		this.muerteElimina = si;
	}

	/**
	 * De vez en cuando (cada 3-5 min) un aviso por megafonia a quien este en el vestibulo.
	 * Los avisos son de antes de empezar ("la expedicion empezara en breve"): con los
	 * ascensores abiertos o la expedicion en marcha no suenan, y nunca a quien ya subio
	 * (espera en el hueco, que tambien es el vestibulo).
	 */
	private void megafonia() {
		if (this.servidor == null || ++this.ticks < this.proximoAviso) {
			return;
		}
		this.proximoAviso = this.ticks + 20L * (180 + new java.util.Random().nextInt(120));
		ServerLevel v = this.nivel();
		if (v == null || this.ascensoresAbiertos || Expedicion.get().enMarcha()) {
			return;
		}
		for (ServerPlayer j : v.players()) {
			if (j.getY() >= PlanoVestibulo.SUELO && !Expedicion.get().pendiente(j)) {
				Sonidos.aJugador(j, Sonidos.MEGAFONIA_VESTIBULO, 0.8F);
			}
		}
	}

	/** Lleva al jugador al vestibulo, delante del mostrador. */
	public void llevar(ServerPlayer j) {
		ServerLevel v = this.nivel();
		if (v == null) {
			BackroomsEvento.LOG.warn("No hay dimension del vestibulo: {} se queda donde esta", j.getGameProfile().name());
			return;
		}
		if (j.isPassenger()) {
			j.stopRiding();
		}
		double dx = (j.getRandom().nextDouble() - 0.5) * 6;
		double dz = (j.getRandom().nextDouble() - 0.5) * 3;
		j.teleportTo(v, PlanoVestibulo.SPAWN_X + dx, PlanoVestibulo.SPAWN_Y, PlanoVestibulo.SPAWN_Z + dz, Set.of(), 180.0F, 0.0F, true);
		j.resetFallDistance();
		j.setHealth(j.getMaxHealth());
		j.getFoodData().setFoodLevel(20);
	}

	/* ----------------------------------------------------- ascensores */

	public boolean ascensoresAbiertos() {
		return this.ascensoresAbiertos;
	}

	/**
	 * Abre o cierra las puertas de los cinco ascensores. Al abrir suena la campanilla en
	 * cada puerta y todo el que esta en el vestibulo ve el aviso en pantalla.
	 */
	public void ascensores(boolean abrir) {
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		if (abrir && this.ascensoresAbiertos) {
			return; // otro /start con todo abierto: ni sonido ni aviso de que se abren
		}
		this.ascensoresAbiertos = abrir;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int suelo = PlanoVestibulo.SUELO;
		for (int cx : PlanoVestibulo.ASCENSORES) {
			for (int hoja = 0; hoja < 2; hoja++) {
				int x = cx - 1 + hoja;
				// fuera, las de acero del rellano; dentro, las de hierro de la cabina
				for (int y = suelo + 1; y <= suelo + 3; y++) {
					v.setBlock(p.set(x, y, PlanoVestibulo.PUERTA_Z), abrir ? Blocks.AIR.defaultBlockState() : PlanoVestibulo.puerta(hoja), 3);
				}
				v.setBlock(p.set(x, suelo + 1, PlanoVestibulo.PUERTA_Z + 1), PlanoVestibulo.puertaCabina(hoja, false, abrir), 2);
				v.setBlock(p.set(x, suelo + 2, PlanoVestibulo.PUERTA_Z + 1), PlanoVestibulo.puertaCabina(hoja, true, abrir), 2);
			}
			if (abrir) {
				v.playSound(null, cx, suelo + 2, PlanoVestibulo.PUERTA_Z, Sonidos.ASCENSOR_PANEL, SoundSource.BLOCKS, 2.0F, 1.0F);
				v.playSound(null, cx, suelo + 2, PlanoVestibulo.PUERTA_Z + 1, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.9F);
			}
		}
		if (abrir) {
			for (ServerPlayer j : v.players()) {
				titulo(j, "ASCENSORES ABIERTOS", "Entra en uno para bajar al Nivel 0");
			}
		}
	}

	/** Quien entra en una cabina abierta baja al Nivel 0 (el staff en creativo o espectador no). */
	private void tickAscensores() {
		if (!this.ascensoresAbiertos || this.ticks % 5 != 0) {
			return;
		}
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		for (ServerPlayer j : List.copyOf(v.players())) {
			if (!j.isCreative() && !j.isSpectator() && PlanoVestibulo.cabina(j.getX(), j.getY(), j.getZ()) >= 0) {
				Expedicion.get().subir(j);
			}
		}
	}

	/** Al hueco de espera de los ascensores (PlanoVestibulo.HUECO_*), cada uno en un sitio. */
	public void alHueco(ServerPlayer j) {
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		double x = PlanoVestibulo.HUECO_X0 + 1 + j.getRandom().nextDouble() * (PlanoVestibulo.HUECO_X1 - PlanoVestibulo.HUECO_X0 - 1);
		double z = PlanoVestibulo.HUECO_Z - 0.5 + j.getRandom().nextDouble() * 2.0;
		j.teleportTo(v, x, PlanoVestibulo.HUECO_Y, z, Set.of(), 180.0F, 0.0F, true);
		j.resetFallDistance();
	}

	/**
	 * El panel de un ascensor del vestibulo: fuera de servicio hasta el /start. Cada jugador
	 * puede pulsarlo una vez cada PANEL_ESPERA ticks: si no, se podia spamear el sonido.
	 */
	public void pulsarPanel(ServerPlayer j, BlockPos panel) {
		Long antes = this.panelPulsado.get(j.getUUID());
		if (antes != null && this.ticks - antes < PANEL_ESPERA) {
			return;
		}
		this.panelPulsado.put(j.getUUID(), this.ticks);
		if (this.ascensoresAbiertos) {
			j.level().playSound(null, panel, Sonidos.ASCENSOR_PANEL, SoundSource.BLOCKS, 1.0F, 1.0F);
			j.displayClientMessage(Component.literal("Entra en la cabina para bajar al Nivel 0").withStyle(ChatFormatting.YELLOW), true);
		} else {
			j.level().playSound(null, panel, Sonidos.ASCENSOR_DENEGADO, SoundSource.BLOCKS, 0.9F, 1.0F);
			j.displayClientMessage(Component.literal("FUERA DE SERVICIO · Se abren cuando empiece la expedición").withStyle(ChatFormatting.RED), true);
		}
	}

	private static void titulo(ServerPlayer j, String titulo, String subtitulo) {
		j.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
		j.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitulo).withStyle(ChatFormatting.GRAY)));
		j.connection.send(new ClientboundSetTitleTextPacket(Component.literal(titulo).withStyle(ChatFormatting.YELLOW)));
	}

	private void alEntrar(ServerPlayer j) {
		if (j.isCreative() || j.isSpectator()) {
			return;
		}
		if (j.level().dimension().equals(DIMENSION)) {
			// en el hueco de los ascensores sin viaje pendiente (se reinicio el servidor): arriba
			if (j.getY() < PlanoVestibulo.SUELO && !Expedicion.get().pendiente(j)) {
				this.llevar(j);
			}
			return;
		}
		Fase f = Fase.de(j.level());
		if (f != null && Misiones.get().enExpedicion(j)) {
			return; // sigue en su fase
		}
		this.llevar(j);
	}

	private void alMorir(ServerPlayer j) {
		if (Fase.de(j.level()) == null || !this.muerteElimina) {
			return;
		}
		boolean estaba = Misiones.get().enExpedicion(j);
		Misiones.get().eliminar(j);
		if (estaba) {
			net.backrooms.evento.expedicion.Eliminacion.get().eliminado(j);
		}
		// el aviso en el chat lo pone Eliminacion#mensajeMuerte, en la misma linea que la causa
	}

	private void alReaparecer(ServerPlayer j, Level donde) {
		Fase f = Fase.de(donde);
		if (f == null) {
			if (!j.isCreative() && !j.isSpectator()) {
				this.llevar(j);
			}
			return;
		}
		if (this.muerteElimina || !Misiones.get().enExpedicion(j)) {
			this.llevar(j);
			j.displayClientMessage(Component.literal("Has caído en los Backrooms. Quedas fuera de la expedición.").withStyle(ChatFormatting.RED), false);
			return;
		}
		ServerLevel nivel = this.servidor.getLevel(f.dimension());
		if (nivel != null) {
			BlockPos p = Fases.get().zonaNueva(nivel, f);
			j.teleportTo(nivel, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), j.getYRot(), 0.0F, true);
			Equipo.vestir(j);
		}
	}

	/* ------------------------------------------------------- rotulos */

	private Path marca() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("vestibulo-decorado");
	}

	private void decorarSiFalta() {
		try {
			if (this.nivel() != null && !Files.exists(this.marca())) {
				this.decorar();
				Files.createDirectories(this.marca().getParent());
				Files.writeString(this.marca(), "si");
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo decorar el vestibulo", e);
		}
	}

	/**
	 * Quita los rotulos que hubiera y los vuelve a poner. Las entidades de un
	 * chunk se cargan aparte y despues que sus bloques: si se buscasen ya (por
	 * ejemplo con nadie en el vestibulo), los rotulos viejos no aparecerian y
	 * quedarian duplicados. Asi que se fuerzan los chunks del edificio y el
	 * trabajo se hace en cuanto sus entidades estan cargadas (tickDecorar).
	 */
	public void decorar() {
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		for (int cx = PlanoVestibulo.X0 >> 4; cx <= PlanoVestibulo.X1 >> 4; cx++) {
			for (int cz = PlanoVestibulo.Z0 >> 4; cz <= PlanoVestibulo.Z1 >> 4; cz++) {
				v.setChunkForced(cx, cz, true);
			}
		}
		this.decorarPendiente = true;
		this.decorarLimite = this.ticks + 20 * 30;
	}

	private void tickDecorar() {
		if (!this.decorarPendiente) {
			return;
		}
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		boolean listos = true;
		for (int cx = PlanoVestibulo.X0 >> 4; cx <= PlanoVestibulo.X1 >> 4 && listos; cx++) {
			for (int cz = PlanoVestibulo.Z0 >> 4; cz <= PlanoVestibulo.Z1 >> 4; cz++) {
				if (!v.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) {
					listos = false;
					break;
				}
			}
		}
		if (!listos && this.ticks < this.decorarLimite) {
			return;
		}
		if (!listos) {
			BackroomsEvento.LOG.warn("Las entidades del vestibulo no terminaron de cargar: se decora igualmente");
		}
		this.decorarPendiente = false;
		this.decorarAhora(v);
		for (int cx = PlanoVestibulo.X0 >> 4; cx <= PlanoVestibulo.X1 >> 4; cx++) {
			for (int cz = PlanoVestibulo.Z0 >> 4; cz <= PlanoVestibulo.Z1 >> 4; cz++) {
				v.setChunkForced(cx, cz, false);
			}
		}
	}

	private void decorarAhora(ServerLevel v) {
		for (Display d : v.getEntitiesOfClass(Display.class, new net.minecraft.world.phys.AABB(
			PlanoVestibulo.X0, PlanoVestibulo.Y0, PlanoVestibulo.Z0, PlanoVestibulo.X1 + 1, PlanoVestibulo.Y1 + 1, PlanoVestibulo.Z1 + 1))) {
			if (d.getTags().contains(ETIQUETA)) {
				d.discard();
			}
		}
		int y = PlanoVestibulo.SUELO;
		// pantalla del escenario: el logo del evento y, debajo, el de PeakMC Studio
		// (las paredes ocupan su bloque entero, asi que el rotulo va 0,1 por delante de su cara).
		// Un item_display 'fixed' dibuja el objeto plano mirando al reves que un text_display:
		// con giro 0 se veia espejado desde el patio de butacas, por eso va a 180.
		objeto(v, 0.5, y + 10.6, -66.9, "logo_pantalla", 180, 8.6F);
		objeto(v, 0.5, y + 5.6, -66.9, "logo_peakmc", 180, 4.6F);
		// rotulos del vestibulo
		texto(v, 0.5, y + 10.6, 32.94, 180, "CENTRO DE EXPEDICIONES", "#E8D9A0", 4.5F, true);
		texto(v, 0.5, y + 8.6, 32.94, 180, "ASCENSORES · NIVEL 0", "#C8BC90", 2.4F, false);
		for (int cx : PlanoVestibulo.ASCENSORES) {
			texto(v, cx, y + 5.2, 32.94, 180, "NIVEL 0", "#FF5040", 1.4F, false);
			// el indicador de planta de dentro, como en la cinematica: rojo sobre negro
			texto(v, cx, y + 3.3, PlanoVestibulo.PUERTA_Z + 2.06, 0, "0", "#FF3A1C", 1.5F, true);
		}
		texto(v, 0.5, y + 10.15, -22.94, 0, "AUDITORIO", "#E8D9A0", 2.6F, true);
		texto(v, 0.5, y + 4.4, 18.5, 0, "INFORMACIÓN", "#E8D9A0", 1.6F, true);
		texto(v, 0.5, y + 4.4, 18.5, 180, "INFORMACIÓN", "#E8D9A0", 1.6F, true);
		texto(v, 36.94, y + 3.4, 1.0, 90, "AGUA DE ALMENDRAS", "#F0E6C8", 1.5F, true);
		texto(v, -35.94, y + 3.4, 1.5, 270, "TAQUILLAS", "#F0E6C8", 1.5F, true);
		texto(v, -17.0, y + 4.6, -18.5, 0, "EQUIPO DE EXPEDICIÓN", "#E8D9A0", 1.4F, true);
		// tablones a los lados de la gran puerta, mirando al vestibulo
		tablon(v, -20.5, y + 5.0, -22.94, 0, "NORMAS DE LA EXPEDICIÓN",
			"Completa tus misiones en cada fase.\nEl radar te lleva a los casetes y,\nal acabar, al ascensor de salida.\n\nSi mueres, quedas fuera.\nNo mires a un Smiler a los ojos.\nCuando suene la alarma, corre.");
		tablon(v, 21.5, y + 5.0, -22.94, 0, "CONTROLES",
			"F   linterna\nC   cámara (graba las misiones)\nE   inventario y misiones\nShift   levantarse de la butaca\n\nLas teclas se cambian en\nOpciones › Controles › Backrooms.");
		// la vitrina del equipo
		String[] equipo = {"linterna", "camara", "casete", "traje_casco"};
		for (int i = 0; i < equipo.length; i++) {
			objetoMod(v, PlanoVestibulo.VITRINA_X[i] + 0.5, y + 2.75, PlanoVestibulo.VITRINA_Z + 0.5, equipo[i]);
		}
		BackroomsEvento.LOG.info("Vestibulo decorado");
	}

	private CommandSourceStack fuente(ServerLevel v) {
		return this.servidor.createCommandSourceStack().withLevel(v).withSuppressedOutput().withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
	}

	private void comando(ServerLevel v, String c) {
		this.servidor.getCommands().performPrefixedCommand(this.fuente(v), c);
	}

	private static String escapar(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private void texto(ServerLevel v, double x, double y, double z, float giro, String texto, String color, float escala, boolean negrita) {
		this.comando(v, String.format(Locale.ROOT,
			"summon minecraft:text_display %.3f %.3f %.3f {Tags:[\"%s\"],Rotation:[%.1ff,0f],billboard:\"fixed\",background:0,shadow:1b,"
				+ "brightness:{sky:15,block:15},line_width:600,text:{text:\"%s\",color:\"%s\",bold:%s,font:\"backrooms:hud\"},"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}",
			x, y, z, ETIQUETA, giro, escapar(texto), color, negrita ? "true" : "false", escala, escala, escala));
	}

	private void tablon(ServerLevel v, double x, double y, double z, float giro, String titulo, String cuerpo) {
		this.comando(v, String.format(Locale.ROOT,
			"summon minecraft:text_display %.3f %.3f %.3f {Tags:[\"%s\"],Rotation:[%.1ff,0f],billboard:\"fixed\",background:-870375408,"
				+ "brightness:{sky:15,block:15},line_width:220,alignment:\"left\","
				+ "text:[{text:\"%s\\n\\n\",color:\"#E8D9A0\",bold:true},{text:\"%s\",color:\"#F2EEE4\"}],"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1.6f,1.6f,1.6f]}}",
			x, y, z, ETIQUETA, giro, escapar(titulo), escapar(cuerpo)));
	}

	/** Un objeto con el modelo `modelo` del mod (items/<modelo>.json), plano y grande. */
	private void objeto(ServerLevel v, double x, double y, double z, String modelo, float giro, float escala) {
		this.comando(v, String.format(Locale.ROOT,
			"summon minecraft:item_display %.3f %.3f %.3f {Tags:[\"%s\"],Rotation:[%.1ff,0f],brightness:{sky:15,block:15},item_display:\"fixed\","
				+ "item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"%s:%s\"}},"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%.2ff,%.2ff,0.05f]}}",
			x, y, z, ETIQUETA, giro, BackroomsEvento.ID, modelo, escala, escala));
	}

	/** Un objeto del mod girando despacio sobre un pedestal de la vitrina. */
	private void objetoMod(ServerLevel v, double x, double y, double z, String id) {
		this.comando(v, String.format(Locale.ROOT,
			"summon minecraft:item_display %.3f %.3f %.3f {Tags:[\"%s\"],billboard:\"vertical\",brightness:{sky:15,block:13},item_display:\"ground\","
				+ "item:{id:\"%s:%s\",count:1},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1.1f,1.1f,1.1f]}}",
			x, y, z, ETIQUETA, BackroomsEvento.ID, id));
	}
}
