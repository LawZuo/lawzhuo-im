package law.imclient.listener;

import law.imcommon.model.IMSendResult;

import java.util.List;

public interface MessageListener<T> {

    void process(List<IMSendResult<T>> result);
}
