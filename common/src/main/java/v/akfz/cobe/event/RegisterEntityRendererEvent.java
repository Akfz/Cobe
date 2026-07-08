package v.akfz.cobe.event;

import v.akfz.aslib.event.api.Event;
import v.akfz.cobe.util.EntityRendererData;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class RegisterEntityRendererEvent extends Event {
    private final List<EntityRendererData> registrars = new ArrayList<>();

    public <T extends Entity> void register(EntityType<T> entityType, EntityRendererProvider<T> entityRendererProvider) {
        this.registrars.add(new EntityRendererData(entityType, entityRendererProvider));
    }

    public List<EntityRendererData> getRegistrars() {
        return this.registrars;
    }
}
