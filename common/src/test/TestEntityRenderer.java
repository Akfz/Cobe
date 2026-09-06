package v.akfz.cobe.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import v.akfz.cobe.core.hitbox.ActiveHitbox;
import v.akfz.cobe.core.hitbox.ActiveHitboxRenderer;
import v.akfz.cobe.core.render.DefaultCobeRenderer;
import v.akfz.cobe.texture.video.VideoPlayerManager;
import v.akfz.cobe.texture.video.VideoTexture;

import java.util.Map;

public class TestEntityRenderer extends EntityRenderer<TestEntity> implements DefaultCobeRenderer<TestEntity> {
    private final int type = 0; //0 - null, 1 - video 2 - buffer

    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        defaultRender(poseStack, entity, buffer, null, null, partialTick, packedLight);

        Map<String, Matrix4f> currentMatrices = entity.getCache().getAllBoneWorldMatrices();
        for (ActiveHitbox root : entity.getHitboxes()) {
            Matrix4f rootMatrix = currentMatrices.getOrDefault(root.boneName, new Matrix4f());
            root.update(rootMatrix, currentMatrices);
        }

        for (ActiveHitbox root : entity.getHitboxes()) {
            new ActiveHitboxRenderer(root).renderDebug(poseStack, buffer.getBuffer(RenderType.LINES), 0x880088FF, 0xCCFF3333);
        }
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