package v.akfz.cobe.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.animation.calc.AnimationController;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;

public class TestEntity extends Entity implements AnimatedObject {
    private final AnimationController controller = new AnimationController(this);
    private final AnimatedObjectCache cache = new AnimatedObjectCache();

    public TestEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);

        fastInit();
        controller.play("PRIKOL",true);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    public String getStrId() {
        return "hihihi" + this.getId();
    }

    @Override
    public AnimationController getController() {
        return controller;
    }

    @Override
    public AnimatedObjectCache getCache() {
        return cache;
    }
}
