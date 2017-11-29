/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.ipc.netty.http.server;

import java.util.Objects;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.util.AttributeKey;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.Connection;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.channel.BootstrapHandlers;
import reactor.ipc.netty.http.HttpResources;
import reactor.ipc.netty.resources.LoopResources;
import reactor.ipc.netty.tcp.TcpServer;

/**
 * @author Stephane Maldini
 */
final class HttpServerBind extends HttpServer {

	static final HttpServerBind INSTANCE = new HttpServerBind();

	final TcpServer tcpServer;

	HttpServerBind() {
		this(DEFAULT_TCP_SERVER);
	}

	HttpServerBind(TcpServer tcpServer) {
		this.tcpServer = Objects.requireNonNull(tcpServer, "tcpServer");
	}

	@Override
	protected TcpServer tcpConfiguration() {
		return tcpServer;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<? extends Connection> bind(TcpServer delegate) {
		ServerBootstrap b;
		try {
			b = delegate.configure();
		}
		catch (Throwable t) {
			Exceptions.throwIfFatal(t);
			return Mono.error(t);
		}

		if (b.config()
		     .group() == null) {
			LoopResources loops = HttpResources.get();

			boolean useNative =
					LoopResources.DEFAULT_NATIVE && !(tcpConfiguration().sslContext() instanceof JdkSslContext);

			EventLoopGroup selector = loops.onServerSelect(useNative);
			EventLoopGroup elg = loops.onServer(useNative);

			b.group(selector, elg)
			 .channel(loops.onServerChannel(elg));
		}

		Integer minCompressionSize = (Integer) b.config()
		                                        .attrs()
		                                        .get(PRODUCE_GZIP);

		b.attr(PRODUCE_GZIP, null);

		BootstrapHandlers.updateConfiguration(b,
				NettyPipeline.HttpInitializer,
				(ctx, channel) -> {
					ChannelPipeline p = channel.pipeline();

					p.addLast(NettyPipeline.HttpCodec, new HttpServerCodec());

					if (minCompressionSize != null && minCompressionSize >= 0) {
						p.addLast(NettyPipeline.CompressionHandler,
								new CompressionHandler(minCompressionSize));
					}

					p.addLast(NettyPipeline.HttpServerHandler,
							new HttpServerHandler(ctx));
				});

		return delegate.bind(b);
	}

	static final AttributeKey<Integer> PRODUCE_GZIP =
			AttributeKey.newInstance("produceGzip");
}
