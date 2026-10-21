package net.backrooms.evento.piscinas;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La puerta de la salida de emergencia de las Piscinas: el hueco de 3x3 de la boveda en la pared
 * (PARTE = columna * 3 + fila, PlanoPiscinas#detallesSalida). Estos bloques no se ven (la boveda
 * que se ve y se abre girando es Boveda) y dejan pasar la luz; solo chocan. Con las tres tarjetas
 * metidas se abre (ABIERTA, la abre Tarjetas): pasa quien ha hecho sus misiones; los demas se
 * topan con ella. Quien entra en el pasillo de detras ve la cinematica y se tira por el tobogan
 * a la Fiesta (TransicionFiesta).
 */
public class PuertaSalida extends HorizontalDirectionalBlock {
	public static final MapCodec<PuertaSalida> CODEC = simpleCodec(PuertaSalida::new);
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 8);
	public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");

	public PuertaSalida(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, 0).setValue(ABIERTA, false));
	}

	@Override
	protected MapCodec<? extends PuertaSalida> codec() {
		return CODEC;
	}

	/**
	 * Si ese jugador puede cruzar (sus misiones hechas). En el servidor lo sabe Misiones; en el
	 * cliente, que es quien mueve al jugador y choca, lo pone BackroomsEventoCliente.
	 */
	public static java.util.function.Predicate<Player> pasaCliente = p -> false;

	private static boolean pasa(Player p) {
		return p instanceof ServerPlayer sp ? net.backrooms.evento.mision.Misiones.get().todasHechas(sp) : pasaCliente.test(p);
	}

	/** Abierta solo deja de ser pared para quien puede cruzar; para los demas (y la Serpiente), sigue cerrada. */
	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		if (estado.getValue(ABIERTA) && ctx instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player p && pasa(p)) {
			return Shapes.empty();
		}
		return Shapes.block();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer sp) {
			if (!estado.getValue(ABIERTA)) {
				sp.displayClientMessage(Component.literal("Cerrada. Hacen falta las tres tarjetas (" + Tarjetas.get().metidas() + "/3).").withStyle(ChatFormatting.GRAY), true);
			} else if (!pasa(sp)) {
				nivel.playSound(null, pos, net.backrooms.evento.Sonidos.ASCENSOR_DENEGADO, SoundSource.BLOCKS, 0.9F, 1.0F);
				sp.displayClientMessage(Component.literal("ACCESO DENEGADO · Completa tus misiones").withStyle(ChatFormatting.RED), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** No se ve nunca: la puerta que se ve es la de la boveda (Boveda). */
	@Override
	protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState estado) {
		return net.minecraft.world.level.block.RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState estado) {
		return Shapes.empty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PARTE, ABIERTA);
	}
}
