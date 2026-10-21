package net.backrooms.evento.mision;

/**
 * Las misiones de un explorador. La primera siempre es recoger casetes; las
 * otras dos salen al azar entre las de grabar con la camara (C), asi que dos
 * jugadores casi nunca tienen las mismas. La de la entidad tiene dos
 * versiones (sola o con las alarmas encendidas) y nunca tocan las dos.
 *
 * Reglas del organizador: los Smilers solo salen de noche, y la Bacteria
 * esta mas furiosa con las alarmas (las luces rojas) encendidas.
 */
public enum TipoMision {
	CASETES("Recoge los casetes", "Alguien estuvo grabando antes que tú. Sus cintas están tiradas por el Nivel 0, lejos unas de otras. El detector solo las capta de cerca."),
	LUCES_ROJAS("Graba las alarmas", "Cuando los tubos se pongan en rojo, grábalo con la cámara (C)."),
	ENTIDAD("Graba una entidad", "Consigue grabar a la Bacteria unos segundos sin que te atrape."),
	ENTIDAD_ALARMA("Graba la Bacteria en alarma", "Grábala con las alarmas encendidas. En rojo está más furiosa: no dejes que te vea primero."),
	SMILER("Graba un Smiler", "Solo salen de noche, en la oscuridad. Grábalo con la cámara antes de que se acerque."),

	/* las de las Piscinas (Nivel 37): las dos primeras de cada uno; la tercera, de todos */
	VALVULAS("Cierra tus válvulas", "Tres válvulas de las Piscinas son tuyas: el radar marca la más cercana. Mantén pulsado el clic derecho para cerrarlas."),
	SERPIENTE("Graba a la Serpiente", "Grábala con la cámara (C) unos segundos sin perderla del encuadre. Que no te vea primero."),
	SALIDA("Abre la salida de emergencia", "Solo hay tres tarjetas en todo el nivel: roja, amarilla y verde. Entre todos hay que encontrarlas y meterlas en los lectores de la puerta."),

	/* las de la Fiesta (Nivel Fun): juntar los tickets dando vueltas y salir por el tobogan del final */
	TICKETS("Consigue los tickets", "Sal del laberinto (las monedas de las mesas abren puertas; los localizadores de las salas seguras te guían), cruza el castillo moviéndote solo en verde y baja por el tobogán: cada vuelta da su bote. Para otra vuelta, salta a un cuadrado de noclip."),
	PREMIO("Sal de la fiesta", "Con tus tickets, la puerta del norte de la sala central se abre. Al fondo del salón, el tobogán: quédate en su boca."),

	/* las de la Estacion Electrica (Nivel 3): son de todos (las va cumpliendo JuegoEstacion) y la ultima, cruzar */
	COMBUSTIBLE("Arranca el generador", "El generador de la nave central casi no tiene combustible. Busca bidones por los sectores y vacíalos en su boca (clic derecho). Sin corriente no funciona nada, y se gasta."),
	PANELES("Repara los paneles", "Cada sector tiene dos paneles averiados. Cada uno pide un material (cable, caja de herramientas o cinta aislante): búscalo por los pasillos y úsalo en el panel. Con los dos de un sector, se abre su sala de fusibles."),
	FUSIBLES("Restablece el circuito", "Lleva los cuatro fusibles a la sala de control y ponlos en su hueco (pared oeste). Después, sube los interruptores (pared este) en el orden de la pantalla. Si te equivocas, salta todo."),
	PASARELA("Cruza la pasarela", "Con el circuito completo, la pasarela del norte tiene corriente. Crúzala hasta la puerta del final antes de que algo te encuentre.");

	public final String titulo;
	public final String descripcion;

	TipoMision(String titulo, String descripcion) {
		this.titulo = titulo;
		this.descripcion = descripcion;
	}

	/** Las de grabar que se sortean: la de la entidad, en una de sus dos versiones. */
	public static java.util.List<TipoMision> deGrabar(java.util.Random azar) {
		return new java.util.ArrayList<>(java.util.List.of(LUCES_ROJAS, azar.nextBoolean() ? ENTIDAD : ENTIDAD_ALARMA, SMILER));
	}
}
