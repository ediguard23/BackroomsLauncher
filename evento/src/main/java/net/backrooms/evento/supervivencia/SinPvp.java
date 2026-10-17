package net.backrooms.evento.supervivencia;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * En todo el evento (recepcion, niveles, vestibulo) los jugadores no se pueden pegar:
 * ni golpes ni nada que lance un jugador (flechas, etc.) hace dano a otro jugador.
 * No depende del "pvp" de server.properties ni de gamerules: va siempre.
 */
public final class SinPvp {
	private SinPvp() {
	}

	public static void registrar() {
		// el golpe ni se cuenta (sin empujon ni sonido de golpe)
		AttackEntityCallback.EVENT.register((jugador, nivel, mano, objetivo, golpe) ->
			objetivo instanceof Player && objetivo != jugador ? InteractionResult.FAIL : InteractionResult.PASS);
		// y cualquier dano que venga de otro jugador, tambien a distancia
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victima, fuente, cantidad) ->
			!(victima instanceof Player && fuente.getEntity() instanceof Player atacante && atacante != victima));
	}
}
