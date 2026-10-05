package net.backrooms.menu.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Los dos logos (Backrooms y PeakMC Studio) como texturas.
 *
 * Se leen del propio jar y no del gestor de recursos, igual que hace Minecraft
 * con el logo de Mojang: asi ya estan en la primera pantalla de carga, cuando
 * los recursos todavia no existen (ver LoadingOverlayMixin).
 *
 * Minecraft no genera mipmaps de estas texturas, asi que cada logo va en
 * varios tamanos (cada uno la mitad del anterior, tools/imagenes) y se dibuja
 * el mas pequeno que no quede por debajo del tamano real en pantalla.
 */
public final class Logos {
	public static final Logos BACKROOMS = new Logos("logo", 4);
	public static final Logos PEAKMC_STUDIO = new Logos("peakmc_studio", 5);

	private final Identifier[] ids;
	private final int[] anchos;
	private final int[] altos;

	private Logos(String base, int niveles) {
		this.ids = new Identifier[niveles];
		this.anchos = new int[niveles];
		this.altos = new int[niveles];
		for (int n = 0; n < niveles; n++) {
			this.ids[n] = Identifier.fromNamespaceAndPath("backrooms", "textures/gui/" + base + "_" + n + ".png");
		}
	}

	public static void registrar(TextureManager tm) {
		BACKROOMS.registrarEn(tm);
		PEAKMC_STUDIO.registrarEn(tm);
	}

	private void registrarEn(TextureManager tm) {
		for (int n = 0; n < this.ids.length; n++) {
			tm.registerAndLoad(this.ids[n], new Textura(this, n));
		}
	}

	/** Ancho / alto del logo (1 hasta que se cargue). */
	public float proporcion() {
		return this.altos[0] > 0 ? (float) this.anchos[0] / this.altos[0] : 1.0F;
	}

	/**
	 * Dibuja el logo con alto `alto` en las coordenadas actuales de la pose;
	 * `pixeles` es cuantos pixeles de pantalla mide una unidad de esas
	 * coordenadas (para elegir el tamano de textura). `color` tine y da alfa.
	 */
	public void dibujar(GuiGraphics g, int x, int y, int alto, float pixeles, int color) {
		int ancho = Math.round(alto * this.proporcion());
		float real = alto * pixeles;
		int n = 0;
		for (int i = this.ids.length - 1; i >= 0; i--) {
			if (this.altos[i] >= real * 0.95F) {
				n = i;
				break;
			}
		}
		int tw = Math.max(1, this.anchos[n]);
		int th = Math.max(1, this.altos[n]);
		g.blit(RenderPipelines.GUI_TEXTURED, this.ids[n], x, y, 0.0F, 0.0F, ancho, alto, tw, th, tw, th, color);
	}

	/** Pixeles de pantalla por unidad de GUI, sin pose extra. */
	public static float escalaGui() {
		return (float) Minecraft.getInstance().getWindow().getGuiScale();
	}

	private static final class Textura extends ReloadableTexture {
		private final Logos logo;
		private final int nivel;

		Textura(Logos logo, int nivel) {
			super(logo.ids[nivel]);
			this.logo = logo;
			this.nivel = nivel;
		}

		@Override
		public TextureContents loadContents(ResourceManager recursos) throws IOException {
			String ruta = "/assets/backrooms/" + this.resourceId().getPath();
			try (InputStream in = Logos.class.getResourceAsStream(ruta)) {
				if (in == null) {
					throw new FileNotFoundException(ruta);
				}
				NativeImage img = NativeImage.read(in);
				this.logo.anchos[this.nivel] = img.getWidth();
				this.logo.altos[this.nivel] = img.getHeight();
				return new TextureContents(img, new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
			}
		}
	}
}
