/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Decodes a {@link DataBuffer} stream into a stream of {@link XMLEvent XMLEvents}.
 *
 * <p>Given the following XML:
 *
 * <pre class="code">
 * &lt;root&gt;
 *     &lt;child&gt;foo&lt;/child&gt;
 *     &lt;child&gt;bar&lt;/child&gt;
 * &lt;/root&gt;
 * </pre>
 *
 * this decoder will produce a {@link Flux} with the following events:
 *
 * <ol>
 * <li>{@link javax.xml.stream.events.StartDocument}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code root}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code root}</li>
 * </ol>
 *
 * <p>Note that this decoder is not registered by default but is used internally
 * by other decoders which are registered by default.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public class XmlEventDecoder extends AbstractDecoder<XMLEvent> {

	/**
	 * Hint key for a {@link ReceivedByteTracker} instance that callers can use
	 * to track the number of received bytes.
	 */
	public static final String BYTE_TRACKER_HINT = XmlEventDecoder.class.getName() + ".byteTracker";

	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();

	private static final boolean AALTO_PRESENT = ClassUtils.isPresent(
			"com.fasterxml.aalto.AsyncXMLStreamReader", XmlEventDecoder.class.getClassLoader());

	boolean useAalto = AALTO_PRESENT;

	private int maxInMemorySize = 256 * 1024;


	public XmlEventDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * Set the max number of bytes this decoder should buffer in memory resulting
	 * in a {@link DataBufferLimitException} when the limit is exceeded.
	 * <p>When joining all buffers and decoding as a whole, the limit is applied
	 * to the entire input.
	 * <p>>When using Aalto XML async parsing, the limit does not apply at the
	 * level of this decoder because the XML events parsed from each buffer are
	 * emitted immediately and the buffer is released.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public Flux<XMLEvent> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (this.useAalto) {
			AaltoDataBufferToXmlEvent mapper = new AaltoDataBufferToXmlEvent(hints);
			return Flux.from(input)
					.flatMapIterable(mapper)
					.doFinally(signalType -> mapper.endOfInput());
		}
		else {
			return DataBufferUtils.join(input, this.maxInMemorySize)
					.flatMapIterable(buffer -> {
						try {
							InputStream is = buffer.asInputStream();
							Iterator<Object> eventReader = inputFactory.createXMLEventReader(is);
							List<XMLEvent> result = new ArrayList<>();
							eventReader.forEachRemaining(event -> result.add((XMLEvent) event));
							return result;
						}
						catch (XMLStreamException ex) {
							throw new DecodingException(ex.getMessage(), ex);
						}
						finally {
							DataBufferUtils.release(buffer);
						}
					});
		}
	}


	/*
	 * Separate static class to isolate Aalto dependency.
	 */
	private static final class AaltoDataBufferToXmlEvent implements Function<DataBuffer, List<? extends XMLEvent>> {

		private static final AsyncXMLInputFactory inputFactory =
				StaxUtils.createDefensiveInputFactory(InputFactoryImpl::new);

		private final AsyncXMLStreamReader<AsyncByteBufferFeeder> streamReader =
				inputFactory.createAsyncForByteBuffer();

		private final XMLEventAllocator eventAllocator = EventAllocatorImpl.getDefaultInstance();

		@Nullable
		private final ReceivedByteTracker byteTracker;

		private AaltoDataBufferToXmlEvent(@Nullable Map<String, Object> hints) {
			this.byteTracker = (hints != null ? (ReceivedByteTracker) hints.get(BYTE_TRACKER_HINT) : null);
		}

		@Override
		public List<? extends XMLEvent> apply(DataBuffer dataBuffer) {
			try {
				if (this.byteTracker != null) {
					this.byteTracker.incrementByteCount(dataBuffer);
				}
				AsyncByteBufferFeeder inputFeeder = this.streamReader.getInputFeeder();
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					while (iterator.hasNext()) {
						inputFeeder.feedInput(iterator.next());
					}
				}
				List<XMLEvent> events = new ArrayList<>();
				while (true) {
					if (this.streamReader.next() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
						// no more events with what currently has been fed to the reader
						break;
					}
					else {
						XMLEvent event = this.eventAllocator.allocate(this.streamReader);
						events.add(event);
						if (event.isEndDocument()) {
							break;
						}
					}
				}
				return events;
			}
			catch (XMLStreamException ex) {
				throw new DecodingException(ex.getMessage(), ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		public void endOfInput() {
			this.streamReader.getInputFeeder().endOfInput();
		}
	}


	/**
	 * Callers of {@link XmlEventDecoder} that buffer emitted XML events at a
	 * higher level, can pass an instance of this tracker as an
	 * {@link XmlEventDecoder#BYTE_TRACKER_HINT} to monitor the total number of
	 * bytes received, and to reset periodically.
	 * <p>For use with Aalto XML async parsing only, in which case this decoder
	 * parses releases each buffer immediately.
	 */
	public static class ReceivedByteTracker {

		/** An instance to use when there is no limit. */
		public static final ReceivedByteTracker NO_OP = new ReceivedByteTracker(-1);

		private final int maxInMemorySize;

		private int byteCount;

		public ReceivedByteTracker(int maxInMemorySize) {
			this.maxInMemorySize = maxInMemorySize;
		}

		public int getMaxInMemorySize() {
			return this.maxInMemorySize;
		}

		public boolean isMaxInMemorySizeExceeded() {
			return (this.maxInMemorySize != -1 && this.byteCount > this.maxInMemorySize);
		}

		public void reset() {
			this.byteCount = 0;
		}

		private void incrementByteCount(DataBuffer buffer) {
			if (this.maxInMemorySize != -1) {
				this.byteCount += buffer.readableByteCount();
			}
		}

		@Override
		public String toString() {
			return this.byteCount + " bytes";
		}
	}

}
