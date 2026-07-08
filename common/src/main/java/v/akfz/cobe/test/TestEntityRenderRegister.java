package v.akfz.cobe.test;

import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.cobe.event.RegisterEntityRendererEvent;
import v.akfz.cobe.CobeRegistries;

public class TestEntityRenderRegister implements Listener {
    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
        event.register(CobeRegistries.TEST_ENTITY_TYPE, TestEntityRenderer::new);
    }
}
