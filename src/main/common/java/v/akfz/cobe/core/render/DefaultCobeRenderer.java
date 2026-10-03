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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        AnimatedObjectCache cache = animated.getCache();
        boolean isSkinned = mesh.isSkinned() && cache != null;

        if (!isSkinned) {
            float[] baked = mesh.getBakedQuadData();
            if (baked == null || baked.length == 0) return;

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();

            float p00 = pose.m00(), p10 = pose.m10(), p20 = pose.m20(), p30 = pose.m30();
            float p01 = pose.m01(), p11 = pose.m11(), p21 = pose.m21(), p31 = pose.m31();
            float p02 = pose.m02(), p12 = pose.m12(), p22 = pose.m22(), p32 = pose.m32();

            float n00 = normal.m00(), n10 = normal.m10(), n20 = normal.m20();
            float n01 = normal.m01(), n11 = normal.m11(), n21 = normal.m21();
            float n02 = normal.m02(), n12 = normal.m12(), n22 = normal.m22();

            boolean forceFullUV = shouldForceFullUV(boneName);
            int stepIdx = 0;
            int totalFloats = baked.length;

            for (int i = 0; i < totalFloats; i += 8) {
                float vx = baked[i];
                float vy = baked[i + 1];
                float vz = baked[i + 2];
                float u  = baked[i + 3];
                float v  = baked[i + 4];
                float nx = baked[i + 5];
                float ny = baked[i + 6];
                float nz = baked[i + 7];

                if (forceFullUV) {
                    float[] stretch = getFullStretchUV(stepIdx & 3);
                    u = stretch[0];
                    v = stretch[1];
                    stepIdx++;
                }

                float wx = p00 * vx + p10 * vy + p20 * vz + p30;
                float wy = p01 * vx + p11 * vy + p21 * vz + p31;
                float wz = p02 * vx + p12 * vy + p22 * vz + p32;

                float tnx = n00 * nx + n10 * ny + n20 * nz;
                float tny = n01 * nx + n11 * ny + n21 * nz;
                float tnz = n02 * nx + n12 * ny + n22 * nz;

                buffer.vertex(wx, wy, wz, red, green, blue, alpha, u, v, packedOverlay, packedLight, tnx, tny, tnz);
            }
            return;
        }

        renderSkinnedMesh(mesh, buffer, packedLight, packedOverlay, red, green, blue, alpha, entityWorldMatrix, cache, boneName);
    }

    default void renderSkinnedMesh(MeshRData mesh, VertexConsumer buffer,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha,
                                   Matrix4f entityWorldMatrix, AnimatedObjectCache cache, String boneName) {
        List<float[]> restVertices = mesh.vertices();
        int vertexCount = restVertices.size();
        if (vertexCount == 0) return;

        RenderContext ctx = CONTEXT.get();
        float[] skinned = ctx.ensureCapacity(vertexCount * 3);

        List<MeshRData.SkinningData> skins = mesh.skinningData();
        int skinsSize = skins != null ? skins.size() : 0;

        for (int i = 0; i < vertexCount; i++) {
            float[] v = restVertices.get(i);
            int idx = i * 3;
            if (i >= skinsSize) {
                skinned[idx]     = v[0];
                skinned[idx + 1] = v[1];
                skinned[idx + 2] = v[2];
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

                float tx = skinMat.m00() * px + skinMat.m10() * py + skinMat.m20() * pz + skinMat.m30();
                float ty = skinMat.m01() * px + skinMat.m11() * py + skinMat.m21() * pz + skinMat.m31();
                float tz = skinMat.m02() * px + skinMat.m12() * py + skinMat.m22() * pz + skinMat.m32();

                sx += tx * w;
                sy += ty * w;
                sz += tz * w;
                hasValidWeight = true;
            }

            if (hasValidWeight) {
                skinned[idx]     = sx;
                skinned[idx + 1] = sy;
                skinned[idx + 2] = sz;
            } else {
                skinned[idx]     = px;
                skinned[idx + 1] = py;
                skinned[idx + 2] = pz;
            }
        }

        Matrix3f normalMatrix = new Matrix3f(entityWorldMatrix);

        float p00 = entityWorldMatrix.m00(), p10 = entityWorldMatrix.m10(), p20 = entityWorldMatrix.m20(), p30 = entityWorldMatrix.m30();
        float p01 = entityWorldMatrix.m01(), p11 = entityWorldMatrix.m11(), p21 = entityWorldMatrix.m21(), p31 = entityWorldMatrix.m31();
        float p02 = entityWorldMatrix.m02(), p12 = entityWorldMatrix.m12(), p22 = entityWorldMatrix.m22(), p32 = entityWorldMatrix.m32();

        float n00 = normalMatrix.m00(), n10 = normalMatrix.m10(), n20 = normalMatrix.m20();
        float n01 = normalMatrix.m01(), n11 = normalMatrix.m11(), n21 = normalMatrix.m21();
        float n02 = normalMatrix.m02(), n12 = normalMatrix.m12(), n22 = normalMatrix.m22();

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

            float v0x = skinned[i0], v0y = skinned[i0 + 1], v0z = skinned[i0 + 2];
            float v1x = skinned[i1], v1y = skinned[i1 + 1], v1z = skinned[i1 + 2];
            float v2x = skinned[i2], v2y = skinned[i2 + 1], v2z = skinned[i2 + 2];

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
                emitVertex(buffer, skinned, vIndices[0], forceFullUV ? getFullStretchUV(0) : uvs.get(uvIndices[0]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, skinned, vIndices[3], forceFullUV ? getFullStretchUV(1) : uvs.get(uvIndices[3]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, skinned, vIndices[2], forceFullUV ? getFullStretchUV(2) : uvs.get(uvIndices[2]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                emitVertex(buffer, skinned, vIndices[1], forceFullUV ? getFullStretchUV(3) : uvs.get(uvIndices[1]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
            } else {
                for (int i = 1; i < count - 1; i++) {
                    emitVertex(buffer, skinned, vIndices[0],     uvs.get(uvIndices[0]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, skinned, vIndices[i + 1], uvs.get(uvIndices[i + 1]), p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, skinned, vIndices[i],     uvs.get(uvIndices[i]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
                    emitVertex(buffer, skinned, vIndices[i],     uvs.get(uvIndices[i]),     p00, p10, p20, p30, p01, p11, p21, p31, p02, p12, p22, p32, red, green, blue, alpha, packedOverlay, packedLight, tnx, tny, tnz);
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