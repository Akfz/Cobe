package v.akfz.cobe.core.cache;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.data.hitbox.HitboxData;

import java.util.HashMap;
import java.util.Map;

public class HitboxCache {
	private static volatile Map<String,HitboxData> CACHED_HITBOX = Map.of();

	public static synchronized void addCacheHitbox(HitboxData hb,String name) {
		if (hb == null || name == null) {
			return;
		}

		Map<String, HitboxData> newMap = new HashMap<>(CACHED_HITBOX);
		newMap.put(name, hb);
		CACHED_HITBOX = Map.copyOf(newMap);
	}

	@Nullable
	public static HitboxData getFromCache(String name) {
		if (name == null) {
			return null;
		}
		return CACHED_HITBOX.get(name);
	}

	public static Map<String, HitboxData> getFromCache() {
		return new HashMap<>(CACHED_HITBOX);
	}
}
