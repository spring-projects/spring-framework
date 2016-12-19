/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.messaging.tcp.reactor;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Simple holder for a decoding {@link Function} and an encoding
 * {@link BiConsumer} to use with Reactor Netty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyCodec<P> {

	private final Function<? super ByteBuf, ? extends Collection<Message<P>>> decoder;

	private final BiConsumer<? super ByteBuf, ? super Message<P>> encoder;


	public ReactorNettyCodec(Function<? super ByteBuf, ? extends Collection<Message<P>>> decoder,
			BiConsumer<? super ByteBuf, ? super Message<P>> encoder) {

		Assert.notNull(decoder, "'decoder' is required");
		Assert.notNull(encoder, "'encoder' is required");
		this.decoder = decoder;
		this.encoder = encoder;
	}

	public Function<? super ByteBuf, ? extends Collection<Message<P>>> getDecoder() {
		return this.decoder;
	}

	public BiConsumer<? super ByteBuf, ? super Message<P>> getEncoder() {
		return this.encoder;
	}

}
