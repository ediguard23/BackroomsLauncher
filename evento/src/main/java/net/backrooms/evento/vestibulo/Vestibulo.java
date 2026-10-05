package net.backrooms.evento.vestibulo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import net.backrooms.evento.BackroomsEvento;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
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

	private Vestibulo() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.decorarSiFalta();
		});
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.alEntrar(h.player));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.megafonia());
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

	/** De vez en cuando (cada 3-5 min) un aviso por megafonia a quien este en el vestibulo. */
	private void megafonia() {
		if (this.servidor == null || ++this.ticks < this.proximoAviso) {
			return;
		}
		this.proximoAviso = this.ticks + 20L * (180 + new java.util.Random().nextInt(120));
		ServerLevel v = this.nivel();
		if (v != null) {
			for (ServerPlayer j : v.players()) {
				net.backrooms.evento.Sonidos.aJugador(j, net.backrooms.evento.Sonidos.MEGAFONIA_VESTIBULO, 0.8F);
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

	private void alEntrar(ServerPlayer j) {
		if (j.isCreative() || j.isSpectator()) {
			return;
		}
		if (j.level().dimension().equals(DIMENSION)) {
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
		Misiones.get().eliminar(j);
		this.servidor.getPlayerList().broadcastSystemMessage(
			Component.literal(j.getGameProfile().name() + " no ha salido del Nivel 0.").withStyle(ChatFormatting.DARK_RED), false);
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

	/** Quita los rotulos que hubiera y los vuelve a poner. */
	public void decorar() {
		ServerLevel v = this.nivel();
		if (v == null) {
			return;
		}
		// carga el edificio entero (es pequeno) para poder poner las entidades
		for (int cx = PlanoVestibulo.X0 >> 4; cx <= PlanoVestibulo.X1 >> 4; cx++) {
			for (int cz = PlanoVestibulo.Z0 >> 4; cz <= PlanoVestibulo.Z1 >> 4; cz++) {
				v.getChunk(cx, cz);
			}
		}
		for (Display d : v.getEntitiesOfClass(Display.class, new net.minecraft.world.phys.AABB(
			PlanoVestibulo.X0, PlanoVestibulo.Y0, PlanoVestibulo.Z0, PlanoVestibulo.X1 + 1, PlanoVestibulo.Y1 + 1, PlanoVestibulo.Z1 + 1))) {
			if (d.getTags().contains(ETIQUETA)) {
				d.discard();
			}
		}
		int y = PlanoVestibulo.SUELO;
		// pantalla del escenario: el logo del evento y, debajo, el de PeakMC Studio
		// (giro 0 = mira al sur; las paredes ocupan su bloque entero, asi que el rotulo va 0,05 por delante de su cara)
		objeto(v, 0.5, y + 10.6, -66.9, "logo_pantalla", 0, 8.6F);
		objeto(v, 0.5, y + 5.6, -66.9, "logo_peakmc", 0, 4.6F);
		// rotulos del vestibulo
		texto(v, 0.5, y + 10.6, 32.94, 180, "CENTRO DE EXPEDICIONES", "#E8D9A0", 4.5F, true);
		texto(v, 0.5, y + 8.6, 32.94, 180, "ASCENSORES · NIVEL 0", "#C8BC90", 2.4F, false);
		for (int cx : PlanoVestibulo.ASCENSORES) {
			texto(v, cx, y + 5.2, 32.94, 180, "▼ N0", "#FF5040", 1.4F, false);
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
