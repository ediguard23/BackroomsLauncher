package net.backrooms.menu;

import java.util.Random;
import net.backrooms.menu.render.Fondo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * El nivel de las Backrooms en el que esta ambientado el evento: fondo,
 * colores, textos y sonidos de los menus.
 *
 * Lo elige el organizador con "level" en pack/evento.json (el launcher lo
 * pasa al mod en config/backrooms-event.json). Por defecto el Nivel 0, el del
 * launcher; las Piscinas (Nivel 37) quedan listas para el proximo evento.
 */
public final class Tema {
	public static final Tema NIVEL_0 = nivel0();
	public static final Tema PISCINAS = piscinas();

	private static Tema actual;

	public String nivel;
	public String lema;
	/** Subtitulo de SENAL PERDIDA. */
	public String desconexion;
	public Fondo fondo;
	/** true: el destello apaga los tubos un instante (Nivel 0); false: entra mas luz (Piscinas). */
	public boolean parpadea;

	public SoundEvent ambiente;
	public SoundEvent musica;
	public SoundEvent encima;
	public SoundEvent clic;
	public SoundEvent entrar;
	public SoundEvent destello;

	/** Textos claros del HUD. */
	public int tubo;
	/** Rotulos secundarios (NIVEL X, titulos de los recuadros). */
	public int acento;
	/** Texto sobre un boton encendido. */
	public int negro;
	/** RGB de los velos que oscurecen el fondo. */
	public int velo;
	/** RGB del fondo de los recuadros y botones del HUD. */
	public int caja;

	public int paseFondo;
	public int paseCintaArriba;
	public int paseCintaAbajo;
	public int paseRanura;
	public int paseTinta;
	public int fotoMarco;
	public int fotoFondo;
	public int pistaOn;
	public int pistaOff;
	public int bola;

	public int papel;
	public int papelRayas;
	public int papelTinta;
	public int papelFecha;
	public int papelTexto;
	public int papelVacio;

	/** RGB del halo del boton de tubo. */
	public int tuboHalo;
	public int tuboMarco;
	public int tuboApagado;
	/** RGB del degradado del tubo encendido. */
	public int tuboArriba;
	public int tuboAbajo;
	public int tuboTinta;
	public int tuboTintaOff;

	private Tema() {
	}

	private static SoundEvent sonido(String ruta) {
		return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("backrooms", ruta));
	}

	private static Tema nivel0() {
		Tema t = new Tema();
		t.nivel = "NIVEL 0";
		t.lema = "Si sales de la realidad por accidente, acabas aquí.";
		t.desconexion = "El zumbido se ha cortado. Esto es lo último que llegó:";
		t.fondo = Fondo.NIVEL_0;
		t.parpadea = true;
		t.sonidos("nivel0");
		t.tubo = 0xFFFFF4C8;
		t.acento = 0xFFD9C062;
		t.negro = 0xFF0D0B05;
		t.velo = 0x0D0B05;
		t.caja = 0x0A0904;
		t.paseFondo = 0xFFEBE4CB;
		t.paseCintaArriba = 0xFFE3C552;
		t.paseCintaAbajo = 0xFFC9A93A;
		t.paseRanura = 0xFF2A2618;
		t.paseTinta = 0xFF1C1A10;
		t.fotoMarco = 0xFF8F8565;
		t.fotoFondo = 0xFFCFC6A6;
		t.pistaOn = 0xFF3D7A3A;
		t.pistaOff = 0xFF9B9172;
		t.bola = 0xFFF6F0DA;
		t.papel = 0xFFEEE5C6;
		t.papelRayas = 0x2E3C5AA0;
		t.papelTinta = 0xFF2A2517;
		t.papelFecha = 0xFF8A3A22;
		t.papelTexto = 0xFF4A4330;
		t.papelVacio = 0xFF6D6550;
		t.tuboHalo = 0xFFEEAA;
		t.tuboMarco = 0xFF3A3422;
		t.tuboApagado = 0xFF2A261B;
		t.tuboArriba = 0xFFFDF0;
		t.tuboAbajo = 0xEFE09A;
		t.tuboTinta = 0xFF2A2312;
		t.tuboTintaOff = 0xFF6D6550;
		return t;
	}

	private static Tema piscinas() {
		Tema t = new Tema();
		t.nivel = "NIVEL 37";
		t.lema = "El agua está templada. No recuerdas haber entrado.";
		t.desconexion = "El agua se lo ha llevado. Esto es lo último que llegó:";
		t.fondo = Fondo.PISCINAS;
		t.parpadea = false;
		t.sonidos("piscinas");
		t.tubo = 0xFFEAFCFF;
		t.acento = 0xFF8FE3EA;
		t.negro = 0xFF06161A;
		t.velo = 0x041A20;
		t.caja = 0x04141A;
		t.paseFondo = 0xFFEEF5F3;
		t.paseCintaArriba = 0xFFA6DFE3;
		t.paseCintaAbajo = 0xFF6CBCC4;
		t.paseRanura = 0xFF15292C;
		t.paseTinta = 0xFF0F2629;
		t.fotoMarco = 0xFF8FA6A3;
		t.fotoFondo = 0xFFC9D6D3;
		t.pistaOn = 0xFF3D7A6A;
		t.pistaOff = 0xFF8FA09E;
		t.bola = 0xFFF6FBFA;
		t.papel = 0xFFF2F5EF;
		t.papelRayas = 0x2E3C6EA0;
		t.papelTinta = 0xFF1F2A2B;
		t.papelFecha = 0xFF2A6F7A;
		t.papelTexto = 0xFF4A5553;
		t.papelVacio = 0xFF6D7A78;
		t.tuboHalo = 0xDDF8FF;
		t.tuboMarco = 0xFF233A3D;
		t.tuboApagado = 0xFF2A3A3C;
		t.tuboArriba = 0xF8FFFF;
		t.tuboAbajo = 0xC4EEF2;
		t.tuboTinta = 0xFF0F2E2F;
		t.tuboTintaOff = 0xFF6D8285;
		return t;
	}

	private void sonidos(String base) {
		this.ambiente = sonido(base + ".ambiente");
		this.musica = sonido(base + ".musica");
		this.encima = sonido(base + ".encima");
		this.clic = sonido(base + ".clic");
		this.entrar = sonido(base + ".entrar");
		this.destello = sonido(base + ".destello");
	}

	/** El tema del evento (se lee una vez de config/backrooms-event.json). */
	public static Tema actual() {
		if (actual == null) {
			String nivel = Evento.cargar().nivel.trim();
			actual = nivel.equals("37") || nivel.equalsIgnoreCase("piscinas") ? PISCINAS : NIVEL_0;
		}
		return actual;
	}

	/* --------------------------------------------- fondo con destellos */

	private static final Random AZAR = new Random();
	private static long proximoDestello = System.currentTimeMillis() + 8000;
	private static long destelloDesde = -1;
	private static boolean dobleParpadeo;

	/**
	 * Dibuja el pasillo del nivel. Cada cierto tiempo hay un destello: en el
	 * Nivel 0 los tubos se apagan un instante (como el parpadeo del launcher),
	 * en las Piscinas entra un golpe de luz por los arcos.
	 */
	public void dibujarFondo(GuiGraphics g, int ancho, int alto) {
		long ahora = System.currentTimeMillis();
		if (ahora >= proximoDestello) {
			destelloDesde = ahora;
			dobleParpadeo = AZAR.nextFloat() < 0.3F;
			proximoDestello = ahora + (this.parpadea ? 7000 + AZAR.nextInt(16000) : 15000 + AZAR.nextInt(20000));
			Sonidos.ui(this.destello, this.parpadea ? 0.5F : 0.6F);
		}
		long t = destelloDesde < 0 ? Long.MAX_VALUE : ahora - destelloDesde;
		if (this.parpadea) {
			Fondo.luz = t < 70 ? 0.25F : t < 150 ? 1.0F : t < 210 ? 0.4F
				: dobleParpadeo && t >= 520 && t < 700 ? 0.15F : 1.0F;
		} else {
			float d = t / 1000.0F;
			Fondo.luz = d < 1.6F ? 1.0F + 0.22F * (float) Math.sin(Math.PI * d / 1.6F) : 1.0F;
		}
		this.fondo.dibujar(g, ancho, alto);
	}
}
