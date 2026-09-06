package v.akfz.cobe.core.hitbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ActiveHitboxRenderer {
	private final ActiveHitbox toRender;

	public ActiveHitboxRenderer(ActiveHitbox toRender) {
		this.toRender = toRender;
	}

	public void renderDebug(PoseStack poseStack, VertexConsumer buffer, int colorAABB, int colorTri) {
		float aAABB = (colorAABB >> 24 & 255) / 255.0F;
		float rAABB = (colorAABB >> 16 & 255) / 255.0F;
		float gAABB = (colorAABB >> 8 & 255) / 255.0F;
		float bAABB = (colorAABB & 255) / 255.0F;

		LevelRenderer.renderLineBox(poseStack, buffer,
				toRender.transformedAABB.minX, toRender.transformedAABB.minY, toRender.transformedAABB.minZ,
				toRender.transformedAABB.maxX, toRender.transformedAABB.maxY, toRender.transformedAABB.maxZ,
				rAABB, gAABB, bAABB, aAABB);

		if (toRender.transformedTriangles != null) {
			for (ActiveHitbox.Triangle tri : toRender.transformedTriangles) {
				drawLine(poseStack, buffer, tri.v0(), tri.v1(), colorTri);
				drawLine(poseStack, buffer, tri.v1(), tri.v2(), colorTri);
				drawLine(poseStack, buffer, tri.v2(), tri.v0(), colorTri);
			}
		}

		if (toRender.children != null) {
			for (ActiveHitbox child : toRender.children) {
				new ActiveHitboxRenderer(child).renderDebug(poseStack, buffer, colorAABB, colorTri);
			}
		}
	}

	private void drawLine(PoseStack poseStack, VertexConsumer buffer, Vector3f start, Vector3f end, int color) {
		PoseStack.Pose lastPose = poseStack.last();
		Matrix4f posMatrix = lastPose.pose();
		Matrix3f normMatrix = lastPose.normal();

		float r = (color >> 16 & 255) / 255.0F;
		float g = (color >> 8 & 255) / 255.0F;
		float b = (color & 255) / 255.0F;
		float a = (color >> 24 & 255) / 255.0F;

		buffer.vertex(posMatrix, start.x, start.y, start.z)
				.color(r, g, b, a)
				.normal(normMatrix, 0, 1, 0)
				.endVertex();
		buffer.vertex(posMatrix, end.x, end.y, end.z)
				.color(r, g, b, a)
				.normal(normMatrix, 0, 1, 0)
				.endVertex();
	}
}