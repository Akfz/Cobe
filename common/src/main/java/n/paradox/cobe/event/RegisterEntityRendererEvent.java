package n.paradox.cobe.event;

import n.paradox.aslib.event.api.Event;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class RegisterEntityRendererEvent extends Event {
    public record EntityRendererData(EntityType<? extends Entity> entityType, EntityRendererProvider<? extends Entity> entityRendererProvider) {}

    private final List<EntityRendererData> registrars = new ArrayList<>();

    public<T extends Entity> void register(EntityType<T> entityType, EntityRendererProvider<T> entityRendererProvider) {
        this.registrars.add(new EntityRendererData(entityType, entityRendererProvider));
    }

    public List<EntityRendererData> getRegistrars() {
        return this.registrars;
    }
}
