package net.backrooms.evento.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Nota tirada en el suelo. Al usarla se lee: cada posicion da siempre la
 * misma nota (sale de las coordenadas), asi dos jugadores leen lo mismo.
 */
public class Nota extends EnSuelo {
	public static final MapCodec<Nota> CODEC = simpleCodec(Nota::new);

	private static final String[] TEXTOS = {
		"Día 3. Las luces no se apagan nunca. Yo sí.",
		"Si oyes pasos que siguen a los tuyos, no te pares. No te pares.",
		"He marcado las paredes con flechas. Las flechas ya no apuntan adonde las dejé.",
		"El agua de almendras ayuda. Lo que hay en los charcos no es agua.",
		"Encontré mi propia letra en una pared. No recuerdo haberla escrito.",
		"Cuenta las habitaciones. Cuando el número baje, corre.",
		"La cámara ve cosas que yo no veo. Graba siempre.",
		"No contestes si alguien dice tu nombre desde la oscuridad.",
		"El zumbido cambia de tono justo antes. Aprende a oírlo.",
		"Somos 12. Éramos 14. Nadie se acuerda de los otros dos.",
		"Hay casetes por el suelo. Alguien estuvo grabando antes que nosotros.",
		"Si las luces se ponen rojas, no estás solo en la sala."
	};

	public Nota(BlockBehaviour.Properties properties) {
		super(properties, Block.box(3, 0, 3, 13, 1, 13), net.minecraft.world.phys.shapes.Shapes.empty());
	}

	@Override
	protected MapCodec<? extends Nota> codec() {
		return CODEC;
	}

	public static String texto(BlockPos pos) {
		long h = pos.asLong() * 0x9E3779B97F4A7C15L;
		return TEXTOS[(int) Math.floorMod(h ^ (h >>> 31), TEXTOS.length)];
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (!nivel.isClientSide()) {
			jugador.displayClientMessage(Component.literal("Una nota arrugada:").withStyle(ChatFormatting.GRAY), false);
			jugador.displayClientMessage(Component.literal("«" + texto(pos) + "»").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW), false);
		}
		return InteractionResult.SUCCESS;
	}
}
