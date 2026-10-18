package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.bloques.EnSuelo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Una nota de la Fiesta, en el suelo: al usarla se lee (VARIANTE elige cual). Cuentan, a trozos
 * y sin explicarlo todo, para que sirven las Feeding Rooms (ver LORE.md, «Nivel Fun»).
 */
public class NotaFiesta extends EnSuelo {
	public static final MapCodec<NotaFiesta> CODEC = simpleCodec(NotaFiesta::new);
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 7);

	private static final String[][] TEXTOS = {
		{"Una hoja con manchas de grasa:", "Menú de hoy: invitados del grupo 14. Servir antes de que se despierten."},
		{"Escrito en el dorso de un ticket:", "Los tickets no son para nosotros. Cuentan cuánto le hemos dado."},
		{"Una servilleta de fiesta:", "El de la mascota me ha guardado un trozo de tarta. No me acuerdo de haber comido nunca."},
		{"Una nota arrugada:", "Si el suelo zumba, no te quedes encima. O sí: abajo hay otro camino."},
		{"Un informe con el sello del Proyecto Umbral:", "Registro de alimentación, sala 3: 28 sujetos. Rendimiento: excelente. Nadie pidió salir."},
		{"Una nota con letra temblorosa:", "Mi hija vino a una fiesta en 1998. Hoy he visto su dibujo en la pared."},
		{"Escrito con cera en un cartón:", "Cuando se va la luz, agáchate. Ve peor de lo que crees."},
		{"Una tarjeta de felicitación:", "El Gran Premio no es una salida. Es una puerta. Y las puertas llevan a algún sitio."}
	};

	public NotaFiesta(BlockBehaviour.Properties propiedades) {
		super(propiedades, Block.box(3, 0, 3, 13, 1, 13), Shapes.empty());
		this.registerDefaultState(this.defaultBlockState().setValue(VARIANTE, 0));
	}

	@Override
	protected MapCodec<? extends NotaFiesta> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (!nivel.isClientSide()) {
			String[] t = TEXTOS[estado.getValue(VARIANTE)];
			nivel.playSound(null, pos, net.backrooms.evento.Sonidos.NOTA_LEER, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F + nivel.getRandom().nextFloat() * 0.2F);
			jugador.displayClientMessage(Component.literal(t[0]).withStyle(ChatFormatting.GRAY), false);
			jugador.displayClientMessage(Component.literal("«" + t[1] + "»").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW), false);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VARIANTE);
	}
}
