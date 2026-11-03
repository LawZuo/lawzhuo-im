package law.implatform.controller;

import jakarta.validation.Valid;
import law.imcommon.util.R;
import law.implatform.model.dto.PrivateMessageDTO;
import law.implatform.model.vo.PrivateMessageVO;
import law.implatform.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 私聊消息
 */

@RestController
@RequestMapping("/message/private")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;


    @PostMapping("/send")
    public R<PrivateMessageVO> sendMessage(@Valid @RequestBody PrivateMessageDTO dto) {
        return R.success(privateMessageService.sendMessage(dto));
    }
}
