package net.backrooms.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

/**
 * Boton que solo pone la zona de clic, el foco con teclado y la narracion.
 * Lo pinta el menu (MenuBackrooms), que dibuja todo en coordenadas de diseno
 * para que se vea igual que el launcher.
 */
public class BotonInvisible extends AbstractButton {
	private final Runnable accion;
	boolean encimaAntes;
	/** Cuando empezo el raton a estar encima (para el parpadeo de entrada). */
	long encimaDesde;

	public BotonInvisible(Component mensaje, Runnable accion) {
		super(0, 0, 0, 0, mensaje);
		this.accion = accion;
	}

	public void colocar(int x, int y, int ancho, int alto) {
		this.setX(x);
		this.setY(y);
		this.setWidth(ancho);
		this.setHeight(alto);
	}

	@Override
	public void onPress(InputWithModifiers entrada) {
		this.accion.run();
	}

	@Override
	protected void renderContents(GuiGraphics g, int x, int y, float parcial) {
		// lo dibuja el menu
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput salida) {
		this.defaultButtonNarrationText(salida);
	}

	@Override
	public void playDownSound(SoundManager sm) {
		Sonidos.ui(Tema.actual().clic, 0.7F);
	}
}
