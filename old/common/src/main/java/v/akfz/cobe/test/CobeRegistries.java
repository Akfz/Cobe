package v.akfz.cobe.test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import v.akfz.aslib.AsLib;
import v.akfz.aslib.annotation.RegisterModule;
import v.akfz.aslib.initializer.generator.GenerateRegistries;

@GenerateRegistries(modId = "cobe")
public class CobeRegistries {
    static {
        AsLib.EVENT_BUS.register(new RegisterEntityRendererListenerTEST());
    }

    @RegisterModule(id = "cobe:testentity")
    public static EntityType<TestEntity> TEST_ENTITY = EntityType.Builder.of(TestEntity::new, MobCategory.MISC)
            .sized(1F, 1F)
            .build("test_entity");
}
