package law.imserver.netty.ws.endecode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import law.imcommon.model.IMSendInfo;

import java.util.List;

/**
 * 消息协议解码器
 */
public class MessageProtocolDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {
    @Override
    protected void decode(
            ChannelHandlerContext ctx,
            TextWebSocketFrame msg,
            List<Object> out
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        IMSendInfo imSendInfo = objectMapper.readValue(msg.text(), IMSendInfo.class);
        out.add(imSendInfo);
    }
}
