package v.akfz.cobe.test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import v.akfz.aslib.AsLib;
import v.akfz.aslib.annotation.RegisterModule;
import v.akfz.aslib.initializer.generator.GenerateRegistries;
import v.akfz.aslib.registry.RegistryHelper;
import v.akfz.db.annotation.DevOnly;

@DevOnly //TODO фикс, чтобы не генерировал и фикс ProdOnly
@GenerateRegistries(modId = "cobe")
public class CobeRegistries {
    static {
        AsLib.EVENT_BUS.register(new RegisterEntityRendererListenerTEST());
    }
    @RegisterModule(id = "cobe:testentity")
    public static EntityType<TestEntity> TEST_ENTITY = RegistryHelper.createEntity(TestEntity::new,MobCategory.MISC,1f,1f,"cobe:testentity");
}
