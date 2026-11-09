package law.implatform.controller;

import jakarta.validation.Valid;
import law.imcommon.util.R;
import law.implatform.model.dto.PrivateMessageDTO;
import law.imcommon.model.vo.PrivateMessageVO;
import law.implatform.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私聊消息
 */

@RestController
@RequestMapping("/message/private")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    // 发送私聊消息
    @PostMapping("/send")
    public R<PrivateMessageVO> sendMessage(@Valid @RequestBody PrivateMessageDTO dto) {
        return R.success(privateMessageService.sendMessage(dto));
    }

    // 拉取离线消息
    @GetMapping(value = "/loadOfflineMessage")
    public R<List<PrivateMessageVO>> loadOfflineMessage(@RequestParam Long minId) {
        return R.success(privateMessageService.loadOfflineMessage(minId));
    }
}
