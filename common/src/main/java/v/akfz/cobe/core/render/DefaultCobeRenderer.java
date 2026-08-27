package v.akfz.cobe.core.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.cache.ModelCache;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.bone.BoneRData;
import v.akfz.cobe.core.data.loader.json.model.BoneTexture;
import v.akfz.cobe.core.data.loader.json.model.ModelData;
import v.akfz.cobe.core.object.AnimatedObject;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface DefaultCobeRenderer<T extends AnimatedObject> {
    ResourceLocation NULL_TEXTURE = new ResourceLocation("cobe", "textures/notexture.png");
    Map<String, ResourceLocation> DYNAMIC_CACHE = new ConcurrentHashMap<>();
    Map<String, ResourceLocation> BONE_TEXTURE_OVERRIDE_CACHE = new ConcurrentHashMap<>();

    String getNameOfModel();

    default boolean shouldForceFullUV(String boneName) { return false; }

    default float[] getFullStretchUV(int stepIdx) {
        return switch (stepIdx) {
            case 0 -> new float[]{0.0F, 0.0F}; case 1 -> new float[]{1.0F, 0.0F};
            case 2 -> new float[]{1.0F, 1.0F}; case 3 -> new float[]{0.0F, 1.0F};
            default -> new float[]{0.0F, 0.0F};
        };
    }

    @Nullable default ModelData getModelData(String name) { return ModelCache.getFromCache(name); }

    static ResourceLocation getOrCreateDynamicTexture(Path path) {
        if (path == null) return NULL_TEXTURE;
        String pathKey = path.toAbsolutePath().toString();
        return DYNAMIC_CACHE.computeIfAbsent(pathKey, key -> {
            try {
                if (!Files.exists(path)) return NULL_TEXTURE;
                try (InputStream is = Files.newInputStream(path)) {
                    NativeImage nativeImage = NativeImage.read(is);
                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                    String uniqueId = "external_" + UUID.nameUUIDFromBytes(pathKey.getBytes());
                    ResourceLocation dynamicRl = new ResourceLocation("cobe_dynamic", uniqueId);
                    Minecraft.getInstance().getTextureManager().register(dynamicRl, dynamicTexture);
                    return dynamicRl;
                }
            } catch (Exception e) { return NULL_TEXTURE; }
        });
    }

    default ResourceLocation resolveTexture(BoneTexture bt) {
        if (bt.locIsRl()) return bt.getRl() != null ? bt.getRl() : NULL_TEXTURE;
        return getOrCreateDynamicTexture(bt.getPath());
    }

    default @Nullable ResourceLocation getBoneTextureOverride(String boneName) {
        String cacheKey = getNameOfModel() + ":" + boneName;
        return BONE_TEXTURE_OVERRIDE_CACHE.computeIfAbsent(cacheKey, key -> {
            ModelData model = getModelData(getNameOfModel());
            if (model != null && model.texturePaths != null) {
                for (BoneTexture bt : model.texturePaths) {
                    if (boneName.equals(bt.getBone())) return resolveTexture(bt);
                }
            }
            return null;
        });
    }

    default RenderType getRenderTypeForBone(String bone) {
        ResourceLocation texture = getBoneTextureOverride(bone);
        return RenderType.entityCutoutNoCull(texture != null ? texture : NULL_TEXTURE);
    }

    default void defaultRender(PoseStack poseStack, T animated, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer,
                               float partialTick, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-1.0f, 1.0f, 1.0f);
        //poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
        //poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));

        Matrix4f entityWorldMatrix = new Matrix4f(poseStack.last().pose());
        ModelData modelData = getModelData(getNameOfModel());

        if (modelData != null && modelData.bones != null) {
            if (animated.getCache() != null && animated.getCache().getRootBones() == null) {
                animated.getCache().setRootBones(modelData.bones);
            }
            for (BoneRData rootBone : modelData.bones) {
                renderBoneRecursively(poseStack, entityWorldMatrix, animated, rootBone, bufferSource, renderType, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            }
        }
        poseStack.popPose();
    }

    default void renderBoneRecursively(PoseStack poseStack, Matrix4f entityWorldMatrix, T animated, BoneRData bone, MultiBufferSource bufferSource, RenderType defaultRenderType, @Nullable VertexConsumer defaultBuffer,
                                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        Matrix4f boneMatrix = animated.getCache() != null ? animated.getCache().getBoneWorldMatrix(bone.name()) : null;

        if (boneMatrix == null) { poseStack.popPose(); return; }

        Matrix4f renderMatrix = new Matrix4f(entityWorldMatrix).mul(boneMatrix);
        poseStack.last().pose().set(renderMatrix);
        poseStack.last().normal().set(new Matrix3f(renderMatrix));

        VertexConsumer activeBuffer = defaultBuffer;
        ResourceLocation boneTextureOverride = getBoneTextureOverride(bone.name());

        if (boneTextureOverride != null) activeBuffer = bufferSource.getBuffer(getRenderTypeForBone(bone.name()));
        else if (activeBuffer == null) activeBuffer = bufferSource.getBuffer(defaultRenderType != null ? defaultRenderType : RenderType.entityCutoutNoCull(NULL_TEXTURE));

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                renderMesh(poseStack, mesh, activeBuffer, packedLight, packedOverlay, red, green, blue, alpha, animated, entityWorldMatrix, bone.name());
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                renderBoneRecursively(poseStack, entityWorldMatrix, animated, child, bufferSource, defaultRenderType, activeBuffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
        poseStack.popPose();
    }

    default void renderMesh(PoseStack poseStack, MeshRData mesh, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha, T animated, Matrix4f entityWorldMatrix, String boneName) {
        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        AnimatedObjectCache cache = animated.getCache();
        boolean isSkinned = mesh.isSkinned() && cache != null;

        Matrix4f activePoseMatrix = isSkinned ? entityWorldMatrix : poseMatrix;
        Matrix3f activeNormalMatrix = isSkinned ? new Matrix3f(entityWorldMatrix) : normalMatrix;

        Vector3f e1 = new Vector3f();
        Vector3f e2 = new Vector3f();
        Vector3f localNormal = new Vector3f();
        Vector3f transformedNormal = new Vector3f();
        Vector4f worldPos = new Vector4f();

        for (MeshRData.FaceData face : mesh.faces()) {
            int[] vertexIndices = face.vertexIndices();
            int[] uvIndices = face.uvIndices();
            int vertexCount = vertexIndices.length;
            if (vertexCount < 3) continue;

            Vector4f skinnedV0 = getSkinnedVertex(mesh.vertices().get(vertexIndices[0]), vertexIndices[0], mesh, cache);
            Vector4f skinnedV1 = getSkinnedVertex(mesh.vertices().get(vertexIndices[1]), vertexIndices[1], mesh, cache);
            Vector4f skinnedV2 = getSkinnedVertex(mesh.vertices().get(vertexIndices[2]), vertexIndices[2], mesh, cache);

            e1.set(skinnedV1.x() - skinnedV0.x(), skinnedV1.y() - skinnedV0.y(), skinnedV1.z() - skinnedV0.z());
            e2.set(skinnedV2.x() - skinnedV0.x(), skinnedV2.y() - skinnedV0.y(), skinnedV2.z() - skinnedV0.z());

            e2.cross(e1, localNormal);
            if (localNormal.lengthSquared() > 0) localNormal.normalize();
            else localNormal.set(0, 1, 0);

            transformedNormal.set(localNormal);
            activeNormalMatrix.transform(transformedNormal);
            if (transformedNormal.lengthSquared() > 0) transformedNormal.normalize();
            else transformedNormal.set(0, 1, 0);

            if (vertexCount == 4) {
                int[] quadIndices = {0, 3, 2, 1};
                int stepIdx = 0;
                for (int step : quadIndices) {
                    float[] uv = shouldForceFullUV(boneName) ? getFullStretchUV(stepIdx++) : mesh.uvs().get(uvIndices[step]);
                    Vector4f skinnedV = getSkinnedVertex(mesh.vertices().get(vertexIndices[step]), vertexIndices[step], mesh, cache);
                    worldPos.set(skinnedV.x(), skinnedV.y(), skinnedV.z(), 1.0f);
                    activePoseMatrix.transform(worldPos);

                    buffer.vertex(worldPos.x(), worldPos.y(), worldPos.z(), red, green, blue, alpha, uv[0], uv[1], packedOverlay, packedLight, transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
                }
            } else {
                for (int i = 1; i < vertexCount - 1; i++) {
                    int[] stepIndices = {0, i + 1, i, i};
                    for (int step : stepIndices) {
                        float[] uv = mesh.uvs().get(uvIndices[step]);
                        Vector4f skinnedV = getSkinnedVertex(mesh.vertices().get(vertexIndices[step]), vertexIndices[step], mesh, cache);
                        worldPos.set(skinnedV.x(), skinnedV.y(), skinnedV.z(), 1.0f);
                        activePoseMatrix.transform(worldPos);

                        buffer.vertex(worldPos.x(), worldPos.y(), worldPos.z(), red, green, blue, alpha, uv[0], uv[1], packedOverlay, packedLight, transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
                    }
                }
            }
        }
    }

    default Vector4f getSkinnedVertex(float[] restPos, int vertexIndex, MeshRData mesh, @Nullable AnimatedObjectCache cache) {
        if (!mesh.isSkinned() || cache == null) {
            return new Vector4f(restPos[0], restPos[1], restPos[2], 1.0f);
        }

        MeshRData.SkinningData skin = mesh.skinningData().get(vertexIndex);
        float sx = 0, sy = 0, sz = 0;
        boolean hasValidWeight = false;
        Vector4f temp = new Vector4f();

        for (int i = 0; i < 4; i++) {
            float weight = skin.weights()[i];
            if (weight <= 0.0f) continue;

            String jointName = skin.joints()[i];
            if (jointName == null || jointName.isEmpty()) continue;

            Matrix4f skinMatrix = cache.getBoneSkinMatrix(jointName);
            if (skinMatrix == null) continue;

            temp.set(restPos[0], restPos[1], restPos[2], 1.0f);
            skinMatrix.transform(temp);

            sx += temp.x() * weight;
            sy += temp.y() * weight;
            sz += temp.z() * weight;
            hasValidWeight = true;
        }

        if (!hasValidWeight) return new Vector4f(restPos[0], restPos[1], restPos[2], 1.0f);
        return new Vector4f(sx, sy, sz, 1.0f);
    }
}