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
 *  panel                     abre el panel del staff: cada nivel con su estado, entidades, misiones,
 *                            sucesos y botones para casi todo lo de abajo (PanelAdmin, PantallaPanel)
 *  buscar <cosa> [jugador]   lleva al jugador a mirar la <cosa> mas cercana del
 *                            Nivel 0 (la busca en el plano, sin cargar chunks)
 *  misiones dar <jugador>    le asigna sus 3 misiones y reparte sus casetes alrededor
 *  misiones ver <jugador>    estado de sus misiones
 *  misiones completar <jugador>  da por hecha la mision en curso (pruebas)
 *  misiones olvidar <jugador>    le quita las misiones
 *  cinematica [jugador]      le pone la cinematica del ascensor, sin viaje
 *  cinematica piscinas [jugador]  la de la bajada a las Piscinas, sin viaje
 *  apagon|alarma [segundos]  fuerza un apagon o una alarma en la fase donde estas (en las
 *                            Piscinas, solo alarma)
 *  inundacion [segundos|parar] la inundacion de las Piscinas: empieza ya (20 s de alarma y el
 *                            agua sube en esos segundos, 40 en las automaticas) o se quita; sin
 *                            nada, como va
 *  luz                       vuelve la luz normal
 *  ambiente [auto on|off]    como esta la luz y si los sucesos van solos
 *  fase <n> [jugador]        lo manda ya a la fase n con misiones nuevas (solo queda la 1)
 *  piscinas [jugador]        lo manda ya a un sitio libre de las Piscinas (Nivel 37)
 *  piscinas ir <sala>        a la sala de ese tipo mas cercana (pasillos, piscina, inundada,
 *                            bovedas, tunel, caracol, terrazas)
 *  piscinas ir valvula|salida|cadaver|tarjeta_roja|tarjeta_amarilla|tarjeta_verde
 *                            delante de su valvula pendiente mas cercana (o de la mas
 *                            cercana), de la salida de emergencia o del escondite de una tarjeta
 *  tarjetas [reponer|abrir]  donde esta cada tarjeta; todas a su escondite y la salida
 *                            cerrada; o la salida abierta ya (pruebas)
 *  piscinas oir <sonido> <jugador>  que oiga ya uno de los sonidos sueltos de las Piscinas
 *                            (gota, agua, orilla, chapoteo, tuberia, grave, grunido, voz, susurro)
 *  recepcion [jugador]       lo lleva a la recepcion (donde se espera el /start)
 *  megafono                  enciende/apaga tu voz para todos (chat de voz sin distancia)
 *  fiesta [jugador]          lo manda a la Fiesta (Nivel Fun) como si subiera de las Piscinas
 *  fiesta ir <sala>          a esa sala de la Fiesta (llegada, salon, laberinto, semaforo, cocina,
 *                            siesta, premio, secreta)
 *  fiesta apagon [segundos]|aparicion  fuerza un apagon (con su caza) o que aparezca el Anfitrion
 *  fiesta tickets|monedas <n> [jugador]  le suma (o resta) tickets o monedas
 *  fiesta ver [jugador]      sus monedas, tickets y puertas pagadas; fiesta recargar: relee la config
 *  vestibulo [jugador]       lo lleva al vestibulo (la sala del final); "vestibulo rehacer" repone los rotulos
 *  muerte eliminar|reaparecer  morir saca de la expedicion (por defecto) o reaparece en la fase
 *  reparar [radio]            deja los chunks de alrededor como los hizo el generador
 *  perdonar                   quita el ban a todos los que cayeron (al acabar el evento o tras una prueba)
 *  escapados [olvidar]       quienes han escapado y en que puesto
 *  estacion [jugador]        lo manda a la Estacion Electrica (Nivel 3) como si bajara del tobogan de la Fiesta
 *  estacion ir <sala>        a esa sala de la Estacion (final: delante de la puerta encadenada; nave, laberinto_1..3, pasillo_final_1..3, central_1..3,
 *                            llegada_2, llegada_3, pasarela)
 *  estacion combustible <n>  pone el deposito del generador en n (0..100)
 *  estacion avanzar|reponer|ver  activa la central siguiente (o corta la cadena), lo deja todo como al
 *                            principio o lo cuenta
 *  invocar bacteria|smiler|serpiente|amasijo  la pone delante
 *
 * Y /start [todos|jugadores]: la cinematica y al Nivel 0 a los que esperan en la recepcion
 * (todos: a todos los conectados; o a los indicados), poco a poco; el staff en creativo o espectador no.
 */
public final class Comandos {
	private static final List<String> COSAS = List.of(
		"enchufe", "dibujo", "cinta", "salida", "nota", "silla", "senal", "vena", "arrastre", "ventilador", "ventilador_grande", "pared_fina", "bacilo", "oscuridad", "hueco");

	private Comandos() {
	}

	public static void registrar() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registros, entorno) -> registrar(dispatcher));
	}

	private static void registrar(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("start")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(c -> empezar(c, Expedicion.get().enRecepcion()))
			.then(Commands.literal("todos")
				.executes(c -> empezar(c, c.getSource().getServer().getPlayerList().getPlayers().stream()
					.filter(j -> !j.isCreative() && !j.isSpectator()).toList())))
			.then(Commands.argument("jugadores", EntityArgument.players())
				.executes(c -> empezar(c, List.copyOf(EntityArgument.getPlayers(c, "jugadores"))))));
		d.register(Commands.literal("backrooms")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("panel").executes(c -> PanelAdmin.abrir(c.getSource().getPlayerOrException()) ? 1 : 0))
			.then(Commands.literal("buscar")
				.then(Commands.argument("cosa", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(COSAS, b))
					.executes(c -> buscar(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player())
						.executes(c -> buscar(c, EntityArgument.getPlayer(c, "jugador"))))))
			// pulsa por el el panel del ascensor de salida (lo mismo que el panel: mira sus misiones)
			.then(Commands.literal("bajar").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
				ServerPlayer j = EntityArgument.getPlayer(c, "jugador");
				net.backrooms.evento.fase.Fases.get().pulsar(j, j.blockPosition());
				c.getSource().sendSuccess(() -> Component.literal("Panel pulsado por " + j.getGameProfile().name()), true);
				return 1;
			})))
			.then(Commands.literal("cinematica")
				.executes(c -> cinematica(c, c.getSource().getPlayerOrException()))
				// la bajada a las Piscinas tal como la ve quien la hace (sin viajar)
				.then(Commands.literal("piscinas")
					.executes(c -> cinematicaPiscinas(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player())
						.executes(c -> cinematicaPiscinas(c, EntityArgument.getPlayer(c, "jugador")))))
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
			.then(Commands.literal("inundacion")
				.executes(c -> {
					String r = net.backrooms.evento.piscinas.Inundacion.get().resumen();
					c.getSource().sendSuccess(() -> Component.literal(r), false);
					return 1;
				})
				.then(Commands.literal("parar").executes(c -> {
					net.backrooms.evento.piscinas.Inundacion.get().parar();
					c.getSource().sendSuccess(() -> Component.literal("Inundacion parada: el agua se va."), true);
					return 1;
				}))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(10, 600)).executes(c -> {
					int s = IntegerArgumentType.getInteger(c, "segundos");
					net.backrooms.evento.piscinas.Inundacion.get().empezar(s);
					c.getSource().sendSuccess(() -> Component.literal("Inundacion: el agua sube en " + s + " s."), true);
					return 1;
				})))
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
				}))
				.then(Commands.literal("serpiente").executes(c -> invocarSerpiente(c.getSource().getPlayerOrException())))
				.then(Commands.literal("amasijo").executes(c -> invocarAmasijo(c.getSource().getPlayerOrException()))))
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
				c.getSource().sendSuccess(() -> Component.literal("Toda la comida (Nivel 0 y Piscinas) vuelve a su sitio."), true);
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
			.then(Commands.literal("piscinas")
				.executes(c -> net.backrooms.evento.piscinas.Piscinas.llegar(c.getSource().getPlayerOrException()) ? 1 : 0)
				.then(irPiscinas())
				.then(Commands.literal("cadaveres").executes(c -> {
					int n = net.backrooms.evento.piscinas.Piscinas.contarCadaveres();
					c.getSource().sendSuccess(() -> Component.literal("Cuerpos en las Piscinas: " + n), false);
					return Math.max(0, n);
				}))
				.then(Commands.literal("oir").then(Commands.argument("sonido", StringArgumentType.word())
					.suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
						java.util.List.of("gota", "agua", "orilla", "chapoteo", "tuberia", "grave", "grunido", "voz", "susurro"), b))
					.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
						ServerPlayer j = EntityArgument.getPlayer(c, "jugador");
						net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(j,
							new net.backrooms.evento.red.SonidoPiscinas(StringArgumentType.getString(c, "sonido")));
						return 1;
					}))))
				.then(Commands.argument("jugador", EntityArgument.player())
					.executes(c -> net.backrooms.evento.piscinas.Piscinas.llegar(EntityArgument.getPlayer(c, "jugador")) ? 1 : 0)))
			.then(tarjetas())
			.then(Commands.literal("fiesta")
				.executes(c -> fiestaLlevar(c.getSource().getPlayerOrException()))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> fiestaLlevar(EntityArgument.getPlayer(c, "jugador"))))
				.then(Commands.literal("ir").then(Commands.argument("sala", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(net.backrooms.evento.fiesta.PlanoFiesta.SALAS.stream().map(s -> s.nombre()), b))
					.executes(c -> {
						ServerPlayer j = c.getSource().getPlayerOrException();
						String n = StringArgumentType.getString(c, "sala");
						ServerLevel f = net.backrooms.evento.fiesta.Fiesta.nivel();
						for (net.backrooms.evento.fiesta.PlanoFiesta.Sala s : net.backrooms.evento.fiesta.PlanoFiesta.SALAS) {
							if (s.nombre().equals(n) && f != null) {
								j.teleportTo(f, (s.x0() + s.x1()) / 2.0 + 0.5, net.backrooms.evento.fiesta.PlanoFiesta.SUELO + 1, (s.z0() + s.z1()) / 2.0 + 0.5,
									java.util.Set.of(), j.getYRot(), 0.0F, true);
								return 1;
							}
						}
						c.getSource().sendFailure(Component.literal("No hay sala " + n));
						return 0;
					})))
				.then(Commands.literal("apagon")
					.executes(c -> fiestaApagon(c, 40))
					.then(Commands.argument("segundos", IntegerArgumentType.integer(5, 600)).executes(c -> fiestaApagon(c, IntegerArgumentType.getInteger(c, "segundos")))))
				.then(Commands.literal("aparicion").executes(c -> {
					ServerPlayer j = c.getSource().getPlayerOrException();
					if (net.backrooms.evento.fiesta.Fiesta.nivel() != null) {
						net.backrooms.evento.fiesta.AnfitrionFiesta.get().aparicion(net.backrooms.evento.fiesta.Fiesta.nivel(), j);
					}
					return 1;
				}))
				.then(Commands.literal("tickets").then(Commands.argument("n", IntegerArgumentType.integer(-100000, 100000))
					.executes(c -> fiestaSumar(c, c.getSource().getPlayerOrException(), true))
					.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> fiestaSumar(c, EntityArgument.getPlayer(c, "jugador"), true)))))
				.then(Commands.literal("monedas").then(Commands.argument("n", IntegerArgumentType.integer(-1000, 1000))
					.executes(c -> fiestaSumar(c, c.getSource().getPlayerOrException(), false))
					.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> fiestaSumar(c, EntityArgument.getPlayer(c, "jugador"), false)))))
				.then(Commands.literal("ver")
					.executes(c -> fiestaVer(c, c.getSource().getPlayerOrException()))
					.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> fiestaVer(c, EntityArgument.getPlayer(c, "jugador")))))
				.then(Commands.literal("juegos").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					ServerPlayer j = EntityArgument.getPlayer(c, "jugador");
					String m = net.backrooms.evento.fiesta.JuegoMemoria.get().estado();
					String r = net.backrooms.evento.fiesta.JuegoRegalos.get().estado(j);
					c.getSource().sendSuccess(() -> Component.literal(m + " | " + r), false);
					return 1;
				})))
				.then(Commands.literal("recargar").executes(c -> {
					net.backrooms.evento.fiesta.ConfigFiesta.cargar();
					c.getSource().sendSuccess(() -> Component.literal("Config de la Fiesta releida (objetivo: "
						+ net.backrooms.evento.fiesta.ConfigFiesta.get().objetivoTickets + " tickets)."), true);
					return 1;
				})))
			.then(Commands.literal("estacion")
				.executes(c -> net.backrooms.evento.estacion.Estacion.llegar(c.getSource().getPlayerOrException(), true) ? 1 : 0)
				.then(Commands.argument("jugador", EntityArgument.player())
					.executes(c -> net.backrooms.evento.estacion.Estacion.llegar(EntityArgument.getPlayer(c, "jugador"), true) ? 1 : 0))
				.then(Commands.literal("ir").then(Commands.argument("sala", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(net.backrooms.evento.estacion.PlanoEstacion.SALAS.stream().map(s -> s.nombre()).distinct(), b))
					.executes(c -> estacionIr(c, StringArgumentType.getString(c, "sala")))))
				.then(Commands.literal("reja").then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(c -> estacionReja(c, IntegerArgumentType.getInteger(c, "n")))))
				.then(Commands.literal("avanzar").executes(c -> {
					net.backrooms.evento.estacion.JuegoEstacion.get().avanzar();
					return estacionVer(c);
				}))
				.then(Commands.literal("reponer").executes(c -> {
					net.backrooms.evento.estacion.JuegoEstacion.get().reponerTodo();
					ServerLevel n = net.backrooms.evento.estacion.Estacion.nivel();
					if (n != null) {
						net.backrooms.evento.estacion.AmasijoEstacion.get().quitarTodos(n);
					}
					c.getSource().sendSuccess(() -> Component.literal("Estación como al principio (rejas cerradas, centrales sin nada, cadena puesta)."), true);
					return 1;
				}))
				.then(Commands.literal("ver").executes(Comandos::estacionVer)))
			.then(Commands.literal("megafono")
				.executes(c -> {
					net.backrooms.evento.expedicion.MegafonoStaff.alternar(c.getSource().getPlayerOrException());
					return 1;
				}))
			.then(Commands.literal("recepcion")
				.executes(c -> {
					Vestibulo.get().recepcion(c.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					Vestibulo.get().recepcion(EntityArgument.getPlayer(c, "jugador"));
					return 1;
				})))
			.then(Commands.literal("vestibulo")
				.executes(c -> {
					Vestibulo.get().llevar(c.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("rehacer").executes(c -> {
					Vestibulo.get().decorar();
					c.getSource().sendSuccess(() -> Component.literal("Reponiendo los rotulos del vestibulo (tarda unos segundos)."), true);
					return 1;
				}))
				.then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					Vestibulo.get().llevar(EntityArgument.getPlayer(c, "jugador"));
					return 1;
				})))
			.then(Commands.literal("muerte")
				.then(Commands.literal("eliminar").executes(c -> muerte(c, true)))
				.then(Commands.literal("reaparecer").executes(c -> muerte(c, false))))
			.then(Commands.literal("reparar")
				.executes(c -> reparar(c, 1))
				.then(Commands.argument("radio", IntegerArgumentType.integer(0, 4)).executes(c -> reparar(c, IntegerArgumentType.getInteger(c, "radio")))))
			.then(Commands.literal("perdonar").executes(c -> {
				int n = net.backrooms.evento.expedicion.Eliminacion.get().perdonar();
				c.getSource().sendSuccess(() -> Component.literal("Ban quitado a " + n + " eliminado" + (n == 1 ? "" : "s") + "."), true);
				return n;
			}))
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
		boolean piscinas = net.backrooms.evento.piscinas.Piscinas.es(nivel);
		if (Fase.de(nivel) == null && !(piscinas && !apagon)) {
			c.getSource().sendFailure(Component.literal(piscinas ? "En las Piscinas no hay apagones (solo alarmas)." : "Aqui no hay fase del Nivel 0 ni Piscinas."));
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

	/**
	 * Deja los chunks de alrededor (radio en chunks) como los hizo el generador: paredes rotas,
	 * bloques puestos o quitados a mano... Solo la franja del nivel (y de 30 a 64).
	 */
	private static int fiestaLlevar(ServerPlayer j) {
		return net.backrooms.evento.fiesta.Fiesta.llegar(j, true) ? 1 : 0;
	}

	private static int fiestaApagon(CommandContext<CommandSourceStack> c, int segundos) {
		ServerLevel f = net.backrooms.evento.fiesta.Fiesta.nivel();
		if (f == null) {
			c.getSource().sendFailure(Component.literal("Este servidor no tiene la Fiesta."));
			return 0;
		}
		net.backrooms.evento.fiesta.AnfitrionFiesta.get().apagon(f, segundos);
		c.getSource().sendSuccess(() -> Component.literal("Apagon en la Fiesta (" + segundos + " s)."), true);
		return 1;
	}

	private static int fiestaSumar(CommandContext<CommandSourceStack> c, ServerPlayer j, boolean tickets) {
		int n = IntegerArgumentType.getInteger(c, "n");
		if (tickets) {
			net.backrooms.evento.fiesta.JuegoFiesta.get().sumarTickets(j, n, "staff");
		} else {
			net.backrooms.evento.fiesta.JuegoFiesta.get().sumarMonedas(j, n);
		}
		return fiestaVer(c, j);
	}

	private static int fiestaVer(CommandContext<CommandSourceStack> c, ServerPlayer j) {
		net.backrooms.evento.fiesta.JuegoFiesta.Datos d = net.backrooms.evento.fiesta.JuegoFiesta.get().datos(j);
		c.getSource().sendSuccess(() -> Component.literal(j.getGameProfile().name() + ": " + d.monedas + " monedas, " + d.tickets + "/"
			+ net.backrooms.evento.fiesta.ConfigFiesta.get().objetivoTickets + " tickets, fichas cogidas " + d.cogidas.size()
			+ ", puertas pagadas " + d.pagados + ", vuelta " + d.vuelta + (d.cobrada ? " (cobrada)" : " (bote "
			+ net.backrooms.evento.fiesta.JuegoFiesta.get().bote(j) + ")") + ", vueltas acabadas " + d.vueltas), false);
		return 1;
	}

	private static int reparar(CommandContext<CommandSourceStack> c, int radio) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer j = c.getSource().getPlayerOrException();
		ServerLevel n = j.level();
		net.minecraft.world.level.chunk.ChunkGenerator gen = n.getChunkSource().getGenerator();
		net.minecraft.world.level.ChunkPos centro = j.chunkPosition();
		int cambiados = 0;
		for (int dx = -radio; dx <= radio; dx++) {
			for (int dz = -radio; dz <= radio; dz++) {
				net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(centro.x + dx, centro.z + dz);
				net.minecraft.world.level.chunk.ProtoChunk p = new net.minecraft.world.level.chunk.ProtoChunk(cp,
					net.minecraft.world.level.chunk.UpgradeData.EMPTY, n, n.palettedContainerFactory(), null);
				gen.fillFromNoise(net.minecraft.world.level.levelgen.blending.Blender.empty(), n.getChunkSource().randomState(), n.structureManager(), p).join();
				net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
				for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
					for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
						for (int y = 30; y <= 64; y++) {
							pos.set(x, y, z);
							net.minecraft.world.level.block.state.BlockState quiero = p.getBlockState(pos);
							if (!n.getBlockState(pos).equals(quiero)) {
								n.setBlock(pos, quiero, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
								cambiados++;
							}
						}
					}
				}
			}
		}
		int total = cambiados;
		c.getSource().sendSuccess(() -> Component.literal("Reparados " + total + " bloques (" + (2 * radio + 1) + "x" + (2 * radio + 1) + " chunks)."), true);
		return total;
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
		int segundos = (int) Math.ceil(n / 3.0 * 0.5) + 37 + 55;
		c.getSource().sendSuccess(() -> Component.literal("Empieza la expedicion: " + n + " jugadores, todos dentro en unos " + segundos + " s."), true);
		return n;
	}

	/**
	 * Pone la Serpiente 7 bloques por delante, mirando al jugador: en el agua, a flor de
	 * agua; en seco, en el suelo. Luego hace lo suyo (ojo: si hay alguien en modo aventura
	 * a menos de 25 bloques, va a por el).
	 */
	private static int invocarSerpiente(ServerPlayer j) {
		net.minecraft.server.level.ServerLevel nivel = j.level();
		net.minecraft.world.phys.Vec3 p = j.position().add(j.getViewVector(1.0F).multiply(1, 0, 1).normalize().scale(7));
		net.minecraft.core.BlockPos.MutableBlockPos b = net.minecraft.core.BlockPos.containing(p.x, j.getEyeY(), p.z).mutable();
		while (b.getY() > nivel.getMinY() && nivel.getBlockState(b).isAir()) {
			b.move(net.minecraft.core.Direction.DOWN);
		}
		boolean agua = nivel.getFluidState(b).is(net.minecraft.tags.FluidTags.WATER);
		double y = agua ? b.getY() + 0.4 : b.getY() + 1.0;
		var s = net.backrooms.evento.mision.Entidades.SERPIENTE.create(nivel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (s == null) {
			return 0;
		}
		float mira = j.getYRot() + 180.0F;
		s.snapTo(p.x, y, p.z, mira, 0.0F);
		s.setYHeadRot(mira);
		s.setYBodyRot(mira);
		nivel.addFreshEntity(s);
		return 1;
	}

	/** A esa sala de la Estacion (la primera con ese nombre), en el suelo de su centro. */
	private static int estacionIr(CommandContext<CommandSourceStack> c, String nombre) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer j = c.getSource().getPlayerOrException();
		ServerLevel n = net.backrooms.evento.estacion.Estacion.nivel();
		if (n == null) {
			c.getSource().sendFailure(Component.literal("Este servidor no tiene la Estación."));
			return 0;
		}
		if (nombre.equals("final")) {
			// delante de la puerta encadenada del final, mirandola
			int[] p = net.backrooms.evento.estacion.PlanoEstacion.PASARELA;
			j.teleportTo(n, p[0] + 1.5, p[1] + 1, p[2] + 2.5, java.util.Set.of(), 180.0F, 0.0F, true);
			return 1;
		}
		for (net.backrooms.evento.estacion.PlanoEstacion.Sala s : net.backrooms.evento.estacion.PlanoEstacion.SALAS) {
			if (s.nombre().equals(nombre)) {
				// en medio (en la de la central, en una esquina: en medio estan las consolas)
				boolean esquina = s.nombre().startsWith("central");
				double x = esquina ? s.x0() + 1.5 : (s.x0() + s.x1()) / 2.0 + 0.5;
				double z = esquina ? s.z0() + 1.5 : (s.z0() + s.z1()) / 2.0 + 0.5;
				j.teleportTo(n, x, s.y() + 1, z, java.util.Set.of(), j.getYRot(), 0.0F, true);
				return 1;
			}
		}
		c.getSource().sendFailure(Component.literal("No hay sala " + nombre));
		return 0;
	}

	/** Delante de la reja n (por el lado de abajo), mirandola. */
	private static int estacionReja(CommandContext<CommandSourceStack> c, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer j = c.getSource().getPlayerOrException();
		ServerLevel nivel = net.backrooms.evento.estacion.Estacion.nivel();
		var rejas = net.backrooms.evento.estacion.PlanoEstacion.REJAS;
		if (nivel == null || n >= rejas.size()) {
			c.getSource().sendFailure(Component.literal("Hay " + rejas.size() + " rejas (0 a " + (rejas.size() - 1) + ")."));
			return 0;
		}
		var r = rejas.get(n);
		net.minecraft.core.Direction m = r.mira();
		j.teleportTo(nivel, r.x() + 0.5 - 2.5 * m.getStepX(), r.y() + 1, r.z() + 0.5 - 2.5 * m.getStepZ(), java.util.Set.of(), m.toYRot(), 10.0F, true);
		return 1;
	}

	private static int estacionVer(CommandContext<CommandSourceStack> c) {
		for (String l : net.backrooms.evento.estacion.JuegoEstacion.get().resumen()) {
			c.getSource().sendSuccess(() -> Component.literal(l), false);
		}
		return 1;
	}

	/** Pone un Amasijo 7 bloques por delante, en el suelo (luego hace lo suyo: ronda y caza). */
	private static int invocarAmasijo(ServerPlayer j) {
		net.minecraft.server.level.ServerLevel nivel = j.level();
		net.minecraft.world.phys.Vec3 p = j.position().add(j.getViewVector(1.0F).multiply(1, 0, 1).normalize().scale(7));
		var a = net.backrooms.evento.mision.Entidades.AMASIJO.create(nivel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (a == null) {
			return 0;
		}
		float mira = j.getYRot() + 180.0F;
		a.snapTo(p.x, j.getY(), p.z, mira, 0.0F);
		a.addTag(net.backrooms.evento.estacion.AmasijoEstacion.INVOCADO);
		a.setYHeadRot(mira);
		a.setYBodyRot(mira);
		nivel.addFreshEntity(a);
		return 1;
	}

	private static int irMision(CommandContext<CommandSourceStack> c, boolean ok) {
		if (!ok) {
			c.getSource().sendFailure(Component.literal("No se encuentra (hay que estar en las Piscinas)."));
		}
		return ok ? 1 : 0;
	}

	/** tarjetas [reponer|abrir]: donde esta cada tarjeta de la salida de las Piscinas. */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> tarjetas() {
		return Commands.literal("tarjetas")
			.executes(c -> {
				for (String l : net.backrooms.evento.piscinas.Tarjetas.get().resumen()) {
					c.getSource().sendSuccess(() -> Component.literal(l), false);
				}
				return 1;
			})
			.then(Commands.literal("reponer").executes(c -> {
				net.backrooms.evento.piscinas.Tarjetas.get().reponer();
				c.getSource().sendSuccess(() -> Component.literal("Las tres tarjetas vuelven a su escondite y la salida se cierra."), true);
				return 1;
			}))
			.then(Commands.literal("abrir").executes(c -> {
				net.backrooms.evento.piscinas.Tarjetas.get().abrirYa();
				c.getSource().sendSuccess(() -> Component.literal("Salida de emergencia abierta."), true);
				return 1;
			}));
	}

	/** Las salas de las Piscinas por su nombre en el comando, en el orden de PlanoPiscinas. */
	private static final String[] SALAS_PISCINAS = {"pasillos", "piscina", "inundada", "bovedas", "tunel", "caracol", "terrazas"};

	/** piscinas ir <tipo>: a la sala de ese tipo mas cercana (pruebas y rodaje). */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> irPiscinas() {
		var ir = Commands.literal("ir");
		for (boolean corre : new boolean[] {false, true}) {
			ir.then(Commands.literal(corre ? "corre" : "detalle").executes(c -> {
				boolean ok = net.backrooms.evento.piscinas.Piscinas.irADetalle(c.getSource().getPlayerOrException(), corre);
				if (!ok) {
					c.getSource().sendFailure(Component.literal("No hay nada asi cerca (o no hay Piscinas)."));
				}
				return ok ? 1 : 0;
			}));
		}
		ir.then(Commands.literal("valvula").executes(c -> irMision(c, net.backrooms.evento.piscinas.Piscinas.irAValvula(c.getSource().getPlayerOrException()))));
		ir.then(Commands.literal("cadaver").executes(c -> irMision(c, net.backrooms.evento.piscinas.Piscinas.irACadaver(c.getSource().getPlayerOrException(), false)))
			.then(Commands.literal("colgado").executes(c -> irMision(c, net.backrooms.evento.piscinas.Piscinas.irACadaver(c.getSource().getPlayerOrException(), true)))));
		ir.then(Commands.literal("salida").executes(c -> irMision(c, net.backrooms.evento.piscinas.Piscinas.irASalida(c.getSource().getPlayerOrException()))));
		for (int k = 0; k < 3; k++) {
			int color = k;
			ir.then(Commands.literal("tarjeta_" + net.backrooms.evento.piscinas.Tarjetas.NOMBRES[k]).executes(c ->
				irMision(c, net.backrooms.evento.piscinas.Piscinas.irATarjeta(c.getSource().getPlayerOrException(), color))));
		}
		for (int k = 0; k < SALAS_PISCINAS.length; k++) {
			int tipo = k;
			ir.then(Commands.literal(SALAS_PISCINAS[k]).executes(c -> {
				boolean ok = net.backrooms.evento.piscinas.Piscinas.irA(c.getSource().getPlayerOrException(), tipo);
				if (!ok) {
					c.getSource().sendFailure(Component.literal("No hay ninguna sala asi cerca (o no hay Piscinas)."));
				}
				return ok ? 1 : 0;
			}));
		}
		return ir;
	}

	private static int cinematicaPiscinas(CommandContext<CommandSourceStack> c, ServerPlayer j) {
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(j, new net.backrooms.evento.red.ViajeAscensor(
			net.backrooms.evento.red.ViajeAscensor.PISCINAS, net.backrooms.evento.piscinas.Piscinas.NOMBRE, net.backrooms.evento.piscinas.Piscinas.SUBTITULO));
		c.getSource().sendSuccess(() -> Component.literal("Bajada a las Piscinas para " + j.getGameProfile().name() + " (solo la animacion)"), false);
		return 1;
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
		sb.append(" | fase ").append(e.fase).append(" | casetes ").append(e.casetes).append('/').append(e.necesarios > 0 ? e.necesarios : e.pendientes.size())
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
			case "hueco" -> {
				// el boquete de una pared hueca, mirado desde fuera
				Plano.CeldaHueca h = p.celdaHueca(x, z);
				if (h == null || !h.izq()) {
					return null;
				}
				for (int d = 0; d < 4; d++) {
					if (h.caras()[d] == Plano.CARA_ROTA) {
						return new int[] {d, 1};
					}
				}
				return null;
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
			case "arrastre" -> d.tipo() == Plano.D_ARRASTRE && d.variante() == 0;
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
