package v.akfz.cobe.core.data.bone;

import v.akfz.cobe.core.data.hitbox.AabbData;
import java.util.List;

public record BoneHitbox(
		String boneName,
		List<float[]> vertices, 
		List<int[]> faces,
		AabbData aabb,
		int triangleCount,
		List<BoneHitbox> children
) {}