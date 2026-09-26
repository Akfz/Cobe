package v.akfz.cobe.core.hitbox;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import v.akfz.cobe.core.data.bone.BoneHitbox;
import v.akfz.cobe.core.data.hitbox.AabbData;
import v.akfz.cobe.core.data.hitbox.HitboxData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActiveHitbox {
	public final String boneName;
	public AABB transformedAABB;
	public final List<Triangle> transformedTriangles;
	public final List<ActiveHitbox> children;
	private final BoneHitbox template;

	private ActiveHitbox(BoneHitbox template, Matrix4f boneMatrix, Map<String, Matrix4f> allBoneMatrices) {
		this.boneName = template.boneName();
		this.template = template;
		this.transformedTriangles = new ArrayList<>();

		if (template.faces() != null && template.vertices() != null) {
			for (int i = 0; i < template.faces().size(); i++) {
				this.transformedTriangles.add(new Triangle(new Vector3f(), new Vector3f(), new Vector3f()));
			}
		}

		this.children = new ArrayList<>();
		if (template.children() != null) {
			for (BoneHitbox child : template.children()) {
				Matrix4f childMatrix = allBoneMatrices.getOrDefault(child.boneName(), boneMatrix);
				this.children.add(new ActiveHitbox(child, childMatrix, allBoneMatrices));
			}
		}
		update(boneMatrix, allBoneMatrices);
	}

	public void update(Matrix4f boneMatrix, Map<String, Matrix4f> allBoneMatrices) {
		this.transformedAABB = transformAABB(template.aabb(), boneMatrix);

		if (template.faces() != null && template.vertices() != null) {
			for (int i = 0; i < template.faces().size(); i++) {
				int[] face = template.faces().get(i);
				Triangle tri = this.transformedTriangles.get(i);

				transformVertex(template.vertices().get(face[0]), boneMatrix, tri.v0);
				transformVertex(template.vertices().get(face[1]), boneMatrix, tri.v1);
				transformVertex(template.vertices().get(face[2]), boneMatrix, tri.v2);
			}
		}

		for (ActiveHitbox child : this.children) {
			Matrix4f childMatrix = allBoneMatrices.getOrDefault(child.boneName, boneMatrix);
			child.update(childMatrix, allBoneMatrices);
		}
	}

	private void transformVertex(float[] localPos, Matrix4f matrix, Vector3f outVec) {
		float x = localPos[0];
		float y = localPos[1];
		float z = localPos[2];

		outVec.x = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
		outVec.y = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();
		outVec.z = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();
	}

	public static List<ActiveHitbox> buildFromData(HitboxData data, Map<String, Matrix4f> allBoneMatrices) {
		List<ActiveHitbox> rootHitboxes = new ArrayList<>();
		if (data.bones() == null) return rootHitboxes;

		float globalScale = data.scale();
		for (BoneHitbox rootBone : data.bones()) {
			Matrix4f baseMatrix = allBoneMatrices.getOrDefault(rootBone.boneName(), new Matrix4f().identity());
			Matrix4f finalMatrix = (globalScale != 1.0f) ? new Matrix4f(baseMatrix).scale(globalScale) : baseMatrix;
			rootHitboxes.add(new ActiveHitbox(rootBone, finalMatrix, allBoneMatrices));
		}
		return rootHitboxes;
	}

	private void transformVertexInPlace(Vector3f localPos, Matrix4f matrix, Vector3f outVec) {
		float x = localPos.x;
		float y = localPos.y;
		float z = localPos.z;
		outVec.x = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
		outVec.y = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();
		outVec.z = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();
	}

	private AABB transformAABB(AabbData localAabb, Matrix4f matrix) {
		float[] min = localAabb.min();
		float[] max = localAabb.max();

		float newMinX = Float.MAX_VALUE, newMinY = Float.MAX_VALUE, newMinZ = Float.MAX_VALUE;
		float newMaxX = -Float.MAX_VALUE, newMaxY = -Float.MAX_VALUE, newMaxZ = -Float.MAX_VALUE;

		for (int x = 0; x <= 1; x++) {
			for (int y = 0; y <= 1; y++) {
				for (int z = 0; z <= 1; z++) {
					float px = x == 0 ? min[0] : max[0];
					float py = y == 0 ? min[1] : max[1];
					float pz = z == 0 ? min[2] : max[2];

					float rx = matrix.m00() * px + matrix.m10() * py + matrix.m20() * pz + matrix.m30();
					float ry = matrix.m01() * px + matrix.m11() * py + matrix.m21() * pz + matrix.m31();
					float rz = matrix.m02() * px + matrix.m12() * py + matrix.m22() * pz + matrix.m32();

					if (rx < newMinX) newMinX = rx;
					if (ry < newMinY) newMinY = ry;
					if (rz < newMinZ) newMinZ = rz;
					if (rx > newMaxX) newMaxX = rx;
					if (ry > newMaxY) newMaxY = ry;
					if (rz > newMaxZ) newMaxZ = rz;
				}
			}
		}
		return new AABB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
	}

	public boolean intersects(AABB other) {
		if (this.transformedAABB.intersects(other)) {
			for (Triangle tri : this.transformedTriangles) {
				if (intersectsTriangleAABB(tri, other)) {
					return true;
				}
			}
		}
		for (ActiveHitbox child : this.children) {
			if (child.intersects(other)) {
				return true;
			}
		}
		return false;
	}

	private boolean intersectsTriangleAABB(Triangle tri, AABB aabb) {
		Vector3f v0 = tri.v0;
		Vector3f v1 = tri.v1;
		Vector3f v2 = tri.v2;

		double centerX = (aabb.minX + aabb.maxX) * 0.5;
		double centerY = (aabb.minY + aabb.maxY) * 0.5;
		double centerZ = (aabb.minZ + aabb.maxZ) * 0.5;
		double extX = (aabb.maxX - aabb.minX) * 0.5;
		double extY = (aabb.maxY - aabb.minY) * 0.5;
		double extZ = (aabb.maxZ - aabb.minZ) * 0.5;

		Vector3f aabbCenter = new Vector3f((float) centerX, (float) centerY, (float) centerZ);
		Vector3f extents = new Vector3f((float) extX, (float) extY, (float) extZ);

		Vector3f e0 = new Vector3f(v1).sub(v0);
		Vector3f e1 = new Vector3f(v2).sub(v1);
		Vector3f e2 = new Vector3f(v0).sub(v2);

		Vector3f[] axes = new Vector3f[13];
		axes[0] = new Vector3f(1, 0, 0);
		axes[1] = new Vector3f(0, 1, 0);
		axes[2] = new Vector3f(0, 0, 1);
		axes[3] = new Vector3f(e0).cross(e1).normalize();

		axes[4] = new Vector3f(1, 0, 0).cross(e0).normalize();
		axes[5] = new Vector3f(1, 0, 0).cross(e1).normalize();
		axes[6] = new Vector3f(1, 0, 0).cross(e2).normalize();
		axes[7] = new Vector3f(0, 1, 0).cross(e0).normalize();
		axes[8] = new Vector3f(0, 1, 0).cross(e1).normalize();
		axes[9] = new Vector3f(0, 1, 0).cross(e2).normalize();
		axes[10] = new Vector3f(0, 0, 1).cross(e0).normalize();
		axes[11] = new Vector3f(0, 0, 1).cross(e1).normalize();
		axes[12] = new Vector3f(0, 0, 1).cross(e2).normalize();

		for (Vector3f axis : axes) {
			if (axis.lengthSquared() < 0.001f) continue;

			float p0 = v0.dot(axis);
			float p1 = v1.dot(axis);
			float p2 = v2.dot(axis);

			float minTri = Math.min(p0, Math.min(p1, p2));
			float maxTri = Math.max(p0, Math.max(p1, p2));

			float centerProj = aabbCenter.dot(axis);
			float radius = extents.x * Math.abs(axis.x) + extents.y * Math.abs(axis.y) + extents.z * Math.abs(axis.z);

			if (minTri > centerProj + radius || maxTri < centerProj - radius) {
				return false;
			}
		}
		return true;
	}

	public record Triangle(Vector3f v0, Vector3f v1, Vector3f v2) {}
}