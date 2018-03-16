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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import reactor.ipc.netty.ConnectionEvents;
import reactor.ipc.netty.channel.ChannelOperations;

import static io.netty.handler.logging.LogLevel.INFO;

/**
 * @author Violeta Georgieva
 */
final class Http2ServerHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2ServerHandler, Http2ServerHandlerBuilder> {

	static final Http2FrameLogger LOGGER = new Http2FrameLogger(INFO, Http2ServerHandler.class);

	final ChannelHandlerContext ctx;
	final ConnectionEvents listener;

	Http2ServerHandlerBuilder(ChannelHandlerContext ctx, ConnectionEvents listener) {
		frameLogger(LOGGER);
		this.ctx = ctx;
		this.listener = listener;
	}

	@Override
	public Http2ServerHandler build() {
		return super.build();
	}

	@Override
	protected Http2ServerHandler build(Http2ConnectionDecoder decoder,
			Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
		Http2ServerHandler handler = new Http2ServerHandler(decoder, encoder, initialSettings);
		frameListener(new InitialHttp2FrameListener(handler, listener));
		return handler;
	}


	private static final class InitialHttp2FrameListener implements Http2FrameListener {
		final Http2ServerHandler handler;
		final ConnectionEvents listener;

		InitialHttp2FrameListener(Http2ServerHandler handler, ConnectionEvents listener) {
			this.handler = handler;
			this.listener = listener;
		}

		@Override
		public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
			return 0;
		}

		@Override
		public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
			listener.onSetup(ctx.channel(), headers, streamId);
			Http2ServerOperations http2ServerOperations = (Http2ServerOperations) ChannelOperations.get(ctx.channel());
			http2ServerOperations.streamsCache.putIfAbsent(streamId, http2ServerOperations);
			handler.decoder().frameListener(http2ServerOperations);
			http2ServerOperations.onHeadersRead(ctx, streamId, headers, padding, endOfStream);
		}

		@Override
		public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
				int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
			onHeadersRead(ctx, streamId, headers, padding, endOfStream);
		}

		@Override
		public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
		}

		@Override
		public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
		}

		@Override
		public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
		}

		@Override
		public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
		}

		@Override
		public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
		}

		@Override
		public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
		}

		@Override
		public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
		}

		@Override
		public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
		}

		@Override
		public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
		}

		@Override
		public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception {
		}
	}
}
