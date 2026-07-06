package n.paradox.cobe.mixin.client;

import n.paradox.aslib.AsLib;
import n.paradox.cobe.event.RegisterEntityRendererEvent;
import n.paradox.cobe.util.EntityRendererData;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderers.class)
public class EntityRenderersClientMixin {

    @Shadow
    private static <T extends Entity> void register(EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider) {
        throw new IllegalStateException();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void cobe$registerEntityRenderers(CallbackInfo ci) {
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