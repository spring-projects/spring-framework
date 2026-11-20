/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
