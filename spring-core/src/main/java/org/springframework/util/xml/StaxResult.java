/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.util.xml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import org.xml.sax.ContentHandler;

/**
 * Implementation of the {@code Result} tagging interface for StAX writers. Can be constructed with a
 * {@code XMLEventConsumer} or a {@code XMLStreamWriter}.
 *
 * <p>This class is necessary because there is no implementation of {@code Source} for StaxReaders in JAXP 1.3.
 * There is a {@code StAXResult} in JAXP 1.4 (JDK 1.6), but this class is kept around for back-ward compatibility
 * reasons.
 *
 * <p>Even though {@code StaxResult} extends from {@code SAXResult}, calling the methods of
 * {@code SAXResult} is <strong>not supported</strong>. In general, the only supported operation on this class is
 * to use the {@code ContentHandler} obtained via {@link #getHandler()} to parse an input source using an
 * {@code XMLReader}. Calling {@link #setHandler(org.xml.sax.ContentHandler)} will result in
 * {@code UnsupportedOperationException}s.
 *
 * @author Arjen Poutsma
 * @see XMLEventWriter
 * @see XMLStreamWriter
 * @see javax.xml.transform.Transformer
 * @since 3.0
 */
class StaxResult extends SAXResult {

	private XMLEventWriter eventWriter;

	private XMLStreamWriter streamWriter;

	/**
	 * Constructs a new instance of the {@code StaxResult} with the specified {@code XMLStreamWriter}.
	 *
	 * @param streamWriter the {@code XMLStreamWriter} to write to
	 */
	StaxResult(XMLStreamWriter streamWriter) {
		super.setHandler(new StaxStreamContentHandler(streamWriter));
		this.streamWriter = streamWriter;
	}

	/**
	 * Constructs a new instance of the {@code StaxResult} with the specified {@code XMLEventWriter}.
	 *
	 * @param eventWriter the {@code XMLEventWriter} to write to
	 */
	StaxResult(XMLEventWriter eventWriter) {
		super.setHandler(new StaxEventContentHandler(eventWriter));
		this.eventWriter = eventWriter;
	}

	/**
	 * Constructs a new instance of the {@code StaxResult} with the specified {@code XMLEventWriter} and
	 * {@code XMLEventFactory}.
	 *
	 * @param eventWriter  the {@code XMLEventWriter} to write to
	 * @param eventFactory the {@code XMLEventFactory} to use for creating events
	 */
	StaxResult(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		super.setHandler(new StaxEventContentHandler(eventWriter, eventFactory));
		this.eventWriter = eventWriter;
	}

	/**
	 * Returns the {@code XMLEventWriter} used by this {@code StaxResult}. If this {@code StaxResult} was
	 * created with an {@code XMLStreamWriter}, the result will be {@code null}.
	 *
	 * @return the StAX event writer used by this result
	 * @see #StaxResult(javax.xml.stream.XMLEventWriter)
	 */
	XMLEventWriter getXMLEventWriter() {
		return eventWriter;
	}

	/**
	 * Returns the {@code XMLStreamWriter} used by this {@code StaxResult}. If this {@code StaxResult} was
	 * created with an {@code XMLEventConsumer}, the result will be {@code null}.
	 *
	 * @return the StAX stream writer used by this result
	 * @see #StaxResult(javax.xml.stream.XMLStreamWriter)
	 */
	XMLStreamWriter getXMLStreamWriter() {
		return streamWriter;
	}

	/**
	 * Throws a {@code UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setHandler(ContentHandler handler) {
		throw new UnsupportedOperationException("setHandler is not supported");
	}
}
