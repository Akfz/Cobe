package v.akfz.cobe.test;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import v.akfz.aslib.annotation.DontCompile;
import v.akfz.cobe.render.CobeRenderer;
import v.akfz.cobe.util.texture.RenderTargetTexture;

@DontCompile
public class TestRenderer extends EntityRenderer<Test> implements CobeRenderer<Test> {

    private static RenderTarget mirrorTarget;
    private static RenderTargetTexture mirrorTexture;
    public static final ResourceLocation MIRROR_LOCATION = new ResourceLocation("cobe", "mirror_texture");

    private static boolean isRenderingMirror = false;

    public TestRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Test entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderMirrorScene(entity, entityYaw, partialTicks, packedLight);

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        poseStack.pushPose();
        float yaw = entity.getViewYRot(partialTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        this.defaultRender(poseStack, entity, bufferSource, null, null, partialTicks, packedLight);
        poseStack.popPose();
    }

    private void renderMirrorScene(Test entity, float entityYaw, float partialTicks, int packedLight) {
        if (isRenderingMirror) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        isRenderingMirror = true;

        try {
            if (mirrorTarget == null) {
                mirrorTarget = new TextureTarget(512, 512, true, Minecraft.ON_OSX);
                mirrorTexture = new RenderTargetTexture(mirrorTarget);
                mc.getTextureManager().register(MIRROR_LOCATION, mirrorTexture);
            }

            double entityX = entity.xo + (entity.getX() - entity.xo) * partialTicks;
            double entityY = entity.yo + (entity.getY() - entity.yo) * partialTicks;
            double entityZ = entity.zo + (entity.getZ() - entity.zo) * partialTicks;

            RenderTarget mainTarget = mc.getMainRenderTarget();
            Matrix4f oldProjMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

            mirrorTarget.bindWrite(true);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

            Matrix4f projectionMatrix = new Matrix4f().setPerspective((float) Math.toRadians(75.0), 1.0f, 0.05f, 100.0f);
            RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

            PoseStack viewStack = new PoseStack();
            viewStack.last().pose().identity();

            viewStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
            viewStack.translate(0, 0, -0.1f);

            RenderSystem.getModelViewStack().pushPose();
            RenderSystem.getModelViewStack().last().pose().set(viewStack.last().pose());
            RenderSystem.applyModelViewMatrix();

            // ИСПРАВЛЕНО: Создаем чистый, изолированный буфер на базе Tesselator,
            // чтобы не мешать отрисовке основного мира и не вызывать багов.
            MultiBufferSource.BufferSource renderBuffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

            for (Entity nearby : entity.level().getEntities(entity, entity.getBoundingBox().inflate(10.0))) {
                if (nearby == entity) continue;

                double nX = nearby.xo + (nearby.getX() - nearby.xo) * partialTicks;
                double nY = nearby.yo + (nearby.getY() - nearby.yo) * partialTicks;
                double nZ = nearby.zo + (nearby.getZ() - nearby.zo) * partialTicks;

                double rX = nX - entityX;
                double rY = nY - entityY;
                double rZ = nZ - entityZ;

                mc.getEntityRenderDispatcher().render(
                        nearby,
                        rX, rY, rZ,
                        nearby.getViewYRot(partialTicks),
                        partialTicks,
                        new PoseStack(),
                        renderBuffers,
                        packedLight
                );
            }

            renderBuffers.endBatch();

            RenderSystem.getModelViewStack().popPose();
            RenderSystem.applyModelViewMatrix();

            mainTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(oldProjMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

        } finally {
            isRenderingMirror = false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Test entity) {
        return null;
    }

    @Override
    public ResourceLocation getBoneTextureOverride(String boneName) {
        return MIRROR_LOCATION;
    }

    @Override
    public boolean shouldForceFullUV(String boneName) {
        return true;
    }

    @Override
    public String getNameOfModel() {
        return "CobeModel";
    }
}