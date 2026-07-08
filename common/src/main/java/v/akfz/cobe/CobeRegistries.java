package v.akfz.cobe;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import v.akfz.aslib.annotation.RegisterModule;
import v.akfz.aslib.initializer.generator.GenerateRegistries;
import v.akfz.cobe.test.TestEntity;

@GenerateRegistries(modId = "cobe")
public class CobeRegistries {
    @RegisterModule(id = "cobe:test_entity")
    public static final EntityType<TestEntity> TEST_ENTITY_TYPE =
            EntityType.Builder.of(TestEntity::new, MobCategory.CREATURE)
                    .sized(1F, 1F)
                    .clientTrackingRange(64)
                    .updateInterval(3)
                    .build("test_entity");
}
