package v.akfz.cobe.test;

import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.cobe.event.RegisterEntityRendererEvent;
import v.akfz.db.annotation.DevOnly;

@DevOnly
public class RegisterEntityRendererListenerTEST implements Listener {
    public RegisterEntityRendererListenerTEST() {
    }

    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
        event.register(CobeRegistries.TEST_ENTITY, TestEntityRenderer::new);
    }
}
