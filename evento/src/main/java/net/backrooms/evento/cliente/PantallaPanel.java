package net.backrooms.evento.cliente;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.red.EstadoPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * El panel del staff (/backrooms panel): una pestana por cada parte del evento (lo general, el Nivel 0,
 * las Piscinas y el Nivel Fun) con lo que pasa ahora (lo manda el servidor, PanelAdmin), sus entidades,
 * sus misiones, sus sucesos y botones para lo que antes eran comandos sueltos. Los botones mandan esos
 * mismos comandos (el servidor comprueba que se es operador); los peligrosos piden pulsar dos veces.
 * Lo que contesta el servidor sale abajo. La rueda del raton baja y sube.
 */
public class PantallaPanel extends Screen {
	private static final int AMARILLO = 0xFFF2D46B;
	private static final int GRIS = 0xFFB8B3A6;
	private static final int BLANCO = 0xFFF4F0E4;
	private static final int ROJO = 0xFFFF6A5A;
	private static final String[] PESTANAS = {"GENERAL", "NIVEL 0", "NIVEL 37", "NIVEL FUN", "NIVEL 3"};

	private static int pestana;
	private static String respuesta = "";
	private EstadoPanel estado;
	private int scroll;
	private int alto;
	private int refrescar = -1;
	private int ticks;
	private String pendiente;
	private int pendienteHasta;
	/** Lo que se dibuja a mano en el contenido: texto en (x, y) con color (y sin el scroll). */
	private final List<Object[]> textos = new ArrayList<>();

	/** Una accion: lo que pone el boton, el comando (sin barra) y si hay que confirmarlo. */
	private record Accion(String texto, String comando, boolean peligrosa) {
		static Accion de(String texto, String comando) {
			return new Accion(texto, comando, false);
		}

		static Accion ojo(String texto, String comando) {
			return new Accion(texto, comando, true);
		}
	}

	private record Entidad(String nombre, String descripcion, Accion accion) {
	}

	/** Lo que hay en cada pestana. */
	private record Seccion(String titulo, List<Accion> acciones) {
	}

	private record Pagina(String descripcion, List<Entidad> entidades, List<TipoMision> misiones, List<Seccion> secciones) {
	}

	public PantallaPanel(EstadoPanel estado) {
		super(Component.literal("Panel de las Backrooms"));
		this.estado = estado;
	}

	/** Llega el estado: abre el panel o refresca el abierto. */
	public static void recibir(EstadoPanel e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof PantallaPanel p) {
			p.estado = e;
			p.rehacer();
		} else {
			mc.setScreen(new PantallaPanel(e));
		}
	}

	/** Lo que contesta el servidor (mensajes del sistema) mientras el panel esta abierto. */
	public static void mensaje(Component m) {
		if (Minecraft.getInstance().screen instanceof PantallaPanel) {
			respuesta = m.getString();
		}
	}

	private static Pagina pagina(int i) {
		return switch (i) {
			case 0 -> new Pagina(
				"La recepción (donde se espera el /start), la salida hacia el Nivel 0 y el vestíbulo, la sala del final adonde van quienes escapan y quienes caen. Los sucesos automáticos (apagones y alarmas) valen para todos los niveles.",
				List.of(), List.of(), List.of(
					new Seccion("EXPEDICIÓN", List.of(Accion.ojo("/start (recepción)", "start"), Accion.ojo("/start a todos", "start todos"),
						Accion.de("Ir a la recepción", "backrooms recepcion"), Accion.de("Ir al vestíbulo", "backrooms vestibulo"))),
					new Seccion("SUCESOS", List.of(Accion.de("Automáticos: SÍ", "backrooms ambiente auto true"), Accion.de("Automáticos: NO", "backrooms ambiente auto false"),
						Accion.de("Luz normal aquí", "backrooms luz"), Accion.de("Entidades auto: SÍ", "backrooms acechadores true"),
						Accion.de("Entidades auto: NO", "backrooms acechadores false"))),
					new Seccion("JUGADORES", List.of(Accion.de("Ver escapados", "backrooms escapados"), Accion.ojo("Perdonar a los caídos", "backrooms perdonar"),
						Accion.de("Morir elimina", "backrooms muerte eliminar"), Accion.de("Morir reaparece", "backrooms muerte reaparecer"),
						Accion.de("Megáfono (tu voz)", "backrooms megafono"), Accion.de("Mi cordura al 100", "backrooms cordura 100"),
						Accion.de("Reponer la comida", "backrooms comida reponer")))));
			case 1 -> new Pagina(
				"Pasillos amarillos sin fin, moqueta húmeda y el zumbido de los tubos. Se llega con el /start (la cinemática y el ascensor). Cada uno tiene sus misiones; con ellas hechas, los ascensores de salida bajan a las Piscinas. Los sucesos se aplican en el nivel donde estás.",
				List.of(new Entidad("Bacteria", "Caza a oscuras y agarra a quien atrapa. Con la alarma (luz roja) está más furiosa.", Accion.de("Invocar", "backrooms invocar bacteria")),
					new Entidad("Smiler", "Solo de noche, en la oscuridad: una sonrisa y dos ojos que se acercan si no le miras.", Accion.de("Invocar", "backrooms invocar smiler"))),
				List.of(TipoMision.CASETES, TipoMision.LUCES_ROJAS, TipoMision.ENTIDAD, TipoMision.ENTIDAD_ALARMA, TipoMision.SMILER),
				List.of(new Seccion("SUCESOS", List.of(Accion.de("Apagón 60 s", "backrooms apagon 60"), Accion.de("Alarma 60 s", "backrooms alarma 60"),
						Accion.de("Luz normal", "backrooms luz"), Accion.de("Cinemática del /start (yo)", "backrooms cinematica"))),
					new Seccion("IR A", List.of(Accion.de("Llevarme al Nivel 0", "backrooms fase 1"), Accion.de("Una salida", "backrooms buscar salida"),
						Accion.de("Una nota", "backrooms buscar nota"), Accion.de("Una pared hueca", "backrooms buscar hueco"),
						Accion.de("Zona a oscuras", "backrooms buscar oscuridad"), Accion.de("Venas de bacilo", "backrooms buscar vena"), Accion.de("Rastro de arrastre", "backrooms buscar arrastre")))));
			case 2 -> new Pagina(
				"Salas de azulejo blanco y agua que no acaba. Cada uno cierra sus válvulas y graba a la Serpiente; entre todos, las tres tarjetas abren la bóveda. Detrás, el pasillo y el tobogán a la Fiesta (con su cinemática). Cada pocos minutos, la inundación.",
				List.of(new Entidad("Serpiente", "Nada en las piscinas hondas y ataca a quien se mete; una guarda la tarjeta roja.", Accion.de("Invocar", "backrooms invocar serpiente")),
					new Entidad("Cadáveres", "Decorado: cuerpos tirados, sentados o colgados, algunos con rastro de arrastre.", Accion.de("Ir a uno", "backrooms piscinas ir cadaver"))),
				List.of(TipoMision.VALVULAS, TipoMision.SERPIENTE, TipoMision.SALIDA),
				List.of(new Seccion("SUCESOS", List.of(Accion.de("Inundación ya (40 s)", "backrooms inundacion 40"), Accion.de("Parar la inundación", "backrooms inundacion parar"),
						Accion.de("Alarma 60 s", "backrooms alarma 60"), Accion.de("Luz normal", "backrooms luz"))),
					new Seccion("TARJETAS Y BÓVEDA", List.of(Accion.de("Dónde están", "backrooms tarjetas"), Accion.ojo("Abrir la bóveda ya", "backrooms tarjetas abrir"),
						Accion.ojo("Todas a su escondite", "backrooms tarjetas reponer"))),
					new Seccion("IR A", List.of(Accion.de("Llevarme a las Piscinas", "backrooms piscinas"), Accion.de("La bóveda (salida)", "backrooms piscinas ir salida"),
						Accion.de("Una válvula", "backrooms piscinas ir valvula"), Accion.de("Tarjeta roja", "backrooms piscinas ir tarjeta_roja"),
						Accion.de("Tarjeta amarilla", "backrooms piscinas ir tarjeta_amarilla"), Accion.de("Tarjeta verde", "backrooms piscinas ir tarjeta_verde"),
						Accion.de("Contar cadáveres", "backrooms piscinas cadaveres")))));
			case 3 -> new Pagina(
				"Las Feeding Rooms: una fiesta infantil vacía. Del cuartito de inicio al laberinto (monedas que abren puertas, salas seguras), el castillo de bolas con el semáforo y la sala central con el bote; con 1500 tickets, la puerta del salón final y su tobogán. En los apagones, el Anfitrión caza.",
				List.of(new Entidad("Anfitrión", "El payaso de la fiesta: con luz ronda sin atacar; en el apagón persigue a quien ve a 8 bloques (no a través de paredes). Uno por cada 5 jugadores.", Accion.de("Que aparezca", "backrooms fiesta aparicion"))),
				List.of(TipoMision.TICKETS, TipoMision.PREMIO),
				List.of(new Seccion("SUCESOS", List.of(Accion.de("Apagón (caza)", "backrooms fiesta apagon"), Accion.de("Luz normal", "backrooms luz"),
						Accion.de("Releer la config", "backrooms fiesta recargar"))),
					new Seccion("JUEGOS Y PREMIOS (YO)", List.of(Accion.de("Mis datos", "backrooms fiesta ver"), Accion.de("Memoria y regalos", "backrooms fiesta juegos @s"),
						Accion.de("+100 tickets", "backrooms fiesta tickets 100"), Accion.de("+5 monedas", "backrooms fiesta monedas 5"))),
					new Seccion("IR A", List.of(Accion.de("Llevarme a la Fiesta", "backrooms fiesta"), Accion.de("Laberinto", "backrooms fiesta ir laberinto"),
						Accion.de("Castillo", "backrooms fiesta ir castillo"), Accion.de("Sala central", "backrooms fiesta ir central"),
						Accion.de("Salón final", "backrooms fiesta ir final"), Accion.de("Memoria", "backrooms fiesta ir memoria"),
						Accion.de("Regalos", "backrooms fiesta ir regalos"), Accion.de("Cocina", "backrooms fiesta ir cocina"),
						Accion.de("Siesta", "backrooms fiesta ir siesta"), Accion.de("Sala secreta", "backrooms fiesta ir secreta")))));
			default -> new Pagina(
				"La Estación Eléctrica, como la de Escape Together: tres pisos que se suben. En cada uno, un laberinto con rejas que se abren con un fusible en su caja (y se cierran si alguien lo saca); al final, un pasillo largo y en un costado la central (fusibles, cinta y cable). Con la central activa se abre la escalera al piso de arriba. La del piso 3 enciende la pasarela; al otro lado, una puerta encadenada (la cizalla). Abajo, el generador gasta gasolina: sin ella, apagón.",
				List.of(new Entidad("Amasijo", "Carne y brazos con una boca. Uno por piso con gente (más con muchos jugadores); no sube escaleras y las rejas cerradas le cortan el paso. Caza a quien ve; a oscuras (sin gasolina) te encuentra sin verte.", Accion.de("Invocar", "backrooms invocar amasijo"))),
				List.of(TipoMision.COMBUSTIBLE, TipoMision.PISO1, TipoMision.PISO2, TipoMision.PISO3, TipoMision.PASARELA),
				List.of(new Seccion("ESTADO", List.of(Accion.de("Ver todo", "backrooms estacion ver"), Accion.de("Generador al 100", "backrooms estacion combustible 100"),
						Accion.de("Generador al 10", "backrooms estacion combustible 10"), Accion.ojo("Vaciar el generador", "backrooms estacion combustible 0"),
						Accion.ojo("Siguiente central", "backrooms estacion avanzar"), Accion.ojo("Todo como al principio", "backrooms estacion reponer"))),
					new Seccion("SUCESOS", List.of(Accion.de("Luz normal", "backrooms luz"))),
					new Seccion("IR A", List.of(Accion.de("Llevarme a la Estación", "backrooms estacion"), Accion.de("Nave del generador", "backrooms estacion ir nave"),
						Accion.de("Laberinto 1", "backrooms estacion ir laberinto_1"), Accion.de("Central 1", "backrooms estacion ir central_1"),
						Accion.de("Laberinto 2", "backrooms estacion ir laberinto_2"), Accion.de("Central 2", "backrooms estacion ir central_2"),
						Accion.de("Laberinto 3", "backrooms estacion ir laberinto_3"), Accion.de("Central 3", "backrooms estacion ir central_3"),
						Accion.de("Pasarela", "backrooms estacion ir pasarela")))));
		};
	}

	private List<String> vivo(int i) {
		return switch (i) {
			case 0 -> this.estado.general();
			case 1 -> this.estado.nivel0();
			case 2 -> this.estado.piscinas();
			case 3 -> this.estado.fiesta();
			default -> this.estado.estacion();
		};
	}

	/* -------------------------------------------------------------- montaje */

	private int x0() {
		return Math.max(10, (this.width - 520) / 2);
	}

	private int x1() {
		return Math.min(this.width - 10, this.x0() + 520);
	}

	private int arriba() {
		return 46;
	}

	private int abajo() {
		return this.height - 26;
	}

	private void rehacer() {
		this.clearWidgets();
		this.init();
	}

	@Override
	protected void init() {
		this.textos.clear();
		int x0 = this.x0();
		int x1 = this.x1();
		int ancho = x1 - x0;
		// las pestanas
		int w = (ancho - 6 * (PESTANAS.length - 1)) / PESTANAS.length;
		for (int i = 0; i < PESTANAS.length; i++) {
			int n = i;
			Button b = Button.builder(Component.literal(i == pestana ? "[ " + PESTANAS[i] + " ]" : PESTANAS[i]), x -> {
				pestana = n;
				this.scroll = 0;
				this.rehacer();
			}).bounds(x0 + i * (w + 6), 18, w, 20).build();
			b.active = i != pestana;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.literal("↻"), x -> this.pedir()).bounds(x1 - 20, 2, 18, 14).build());

		Pagina p = pagina(pestana);
		int y = this.arriba() + 4 - this.scroll;
		int lineaAlto = this.font.lineHeight + 2;
		y = this.parrafo(p.descripcion(), x0 + 4, y, ancho - 8, GRIS) + 6;
		y = this.titulo("AHORA", x0, y);
		for (String l : this.vivo(pestana)) {
			y = this.parrafo(l, x0 + 8, y, ancho - 12, BLANCO);
		}
		y += 6;
		if (!p.entidades().isEmpty()) {
			y = this.titulo("ENTIDADES", x0, y);
			for (Entidad e : p.entidades()) {
				this.texto(e.nombre(), x0 + 8, y, AMARILLO);
				this.boton(e.accion(), x1 - 96, y - 4, 92);
				y = this.parrafo(e.descripcion(), x0 + 16, y + lineaAlto, ancho - 120, GRIS) + 6;
			}
		}
		if (!p.misiones().isEmpty()) {
			y = this.titulo("MISIONES", x0, y);
			for (TipoMision m : p.misiones()) {
				this.texto(m.titulo, x0 + 8, y, AMARILLO);
				y = this.parrafo(m.descripcion, x0 + 16, y + lineaAlto, ancho - 24, GRIS) + 4;
			}
			y += 4;
		}
		for (Seccion s : p.secciones()) {
			y = this.titulo(s.titulo(), x0, y);
			int bx = x0 + 8;
			int bw = (ancho - 16 - 2 * 6) / 3;
			int k = 0;
			for (Accion a : s.acciones()) {
				this.boton(a, bx + (k % 3) * (bw + 6), y, bw);
				k++;
				if (k % 3 == 0) {
					y += 24;
				}
			}
			if (k % 3 != 0) {
				y += 24;
			}
			y += 6;
		}
		this.alto = y + this.scroll - this.arriba();
	}

	private int titulo(String t, int x, int y) {
		this.texto(t, x + 4, y, AMARILLO);
		this.textos.add(new Object[] {"linea", x + 4 + this.font.width(t) + 6, y + 4, this.x1() - 4});
		return y + this.font.lineHeight + 6;
	}

	private void texto(String t, int x, int y, int color) {
		this.textos.add(new Object[] {Component.literal(t).getVisualOrderText(), x, y, color});
	}

	private int parrafo(String t, int x, int y, int ancho, int color) {
		for (FormattedCharSequence l : this.font.split(Component.literal(t), ancho)) {
			this.textos.add(new Object[] {l, x, y, color});
			y += this.font.lineHeight + 1;
		}
		return y;
	}

	private void boton(Accion a, int x, int y, int w) {
		if (y < this.arriba() || y + 20 > this.abajo()) {
			return;
		}
		boolean esperando = a.comando().equals(this.pendiente) && this.ticks < this.pendienteHasta;
		Component texto = Component.literal(esperando ? "¿SEGURO? (otra vez)" : a.texto());
		Button b = Button.builder(texto, x2 -> this.pulsar(a)).bounds(x, y, w, 20)
			.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("/" + a.comando()))).build();
		this.addRenderableWidget(b);
	}

	private void pulsar(Accion a) {
		if (a.peligrosa() && !(a.comando().equals(this.pendiente) && this.ticks < this.pendienteHasta)) {
			this.pendiente = a.comando();
			this.pendienteHasta = this.ticks + 60;
			this.rehacer();
			return;
		}
		this.pendiente = null;
		if (this.minecraft.player != null) {
			respuesta = "";
			this.minecraft.player.connection.sendCommand(a.comando());
		}
		this.refrescar = this.ticks + 10;
	}

	private void pedir() {
		if (this.minecraft.player != null) {
			this.minecraft.player.connection.sendCommand("backrooms panel");
		}
	}

	@Override
	public void tick() {
		this.ticks++;
		if (this.refrescar >= 0 && this.ticks >= this.refrescar) {
			this.refrescar = -1;
			this.pedir();
		}
		if (this.pendiente != null && this.ticks >= this.pendienteHasta) {
			this.pendiente = null;
			this.rehacer();
		}
		// lo de ahora, cada 5 s
		if (this.ticks % 100 == 0) {
			this.pedir();
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		int max = Math.max(0, this.alto - (this.abajo() - this.arriba()) + 10);
		int nuevo = Math.max(0, Math.min(max, this.scroll - (int) (dy * 24)));
		if (nuevo != this.scroll) {
			this.scroll = nuevo;
			this.rehacer();
		}
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		int x0 = this.x0();
		int x1 = this.x1();
		g.fill(x0 - 6, 0, x1 + 6, this.height, 0xE0100D08);
		g.fill(x0 - 6, 0, x1 + 6, 1, 0xFF6B5B2E);
		g.drawString(this.font, "PANEL DE LAS BACKROOMS", x0, 5, AMARILLO, false);
		g.enableScissor(x0 - 6, this.arriba(), x1 + 6, this.abajo());
		for (Object[] t : this.textos) {
			if ("linea".equals(t[0])) {
				g.fill((int) t[1], (int) t[2], (int) t[3], (int) t[2] + 1, 0x55F2D46B);
			} else {
				g.drawString(this.font, (FormattedCharSequence) t[0], (int) t[1], (int) t[2], (int) t[3], false);
			}
		}
		g.disableScissor();
		super.render(g, mx, my, parcial);
		// lo ultimo que ha contestado el servidor
		g.fill(x0 - 6, this.abajo() + 2, x1 + 6, this.height, 0xFF0A0805);
		String r = respuesta.isEmpty() ? "Pulsa un botón: aquí sale lo que conteste el servidor. Esc para cerrar." : respuesta;
		boolean error = r.startsWith("No ") || r.contains("no existe") || r.contains("Unknown");
		g.drawString(this.font, this.font.plainSubstrByWidth(r, x1 - x0), x0, this.abajo() + 9, error ? ROJO : GRIS, false);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		// el mundo se ve detras (sin el desenfoque de los menus)
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
