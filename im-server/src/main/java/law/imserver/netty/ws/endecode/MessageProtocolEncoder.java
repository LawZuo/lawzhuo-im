package law.imserver.netty.ws.endecode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import law.imcommon.model.IMSendInfo;

import java.util.List;

public class MessageProtocolEncoder extends MessageToMessageEncoder<IMSendInfo> {
    @Override
    protected void encode(
            ChannelHandlerContext ctx,
            IMSendInfo msg,
            List<Object> out
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String text = objectMapper.writeValueAsString(msg);
        // 必须放 TextWebSocketFrame；放 String 时下游帧编码器无法处理，写入会静默失败
        out.add(new TextWebSocketFrame(text));
    }
}
