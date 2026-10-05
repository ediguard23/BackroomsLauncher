package net.backrooms.evento;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Locale;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.expedicion.Expedicion;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.vestibulo.Vestibulo;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
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
 *  misiones dar <jugador>    le asigna sus 3 misiones y reparte sus casetes alrededor
 *  misiones ver <jugador>    estado de sus misiones
 *  misiones completar <jugador>  da por hecha la mision en curso (pruebas)
 *  misiones olvidar <jugador>    le quita las misiones
 *  cinematica [jugador]      le pone la cinematica del ascensor, sin viaje
 *  apagon|alarma [segundos]  fuerza un apagon o una alarma en la fase donde estas
 *  luz                       vuelve la luz normal
 *  ambiente [auto on|off]    como esta la luz y si los sucesos van solos
 *  fase <n> [jugador]        lo manda ya a la fase n con misiones nuevas
 *  vestibulo [jugador]       lo lleva al vestibulo; "vestibulo rehacer" repone los rotulos
 *  muerte eliminar|reaparecer  morir saca de la expedicion (por defecto) o reaparece en la fase
 *  escapados [olvidar]       quienes han escapado y en que puesto
 *
 * Y /start [jugadores]: manda al Nivel 0 a todos (los que no esten en
 * creativo ni espectador) o a los indicados, poco a poco y con cinematica.
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
		d.register(Commands.literal("start")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(c -> empezar(c, c.getSource().getServer().getPlayerList().getPlayers().stream()
				.filter(j -> !j.isCreative() && !j.isSpectator()).toList()))
			.then(Commands.argument("jugadores", EntityArgument.players())
				.executes(c -> empezar(c, List.copyOf(EntityArgument.getPlayers(c, "jugadores"))))));
		d.register(Commands.literal("backrooms")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("buscar")
				.then(Commands.argument("cosa", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(COSAS, b))
					.executes(c -> buscar(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player())
						.executes(c -> buscar(c, EntityArgument.getPlayer(c, "jugador"))))))
			.then(Commands.literal("cinematica")
				.executes(c -> cinematica(c, c.getSource().getPlayerOrException()))
				.then(Commands.argument("jugador", EntityArgument.player())
					.executes(c -> cinematica(c, EntityArgument.getPlayer(c, "jugador")))))
			.then(Commands.literal("apagon")
				.executes(c -> suceso(c, true, 60))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(5, 600))
					.executes(c -> suceso(c, true, IntegerArgumentType.getInteger(c, "segundos")))))
			.then(Commands.literal("alarma")
				.executes(c -> suceso(c, false, 45))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(5, 600))
					.executes(c -> suceso(c, false, IntegerArgumentType.getInteger(c, "segundos")))))
			.then(Commands.literal("invocar")
				.then(Commands.literal("bacteria").executes(c -> {
					ServerPlayer j = c.getSource().getPlayerOrException();
					net.minecraft.world.phys.Vec3 delante = j.getViewVector(1.0F).multiply(1, 0, 1).normalize().scale(6);
					net.backrooms.evento.entidad.Acechadores.get().poner(j.level(), net.minecraft.core.BlockPos.containing(j.position().add(delante)));
					return 1;
				}))
				.then(Commands.literal("smiler").executes(c -> {
					ServerPlayer j = c.getSource().getPlayerOrException();
					var s = net.backrooms.evento.entidad.Acechadores.get().smiler(j);
					if (s == null) {
						c.getSource().sendFailure(Component.literal("No hay sitio para el Smiler."));
						return 0;
					}
					return 1;
				})))
			.then(Commands.literal("acechadores").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
				boolean si = BoolArgumentType.getBool(c, "si");
				net.backrooms.evento.entidad.Acechadores.get().activos(si);
				c.getSource().sendSuccess(() -> Component.literal("Bacterias y Smilers automaticos: " + (si ? "si" : "no")), true);
				return 1;
			})))
			.then(Commands.literal("cordura").then(Commands.argument("valor", IntegerArgumentType.integer(0, 100)).executes(c -> {
				ServerPlayer j = c.getSource().getPlayerOrException();
				net.backrooms.evento.supervivencia.Supervivencia.get().cordura(j, IntegerArgumentType.getInteger(c, "valor"));
				return 1;
			})))
			.then(Commands.literal("comida").then(Commands.literal("reponer").executes(c -> {
				net.backrooms.evento.supervivencia.Comida.get().reponer();
				c.getSource().sendSuccess(() -> Component.literal("Toda la comida del Nivel 0 vuelve a su sitio."), true);
				return 1;
			})))
			.then(Commands.literal("luz").executes(c -> {
				Ambiente.get().terminar(c.getSource().getLevel());
				c.getSource().sendSuccess(() -> Component.literal("Luz normal."), true);
				return 1;
			}))
			.then(Commands.literal("ambiente")
				.executes(c -> {
					c.getSource().sendSuccess(() -> Component.literal(Ambiente.get().resumen(c.getSource().getLevel())), false);
					return 1;
				})
				.then(Commands.literal("auto").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
					boolean si = BoolArgumentType.getBool(c, "si");
					Ambiente.get().automatico(si);
					c.getSource().sendSuccess(() -> Component.literal("Apagones y alarmas automaticos: " + (si ? "si" : "no")), true);
					return 1;
				}))))
			.then(Commands.literal("fase")
				.then(Commands.argument("n", IntegerArgumentType.integer(1, Fase.TODAS.length))
					.executes(c -> fase(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player())
						.executes(c -> fase(c, EntityArgument.getPlayer(c, "jugador"))))))
			.then(Commands.literal("vestibulo")
				.executes(c -> {
					Vestibulo.get().llevar(c.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("rehacer").executes(c -> {
					Vestibulo.get().decorar();
					c.getSource().sendSuccess(() -> Component.literal("Rotulos del vestibulo repuestos."), true);
					return 1;
				}))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					Vestibulo.get().llevar(EntityArgument.getPlayer(c, "jugador"));
					return 1;
				})))
			.then(Commands.literal("muerte")
				.then(Commands.literal("eliminar").executes(c -> muerte(c, true)))
				.then(Commands.literal("reaparecer").executes(c -> muerte(c, false))))
			.then(Commands.literal("escapados")
				.executes(c -> {
					List<String> l = Fases.get().escapados();
					StringBuilder sb = new StringBuilder("Escapados (" + l.size() + "):");
					for (int i = 0; i < l.size(); i++) {
						sb.append(" #").append(i + 1).append(' ').append(l.get(i));
					}
					c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
					return l.size();
				})
				.then(Commands.literal("olvidar").executes(c -> {
					Fases.get().olvidarEscapados();
					c.getSource().sendSuccess(() -> Component.literal("Lista de escapados vaciada."), true);
					return 1;
				})))
			.then(Commands.literal("misiones")
				.then(Commands.literal("dar").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					ServerPlayer j = EntityArgument.getPlayer(c, "jugador");
					Fase fase = Fase.de(j.level());
					Misiones.get().asignar(j, j.blockPosition(), fase == null ? Fase.TODAS[0] : fase);
					c.getSource().sendSuccess(() -> Component.literal("Misiones asignadas a " + j.getGameProfile().name()), true);
					return 1;
				})))
				.then(Commands.literal("ver").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> verMisiones(c, EntityArgument.getPlayer(c, "jugador")))))
				.then(Commands.literal("completar").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					ServerPlayer j = EntityArgument.getPlayer(c, "jugador");
					Misiones.Estado e = Misiones.get().estado(j);
					if (e == null || e.actual >= e.misiones.size()) {
						c.getSource().sendFailure(Component.literal("No tiene mision en curso."));
						return 0;
					}
					Misiones.get().completar(j, e.misiones.get(e.actual));
					return 1;
				})))
				.then(Commands.literal("olvidar").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					Misiones.get().olvidar(EntityArgument.getPlayer(c, "jugador"));
					return 1;
				})))));
	}

	private static int suceso(CommandContext<CommandSourceStack> c, boolean apagon, int segundos) {
		ServerLevel nivel = c.getSource().getLevel();
		if (Fase.de(nivel) == null) {
			c.getSource().sendFailure(Component.literal("Aqui no hay fase del Nivel 0."));
			return 0;
		}
		if (apagon) {
			Ambiente.get().apagon(nivel, segundos);
		} else {
			Ambiente.get().alarma(nivel, segundos);
		}
		c.getSource().sendSuccess(() -> Component.literal((apagon ? "Apagon" : "Alarma") + " de " + segundos + " s."), true);
		return 1;
	}

	private static int fase(CommandContext<CommandSourceStack> c, ServerPlayer j) {
		Fase f = Fase.numero(IntegerArgumentType.getInteger(c, "n"));
		Fases.get().mandar(j, f);
		c.getSource().sendSuccess(() -> Component.literal(j.getGameProfile().name() + " a la fase " + f.numero() + " (" + f.nombre() + ")"), true);
		return 1;
	}

	private static int muerte(CommandContext<CommandSourceStack> c, boolean elimina) {
		Vestibulo.get().muerteElimina(elimina);
		c.getSource().sendSuccess(() -> Component.literal(elimina ? "Morir saca de la expedicion." : "Al morir se reaparece en la misma fase."), true);
		return 1;
	}

	private static int empezar(CommandContext<CommandSourceStack> c, List<ServerPlayer> jugadores) {
		int n = Expedicion.get().empezar(jugadores);
		if (n == 0) {
			c.getSource().sendFailure(Component.literal("Nadie que mandar (los de creativo y espectador se quedan)."));
			return 0;
		}
		int segundos = (int) Math.ceil(n / 3.0 * 0.5) + 55;
		c.getSource().sendSuccess(() -> Component.literal("Empieza la expedicion: " + n + " jugadores, todos dentro en unos " + segundos + " s."), true);
		return n;
	}

	private static int cinematica(CommandContext<CommandSourceStack> c, ServerPlayer j) {
		Expedicion.get().verCinematica(j);
		c.getSource().sendSuccess(() -> Component.literal("Cinematica para " + j.getGameProfile().name()), false);
		return 1;
	}

	private static int verMisiones(CommandContext<CommandSourceStack> c, ServerPlayer j) {
		Misiones.Estado e = Misiones.get().estado(j);
		if (e == null) {
			c.getSource().sendFailure(Component.literal(j.getGameProfile().name() + " no tiene misiones."));
			return 0;
		}
		StringBuilder sb = new StringBuilder(j.getGameProfile().name() + ": ");
		for (int i = 0; i < e.misiones.size(); i++) {
			TipoMision m = e.misiones.get(i);
			sb.append(i < e.actual ? "[hecha] " : i == e.actual ? "[en curso] " : "[ ] ").append(m.titulo).append(i < e.misiones.size() - 1 ? " | " : "");
		}
		sb.append(" | fase ").append(e.fase).append(" | casetes ").append(e.casetes).append('/').append(e.pendientes.size())
			.append(e.eliminado ? " | ELIMINADO" : "").append(e.escapado > 0 ? " | ESCAPADO #" + e.escapado : "");
		for (int[] pos : e.pendientes) {
			sb.append(pos[2] == 1 ? " [x]" : " (" + pos[0] + "," + pos[1] + ")");
		}
		c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
		return 1;
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
