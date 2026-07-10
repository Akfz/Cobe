package v.akfz.cobe.event.listener;

import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.cobe.CobeModGenReg;
import v.akfz.cobe.event.RegisterEntityRendererEvent;
import v.akfz.cobe.test.TestRenderer;

// пример
public class RegisterEntityRendererListener implements Listener {
    public RegisterEntityRendererListener() {
    }

    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
        event.register(CobeModGenReg.TEST_ENTITY, TestRenderer::new);
    }
}

