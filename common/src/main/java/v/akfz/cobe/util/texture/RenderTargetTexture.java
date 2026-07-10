package v.akfz.cobe.util.texture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

//передавать готовый буффер
public class RenderTargetTexture extends AbstractTexture {
    private final RenderTarget renderTarget;

    public RenderTargetTexture(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    @Override
    public void load(ResourceManager resourceManager) {
    }

    @Override
    public void bind() {
        GlStateManager._bindTexture(this.renderTarget.getColorTextureId());
    }

    @Override
    public int getId() {
        return this.renderTarget.getColorTextureId();
    }

    @Override
    public void close() {
        super.close();
        if (this.renderTarget != null) {
            this.renderTarget.destroyBuffers();
        }
    }
}