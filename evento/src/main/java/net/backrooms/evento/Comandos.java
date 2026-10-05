package net.backrooms.evento;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Locale;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.mundo.Plano;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos del staff: /backrooms ...
 *
 *  buscar <cosa> [jugador]   lleva al jugador a mirar la <cosa> mas cercana del
 *                            Nivel 0 (la busca en el plano, sin cargar chunks)
 */
public final class Comandos {
	private static final List<String> COSAS = List.of(
		"enchufe", "dibujo", "cinta", "salida", "nota", "silla", "senal", "vena", "ventilador", "ventilador_grande", "pared_fina", "bacilo", "oscuridad");

	private Comandos() {
	}

	public static void registrar() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registros, entorno) -> registrar(dispatcher));
	}

	private static void registrar(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("backrooms")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("buscar")
				.then(Commands.argument("cosa", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(COSAS, b))
					.executes(c -> buscar(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player())
						.executes(c -> buscar(c, EntityArgument.getPlayer(c, "jugador")))))));
	}

	private static int buscar(CommandContext<CommandSourceStack> c, ServerPlayer jugador) {
		String cosa = StringArgumentType.getString(c, "cosa").toLowerCase(Locale.ROOT);
		ServerLevel nivel = jugador.level();
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
			c.getSource().sendFailure(Component.literal("Este mundo no es el Nivel 0."));
			return 0;
		}
		Plano p = gen.plano(nivel.getChunkSource().randomState());
		int x0 = jugador.getBlockX();
		int z0 = jugador.getBlockZ();
		// espiral por anillos: lo mas cercano primero
		for (int r = 4; r <= 600; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = x0 + dx;
					int z = z0 + dz;
					int[] mira = coincide(p, cosa, x, z);
					if (mira != null) {
						mirar(c.getSource(), jugador, x, z, mira[0], mira[1]);
						c.getSource().sendSuccess(() -> Component.literal("«" + cosa + "» en " + x + " " + z), false);
						return 1;
					}
				}
			}
		}
		c.getSource().sendFailure(Component.literal("No hay «" + cosa + "» a menos de 600 bloques."));
		return 0;
	}

	/**
	 * Si en (x, z) esta la cosa buscada, devuelve {direccion desde la que
	 * mirarla (0 N, 1 E, 2 S, 3 O), altura de la mirada}; si no, null.
	 */
	private static int[] coincide(Plano p, String cosa, int x, int z) {
		switch (cosa) {
			case "ventilador", "ventilador_grande" -> {
				int v = p.ventilador(x, z);
				return v == (cosa.equals("ventilador") ? 1 : 2) && !p.pared(x + 3, z) ? new int[] {1, 2} : null;
			}
			case "pared_fina" -> {
				int f = p.paredFina(x, z);
				return f == 0 ? null : new int[] {f == 1 ? 2 : 1, 1};
			}
			case "bacilo" -> {
				return p.pared(x, z) && p.paredBacilo(x, z) && !p.pared(x, z + 1) ? new int[] {2, 1} : null;
			}
			case "oscuridad" -> {
				return !p.pared(x, z) && p.oscuridad(x, z) == 2 ? new int[] {1, 1} : null;
			}
			default -> {
			}
		}
		if (p.pared(x, z)) {
			return null;
		}
		Plano.Deco d = p.decoracion(x, z);
		boolean es = switch (cosa) {
			case "enchufe" -> d.tipo() == Plano.D_ENCHUFE || d.tipo() == Plano.D_ENCHUFE_MANCHADO;
			case "dibujo" -> d.tipo() == Plano.D_DIBUJO && d.variante() < 12;
			case "cinta" -> d.tipo() == Plano.D_DIBUJO && d.variante() >= 12;
			case "salida" -> d.tipo() == Plano.D_SALIDA;
			case "nota" -> d.tipo() == Plano.D_NOTA;
			case "silla" -> d.tipo() == Plano.D_SILLA || d.tipo() == Plano.D_SILLA_VOLCADA;
			case "senal" -> d.tipo() == Plano.D_SENAL;
			case "vena" -> d.tipo() == Plano.D_VENA;
			default -> false;
		};
		if (!es) {
			return null;
		}
		// lo pegado a la pared se mira desde delante; lo del suelo, desde donde mira
		return new int[] {d.mira(), d.tipo() == Plano.D_SALIDA ? 2 : 1};
	}

	/** Pone al jugador a 3 bloques de (x, z), en la direccion `lado`, mirandolo. */
	private static void mirar(CommandSourceStack fuente, ServerPlayer jugador, int x, int z, int lado, int altura) {
		int[][] dir = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		double sx = x + 0.5 + dir[lado][0] * 3;
		double sz = z + 0.5 + dir[lado][1] * 3;
		double tx = x + 0.5;
		double tz = z + 0.5;
		float yaw = (float) Math.toDegrees(Math.atan2(-(tx - sx), tz - sz));
		float pitch = altura == 2 ? -25 : altura == 1 ? 18 : 0;
		String cmd = String.format(Locale.ROOT, "tp %s %.2f %d %.2f %.1f %.1f", jugador.getGameProfile().name(), sx, GeneradorNivel0.SUELO + 1, sz, yaw, pitch);
		fuente.getServer().getCommands().performPrefixedCommand(fuente.withSuppressedOutput().withPermission(fuente.permissions()), cmd);
	}
}
