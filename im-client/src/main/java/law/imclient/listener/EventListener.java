package law.imclient.listener;

import law.imcommon.model.IMUserEvent;

import java.util.List;

public interface EventListener {

    void process(List<IMUserEvent> event);
}
