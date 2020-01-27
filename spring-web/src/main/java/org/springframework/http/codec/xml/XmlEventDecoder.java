/*
 * Copyright 2002-2019 the original author or authors.
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
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
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
 * &lt;root>
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

	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();

	private static final boolean aaltoPresent = ClassUtils.isPresent(
			"com.fasterxml.aalto.AsyncXMLStreamReader", XmlEventDecoder.class.getClassLoader());

	boolean useAalto = aaltoPresent;

	private int maxInMemorySize = 256 * 1024;


	public XmlEventDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * Set the max number of bytes that can be buffered by this decoder. This
	 * is either the size the entire input when decoding as a whole, or when
	 * using async parsing via Aalto XML, it is size one top-level XML tree.
	 * When the limit is exceeded, {@link DataBufferLimitException} is raised.
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
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Flux<XMLEvent> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (this.useAalto) {
			AaltoDataBufferToXmlEvent mapper = new AaltoDataBufferToXmlEvent(this.maxInMemorySize);
			return Flux.from(input)
					.flatMapIterable(mapper)
					.doFinally(signalType -> mapper.endOfInput());
		}
		else {
			return DataBufferUtils.join(input, this.maxInMemorySize)
					.flatMapIterable(buffer -> {
						try {
							InputStream is = buffer.asInputStream();
							Iterator eventReader = inputFactory.createXMLEventReader(is);
							List<XMLEvent> result = new ArrayList<>();
							eventReader.forEachRemaining(event -> result.add((XMLEvent) event));
							return result;
						}
						catch (XMLStreamException ex) {
							throw Exceptions.propagate(ex);
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
	private static class AaltoDataBufferToXmlEvent implements Function<DataBuffer, List<? extends XMLEvent>> {

		private static final AsyncXMLInputFactory inputFactory =
				StaxUtils.createDefensiveInputFactory(InputFactoryImpl::new);

		private final AsyncXMLStreamReader<AsyncByteBufferFeeder> streamReader =
				inputFactory.createAsyncForByteBuffer();

		private final XMLEventAllocator eventAllocator = EventAllocatorImpl.getDefaultInstance();

		private final int maxInMemorySize;

		private int byteCount;

		private int elementDepth;


		public AaltoDataBufferToXmlEvent(int maxInMemorySize) {
			this.maxInMemorySize = maxInMemorySize;
		}


		@Override
		public List<? extends XMLEvent> apply(DataBuffer dataBuffer) {
			try {
				increaseByteCount(dataBuffer);
				this.streamReader.getInputFeeder().feedInput(dataBuffer.asByteBuffer());
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
						checkDepthAndResetByteCount(event);
					}
				}
				if (this.maxInMemorySize > 0 && this.byteCount > this.maxInMemorySize) {
					raiseLimitException();
				}
				return events;
			}
			catch (XMLStreamException ex) {
				throw Exceptions.propagate(ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		private void increaseByteCount(DataBuffer dataBuffer) {
			if (this.maxInMemorySize > 0) {
				if (dataBuffer.readableByteCount() > Integer.MAX_VALUE - this.byteCount) {
					raiseLimitException();
				}
				else {
					this.byteCount += dataBuffer.readableByteCount();
				}
			}
		}

		private void checkDepthAndResetByteCount(XMLEvent event) {
			if (this.maxInMemorySize > 0) {
				if (event.isStartElement()) {
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
					this.elementDepth++;
				}
				else if (event.isEndElement()) {
					this.elementDepth--;
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
				}
			}
		}

		private void raiseLimitException() {
			throw new DataBufferLimitException(
					"Exceeded limit on max bytes per XML top-level node: " + this.maxInMemorySize);
		}

		public void endOfInput() {
			this.streamReader.getInputFeeder().endOfInput();
		}
	}



}
