/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@code XMLEventReader}s.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
abstract class AbstractXMLEventReader implements XMLEventReader {

	private boolean closed;


	@Override
	public Object next() {
		try {
			return nextEvent();
		}
		catch (XMLStreamException ex) {
			throw new NoSuchElementException(ex.getMessage());
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(
				"remove not supported on " + ClassUtils.getShortName(getClass()));
	}

	/**
	 * This implementation throws an {@code IllegalArgumentException} for any property.
	 * @throws IllegalArgumentException when called
	 */
	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		throw new IllegalArgumentException("Property not supported: [" + name + "]");
	}

	@Override
	public void close() {
		this.closed = true;
	}

	/**
	 * Check if the reader is closed, and throws a {@code XMLStreamException} if so.
	 * @throws XMLStreamException if the reader is closed
	 * @see #close()
	 */
	protected void checkIfClosed() throws XMLStreamException {
		if (this.closed) {
			throw new XMLStreamException("XMLEventReader has been closed");
		}
	}

}
