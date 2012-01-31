/*
 * Copyright 2002-2009 the original author or authors.
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
 * Implementation of the <code>Result</code> tagging interface for StAX writers. Can be constructed with a
 * <code>XMLEventConsumer</code> or a <code>XMLStreamWriter</code>.
 *
 * <p>This class is necessary because there is no implementation of <code>Source</code> for StaxReaders in JAXP 1.3.
 * There is a <code>StAXResult</code> in JAXP 1.4 (JDK 1.6), but this class is kept around for back-ward compatibility
 * reasons.
 *
 * <p>Even though <code>StaxResult</code> extends from <code>SAXResult</code>, calling the methods of
 * <code>SAXResult</code> is <strong>not supported</strong>. In general, the only supported operation on this class is
 * to use the <code>ContentHandler</code> obtained via {@link #getHandler()} to parse an input source using an
 * <code>XMLReader</code>. Calling {@link #setHandler(org.xml.sax.ContentHandler)} will result in
 * <code>UnsupportedOperationException</code>s.
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
	 * Constructs a new instance of the <code>StaxResult</code> with the specified <code>XMLStreamWriter</code>.
	 *
	 * @param streamWriter the <code>XMLStreamWriter</code> to write to
	 */
	StaxResult(XMLStreamWriter streamWriter) {
		super.setHandler(new StaxStreamContentHandler(streamWriter));
		this.streamWriter = streamWriter;
	}

	/**
	 * Constructs a new instance of the <code>StaxResult</code> with the specified <code>XMLEventWriter</code>.
	 *
	 * @param eventWriter the <code>XMLEventWriter</code> to write to
	 */
	StaxResult(XMLEventWriter eventWriter) {
		super.setHandler(new StaxEventContentHandler(eventWriter));
		this.eventWriter = eventWriter;
	}

	/**
	 * Constructs a new instance of the <code>StaxResult</code> with the specified <code>XMLEventWriter</code> and
	 * <code>XMLEventFactory</code>.
	 *
	 * @param eventWriter  the <code>XMLEventWriter</code> to write to
	 * @param eventFactory the <code>XMLEventFactory</code> to use for creating events
	 */
	StaxResult(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		super.setHandler(new StaxEventContentHandler(eventWriter, eventFactory));
		this.eventWriter = eventWriter;
	}

	/**
	 * Returns the <code>XMLEventWriter</code> used by this <code>StaxResult</code>. If this <code>StaxResult</code> was
	 * created with an <code>XMLStreamWriter</code>, the result will be <code>null</code>.
	 *
	 * @return the StAX event writer used by this result
	 * @see #StaxResult(javax.xml.stream.XMLEventWriter)
	 */
	XMLEventWriter getXMLEventWriter() {
		return eventWriter;
	}

	/**
	 * Returns the <code>XMLStreamWriter</code> used by this <code>StaxResult</code>. If this <code>StaxResult</code> was
	 * created with an <code>XMLEventConsumer</code>, the result will be <code>null</code>.
	 *
	 * @return the StAX stream writer used by this result
	 * @see #StaxResult(javax.xml.stream.XMLStreamWriter)
	 */
	XMLStreamWriter getXMLStreamWriter() {
		return streamWriter;
	}

	/**
	 * Throws a <code>UnsupportedOperationException</code>.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setHandler(ContentHandler handler) {
		throw new UnsupportedOperationException("setHandler is not supported");
	}
}
