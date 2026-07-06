package n.paradox.cobe.util;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public record EntityRendererData(EntityType<? extends Entity> entityType, EntityRendererProvider<? extends Entity> entityRendererProvider) {}