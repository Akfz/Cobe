package v.akfz.cobe.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import v.akfz.cobe.core.animation.AnimationController;
import v.akfz.cobe.core.animation.AsyncAnimationEngine;
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.object.AnimatedObject;

public class TestEntity extends Entity implements AnimatedObject {
    private final AnimationController controller = new AnimationController(this);
    private final AnimatedObjectCache cache = new AnimatedObjectCache();

    public TestEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        fastInit();
        this.getController().play("baked_animation", true);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        AsyncAnimationEngine.getInstance().unregister(this.getStrId());
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}

    @Override
    public String getStrId() {
        return "test" + this.getId();
    }

    @Override
    public AnimationController getController() {
        return controller;
    }

    @Override
    public AnimatedObjectCache getCache() {
        return cache;
    }

    @Override
    public Vec3 getPos() {
        return super.getPosition(0);
    }
}