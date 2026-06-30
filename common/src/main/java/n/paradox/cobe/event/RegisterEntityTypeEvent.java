package n.paradox.cobe.event;

import n.paradox.aslib.event.api.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class RegisterEntityTypeEvent extends Event {

    public record EntityTypeData(ResourceLocation idEntity, EntityType<?> entityType) {}

    private final List<EntityTypeData> registrars = new ArrayList<>();

    public void register(ResourceLocation id, EntityType<?> entityType) {
        this.registrars.add(new EntityTypeData(id, entityType));
    }

    public List<EntityTypeData> getRegistrars() {
        return this.registrars;
    }
}