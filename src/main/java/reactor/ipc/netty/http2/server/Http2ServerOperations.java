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
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.Connection;
import reactor.ipc.netty.ConnectionEvents;
import reactor.ipc.netty.http.HttpOperations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @author Violeta Georgieva
 */
final class Http2ServerOperations extends Http2StreamOperations implements Http2FrameListener {

	@SuppressWarnings("unchecked")
	static Http2ServerOperations bindHttp2(Connection connection, ConnectionEvents listener, Object msg, Integer streamId) {
		return new Http2ServerOperations(connection, listener, (Http2Headers) msg, streamId);
	}

	final DirectProcessor<Http2StreamOutbound> streams = DirectProcessor.create();
	final ConcurrentHashMap<Integer, Http2StreamOperations> streamsCache = new ConcurrentHashMap<>();

	Http2ServerOperations(Connection c, ConnectionEvents listener, Http2Headers headers, Integer streamId) {
		super(c, listener, headers, streamId);
	}

	Flux<Http2StreamOutbound> streams() {
		return streams;
	}

	@Override
	public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
		return 0;
	}

	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
		Http2StreamOperations http2StreamOperations = streamsCache.get(streamId);
		if (http2StreamOperations == null) {
			http2StreamOperations = new Http2StreamOperations(connection(), listener(), headers, streamId);
			streamsCache.putIfAbsent(streamId, http2StreamOperations);
		}
		streams.onNext(http2StreamOperations);
		onInboundNext(ctx, headers, endOfStream);
	}

	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
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
