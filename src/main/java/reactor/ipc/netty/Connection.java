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
package reactor.ipc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;

/**
 * Hold contextual information for the underlying {@link Channel}
 *
 * @author Stephane Maldini
 * @since 0.6
 */
@FunctionalInterface
public interface Connection extends DisposableChannel {

	/**
	 * Return false if it will force a close on terminal protocol events thus defeating
	 * any pooling strategy
	 * Return true (default) if it will release on terminal protocol events thus
	 * keeping alive the channel if possible.
	 *
	 * @return whether or not the underlying {@link Channel} will be closed on terminal
	 * handler event
	 */
	static boolean isPersistent(Channel channel) {
		return !channel.hasAttr(ReactorNetty.PERSISTENT_CHANNEL) ||
				channel.attr(ReactorNetty.PERSISTENT_CHANNEL).get();
	}

	/**
	 * Add a {@link ChannelHandler} with {@link #addHandlerFirst} if of type of
	 * {@link io.netty.channel.ChannelOutboundHandler} otherwise with
	 * {@link #addHandlerLast}. Implementation may add more auto handling in particular
	 * HTTP based context will prepend an HttpContent body extractor.
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param handler handler instance
	 *
	 * @return this Connection

	 */
	default Connection addHandler(ChannelHandler handler){
		return addHandler(handler.getClass().getSimpleName(), handler);
	}

	/**
	 * Add a {@link ChannelHandler} with {@link #addHandlerFirst} if of type of
	 * {@link io.netty.channel.ChannelOutboundHandler} otherwise with
	 * {@link #addHandlerLast}. Implementation may add more auto handling in particular
	 * HTTP based context will prepend an HttpContent body extractor.
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param name handler name
	 * @param handler handler instance
	 *
	 * @return this Connection
	 */
	default Connection addHandler(String name, ChannelHandler handler){
		if(handler instanceof ChannelOutboundHandler){
			addHandlerFirst(name, handler);
		}
		else {
			addHandlerLast(name, handler);
		}
		return this;
	}

	/**
	 * Add a {@link ChannelHandler} to the end of the "user" {@link io.netty.channel.ChannelPipeline},
	 * that is just before the reactor-added handlers (like {@link NettyPipeline#ReactiveBridge}.
	 * If a handler with a similar name already exists, this operation is skipped.
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param handler handler instance
	 *
	 * @return this Connection

	 */
	default Connection addHandlerLast(ChannelHandler handler){
		return addHandlerLast(handler.getClass().getSimpleName(), handler);
	}

	/**
	 * Add a {@link ChannelHandler} to the end of the "user" {@link io.netty.channel.ChannelPipeline},
	 * that is just before the reactor-added handlers (like {@link NettyPipeline#ReactiveBridge}.
	 * If a handler with a similar name already exists, this operation is skipped.
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param name handler name
	 * @param handler handler instance
	 *
	 * @return this Connection
	 */
	default Connection addHandlerLast(String name, ChannelHandler handler){
		ReactorNetty.addHandlerBeforeReactorEndHandlers(this, name, handler);
		return this;
	}

	/**
	 * Add a {@link ChannelHandler} to the beginning of the "user" {@link io.netty.channel.ChannelPipeline},
	 * that is just after the reactor-added codecs. If a handler with a similar name already
	 * exists, this operation is skipped. 
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}.
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param handler handler instance
	 *
	 * @return this Connection
	 */
	default Connection addHandlerFirst(ChannelHandler handler){
		return addHandlerFirst(handler.getClass().getSimpleName(), handler);
	}

	/**
	 * Add a {@link ChannelHandler} to the beginning of the "user" {@link io.netty.channel.ChannelPipeline},
	 * that is just after the reactor-added codecs. If a handler with a similar name already
	 * exists, this operation is skipped. 
	 * <p>
	 * {@code [ [reactor codecs], [<- user FIRST HANDLERS added here, user LAST HANDLERS added here ->], [reactor handlers] ]}
	 * <p>
	 * If effectively added, the handler will be safely removed when the channel is made
	 * inactive (pool release).
	 *
	 * @param name handler name
	 * @param handler handler instance
	 *
	 * @return this Connection
	 */
	default Connection addHandlerFirst(String name, ChannelHandler handler){
		ReactorNetty.addHandlerAfterReactorCodecs(this, name, handler);
		return this;
	}

	/**
	 * Mark the underlying channel as persistent or not.
	 * If false, it will force a close on terminal protocol events thus defeating
	 * any pooling strategy
	 * if true (default), it will release on terminal protocol events thus
	 * keeping alive the channel if possible.
	 *
	 * @param persist the boolean flag to mark the {@link Channel} as fully disposable
	 * or reusable when a user handler has terminated
	 *
	 * @return this Connection
	 */
	default Connection markPersistent(boolean persist){
		if(persist && !channel().hasAttr(ReactorNetty.PERSISTENT_CHANNEL)) {
			return this;
		}
		else {
			channel().attr(ReactorNetty.PERSISTENT_CHANNEL)
			         .set(persist);
		}
		return this;
	}

	/**
	 * Remove a named handler if present and return this context
	 *
	 * @param name handler name
	 *
	 * @return this Connection
	 */
	default Connection removeHandler(String name) {
		ReactorNetty.removeHandler(channel(), name);
		return this;
	}

	/**
	 * Replace a named handler if present and return this context.
	 * If handler wasn't present, an {@link RuntimeException} will be thrown.
	 * <p>
	 *     Note: if the new handler is of different type, dependent handling like
	 *     the "extractor" introduced via HTTP-based {@link #addHandler} might not
	 *     expect/support the new messages type.
	 *
	 * @param name handler name
	 *
	 * @return this Connection
	 */
	default Connection replaceHandler(String name, ChannelHandler handler) {
		ReactorNetty.replaceHandler(channel(), name, handler);
		return this;
	}
}