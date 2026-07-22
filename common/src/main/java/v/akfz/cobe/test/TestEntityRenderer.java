package v.akfz.cobe.test;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.cobe.loader.json.model.ModelData;
import v.akfz.cobe.render.CobeRenderer;

public class TestEntityRenderer extends EntityRenderer<TestEntity> implements CobeRenderer<TestEntity> {
    private static ModelData cachedModel;

    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        defaultRender(poseStack, entity, buffer, null, null, partialTick, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TestEntity entity) {
        return new ResourceLocation("cobe", "textures/entity/test.png");
    }

    @Override
    public ModelData getModelData(String name) {
        if (cachedModel == null) {
            cachedModel = GsonHelper.read(new ResourceLocation("cobe", "test/t.json"), ModelData.class);
        }
        return cachedModel;
    }

    @Override
    public String getNameOfModel() {
        return "";
    }
}