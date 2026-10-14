package net.backrooms.evento.piscinas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lo que necesita una valvula para que su volante gire suave: solo vive en el cliente (no
 * guarda nada). Cada vez que alguien la gira, el servidor manda un evento de bloque
 * (Valvulas#girar) y el volante sigue girando unos ticks; ValvulaRenderer lo dibuja con el
 * angulo interpolado entre ticks.
 */
public class ValvulaEntidad extends BlockEntity {
	/** Grados por tick mientras se gira (media vuelta por segundo). */
	private static final float VELOCIDAD = 9.0F;
	/** Ticks que sigue girando tras cada uso (Minecraft repite el uso cada 4). */
	private static final int INERCIA = 6;

	private float angulo;
	private float anguloAntes;
	private long girandoHasta;

	public ValvulaEntidad(BlockPos pos, BlockState estado) {
		super(BloquesPiscinas.VALVULA_ENTIDAD, pos, estado);
	}

	public static void tickCliente(Level nivel, BlockPos pos, BlockState estado, ValvulaEntidad v) {
		v.anguloAntes = v.angulo;
		if (nivel.getGameTime() < v.girandoHasta) {
			v.angulo += VELOCIDAD;
		}
	}

	/** Angulo del volante en grados, interpolado. */
	public float angulo(float parcial) {
		return this.anguloAntes + (this.angulo - this.anguloAntes) * parcial;
	}

	@Override
	public boolean triggerEvent(int id, int dato) {
		if (id == 1) {
			if (this.level != null) {
				this.girandoHasta = this.level.getGameTime() + INERCIA;
			}
			return true;
		}
		return super.triggerEvent(id, dato);
	}
}
