package n.paradox.cobe.event.listener;

import n.paradox.aslib.event.api.Listener;
import n.paradox.aslib.event.api.Subscribe;
import n.paradox.cobe.event.RegisterEntityRendererEvent;

// пример
public class RegisterEntityRendererListener implements Listener {
    public RegisterEntityRendererListener() {
    }

    @Subscribe
    public void execute(RegisterEntityRendererEvent event) {
    }
}

