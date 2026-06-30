package v.akfz.cobe.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import v.akfz.cobe.json.model.ModelData;
import v.akfz.cobe.json.model.BoneTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface CobeRenderer<T extends AnimatedObject> {

    ResourceLocation NULL_TEXTURE = new ResourceLocation("cobe", "textures/notexture.png");

    Map<String, ResourceLocation> DYNAMIC_CACHE = new ConcurrentHashMap<>();

    ModelData getModel();
    T getAnimatedObject();

    /**
     * Регистрирует внешнюю текстуру напрямую по уже готовому и разрешенному объекту Path.
     */
    static ResourceLocation getOrCreateDynamicTexture(Path path) {
        if (path == null) {
            return NULL_TEXTURE;
        }

        String pathKey = path.toAbsolutePath().toString();
        return DYNAMIC_CACHE.computeIfAbsent(pathKey, key -> {
            try {
                if (!Files.exists(path)) {
                    System.err.println("[Cobe] External texture not found at path: " + path.toAbsolutePath());
                    return NULL_TEXTURE;
                }

                try (InputStream is = Files.newInputStream(path)) {
                    NativeImage nativeImage = NativeImage.read(is);
                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

                    String uniqueId = "external_" + UUID.nameUUIDFromBytes(pathKey.getBytes());
                    ResourceLocation dynamicRl = new ResourceLocation("cobe_dynamic", uniqueId);

                    Minecraft.getInstance().getTextureManager().register(dynamicRl, dynamicTexture);
                    System.out.println("[Cobe] Successfully registered external texture: " + dynamicRl + " from " + path.toAbsolutePath());
                    return dynamicRl;
                }
            } catch (Exception e) {
                System.err.println("[Cobe] Failed to load external texture: " + pathKey);
                e.printStackTrace();
                return NULL_TEXTURE;
            }
        });
    }

    /**
     * Метод-совместитель для обработки сырых строковых путей из JSON.
     */
    static ResourceLocation getOrCreateDynamicTexture(String loc) {
        if (loc == null || loc.isEmpty()) {
            return NULL_TEXTURE;
        }

        ResourceLocation rl = ResourceLocation.tryParse(loc);
        return rl != null ? rl : NULL_TEXTURE;
    }

    /**
     * Безопасно возвращает дефолтную текстуру модели.
     */
    default ResourceLocation getTexture() {
        ModelData model = getModel();
        if (model != null && model.texturePaths != null && !model.texturePaths.isEmpty()) {
            BoneTexture bt = model.texturePaths.get(0);
            if (bt.locIsRl()) {
                return bt.getRl() != null ? bt.getRl() : NULL_TEXTURE;
            } else {
                return getOrCreateDynamicTexture(bt.getPath());
            }
        }
        return NULL_TEXTURE;
    }

    /**
     * Ищет кастомную текстуру для конкретной кости по её имени.
     */
    default @Nullable ResourceLocation getBoneTextureOverride(String boneName) {
        ModelData model = getModel();
        if (model != null && model.texturePaths != null) {
            for (BoneTexture bt : model.texturePaths) {
                if (bt.getBone() != null && bt.getBone().equals(boneName)) {
                    if (bt.locIsRl()) {
                        return bt.getRl() != null ? bt.getRl() : NULL_TEXTURE;
                    } else {
                        return getOrCreateDynamicTexture(bt.getPath());
                    }
                }
            }
        }
        return null;
    }

    default RenderType getRenderType() {
        return RenderType.entityCutoutNoCull(getTexture());
    }

    default void defaultRender(PoseStack poseStack, T animated, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer,
                               float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        float alpha = 1.0f;
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        if (renderType == null) {
            renderType = getRenderType();
        }

        ModelData modelData = getModel();
        if (modelData != null && modelData.bones != null) {
            if (animated.getCache() != null && animated.getCache().getRootBones() == null) {
                animated.getCache().setRootBones(modelData.bones);
            }

            for (BoneRData rootBone : modelData.bones) {
                renderBoneRecursively(poseStack, animated, rootBone, bufferSource, renderType, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        poseStack.popPose();
    }

    private void renderBoneRecursively(PoseStack poseStack, T animated, BoneRData bone, MultiBufferSource bufferSource, RenderType defaultRenderType, @Nullable VertexConsumer defaultBuffer,
                                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (animated.getCache() != null) {
            Matrix4f boneMatrix = animated.getCache().getMatrix(bone.name());
            if (boneMatrix != null) {
                poseStack.last().pose().mul(boneMatrix);
            }
        }

        VertexConsumer activeBuffer = defaultBuffer;
        ResourceLocation boneTextureOverride = getBoneTextureOverride(bone.name());

        if (boneTextureOverride != null) {
            activeBuffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(boneTextureOverride));
        } else if (activeBuffer == null) {
            activeBuffer = bufferSource.getBuffer(defaultRenderType);
        }

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                renderMesh(poseStack, mesh, activeBuffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                renderBoneRecursively(poseStack, animated, child, bufferSource, defaultRenderType, defaultBuffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        poseStack.popPose();
    }

    private void renderMesh(PoseStack poseStack, MeshRData mesh, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        for (MeshRData.FaceData face : mesh.faces()) {
            int[] vertexIndices = face.vertexIndices();
            int[] uvIndices = face.uvIndices();
            int vertexCount = vertexIndices.length;
            if (vertexCount < 3) continue;

            // 1. Математически верный расчет нормали плоскости (Векторное произведение e1 x e2)
            float[] v0 = mesh.vertices().get(vertexIndices[0]);
            float[] v1 = mesh.vertices().get(vertexIndices[1]);
            float[] v2 = mesh.vertices().get(vertexIndices[2]);

            float e1x = (v1[0] - v0[0]) / 16.0f;
            float e1y = (v1[1] - v0[1]) / 16.0f;
            float e1z = (v1[2] - v0[2]) / 16.0f;

            float e2x = (v2[0] - v0[0]) / 16.0f;
            float e2y = (v2[1] - v0[1]) / 16.0f;
            float e2z = (v2[2] - v0[2]) / 16.0f;

            // e1 x e2
            Vector3f localNormal = new Vector3f(
                    e1y * e2z - e1z * e2y,
                    e1z * e2x - e1x * e2z,
                    e1x * e2y - e1y * e2x
            );
            if (localNormal.lengthSquared() > 0) {
                localNormal.normalize();
            } else {
                localNormal.set(0, 1, 0);
            }

            Vector3f transformedNormal = new Vector3f(localNormal);
            normalMatrix.transform(transformedNormal);
            if (transformedNormal.lengthSquared() > 0) {
                transformedNormal.normalize();
            } else {
                transformedNormal.set(0, 1, 0);
            }

            // 2. Рендеринг геометрии строго группами по 4 вершины (QUADS)
            if (vertexCount == 4) {
                // Если это четырехугольник (как ваш Plane), рисуем его напрямую
                int[] quadIndices = {0, 1, 2, 3};
                for (int step : quadIndices) {
                    int vertexIndex = vertexIndices[step];
                    int uvIndex = uvIndices[step];
                    float[] vertexPos = mesh.vertices().get(vertexIndex);
                    float[] uv = mesh.uvs().get(uvIndex);

                    Vector4f worldPos = new Vector4f(
                            vertexPos[0] / 16.0f,
                            vertexPos[1] / 16.0f,
                            vertexPos[2] / 16.0f,
                            1.0f
                    );
                    poseMatrix.transform(worldPos);

                    buffer.vertex(
                            worldPos.x(), worldPos.y(), worldPos.z(),
                            red, green, blue, alpha,
                            uv[0], uv[1],
                            packedOverlay, packedLight,
                            transformedNormal.x(), transformedNormal.y(), transformedNormal.z()
                    );
                }
            } else {
                // Для треугольников и n-гонов разбиваем их на треугольники,
                // но каждый треугольник отправляем как вырожденный квад {0, i, i + 1, i + 1}
                for (int i = 1; i < vertexCount - 1; i++) {
                    int[] stepIndices = {0, i, i + 1, i + 1};
                    for (int step : stepIndices) {
                        int vertexIndex = vertexIndices[step];
                        int uvIndex = uvIndices[step];
                        float[] vertexPos = mesh.vertices().get(vertexIndex);
                        float[] uv = mesh.uvs().get(uvIndex);

                        Vector4f worldPos = new Vector4f(
                                vertexPos[0] / 16.0f,
                                vertexPos[1] / 16.0f,
                                vertexPos[2] / 16.0f,
                                1.0f
                        );
                        poseMatrix.transform(worldPos);

                        buffer.vertex(
                                worldPos.x(), worldPos.y(), worldPos.z(),
                                red, green, blue, alpha,
                                uv[0], uv[1],
                                packedOverlay, packedLight,
                                transformedNormal.x(), transformedNormal.y(), transformedNormal.z()
                        );
                    }
                }
            }
        }
    }
}