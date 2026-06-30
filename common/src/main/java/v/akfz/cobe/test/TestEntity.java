package v.akfz.cobe.test;

import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.animation.AnimationController;
import v.akfz.cobe.aengine.animation.AsyncAnimationEngine;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

public class TestEntity extends Entity implements AnimatedObject {

    private final AnimationController controller = new AnimationController();
    private final AnimatedObjectCache cache = new AnimatedObjectCache();

    public static final EntityType<TestEntity> TEST_ENTITY = EntityType.Builder
            .<TestEntity>of(TestEntity::new, MobCategory.AMBIENT)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("test_entity");

    public TestEntity(EntityType<? extends TestEntity> entityType, Level level) {
        super(entityType, level);

        if (level.isClientSide()) {
            AsyncAnimationEngine.getInstance().register(this.getStrId(), this);
            this.controller.play("testanimation", true);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Обязательно отписываем сущность из пула потоков при деспавне
        if (this.level().isClientSide()) {
            AsyncAnimationEngine.getInstance().unregister(this.getStrId());
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    public String getStrId() {
        return "TestEntity" + getId();
    }

    @Override
    public AnimationController getController() {
        return this.controller;
    }

    @Override
    public AnimatedObjectCache getCache() {
        return this.cache;
    }
}