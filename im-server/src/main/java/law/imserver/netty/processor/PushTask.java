package law.imserver.netty.processor;

import io.netty.channel.ChannelFuture;
import law.imcommon.model.IMUserInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PushTask {

    private final IMUserInfo receiver;

    private final ChannelFuture future;
}
