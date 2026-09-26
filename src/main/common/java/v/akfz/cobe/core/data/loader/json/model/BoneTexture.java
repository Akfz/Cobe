package v.akfz.cobe.core.data.loader.json.model;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class BoneTexture {
    @SerializedName("nameBone")
    private final String bone;

    @SerializedName("location")
    private final String loc;

    public BoneTexture(String nameBone, String location) {
        this.bone = nameBone;
        this.loc = location != null ? location : "";
    }

    public String getBone() {
        return bone;
    }

    public boolean locIsRl() {
        return !loc.startsWith("/");
    }

    @Nullable
    public ResourceLocation getRl() {
        if (!locIsRl()) {
            return null;
        }
        return ResourceLocation.tryParse(loc);
    }

    @Nullable
    public Path getPath() {
        if (locIsRl()) {
            return null;
        }
        String relativeLoc = loc.startsWith("/") ? loc.substring(1) : loc;
        return Minecraft.getInstance().gameDirectory.toPath().resolve(relativeLoc);
    }
}
