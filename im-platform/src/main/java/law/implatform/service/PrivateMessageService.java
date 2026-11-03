package law.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import law.implatform.entity.PrivateMessage;
import law.implatform.model.dto.PrivateMessageDTO;
import law.implatform.model.vo.PrivateMessageVO;

public interface PrivateMessageService extends IService<PrivateMessage> {

    /**
     * 发送私聊消息(高并发接口，查询mysql接口都要进行缓存)
     *
     * @param dto 私聊消息
     * @return 消息id
     */
    PrivateMessageVO sendMessage(PrivateMessageDTO dto);

    /**
     * 保存私聊消息
     */
    void saveMessage(PrivateMessage message);
}
