package n.paradox.cobe.event;

import n.paradox.aslib.event.api.Event;
import n.paradox.cobe.util.EntityRendererData;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class RegisterEntityRendererEvent extends Event {
    private final List<EntityRendererData> registrars = new ArrayList<>();

    public void register(EntityType<? extends Entity> entityType, EntityRendererProvider<? extends Entity> entityRendererProvider) {
        this.registrars.add(new EntityRendererData(entityType, entityRendererProvider));
    }

    public List<EntityRendererData> getRegistrars() {
        return this.registrars;
    }
}
