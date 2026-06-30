package v.akfz.cobe.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import n.paradox.aslib.util.json.GsonHelper;
import net.minecraft.client.gui.Font;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;
import v.akfz.cobe.aengine.data.cache.AnimationCache;
import v.akfz.cobe.json.animation.Animation;
import v.akfz.cobe.json.animation.AnimationsData;
import v.akfz.cobe.json.model.ModelData;
import v.akfz.cobe.render.CobeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class TestEntityRenderer extends EntityRenderer<TestEntity> implements CobeRenderer<TestEntity> {

    private final ModelData cachedModel;

    private TestEntity currentEntity;

    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.cachedModel = GsonHelper.read(Minecraft.getInstance().gameDirectory.toPath().resolve("test").resolve("test.json"), ModelData.class);
        System.out.println(cachedModel);
        AnimationsData data = GsonHelper.read(Minecraft.getInstance().gameDirectory.toPath().resolve("test").resolve("testanims.json"), AnimationsData.class);
        System.out.println(data);
        AnimationCache.CACHED_ANIMATIONS.put("testanimation", data.animations.get(0));
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.entityCutout(getTexture());
    }

    @Override
    public void render(TestEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.currentEntity = entity;

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        poseStack.pushPose();

        float yaw = entity.getViewYRot(partialTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        // 1. Рендерим саму 3D модель
        this.defaultRender(poseStack, entity, bufferSource, null, null, entityYaw, partialTicks, packedLight);

        // 2. Рендерим отладочную геометрию (скелет) поверх модели
        renderMatrixDebug(entity, poseStack, bufferSource);

        poseStack.popPose();

        this.currentEntity = null;
    }

    /**
     * Запускает построение рекурсивного скелета модели.
     */
    private void renderMatrixDebug(TestEntity entity, PoseStack poseStack, MultiBufferSource bufferSource) {
        AnimatedObjectCache cache = entity.getCache();
        if (cache == null) return;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        if (this.cachedModel != null && this.cachedModel.bones != null) {
            Vector3f modelRootOrigin = new Vector3f(0f, 0f, 0f);
            renderHierarchyDebug(poseStack, buffer, this.cachedModel.bones, cache, modelRootOrigin,bufferSource);
        }
    }

    /**
     * Рекурсивный обход костей для отрисовки локальных (зеленых) и мировых (красных) связей скелета.
     */
    /**
     * Рекурсивный обход костей для отрисовки локальных (зеленых) и мировых (красных) связей скелета.
     */
    private void renderHierarchyDebug(PoseStack poseStack, VertexConsumer buffer, List<BoneRData> bones, AnimatedObjectCache cache, Vector3f parentWorldPos, MultiBufferSource bufferSource) {
        if (bones == null || bones.isEmpty()) return;

        Font font = Minecraft.getInstance().font;

        for (BoneRData bone : bones) {
            // Перед отрисовкой линий ВСЕГДА берем свежий буфер,
            // так как предыдущая итерация или вызов текста могли его закрыть.
            VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

            // 1. Получаем мировые и локальные матрицы кости
            Matrix4f boneLocal = cache.getBoneLocalMatrix(bone.name());
            Matrix4f boneWorld = cache.getBoneWorldMatrix(bone.name());

            Vector3f boneLocalPos = new Vector3f();
            boneLocal.getTranslation(boneLocalPos);

            Vector3f boneWorldPos = new Vector3f();
            boneWorld.getTranslation(boneWorldPos);

            // Вычисляем локальную точку смещения в системе координат родителя
            Vector3f boneLocalOffset = new Vector3f(parentWorldPos).add(boneLocalPos);

            // Отрисовываем локальный вектор кости (Зеленая линия от родителя до локальной точки смещения)
            drawLine(poseStack, lineBuffer, parentWorldPos.x, parentWorldPos.y, parentWorldPos.z, boneLocalOffset.x, boneLocalOffset.y, boneLocalOffset.z, 0, 255, 0);
            drawCross(poseStack, lineBuffer, boneLocalOffset.x, boneLocalOffset.y, boneLocalOffset.z, 0.06f, 0, 255, 0);

            // Отрисовываем мировой вектор кости (Красная линия от родителя до реальной точки в мире)
            drawLine(poseStack, lineBuffer, parentWorldPos.x, parentWorldPos.y, parentWorldPos.z, boneWorldPos.x, boneWorldPos.y, boneWorldPos.z, 255, 0, 0);
            drawCross(poseStack, lineBuffer, boneWorldPos.x, boneWorldPos.y, boneWorldPos.z, 0.10f, 255, 0, 0);

            // 3. Меши этой кости
            if (bone.meshes() != null) {
                for (MeshRData mesh : bone.meshes()) {
                    Matrix4f meshLocal = cache.getMeshLocalMatrix(mesh);
                    Matrix4f meshWorld = cache.getMeshWorldMatrix(mesh);

                    Vector3f meshLocPos = new Vector3f();
                    meshLocal.getTranslation(meshLocPos);

                    Vector3f meshWrldPos = new Vector3f();
                    meshWorld.getTranslation(meshWrldPos);

                    Vector3f meshLocalOffset = new Vector3f(boneWorldPos).add(meshLocPos);

                    // Отрисовываем локальную и мировую позицию меша относительно родительской кости
                    drawLine(poseStack, lineBuffer, boneWorldPos.x, boneWorldPos.y, boneWorldPos.z, meshLocalOffset.x, meshLocalOffset.y, meshLocalOffset.z, 0, 255, 100);
                    drawCross(poseStack, lineBuffer, meshLocalOffset.x, meshLocalOffset.y, meshLocalOffset.z, 0.03f, 0, 255, 100);

                    drawLine(poseStack, lineBuffer, boneWorldPos.x, boneWorldPos.y, boneWorldPos.z, meshWrldPos.x, meshWrldPos.y, meshWrldPos.z, 255, 100, 0);
                    drawCross(poseStack, lineBuffer, meshWrldPos.x, meshWrldPos.y, meshWrldPos.z, 0.06f, 255, 100, 0);
                }
            }

            // [ТЕКСТ] Отрисовка имени кости в 3D пространстве над мировым крестиком
            // Перенесли в конец обработки текущей кости, чтобы не ломать стрим линий выше
            poseStack.pushPose();
            poseStack.translate(boneWorldPos.x, boneWorldPos.y + 0.12f, boneWorldPos.z);

            // Биллбординг
            Matrix4f textMatrix = poseStack.last().pose();
            textMatrix.m00(1.0f); textMatrix.m01(0.0f); textMatrix.m02(0.0f);
            textMatrix.m10(0.0f); textMatrix.m11(1.0f); textMatrix.m12(0.0f);
            textMatrix.m20(0.0f); textMatrix.m21(0.0f); textMatrix.m22(1.0f);

            float textScale = 0.015f;
            poseStack.scale(textScale, -textScale, textScale);

            String boneName = bone.name();
            float textWidth = font.width(boneName);

            font.drawInBatch(
                    boneName,
                    -textWidth / 2.0f, 0.0f,
                    0xFFFFFF,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );
            poseStack.popPose();

            // 4. Рекурсивно обрабатываем дочерние кости
            if (bone.children() != null && !bone.children().isEmpty()) {
                renderHierarchyDebug(poseStack, lineBuffer, bone.children(), cache, boneWorldPos, bufferSource);
            }
        }
    }

    private void drawLine(PoseStack poseStack, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b) {
        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len; dy /= len; dz /= len;
        }

        buffer.vertex(poseMatrix, x1, y1, z1).color(r, g, b, 255).normal(normalMatrix, dx, dy, dz).endVertex();
        buffer.vertex(poseMatrix, x2, y2, z2).color(r, g, b, 255).normal(normalMatrix, dx, dy, dz).endVertex();
    }

    private void drawCross(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z, float size, int r, int g, int b) {
        drawLine(poseStack, buffer, x - size, y, z, x + size, y, z, r, g, b);
        drawLine(poseStack, buffer, x, y - size, z, x, y + size, z, r, g, b);
        drawLine(poseStack, buffer, x, y, z - size, x, y, z + size, r, g, b);
    }

    @Override
    public ModelData getModel() {
        return this.cachedModel;
    }

    @Override
    public TestEntity getAnimatedObject() {
        return this.currentEntity;
    }

    @Override
    public ResourceLocation getTextureLocation(TestEntity entity) {
        return getTexture();
    }
}