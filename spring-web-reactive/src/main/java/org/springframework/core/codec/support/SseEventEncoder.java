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

package org.springframework.core.codec.support;

import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.sse.SseEvent;

/**
 * An encoder for {@link SseEvent}s that also supports any other kind of {@link Object}
 * (in that case, the object will be the data of the {@link SseEvent}).
 * @author Sebastien Deleuze
 */
public class SseEventEncoder extends AbstractEncoder<Object> {

	private final Encoder<String> stringEncoder;

	private final List<Encoder<?>> dataEncoders;


	public SseEventEncoder(Encoder<String> stringEncoder, List<Encoder<?>> dataEncoders) {
		super(new MimeType("text", "event-stream"));
		Assert.notNull(stringEncoder, "'stringEncoder' must not be null");
		Assert.notNull(dataEncoders, "'dataEncoders' must not be null");
		this.stringEncoder = stringEncoder;
		this.dataEncoders = dataEncoders;
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType type, MimeType sseMimeType, Object... hints) {

		return Flux.from(inputStream).flatMap(input -> {
			SseEvent event = (SseEvent.class.equals(type.getRawClass()) ? (SseEvent)input : new SseEvent(input));

			StringBuilder sb = new StringBuilder();

			if (event.getId() != null) {
				sb.append("id:");
				sb.append(event.getId());
				sb.append("\n");
			}

			if (event.getName() != null) {
				sb.append("event:");
				sb.append(event.getName());
				sb.append("\n");
			}

			if (event.getReconnectTime() != null) {
				sb.append("retry:");
				sb.append(event.getReconnectTime().toString());
				sb.append("\n");
			}

			if (event.getComment() != null) {
				sb.append(":");
				sb.append(event.getComment().replaceAll("\\n", "\n:"));
				sb.append("\n");
			}

			Object data = event.getData();
			Flux<DataBuffer> dataBuffer = Flux.empty();
			MimeType stringMimeType = this.stringEncoder.getEncodableMimeTypes().get(0);
			MimeType mimeType = (event.getMimeType() == null ?
	                (data instanceof String ? stringMimeType : new MimeType("*")) : event.getMimeType());
			if (data != null) {
				sb.append("data:");
				if (data instanceof String && mimeType.isCompatibleWith(stringMimeType)) {
					sb.append(((String)data).replaceAll("\\n", "\ndata:")).append("\n");
				}
				else {
					Optional<Encoder<?>> encoder = dataEncoders
						.stream()
						.filter(e -> e.canEncode(ResolvableType.forClass(data.getClass()), mimeType))
						.findFirst();

					if (encoder.isPresent()) {
						dataBuffer = ((Encoder<Object>)encoder.get())
								.encode(Mono.just(data), bufferFactory, ResolvableType.forClass(data.getClass()), mimeType)
								.concatWith(encodeString("\n", bufferFactory, stringMimeType));
					}
					else {
						throw new CodecException("No suitable encoder found!");
					}
				}
			}

			return Flux
					.concat(encodeString(sb.toString(), bufferFactory, stringMimeType), dataBuffer)
					.reduce((buf1, buf2) -> buf1.write(buf2))
					.concatWith(encodeString("\n", bufferFactory, stringMimeType).map(b -> new FlushingDataBuffer(b)));
		});

	}

	private Flux<DataBuffer> encodeString(String str, DataBufferFactory bufferFactory, MimeType mimeType) {
		return stringEncoder.encode(Mono.just(str), bufferFactory, ResolvableType.forClass(String.class), mimeType);
	}

}
