package v.akfz.cobe.test;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import v.akfz.cobe.aengine.data.cache.ModelCache;
import v.akfz.cobe.loader.util.FileLoader;
import v.akfz.cobe.render.CobeRenderer;

public class TestEntityRenderer extends EntityRenderer<TestEntity> implements CobeRenderer<TestEntity> {
    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        FileLoader.loadModelFile(new ResourceLocation("cobe","model/testmod.json"));
        FileLoader.loadAnimationFile(new ResourceLocation("cobe","animation/testanim.json"));
    }

    @Override
    public void render(TestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        defaultRender(poseStack,entity,buffer, null,null,partialTick,packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TestEntity entity) {
        return null;
    }

    @Override
    public String getNameOfModel() {
        return "CobeModel";
    }
}
