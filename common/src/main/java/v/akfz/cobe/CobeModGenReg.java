package v.akfz.cobe;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import v.akfz.aslib.annotation.DontCompile;
import v.akfz.aslib.annotation.RegisterModule;
import v.akfz.aslib.initializer.generator.GenerateRegistries;
import v.akfz.cobe.test.Test;

@DontCompile
@GenerateRegistries(modId="cobe")
public class CobeModGenReg {
    @RegisterModule(id = "cobe:test")
    public static final EntityType<Test> TEST_ENTITY = EntityType.Builder.<Test>of(Test::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("test");
}
