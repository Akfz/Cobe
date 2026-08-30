package v.akfz.cobe.test;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import v.akfz.cobe.core.render.DefaultCobeRenderer;
import v.akfz.cobe.texture.video.VideoPlayerManager;
import v.akfz.cobe.texture.video.VideoTexture;

public class TestEntityRenderer extends EntityRenderer<TestEntity> implements DefaultCobeRenderer<TestEntity> {
    private final int type = 0; //0 - null, 1 - video 2 - buffer

    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        defaultRender(poseStack, entity, buffer, null, null, partialTick, packedLight);
    }
    @Override
	public ResourceLocation getTextureLocation(TestEntity entity) {
		return null;
	}
/*
    @Override
    public boolean shouldForceFullUV(String boneName) {
        return type == 0;
    }

    @Override
    public ResourceLocation getBoneTextureOverride(String boneName) {
        if (type == 1) {
            return VideoPlayerManager.getInstance().getOrCreatePlayer(new ResourceLocation("eww", "test/video.mp4"), true, () -> {});
        } else if (type == 2){
            return null;
        } else {
            return DefaultCobeRenderer.super.getBoneTextureOverride(boneName);
        }
    }
 */

    @Override
    public String getNameOfModel() {
        return "CobeModel";
    }
}