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
	PISO1("Activa la central del piso 1", "Cruza el laberinto: las rejas se abren con un fusible en su caja (en el muro de al lado); si alguien lo saca, se cierran. El laberinto sube por escaleras: arriba del todo, un pasillo largo y en un costado la central. Ponle 2 fusibles y 1 cinta aislante: se abre su puerta al piso 2."),
	PISO2("Activa la central del piso 2", "Otro laberinto, otra central: 2 fusibles, 1 cinta y 1 cable. Las rejas también sirven para cerrarle el paso a lo que te persigue. En los buzones, con un fusible, puede haber algo útil... o nada."),
	PISO3("Activa la central del piso 3", "La última: 3 fusibles, 2 cintas y 2 cables. Da corriente a la pasarela. En algún trastero de este piso hay una cizalla: hará falta."),
	PASARELA("Cruza la pasarela", "La pasarela tiene corriente. Al otro lado, la puerta del final está encadenada: córtala con la cizalla y sal.");

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
