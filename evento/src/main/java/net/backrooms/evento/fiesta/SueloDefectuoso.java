package net.backrooms.evento.fiesta;

import com.mojang.serialization.MapCodec;
import net.backrooms.evento.Sonidos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El suelo defectuoso de la Fiesta (PlanoFiesta.PARKOURS): parece goma o moqueta como el de
 * alrededor, pero su textura tiembla y se corrompe a ratos (animada), zumba muy bajo y suelta
 * alguna chispa. Si alguien se queda encima, cruje y al momento cede: deja de verse y de chocar
 * (ROTO) y se cae a la zona de parkour de debajo; con los de al lado (el parche entero). Al
 * rato vuelve a estar como si nada. VARIANTE: 0 goma, 1 moqueta de confeti.
 */
public class SueloDefectuoso extends Block {
	public static final MapCodec<SueloDefectuoso> CODEC = simpleCodec(SueloDefectuoso::new);
	public static final BooleanProperty ROTO = BooleanProperty.create("roto");
	public static final IntegerProperty VARIANTE = IntegerProperty.create("variante", 0, 1);
	/** Ticks encima hasta que cede, y ticks roto hasta que se rehace. */
	private static final int CEDE = 16;
	private static final int REHACE = 80;

	public SueloDefectuoso(BlockBehaviour.Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(ROTO, false).setValue(VARIANTE, 0));
	}

	@Override
	protected MapCodec<? extends SueloDefectuoso> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return estado.getValue(ROTO) ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext ctx) {
		return estado.getValue(ROTO) ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return estado.getValue(ROTO) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Override
	public void stepOn(Level nivel, BlockPos pos, BlockState estado, Entity quien) {
		if (nivel instanceof ServerLevel s && quien instanceof Player && !estado.getValue(ROTO) && !s.getBlockTicks().hasScheduledTick(pos, this)) {
			s.playSound(null, pos, Sonidos.SUELO_CRUJE, SoundSource.BLOCKS, 0.8F, 0.9F + s.getRandom().nextFloat() * 0.2F);
			s.scheduleTick(pos, this, CEDE);
		}
		super.stepOn(nivel, pos, estado, quien);
	}

	@Override
	protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource azar) {
		if (estado.getValue(ROTO)) {
			// se rehace si no queda nadie dentro del hueco
			if (nivel.getEntitiesOfClass(Player.class, new AABB(pos).inflate(0.1, 1.0, 0.1)).isEmpty()) {
				nivel.setBlock(pos, estado.setValue(ROTO, false), Block.UPDATE_CLIENTS);
			} else {
				nivel.scheduleTick(pos, this, 20);
			}
			return;
		}
		// sigue alguien encima? cede el parche entero
		if (nivel.getEntitiesOfClass(Player.class, new AABB(pos.above()).inflate(0.3, 0.5, 0.3)).isEmpty()) {
			return;
		}
		this.romper(nivel, pos, 0);
		nivel.playSound(null, pos, Sonidos.SUELO_CAE, SoundSource.BLOCKS, 1.2F, 1.0F);
	}

	private void romper(ServerLevel nivel, BlockPos pos, int profundidad) {
		BlockState e = nivel.getBlockState(pos);
		if (!e.is(this) || e.getValue(ROTO) || profundidad > 4) {
			return;
		}
		nivel.setBlock(pos, e.setValue(ROTO, true), Block.UPDATE_CLIENTS);
		nivel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.4, 0.1, 0.4, 0.05);
		nivel.scheduleTick(pos, this, REHACE + nivel.getRandom().nextInt(20));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			this.romper(nivel, pos.relative(d), profundidad + 1);
		}
	}

	/** De vez en cuando, sin nadie: un zumbido muy bajo y una chispa (la pista). */
	@Override
	public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource azar) {
		if (estado.getValue(ROTO) || azar.nextInt(14) != 0) {
			return;
		}
		nivel.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + azar.nextDouble(), pos.getY() + 1.02, pos.getZ() + azar.nextDouble(), 0, 0.02, 0);
		if (azar.nextInt(3) == 0) {
			nivel.playLocalSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, Sonidos.SUELO_ZUMBIDO, SoundSource.BLOCKS, 0.35F, 0.9F + azar.nextFloat() * 0.2F, false);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTO, VARIANTE);
	}
}
