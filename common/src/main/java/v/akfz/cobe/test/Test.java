package v.akfz.cobe.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import v.akfz.aslib.annotation.DontCompile;
import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.animation.calc.AnimationController;
import v.akfz.cobe.aengine.animation.calc.AsyncAnimationEngine;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;

@DontCompile
public class Test extends Entity implements AnimatedObject {

    private final AnimationController controller = new AnimationController(this);
    private final AnimatedObjectCache cache = new AnimatedObjectCache();

    public Test(EntityType<?> entityType, Level level) {
        super(entityType, level);

        if (level.isClientSide()) {
            AsyncAnimationEngine.getInstance().register(this.getStrId(), this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level().isClientSide()) {
            AsyncAnimationEngine.getInstance().unregister(this.getStrId());
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}

    @Override
    public String getStrId() {
        return "test_" + getStringUUID();
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