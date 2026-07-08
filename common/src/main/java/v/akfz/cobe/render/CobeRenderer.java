package v.akfz.cobe.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;
import v.akfz.cobe.aengine.data.cache.ModelCache;
import v.akfz.cobe.loader.json.model.ModelData;
import v.akfz.cobe.loader.json.model.BoneTexture;
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

//к любому рендеру просто implements CobeRenderer прописывать и все заработает
public interface CobeRenderer<T extends AnimatedObject> {

    ResourceLocation NULL_TEXTURE = new ResourceLocation("cobe", "textures/notexture.png");

    Map<String, ResourceLocation> DYNAMIC_CACHE = new ConcurrentHashMap<>();

    String getNameOfModel();

    @Nullable
    default ModelData getModelData(String name) {
        return ModelCache.getFromCache(name);
    }

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
                    return dynamicRl;
                }
            } catch (Exception e) {
                System.err.println("[Cobe] Failed to load external texture: " + pathKey);
                e.printStackTrace();
                return NULL_TEXTURE;
            }
        });
    }

    default ResourceLocation resolveTexture(BoneTexture bt) {
        if (bt.locIsRl()) {
            ResourceLocation rl = bt.getRl();
            if (rl != null) {
                boolean resourceExists = false;
                try {
                    resourceExists = Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
                } catch (Exception ignored) {}

                if (resourceExists) {
                    return rl;
                }

                Path path = Minecraft.getInstance().gameDirectory.toPath().resolve(rl.getPath());

                if (!Files.exists(path)) {
                    Path fallback = Path.of(rl.getPath());
                    if (Files.exists(fallback)) {
                        path = fallback;
                    }
                }
                return getOrCreateDynamicTexture(path);
            }
        } else {
            Path path = bt.getPath();

            if (path != null && !Files.exists(path)) {
                try {
                    String relativeLoc = path.toString().substring(Minecraft.getInstance().gameDirectory.toPath().toString().length() + 1);
                    Path fallback = Path.of(relativeLoc);
                    if (Files.exists(fallback)) {
                        path = fallback;
                    }
                } catch (Exception ignored) {}
            }
            return getOrCreateDynamicTexture(path);
        }
        return NULL_TEXTURE;
    }

    default @Nullable ResourceLocation getBoneTextureOverride(String boneName) {
        ModelData model = getModelData(getNameOfModel());
        if (model != null && model.texturePaths != null) {
            for (BoneTexture bt : model.texturePaths) {
                if (bt.getBone() != null && bt.getBone().equals(boneName)) {
                    return resolveTexture(bt);
                }
            }
        }
        return null;
    }

    default RenderType getRenderTypeForBone(String bone) {
        return RenderType.entityCutoutNoCull(getBoneTextureOverride(bone));
    }

    default void defaultRender(PoseStack poseStack, T animated, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer,
                               float partialTick, int packedLight) {
        poseStack.pushPose();

        poseStack.scale(-1.0f, 1.0f, 1.0f);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));

        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        float alpha = 1.0f;
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        Matrix4f entityWorldMatrix = new Matrix4f(poseStack.last().pose());

        ModelData modelData = getModelData(getNameOfModel());
        if (modelData != null && modelData.bones != null) {
            if (animated.getCache() != null && animated.getCache().getRootBones() == null) {
                animated.getCache().setRootBones(modelData.bones);
            }

            for (BoneRData rootBone : modelData.bones) {
                renderBoneRecursively(poseStack, entityWorldMatrix, animated, rootBone, bufferSource, renderType, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        poseStack.popPose();
    }

    private void renderBoneRecursively(PoseStack poseStack, Matrix4f entityWorldMatrix, T animated, BoneRData bone, MultiBufferSource bufferSource, RenderType defaultRenderType, @Nullable VertexConsumer defaultBuffer,
                                       int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        Matrix4f boneMatrix = null;

        if (animated.getCache() != null) {
            boneMatrix = animated.getCache().getBoneWorldMatrix(bone.name());
        }

        if (boneMatrix == null) {
            System.out.println("bone matrix is null");
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
            if (defaultRenderType == null) defaultRenderType = RenderType.entityCutoutNoCull(NULL_TEXTURE);
            activeBuffer = bufferSource.getBuffer(defaultRenderType);
        }

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                renderMesh(poseStack, mesh, activeBuffer, packedLight, packedOverlay, red, green, blue, alpha, animated, entityWorldMatrix);
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                renderBoneRecursively(poseStack, entityWorldMatrix, animated, child, bufferSource, defaultRenderType, activeBuffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        poseStack.popPose();
    }

    private void renderMesh(PoseStack poseStack, MeshRData mesh, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha, T animated, Matrix4f entityWorldMatrix) {
        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        AnimatedObjectCache cache = animated.getCache();
        boolean isSkinned = mesh.isSkinned() && cache != null;

        Matrix4f activePoseMatrix = isSkinned ? entityWorldMatrix : poseMatrix;
        Matrix3f activeNormalMatrix = isSkinned ? new Matrix3f(entityWorldMatrix) : normalMatrix;

        for (MeshRData.FaceData face : mesh.faces()) {
            int[] vertexIndices = face.vertexIndices();
            int[] uvIndices = face.uvIndices();
            int vertexCount = vertexIndices.length;
            if (vertexCount < 3) continue;

            float[] v0 = mesh.vertices().get(vertexIndices[0]);
            float[] v1 = mesh.vertices().get(vertexIndices[1]);
            float[] v2 = mesh.vertices().get(vertexIndices[2]);

            Vector4f skinnedV0 = getSkinnedVertex(v0, vertexIndices[0], mesh, cache);
            Vector4f skinnedV1 = getSkinnedVertex(v1, vertexIndices[1], mesh, cache);
            Vector4f skinnedV2 = getSkinnedVertex(v2, vertexIndices[2], mesh, cache);

            float e1x = (skinnedV1.x() - skinnedV0.x());
            float e1y = (skinnedV1.y() - skinnedV0.y());
            float e1z = (skinnedV1.z() - skinnedV0.z());

            float e2x = (skinnedV2.x() - skinnedV0.x());
            float e2y = (skinnedV2.y() - skinnedV0.y());
            float e2z = (skinnedV2.z() - skinnedV0.z());

            Vector3f localNormal = new Vector3f(
                    e2y * e1z - e2z * e1y,
                    e2z * e1x - e2x * e1z,
                    e2x * e1y - e2y * e1x
            );
            if (localNormal.lengthSquared() > 0) {
                localNormal.normalize();
            } else {
                localNormal.set(0, 1, 0);
            }

            Vector3f transformedNormal = new Vector3f(localNormal);
            activeNormalMatrix.transform(transformedNormal);
            if (transformedNormal.lengthSquared() > 0) {
                transformedNormal.normalize();
            } else {
                transformedNormal.set(0, 1, 0);
            }

            if (vertexCount == 4) {
                int[] quadIndices = {0, 3, 2, 1};
                for (int step : quadIndices) {
                    int vertexIndex = vertexIndices[step];
                    int uvIndex = uvIndices[step];
                    float[] vertexPos = mesh.vertices().get(vertexIndex);
                    float[] uv = mesh.uvs().get(uvIndex);

                    Vector4f skinnedV = getSkinnedVertex(vertexPos, vertexIndex, mesh, cache);
                    Vector4f worldPos = new Vector4f(skinnedV.x(), skinnedV.y(), skinnedV.z(), 1.0f);
                    activePoseMatrix.transform(worldPos);

                    buffer.vertex(
                            worldPos.x(), worldPos.y(), worldPos.z(),
                            red, green, blue, alpha,
                            uv[0], uv[1],
                            packedOverlay, packedLight,
                            transformedNormal.x(), transformedNormal.y(), transformedNormal.z()
                    );
                }
            } else {
                for (int i = 1; i < vertexCount - 1; i++) {
                    int[] stepIndices = {0, i + 1, i, i};
                    for (int step : stepIndices) {
                        int vertexIndex = vertexIndices[step];
                        int uvIndex = uvIndices[step];
                        float[] vertexPos = mesh.vertices().get(vertexIndex);
                        float[] uv = mesh.uvs().get(uvIndex);

                        Vector4f skinnedV = getSkinnedVertex(vertexPos, vertexIndex, mesh, cache);
                        Vector4f worldPos = new Vector4f(skinnedV.x(), skinnedV.y(), skinnedV.z(), 1.0f);
                        activePoseMatrix.transform(worldPos);

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

    private Vector4f getSkinnedVertex(float[] restPos, int vertexIndex, MeshRData mesh, @Nullable AnimatedObjectCache cache) {
        if (!mesh.isSkinned() || cache == null) {
            return new Vector4f(restPos[0], restPos[1], restPos[2], 1.0f);
        }

        MeshRData.SkinningData skin = mesh.skinningData().get(vertexIndex);
        Vector3f skinned = new Vector3f(0, 0, 0);

        for (int i = 0; i < 4; i++) {
            float weight = skin.weights()[i];
            if (weight <= 0.0f) continue;

            String jointName = skin.joints()[i];
            if (jointName == null || jointName.isEmpty()) continue;

            Matrix4f worldMatrix = cache.getBoneWorldMatrix(jointName);
            Matrix4f restMatrix = cache.getBoneRestWorldMatrix(jointName);

            Matrix4f jointSkinMatrix = new Matrix4f(worldMatrix).mul(new Matrix4f(restMatrix).invert());

            Vector4f temp = new Vector4f(restPos[0], restPos[1], restPos[2], 1.0f);
            jointSkinMatrix.transform(temp);

            skinned.add(new Vector3f(temp.x(), temp.y(), temp.z()).mul(weight));
        }

        return new Vector4f(skinned.x(), skinned.y(), skinned.z(), 1.0f);
    }
}