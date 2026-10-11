package net.backrooms.evento.cliente.piscinas;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.backrooms.evento.Sonidos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;

/**
 * El susurro que dice el nombre de quien lo oye, justo detras de su cabeza (en 3D: si se
 * gira, se oye donde estaba). El sonido se sintetiza aqui (Susurrador) y se le da al motor
 * como un stream propio: en sounds.json, "piscinas.susurro" es solo un hueco vacio en
 * streaming (FabricSoundInstance#getAudioStream).
 */
public final class SusurroNombre extends AbstractSoundInstance {
	private final short[] muestras;

	private SusurroNombre(short[] muestras, Vec3 donde) {
		super(Sonidos.PISCINAS_SUSURRO, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
		this.muestras = muestras;
		this.x = donde.x;
		this.y = donde.y;
		this.z = donde.z;
		this.volume = 0.75F;
		this.pitch = 1.0F;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.relative = false;
	}

	/** Le susurra su nombre al jugador de este cliente, a un paso detras de la cabeza. */
	public static void sonar(Minecraft mc, LocalPlayer j) {
		RandomSource azar = j.getRandom();
		Vec3 mira = j.getViewVector(1.0F).multiply(1, 0, 1).normalize();
		Vec3 lado = new Vec3(-mira.z, 0, mira.x).scale((azar.nextFloat() - 0.5F) * 0.8F);
		Vec3 donde = j.getEyePosition().subtract(mira.scale(1.1)).add(lado).add(0, 0.1, 0);
		short[] m = Susurrador.susurrar(j.getGameProfile().name(), azar.nextLong());
		mc.getSoundManager().play(new SusurroNombre(m, donde));
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary sonidos, Identifier id, boolean repetir) {
		return CompletableFuture.completedFuture(new Pcm(this.muestras));
	}

	/** Las muestras, de trozo en trozo, como las pide el canal de OpenAL; null al acabar. */
	private static final class Pcm implements AudioStream {
		private static final AudioFormat FORMATO = new AudioFormat(Susurrador.SR, 16, 1, true, false);
		private final short[] muestras;
		private int i;

		Pcm(short[] muestras) {
			this.muestras = muestras;
		}

		@Override
		public AudioFormat getFormat() {
			return FORMATO;
		}

		@Override
		public ByteBuffer read(int bytes) {
			int n = Math.min(bytes / 2, this.muestras.length - this.i);
			if (n <= 0) {
				return null;
			}
			ByteBuffer b = BufferUtils.createByteBuffer(n * 2);
			for (int k = 0; k < n; k++) {
				short s = this.muestras[this.i++];
				b.put((byte) (s & 0xFF));
				b.put((byte) ((s >> 8) & 0xFF));
			}
			b.flip();
			return b;
		}

		@Override
		public void close() {
		}
	}
}
