/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
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
package reactor.ipc.netty.http2.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.Connection;
import reactor.ipc.netty.ConnectionEvents;
import reactor.ipc.netty.FutureMono;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.channel.ChannelOperations;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @author Violeta Georgieva
 */
class Http2StreamOperations extends ChannelOperations<Http2StreamInbound, Http2StreamOutbound>
		implements Http2StreamInbound, Http2StreamOutbound {

	final static AtomicIntegerFieldUpdater<Http2StreamOperations> HTTP_STATE =
			AtomicIntegerFieldUpdater.newUpdater(Http2StreamOperations.class,
					"statusAndHeadersSent");
	volatile int statusAndHeadersSent = 0;

	static final int READY        = 0;
	static final int HEADERS_SENT = 1;
	static final int BODY_SENT    = 2;

	final Http2Headers nettyRequest;
	final Http2Headers nettyResponse;
	final Integer streamId;

	Http2StreamOperations(Connection connection, ConnectionEvents listener, Http2Headers headers, Integer streamId) {
		super(connection, listener);
		this.nettyRequest = headers;
		this.streamId = streamId;
		this.nettyResponse = new DefaultHttp2Headers().status(OK.codeAsText());
	}

	@Override
	public int streamId() {
		if (streamId != null) {
			return streamId;
		}
		return 0;
	}

	/**
	 * Outbound Netty Http2Headers
	 *
	 * @return Outbound Netty Http2Headers
	 */
	protected Http2Headers outboundHttpMessage() {
		return nettyResponse;
	}

	protected void onInboundNext(ChannelHandlerContext ctx, Object msg, boolean endOfStream) {
		if (msg instanceof Http2Headers) {
			if (!endOfStream) {
				super.onInboundNext(ctx, msg);
			}
			else {
				onInboundComplete();
			}
		}
		else {
			super.onInboundNext(ctx, msg);
		}
	}

	@Override
	public Mono<Void> then() {
		if (markSentHeaders()) {
			return FutureMono.deferFuture(() -> {
				Http2ServerHandler http2ServerHandler = connection().channel().pipeline().get(Http2ServerHandler.class);

				ChannelHandlerContext ctx = connection().channel().pipeline().lastContext();

				return http2ServerHandler.encoder()
				                         .writeHeaders(ctx, streamId, nettyResponse,
				                                      0, false, ctx.newPromise());
			});
		}
		else {
			return Mono.empty();
		}
	}

	/*@Override
	public NettyOutbound sendObject(Object message) {
		return then(FutureMono.deferFuture(() -> {
			Http2ServerHandler http2ServerHandler = connection().channel().pipeline().get(Http2ServerHandler.class);

			ChannelHandlerContext ctx = connection().channel().pipeline().lastContext();

			http2ServerHandler.encoder().writeHeaders(ctx, streamId, nettyResponse, 0, false, ctx.newPromise());

			ByteBuf msg = null;
			if (message instanceof String) {
				msg = Unpooled.copiedBuffer((String) message, Charset.defaultCharset());
			}
			return http2ServerHandler.encoder().writeData(ctx, streamId, msg, 0, true, ctx.newPromise());
		}));
	}*/

	/**
	 * Mark the headers sent
	 *
	 * @return true if marked for the first time
	 */
	protected final boolean markSentHeaders() {
		return HTTP_STATE.compareAndSet(this, READY, HEADERS_SENT);
	}

	/**
	 * Mark the body sent
	 *
	 * @return true if marked for the first time
	 */
	protected final boolean markSentBody() {
		return HTTP_STATE.compareAndSet(this, HEADERS_SENT, BODY_SENT);
	}

	/**
	 * Mark the headers and body sent
	 *
	 * @return true if marked for the first time
	 */
	protected final boolean markSentHeaderAndBody() {
		return HTTP_STATE.compareAndSet(this, READY, BODY_SENT);
	}
}
