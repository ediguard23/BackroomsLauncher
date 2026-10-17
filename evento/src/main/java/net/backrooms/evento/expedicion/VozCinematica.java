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
 * esta el mod de voz): quien esta en una cinematica (EnCinematica) ni habla ni oye.
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
			}
		});
		registro.registerEvent(EntitySoundPacketEvent.class, VozCinematica::alOir);
		registro.registerEvent(LocationalSoundPacketEvent.class, VozCinematica::alOir);
		registro.registerEvent(StaticSoundPacketEvent.class, VozCinematica::alOir);
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
