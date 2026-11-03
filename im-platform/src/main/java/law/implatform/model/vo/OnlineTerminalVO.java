package law.implatform.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 用户在线的终端类型
 */
@Data
@AllArgsConstructor
public class OnlineTerminalVO {

    /**
     * @Data
     * @AllArgsConstructor
     */
    private Long userId;

    /**
     * 在线终端类型
     */
    private List<Integer> terminals;
}
