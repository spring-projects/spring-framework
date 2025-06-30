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

package org.springframework.util.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Implementation of the {@code Source} tagging interface for StAX readers. Can be constructed with
 * an {@code XMLEventReader} or an {@code XMLStreamReader}.
 *
 * <p>This class is necessary because there is no implementation of {@code Source} for StAX Readers
 * in JAXP 1.3. There is a {@code StAXSource} in JAXP 1.4 (JDK 1.6), but this class is kept around
 * for backwards compatibility reasons.
 *
 * <p>Even though {@code StaxSource} extends from {@code SAXSource}, calling the methods of
 * {@code SAXSource} is <strong>not supported</strong>. In general, the only supported operation
 * on this class is to use the {@code XMLReader} obtained via {@link #getXMLReader()} to parse the
 * input source obtained via {@link #getInputSource()}. Calling {@link #setXMLReader(XMLReader)}
 * or {@link #setInputSource(InputSource)} will result in {@code UnsupportedOperationException #setInputSource(InputSource)} will result in {@code UnsupportedOperationExceptions}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEventReader
 * @see XMLStreamReader
 * @see javax.xml.transform.Transformer
 */
class StaxSource extends SAXSource {

	private @Nullable XMLEventReader eventReader;

	private @Nullable XMLStreamReader streamReader;


	/**
	 * Construct a new instance of the {@code StaxSource} with the specified {@code XMLEventReader}.
	 * The supplied event reader must be in {@code XMLStreamConstants.START_DOCUMENT} or
	 * {@code XMLStreamConstants.START_ELEMENT} state.
	 * @param eventReader the {@code XMLEventReader} to read from
	 * @throws IllegalStateException if the reader is not at the start of a document or element
	 */
	StaxSource(XMLEventReader eventReader) {
		super(new StaxEventXMLReader(eventReader), new InputSource());
		this.eventReader = eventReader;
	}

	/**
	 * Construct a new instance of the {@code StaxSource} with the specified {@code XMLStreamReader}.
	 * The supplied stream reader must be in {@code XMLStreamConstants.START_DOCUMENT} or
	 * {@code XMLStreamConstants.START_ELEMENT} state.
	 * @param streamReader the {@code XMLStreamReader} to read from
	 * @throws IllegalStateException if the reader is not at the start of a document or element
	 */
	StaxSource(XMLStreamReader streamReader) {
		super(new StaxStreamXMLReader(streamReader), new InputSource());
		this.streamReader = streamReader;
	}


	/**
	 * Return the {@code XMLEventReader} used by this {@code StaxSource}.
	 * <p>If this {@code StaxSource} was created with an {@code XMLStreamReader},
	 * the result will be {@code null}.
	 * @return the StAX event reader used by this source
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable XMLEventReader getXMLEventReader() {
		return this.eventReader;
	}

	/**
	 * Return the {@code XMLStreamReader} used by this {@code StaxSource}.
	 * <p>If this {@code StaxSource} was created with an {@code XMLEventReader},
	 * the result will be {@code null}.
	 * @return the StAX event reader used by this source
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable XMLStreamReader getXMLStreamReader() {
		return this.streamReader;
	}


	/**
	 * Throws an {@code UnsupportedOperationException}.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * Throws an {@code UnsupportedOperationException}.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}

}
