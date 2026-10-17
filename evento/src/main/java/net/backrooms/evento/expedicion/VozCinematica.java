package net.backrooms.evento.expedicion;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import org.jspecify.annotations.Nullable;

/**
 * Plugin de Simple Voice Chat (entrypoint "voicechat" de fabric.mod.json; solo se carga si
 * esta el mod de voz): quien esta en una cinematica (EnCinematica) ni habla ni oye. Y lo
 * que dice el staff con el megafono encendido (MegafonoStaff) lo oyen todos, sin distancia.
 */
public class VozCinematica implements VoicechatPlugin {
	@Override
	public String getPluginId() {
		return "backrooms_evento";
	}

	@Override
	public void registerEvents(EventRegistration registro) {
		registro.registerEvent(MicrophonePacketEvent.class, e -> {
			if (enCinematica(e.getSenderConnection())) {
				e.cancel();
				return;
			}
			VoicechatConnection habla = e.getSenderConnection();
			if (habla != null && habla.getPlayer() != null && MegafonoStaff.activo(habla.getPlayer().getUuid())) {
				megafono(e, habla.getPlayer().getUuid());
			}
		});
		registro.registerEvent(EntitySoundPacketEvent.class, VozCinematica::alOir);
		registro.registerEvent(LocationalSoundPacketEvent.class, VozCinematica::alOir);
		registro.registerEvent(StaticSoundPacketEvent.class, VozCinematica::alOir);
	}

	/** La voz del staff a todos, como sonido sin posicion (en vez de solo a los de cerca). */
	private static void megafono(MicrophonePacketEvent e, java.util.UUID quien) {
		de.maxhenkel.voicechat.api.VoicechatServerApi api = e.getVoicechat();
		de.maxhenkel.voicechat.api.packets.StaticSoundPacket p = e.getPacket().staticSoundPacketBuilder().channelId(quien).build();
		for (java.util.UUID u : MegafonoStaff.jugadores()) {
			if (u.equals(quien) || EnCinematica.en(u)) {
				continue;
			}
			VoicechatConnection c = api.getConnectionOf(u);
			if (c != null) {
				api.sendStaticSoundPacketTo(c, p);
			}
		}
		// los de cerca ya la oyen por el megafono: sin el eco de la voz normal
		e.cancel();
	}

	private static void alOir(SoundPacketEvent<?> e) {
		if (enCinematica(e.getReceiverConnection())) {
			e.cancel();
		}
	}

	private static boolean enCinematica(@Nullable VoicechatConnection c) {
		return c != null && c.getPlayer() != null && EnCinematica.en(c.getPlayer().getUuid());
	}
}
