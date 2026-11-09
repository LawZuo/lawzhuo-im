package law.imclient.listener;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import law.imclient.annotation.IMListener;
import law.imcommon.enums.IMListenerType;
import law.imcommon.model.IMBatchSendResult;
import law.imcommon.model.IMSendResult;
import law.imcommon.model.IMUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 离线消息
 */
@Component
public class MessageListenerMulticaster {

    @Autowired(required = false)
    private List<MessageListener> messageListeners = Collections.emptyList();

    public void multicast(IMListenerType listenerType, List<IMBatchSendResult> batchResults) {
        if (CollUtil.isEmpty(batchResults)) {
            return;
        }
        List<IMSendResult> results = new ArrayList<>();
        for (IMBatchSendResult batchResult : batchResults) {
            List<IMUserInfo> receivers = batchResult.getReceivers();
            for (IMUserInfo receiver : receivers) {
                IMSendResult result = new IMSendResult();
                result.setSender(batchResult.getSender());
                result.setCode(batchResult.getCode());
                result.setReceiver(receiver);
                result.setData(batchResult.getData());
                results.add(result);
            }
        }

        for (MessageListener listener : messageListeners) {
            IMListener annotation = listener.getClass().getAnnotation(IMListener.class);
            if (annotation != null && (annotation.type().equals(IMListenerType.ALL) || annotation.type()
                    .equals(listenerType))) {
                results.forEach(result -> {
                    // 将data转回对象类型
                    if (result.getData() instanceof JSONObject) {
                        Type superClass = listener.getClass().getGenericInterfaces()[0];
                        Type type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
                        JSONObject data = (JSONObject)result.getData();
                        result.setData(data.toJavaObject(type));
                    }
                });
                // 回调到调用方处理
                listener.process(results);
            }
        }
    }
}
