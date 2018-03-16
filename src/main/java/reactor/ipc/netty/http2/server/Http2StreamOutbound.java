package reactor.ipc.netty.http2.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyOutbound;

import java.nio.charset.Charset;

public interface Http2StreamOutbound extends NettyOutbound {

	int streamId();
}
