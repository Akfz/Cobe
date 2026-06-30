package n.paradox.cobe.mixin;

import n.paradox.aslib.AsLib;
import n.paradox.cobe.event.RegisterEntityTypeEvent;
import n.paradox.cobe.event.listener.RegisterEntityTypeListener;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {

    @Inject(method = "bootStrap", at = @At("HEAD"))
    private static void cobe$registerEntityTypes(CallbackInfo ci) {
        AsLib.EVENT_BUS.register(new RegisterEntityTypeListener());

        RegisterEntityTypeEvent event = new RegisterEntityTypeEvent();
        AsLib.EVENT_BUS.post(event);

        for (RegisterEntityTypeEvent.EntityTypeData data : event.getRegistrars()) {
            Registry.register(BuiltInRegistries.ENTITY_TYPE, data.idEntity(), data.entityType());
        }
    }
}