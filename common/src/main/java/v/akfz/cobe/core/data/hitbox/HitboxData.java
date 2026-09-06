package v.akfz.cobe.core.data.hitbox;

import v.akfz.aslib.util.json.JsonData;
import v.akfz.cobe.core.data.bone.BoneHitbox;

import java.util.List;

public record HitboxData(
		int hbVersion,
		float scale,
		int detailLevel,
		int totalTriangles,
		List<BoneHitbox> bones
) implements JsonData {}

