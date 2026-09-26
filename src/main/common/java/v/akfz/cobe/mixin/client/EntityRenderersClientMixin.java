package v.akfz.cobe.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import v.akfz.aslib.AsLib;
import v.akfz.cobe.event.RegisterEntityRendererEvent;
import v.akfz.cobe.util.EntityRendererData;

import java.util.Map;

@Mixin(EntityRenderers.class)
public class EntityRenderersClientMixin {

    @Shadow
    public static <T extends Entity> void register(EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider) {
        throw new IllegalStateException("Mixin not applied");
    }

    @Inject(method = "createEntityRenderers", at = @At("HEAD"))
    private static void cobe$registerEntityRenderers(
            EntityRendererProvider.Context context,
            CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?>>> cir
    ) {
        RegisterEntityRendererEvent event = new RegisterEntityRendererEvent();
        AsLib.EVENT_BUS.post(event);
        event.getRegistrars().forEach(EntityRenderersClientMixin::cobe$helperRegister);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Entity> void cobe$helperRegister(EntityRendererData data) {
        EntityType<E> type = (EntityType<E>) data.entityType();
        EntityRendererProvider<E> provider = (EntityRendererProvider<E>) data.entityRendererProvider();
        register(type, provider);
    }
}