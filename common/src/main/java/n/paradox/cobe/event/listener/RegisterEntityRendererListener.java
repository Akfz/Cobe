package n.paradox.cobe.event.listener;

import n.paradox.aslib.event.api.Listener;
import n.paradox.aslib.event.api.Subscribe;
import n.paradox.cobe.event.RegisterEntityRendererEvent;
import v.akfz.cobe.test.TestEntity;
import v.akfz.cobe.test.TestEntityRenderer;

public class RegisterEntityRendererListener implements Listener {
    public RegisterEntityRendererListener() {
    }

    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
        event.register(TestEntity.TEST_ENTITY, TestEntityRenderer::new);
    }
}

