package v.akfz.cobe.core.hitbox;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class HitboxRaycastHelper {

	public record HitResult(String boneName, Vec3 hitPos, double distanceSq) {}

	@Nullable
	public static HitResult raycast(List<ActiveHitbox> rootHitboxes, Vec3 from, Vec3 to) {
		HitResult closest = null;
		for (ActiveHitbox hitbox : rootHitboxes) {
			HitResult res = raycastHitbox(hitbox, from, to);
			if (res != null) {
				if (closest == null || res.distanceSq() < closest.distanceSq()) {
					closest = res;
				}
			}
		}
		return closest;
	}

	private static HitResult raycastHitbox(ActiveHitbox hitbox, Vec3 from, Vec3 to) {
		if (!hitbox.transformedAABB.clip(from, to).isPresent()) {
			return null;
		}

		HitResult closestResult = null;
		Vector3f rayOrigin = new Vector3f((float) from.x, (float) from.y, (float) from.z);
		Vector3f rayDir = new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z)).normalize();

		float maxDist = (float) from.distanceTo(to);

		for (ActiveHitbox.Triangle tri : hitbox.transformedTriangles) {
			float dist = intersectRayTriangle(rayOrigin, rayDir, tri.v0(), tri.v1(), tri.v2());
			if (dist > 0.0f && dist <= maxDist) {
				Vec3 hit = from.add(new Vec3(rayDir.x, rayDir.y, rayDir.z).scale(dist));
				double dSq = from.distanceToSqr(hit);
				if (closestResult == null || dSq < closestResult.distanceSq()) {
					closestResult = new HitResult(hitbox.boneName, hit, dSq);
				}
			}
		}

		for (ActiveHitbox child : hitbox.children) {
			HitResult childRes = raycastHitbox(child, from, to);
			if (childRes != null) {
				if (closestResult == null || childRes.distanceSq() < closestResult.distanceSq()) {
					closestResult = childRes;
				}
			}
		}

		return closestResult;
	}

	private static float intersectRayTriangle(Vector3f orig, Vector3f dir, Vector3f v0, Vector3f v1, Vector3f v2) {
		float edge1X = v1.x - v0.x;
		float edge1Y = v1.y - v0.y;
		float edge1Z = v1.z - v0.z;

		float edge2X = v2.x - v0.x;
		float edge2Y = v2.y - v0.y;
		float edge2Z = v2.z - v0.z;

		float pvecX = dir.y * edge2Z - dir.z * edge2Y;
		float pvecY = dir.z * edge2X - dir.x * edge2Z;
		float pvecZ = dir.x * edge2Y - dir.y * edge2X;

		float det = edge1X * pvecX + edge1Y * pvecY + edge1Z * pvecZ;
		if (Math.abs(det) < 1e-7f) return -1f;

		float invDet = 1.0f / det;

		float tvecX = orig.x - v0.x;
		float tvecY = orig.y - v0.y;
		float tvecZ = orig.z - v0.z;

		float u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet;
		if (u < 0.0f || u > 1.0f) return -1f;

		float qvecX = tvecY * edge1Z - tvecZ * edge1Y;
		float qvecY = tvecZ * edge1X - tvecX * edge1Z;
		float qvecZ = tvecX * edge1Y - tvecY * edge1X;

		float v = (dir.x * qvecX + dir.y * qvecY + dir.z * qvecZ) * invDet;
		if (v < 0.0f || u + v > 1.0f) return -1f;

		float t = (edge2X * qvecX + edge2Y * qvecY + edge2Z * qvecZ) * invDet;
		return t > 0.0001f ? t : -1f;
	}
}