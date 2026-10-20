package net.backrooms.evento.cliente.fiesta;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.cliente.AmbienteCliente;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.fiesta.Fiesta;
import net.backrooms.evento.fiesta.PlanoFiesta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que se oye en la Fiesta, en el cliente (sonidos en tools/sonidos/fiesta.js). Una fiesta
 * infantil que lleva demasiado tiempo encendida donde no deberia haber nadie:
 *
 *   el aire de la sala         en bucle, muy bajo (zumbido electrico y ventilacion)
 *   la musica de la fiesta     desde el altavoz mas cercano, lejana y torcida; se calla en el
 *                              apagon y a veces se queda en silencio un rato largo. Baja mucho
 *                              mientras habla o se rie el Anfitrion o habla la megafonia (Cancion)
 *   un globo que cruje         cada 20-60 s, por encima, a 4-15 bloques
 *   un juguete que se enciende cada 45-120 s, a 8-25 bloques, sin que nadie lo toque
 *   un nino que habla          cada 1,5-4 min, desde una sala en la que no estas
 *   un anuncio de la megafonia cada 3-6 min, desde el altavoz mas cercano (cada uno el suyo)
 *   la risa del Anfitrion      cada 3-7 min, muy lejos y sin saber de donde
 *   la puerta de la salida     zumba (como un fluorescente) cada 2,5 s si estas a menos de 24 bloques
 *
 * Nada suena a la vez para todos: cada cliente sortea lo suyo.
 */
public final class AmbienteFiesta {
	private enum Tipo {
		MUSICA(36, 48), GLOBO(20, 60), JUGUETE(45, 120), NINO(90, 240), ANUNCIO(180, 360), RISA(180, 420);

		final int min;
		final int max;

		Tipo(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	private static final RandomSource AZAR = RandomSource.create();
	private static final long[] SIGUIENTE = new long[Tipo.values().length];
	private static long ticks;
	private static boolean dentro;
	private static Aire aire;
	private static int anuncio;
	private static long zumbido;
	/** La cancion que suena ahora (no se pone otra encima hasta que acabe). */
	private static SoundInstance cancion;
	/** Hasta cuando (reloj del sistema) la musica va bajita: alguien habla. */
	private static long bajaHasta;
	private static boolean escuchando;

	private AmbienteFiesta() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer j = mc.player;
		if (mc.level == null || j == null || !Fiesta.es(mc.level)) {
			dentro = false;
			return;
		}
		if (mc.isPaused() || CinematicaCliente.activa() || !j.isAlive()) {
			return;
		}
		ticks++;
		if (!escuchando) {
			// cada voz del Anfitrion o de la megafonia baja la musica unos segundos
			escuchando = true;
			mc.getSoundManager().addListener((s, eventos, alcance) -> {
				String p = s.getIdentifier().getPath();
				if (p.startsWith("anfitrion.voz") || p.startsWith("anfitrion.risa") || p.startsWith("anfitrion.detecta") || p.startsWith("fiesta.anuncio")
					|| p.startsWith("fiesta.bienvenida") || p.startsWith("fiesta.apagon") || p.startsWith("fiesta.luz")) {
					bajaHasta = net.minecraft.util.Util.getMillis() + (p.startsWith("fiesta") ? 7000 : 4500);
				}
			});
		}
		if (aire == null || aire.isStopped() || !mc.getSoundManager().isActive(aire)) {
			aire = new Aire();
			mc.getSoundManager().play(aire);
		}
		if (!dentro) {
			dentro = true;
			anuncio = AZAR.nextInt(Sonidos.FIESTA_ANUNCIOS.length);
			for (Tipo t : Tipo.values()) {
				SIGUIENTE[t.ordinal()] = ticks + Math.round(20 * (t.min + AZAR.nextFloat() * (t.max - t.min)) * (t == Tipo.MUSICA ? 0.05F : 0.6F));
			}
		}
		boolean apagon = AmbienteCliente.apagon();
		// los noclip de la sala central echan particulas de portal hacia arriba (para que se vean de lejos)
		if (ticks % 3 == 0) {
			for (int[] n : PlanoFiesta.NOCLIPS) {
				if (j.distanceToSqr(n[0] + 1, PlanoFiesta.SUELO, n[1] + 1) < 40 * 40) {
					mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, n[0] + AZAR.nextDouble() * 2, PlanoFiesta.SUELO + 0.6,
						n[1] + AZAR.nextDouble() * 2, 0.0, 0.6 + AZAR.nextDouble() * 0.6, 0.0);
					mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, n[0] + AZAR.nextDouble() * 2, PlanoFiesta.SUELO + 1.0,
						n[1] + AZAR.nextDouble() * 2, 0.0, 0.04, 0.0);
				}
			}
		}
		// la puerta roja de la salida del laberinto zumba: la pista de que esta cerca
		Vec3 salida = new Vec3(PlanoFiesta.SALIDA_X - 0.5, PlanoFiesta.SUELO + 2.0, PlanoFiesta.SALIDA_Z + 0.5);
		if (ticks >= zumbido && j.position().distanceTo(salida) < 24) {
			zumbido = ticks + 50;
			sonar(mc, Sonidos.FIESTA_SALIDA, salida, 1.0F, 0.97F + AZAR.nextFloat() * 0.06F);
		}
		for (Tipo t : Tipo.values()) {
			if (ticks < SIGUIENTE[t.ordinal()]) {
				continue;
			}
			programar(t);
			Vec3 ojo = j.getEyePosition();
			switch (t) {
				case MUSICA -> {
					// a veces, un silencio largo (la cancion se acaba y no empieza otra); nunca dos a la vez,
					// y en el castillo no (alli suena la del semaforo)
					if (cancion != null && mc.getSoundManager().isActive(cancion)) {
						SIGUIENTE[t.ordinal()] = ticks + 100;
						continue;
					}
					if (apagon || AZAR.nextFloat() < 0.15F || PlanoFiesta.enCastillo(j.getX(), j.getZ())) {
						SIGUIENTE[t.ordinal()] += 20L * (20 + AZAR.nextInt(40));
						continue;
					}
					int[] a = altavoz(ojo);
					if (a != null) {
						cancion = new Cancion(new Vec3(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5));
						mc.getSoundManager().play(cancion);
					}
				}
				case GLOBO -> sonar(mc, Sonidos.FIESTA_GLOBO, alrededor(ojo, 4, 15, 2.0, 6.0), 0.5F + AZAR.nextFloat() * 0.3F, 0.85F + AZAR.nextFloat() * 0.3F);
				case JUGUETE -> sonar(mc, Sonidos.FIESTA_JUGUETE, alrededor(ojo, 8, 25, -1.0, 0.5), 0.6F + AZAR.nextFloat() * 0.3F, 0.9F + AZAR.nextFloat() * 0.2F);
				case NINO -> {
					// desde otra sala: la de verdad mas lejana de las cercanas, nunca la tuya
					PlanoFiesta.Sala aqui = PlanoFiesta.sala(j.getX(), j.getZ());
					PlanoFiesta.Sala otra = null;
					for (int k = 0; k < 8 && otra == null; k++) {
						PlanoFiesta.Sala s = PlanoFiesta.SALAS.get(AZAR.nextInt(PlanoFiesta.SALAS.size()));
						if (s != aqui) {
							otra = s;
						}
					}
					if (otra != null) {
						Vec3 p = new Vec3((otra.x0() + otra.x1()) / 2.0, PlanoFiesta.SUELO + 2, (otra.z0() + otra.z1()) / 2.0);
						if (p.distanceTo(ojo) > 70) {
							p = ojo.add(p.subtract(ojo).normalize().scale(30));
						}
						sonar(mc, Sonidos.FIESTA_NINO, p, 1.0F, 0.92F + AZAR.nextFloat() * 0.14F);
					}
				}
				case ANUNCIO -> {
					if (apagon) {
						continue;
					}
					int[] a = altavoz(ojo);
					if (a != null) {
						anuncio = (anuncio + 1) % Sonidos.FIESTA_ANUNCIOS.length;
						sonar(mc, Sonidos.FIESTA_ANUNCIOS[anuncio], new Vec3(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5), 2.2F, 1.0F);
					}
				}
				case RISA -> sonar(mc, Sonidos.ANFITRION_RISA, alrededor(ojo, 40, 70, 0.0, 3.0), 1.4F, 0.9F + AZAR.nextFloat() * 0.15F);
				default -> {
				}
			}
		}
	}

	private static void programar(Tipo t) {
		SIGUIENTE[t.ordinal()] = ticks + Math.round(20 * (t.min + AZAR.nextFloat() * (t.max - t.min)));
	}

	/** El altavoz mas cercano (a menos de 60 bloques), o null. */
	private static int[] altavoz(Vec3 ojo) {
		int[] mejor = null;
		double d = 60 * 60;
		for (int[] a : PlanoFiesta.ALTAVOCES) {
			double dd = ojo.distanceToSqr(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5);
			if (dd < d) {
				d = dd;
				mejor = a;
			}
		}
		return mejor;
	}

	private static SoundInstance sonar(Minecraft mc, SoundEvent sonido, Vec3 p, float volumen, float tono) {
		SoundInstance s = new SimpleSoundInstance(sonido.location(), SoundSource.AMBIENT, volumen, tono,
			RandomSource.create(AZAR.nextLong()), false, 0, SoundInstance.Attenuation.LINEAR, p.x, p.y, p.z, false);
		mc.getSoundManager().play(s);
		return s;
	}

	private static Vec3 alrededor(Vec3 o, double min, double max, double dy0, double dy1) {
		double ang = AZAR.nextDouble() * Math.PI * 2;
		double d = min + AZAR.nextDouble() * (max - min);
		return new Vec3(o.x + Math.cos(ang) * d, o.y + dy0 + AZAR.nextDouble() * (dy1 - dy0), o.z + Math.sin(ang) * d);
	}

	/** La cancion de la fiesta desde un altavoz: baja a una cuarta parte mientras alguien habla y vuelve despacio. */
	private static final class Cancion extends AbstractTickableSoundInstance {
		private static final float VOLUMEN = 1.6F;

		Cancion(Vec3 p) {
			super(Sonidos.FIESTA_MUSICA, SoundSource.AMBIENT, RandomSource.create(AZAR.nextLong()));
			this.looping = false;
			this.delay = 0;
			this.volume = VOLUMEN;
			this.x = p.x;
			this.y = p.y;
			this.z = p.z;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !Fiesta.es(mc.level)) {
				this.stop();
				return;
			}
			float quiero = net.minecraft.util.Util.getMillis() < bajaHasta ? VOLUMEN * 0.25F : VOLUMEN;
			this.volume += (quiero - this.volume) * (quiero < this.volume ? 0.3F : 0.04F);
		}
	}

	/** El aire de la Fiesta en bucle; en el apagon baja (sin ventilacion), al salir se apaga. */
	private static final class Aire extends AbstractTickableSoundInstance {
		Aire() {
			super(Sonidos.FIESTA_AMBIENTE, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			boolean aqui = mc.level != null && Fiesta.es(mc.level) && mc.player != null;
			float quiero = !aqui ? 0.0F : AmbienteCliente.apagon() ? 0.15F : 0.45F;
			this.volume += (quiero - this.volume) * 0.05F;
			if (!aqui && this.volume < 0.01F) {
				this.stop();
			}
		}
	}
}
