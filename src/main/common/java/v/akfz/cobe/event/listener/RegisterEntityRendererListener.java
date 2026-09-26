package v.akfz.cobe.event.listener;

import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.cobe.event.RegisterEntityRendererEvent;

/**
 * Example (not registered)
 */
public class RegisterEntityRendererListener implements Listener {
    public RegisterEntityRendererListener() {
    }

    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
    }
}

