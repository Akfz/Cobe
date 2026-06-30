package n.paradox.cobe.event.listener;

import n.paradox.aslib.event.api.Listener;
import n.paradox.aslib.event.api.Subscribe;
import n.paradox.cobe.event.RegisterEntityTypeEvent;
import v.akfz.cobe.test.TestEntity;
import net.minecraft.resources.ResourceLocation;

public class RegisterEntityTypeListener implements Listener {
    public RegisterEntityTypeListener() {
    }

    @Subscribe
    public void execute(RegisterEntityTypeEvent event) {
        event.register(new ResourceLocation("cobe", "test_entity"), TestEntity.TEST_ENTITY);
    }
}
