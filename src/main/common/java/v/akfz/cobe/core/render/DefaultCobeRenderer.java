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
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.cache.ModelCache;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.bone.BoneRData;
import v.akfz.cobe.core.data.loader.json.model.BoneTexture;
import v.akfz.cobe.core.data.loader.json.model.ModelData;
import v.akfz.cobe.core.object.AnimatedObject;
import v.akfz.cobe.core.render.pipeline.SkinningPipeline;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface DefaultCobeRenderer<T extends AnimatedObject> {
    ResourceLocation NULL_TEXTURE = new ResourceLocation("cobe", "textures/notexture.png");
    Map<String, ResourceLocation> DYNAMIC_CACHE = new ConcurrentHashMap<>();
    Map<String, ResourceLocation> BONE_TEXTURE_OVERRIDE_CACHE = new ConcurrentHashMap<>();

    class RenderContext {
        float[] vertexBuffer = new float[16384 * 3];

        float[] ensureCapacity(int neededFloats) {
            if (vertexBuffer.length < neededFloats) {
                vertexBuffer = new float[Math.max(vertexBuffer.length * 2, neededFloats)];
            }
            return vertexBuffer;
        }
    }

    ThreadLocal<RenderContext> CONTEXT = ThreadLocal.withInitial(RenderContext::new);

    String getNameOfModel();

    default boolean shouldForceFullUV(String boneName) { return false; }

    default float[] getFullStretchUV(int stepIdx) {
        return switch (stepIdx) {
            case 0 -> new float[]{0.0F, 0.0F};
            case 1 -> new float[]{1.0F, 0.0F};
            case 2 -> new float[]{1.0F, 1.0F};
            case 3 -> new float[]{0.0F, 1.0F};
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
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));

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
        if (boneMatrix == null) {
            poseStack.popPose();
            return;
        }

        Matrix4f renderMatrix = new Matrix4f(entityWorldMatrix).mul(boneMatrix);
        poseStack.last().pose().set(renderMatrix);
        poseStack.last().normal().set(new Matrix3f(renderMatrix));

        VertexConsumer activeBuffer = defaultBuffer;
        ResourceLocation boneTextureOverride = getBoneTextureOverride(bone.name());

        if (boneTextureOverride != null) {
            activeBuffer = bufferSource.getBuffer(getRenderTypeForBone(bone.name()));
        } else if (activeBuffer == null) {
            activeBuffer = bufferSource.getBuffer(defaultRenderType != null ? defaultRenderType : RenderType.entityCutoutNoCull(NULL_TEXTURE));
        }

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
        List<float[]> restVertices = mesh.vertices();
        int vertexCount = restVertices.size();
        if (vertexCount == 0) return;

        AnimatedObjectCache cache = animated.getCache();
        boolean isSkinned = mesh.isSkinned() && cache != null;

        RenderContext ctx = CONTEXT.get();
        float[] vertices = ctx.ensureCapacity(vertexCount * 3);

        if (SkinningPipeline.isGpuSkinning()) {
            SkinningPipeline.prepareMeshSkinning(cache, mesh);
        }

        if (isSkinned && !SkinningPipeline.isGpuSkinning()) {
            List<MeshRData.SkinningData> skins = mesh.skinningData();
            for (int i = 0; i < vertexCount; i++) {
                float[] v = restVertices.get(i);
                int idx = i * 3;
                if (skins == null || i >= skins.size()) {
                    vertices[idx] = v[0]; vertices[idx + 1] = v[1]; vertices[idx + 2] = v[2];
                    continue;
                }

                MeshRData.SkinningData skin = skins.get(i);
                float px = v[0], py = v[1], pz = v[2];
                float sx = 0, sy = 0, sz = 0;
                boolean hasValidWeight = false;

                float[] weights = skin.weights();
                String[] joints = skin.joints();
                int limit = Math.min(4, weights.length);

                for (int j = 0; j < limit; j++) {
                    float w = weights[j];
                    if (w <= 0.0f) continue;
                    String joint = joints[j];
                    if (joint == null || joint.isEmpty()) continue;

                    Matrix4f skinMat = cache.getBoneSkinMatrix(joint);
                    if (skinMat == null) continue;

                    sx += (skinMat.m00() * px + skinMat.m10() * py + skinMat.m20() * pz + skinMat.m30()) * w;
                    sy += (skinMat.m01() * px + skinMat.m11() * py + skinMat.m21() * pz + skinMat.m31()) * w;
                    sz += (skinMat.m02() * px + skinMat.m12() * py + skinMat.m22() * pz + skinMat.m32()) * w;
                    hasValidWeight = true;
                }

                if (hasValidWeight) {
                    vertices[idx]     = sx;
                    vertices[idx + 1] = sy;
                    vertices[idx + 2] = sz;
                } else {
                    vertices[idx]     = px;
                    vertices[idx + 1] = py;
                    vertices[idx + 2] = pz;
                }
            }
        } else {
            for (int i = 0; i < vertexCount; i++) {
                float[] v = restVertices.get(i);
                int idx = i * 3;
                vertices[idx]     = v[0];
                vertices[idx + 1] = v[1];
                vertices[idx + 2] = v[2];
            }
        }

        Matrix4f activePoseMatrix = isSkinned ? entityWorldMatrix : poseStack.last().pose();
        Matrix3f activeNormalMatrix = isSkinned ? new Matrix3f(entityWorldMatrix) : poseStack.last().normal();

        float p00 = activePoseMatrix.m00(), p10 = activePoseMatrix.m10(), p20 = activePoseMatrix.m20(), p30 = activePoseMatrix.m30();
        float p01 = activePoseMatrix.m01(), p11 = activePoseMatrix.m11(), p21 = activePoseMatrix.m21(), p31 = activePoseMatrix.m31();
        float p02 = activePoseMatrix.m02(), p12 = activePoseMatrix.m12(), p22 = activePoseMatrix.m22(), p32 = activePoseMatrix.m32();

        float n00 = activeNormalMatrix.m00(), n10 = activeNormalMatrix.m10(), n20 = activeNormalMatrix.m20();
        float n01 = activeNormalMatrix.m01(), n11 = activeNormalMatrix.m11(), n21 = activeNormalMatrix.m21();
        float n02 = activeNormalMatrix.m02(), n12 = activeNormalMatrix.m12(), n22 = activeNormalMatrix.m22();

        boolean forceFullUV = shouldForceFullUV(boneName);
        List<float[]> uvs = mesh.uvs();

        for (MeshRData.FaceData face : mesh.faces()) {
            int[] vIndices = face.vertexIndices();
            int[] uvIndices = face.uvIndices();
            int count = vIndices.length;
            if (count < 3) continue;

            int i0 = vIndices[0] * 3;
            int i1 = vIndices[1] * 3;
            int i2 = vIndices[2] * 3;

            float v0x = vertices[i0], v0y = vertices[i0 + 1], v0z = vertices[i0 + 2];
            float v1x = vertices[i1], v1y = vertices[i1 + 1], v1z = vertices[i1 + 2];
            float v2x = vertices[i2], v2y = vertices[i2 + 1], v2z = vertices[i2 + 2];

            float e1x = v1x - v0x, e1y = v1y - v0y, e1z = v1z - v0z;
            float e2x = v2x - v0x, e2y = v2y - v0y, e2z = v2z - v0z;

            float nx = e2y * e1z - e2z * e1y;
            float ny = e2z * e1x - e2x * e1z;
            float nz = e2x * e1y - e2y * e1x;

            float lenSq = nx * nx + ny * ny + nz * nz;
            if (lenSq > 1e-6f) {
                float invLen = (float) (1.0 / Math.sqrt(lenSq));
                nx *= invLen; ny *= invLen; nz *= invLen;
            } else {
                nx = 0f; ny = 1f; nz = 0f;
            }

            float tnx = n00 * nx + n10 * ny + n20 * nz;
            float tny = n01 * nx + n11 * ny + n21 * nz;
            float tnz = n02 * nx + n12 * ny + n22 * nz;

            float tlenSq = tnx * tnx + tny * tny + tnz * tnz;
            if (tlenSq > 1e-6f) {
                float invLen = (float) (1.0 / Math.sqrt(tlenSq));
                tnx *= invLen; tny *= invLen; tnz *= invLen;
            } else {
                tnx = 0f; tny = 1f; tnz = 0f;
            }

            if (count == 4) {
                emitVertex(buffer, vertices, vIndices[0], forceFullUV ? getFullStretchUV(0) : uvs.get(uvIndices[0]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, vertices, vIndices[3], forceFullUV ? getFullStretchUV(1) : uvs.get(uvIndices[3]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, vertices, vIndices[2], forceFullUV ? getFullStretchUV(2) : uvs.get(uvIndices[2]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, vertices, vIndices[1], forceFullUV ? getFullStretchUV(3) : uvs.get(uvIndices[1]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
            } else {
                for (int i = 1; i < count - 1; i++) {
                    emitVertex(buffer, vertices, vIndices[0],     uvs.get(uvIndices[0]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, vertices, vIndices[i + 1], uvs.get(uvIndices[i + 1]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, vertices, vIndices[i],     uvs.get(uvIndices[i]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, vertices, vIndices[i],     uvs.get(uvIndices[i]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                }
            }
        }
    }

    private void emitVertex(VertexConsumer buffer, float[] vertices, int vertexIdx, float[] uv,
                            float p00, float p10, float p20, float p30,
                            float p01, float p11, float p21, float p31,
                            float p02, float p12, float p22, float p32,
                            float red, float green, float blue, float alpha,
                            int packedOverlay, int packedLight,
                            float nx, float ny, float nz) {
        int idx = vertexIdx * 3;
        float vx = vertices[idx];
        float vy = vertices[idx + 1];
        float vz = vertices[idx + 2];

        float wx = p00 * vx + p10 * vy + p20 * vz + p30;
        float wy = p01 * vx + p11 * vy + p21 * vz + p31;
        float wz = p02 * vx + p12 * vy + p22 * vz + p32;

        buffer.vertex(wx, wy, wz, red, green, blue, alpha, uv[0], uv[1], packedOverlay, packedLight, nx, ny, nz);
    }
}