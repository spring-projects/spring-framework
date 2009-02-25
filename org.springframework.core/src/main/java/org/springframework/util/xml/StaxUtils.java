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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Convenience methods for working with the StAX API.
 *
 * <p>In particular, methods for using StAX in combination with the TrAX API
 * (<code>javax.xml.transform</code>), and converting StAX readers/writers
 * into SAX readers/handlers and vice-versa.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class StaxUtils {

	private static boolean jaxp14Available =
			ClassUtils.isPresent("javax.xml.transform.stax.StAXSource", StaxUtils.class.getClassLoader());


	/**
	 * Create a StAX {@link Source} for the given {@link XMLStreamReader}.
	 * @param streamReader the StAX stream reader
	 * @return a source wrapping <code>streamReader</code>
	 */
	public static Source createStaxSource(XMLStreamReader streamReader) {
		if (jaxp14Available) {
			return Jaxp14StaxHandler.createStaxSource(streamReader);
		}
		else {
			return new StaxSource(streamReader);
		}
	}

	/**
	 * Create a StAX {@link Source} for the given {@link XMLEventReader}.
	 * @param eventReader the StAX event reader
	 * @return a source wrapping <code>streamReader</code>
	 * @throws XMLStreamException in case of StAX errors
	 */
	public static Source createStaxSource(XMLEventReader eventReader) throws XMLStreamException {
		if (jaxp14Available) {
			return Jaxp14StaxHandler.createStaxSource(eventReader);
		}
		else {
			return new StaxSource(eventReader);
		}
	}

	/**
	 * Indicate whether the given {@link javax.xml.transform.Source} is a StAX Source.
	 * @return <code>true</code> if <code>source</code> is a Spring StaxSource or JAXP
	 * 1.4 {@link javax.xml.transform.stax.StAXSource}; <code>false</code> otherwise.
	 */
	public static boolean isStaxSource(Source source) {
		return (source instanceof StaxSource || (jaxp14Available && Jaxp14StaxHandler.isStaxSource(source)));
	}

	/**
	 * Create a StAX {@link Result} for the given {@link XMLStreamWriter}.
	 * @param streamWriter the StAX stream writer
	 * @return a result wrapping <code>streamWriter</code>
	 */
	public static Result createStaxResult(XMLStreamWriter streamWriter) {
		if (jaxp14Available) {
			return Jaxp14StaxHandler.createStaxResult(streamWriter);
		}
		else {
			return new StaxResult(streamWriter);
		}
	}

	/**
	 * Create a StAX {@link Result} for the given {@link XMLEventWriter}.
	 * @param eventWriter the StAX event writer
	 * @return a result wrapping <code>streamReader</code>
	 * @throws XMLStreamException in case of StAX errors
	 */
	public static Result createStaxResult(XMLEventWriter eventWriter) throws XMLStreamException {
		if (jaxp14Available) {
			return Jaxp14StaxHandler.createStaxResult(eventWriter);
		}
		else {
			return new StaxResult(eventWriter);
		}
	}

	/**
	 * Indicate whether the given {@link javax.xml.transform.Result} is a StAX Result.
	 * @return <code>true</code> if <code>result</code> is a Spring StaxResult or JAXP
	 * 1.4 {@link javax.xml.transform.stax.StAXResult}; <code>false</code> otherwise.
	 */
	public static boolean isStaxResult(Result result) {
		return (result instanceof StaxResult || (jaxp14Available && Jaxp14StaxHandler.isStaxResult(result)));
	}

	/**
	 * Return the {@link javax.xml.stream.XMLStreamReader} for the given StAX Source.
	 * @param source a Spring StaxSource or JAXP 1.4 {@link javax.xml.transform.stax.StAXSource}
	 * @return the {@link javax.xml.stream.XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring StaxSource
	 * nor JAXP 1.4 {@link javax.xml.transform.stax.StAXSource}
	 */
	public static XMLStreamReader getXMLStreamReader(Source source) {
		if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLStreamReader();
		}
		else if (jaxp14Available) {
			return Jaxp14StaxHandler.getXMLStreamReader(source);
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * Return the {@link javax.xml.stream.XMLEventReader} for the given StAX Source.
	 * @param source a Spring StaxSource or JAXP 1.4  {@link javax.xml.transform.stax.StAXSource}
	 * @return the {@link javax.xml.stream.XMLEventReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring StaxSource
	 * nor a JAXP 1.4 {@link javax.xml.transform.stax.StAXSource}
	 */
	public static XMLEventReader getXMLEventReader(Source source) {
		if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLEventReader();
		}
		else if (jaxp14Available) {
			return Jaxp14StaxHandler.getXMLEventReader(source);
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * Return the {@link javax.xml.stream.XMLStreamWriter} for the given StAX Result.
	 * @param result a Spring StaxResult or JAXP 1.4 {@link javax.xml.transform.stax.StAXResult}
	 * @return the {@link javax.xml.stream.XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring StaxResult
	 * nor a JAXP 1.4 {@link javax.xml.transform.stax.StAXResult}
	 */
	public static XMLStreamWriter getXMLStreamWriter(Result result) {
		if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLStreamWriter();
		}
		else if (jaxp14Available) {
			return Jaxp14StaxHandler.getXMLStreamWriter(result);
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * Return the {@link XMLEventWriter} for the given StAX Result.
	 * @param result a Spring StaxResult or JAXP 1.4 {@link javax.xml.transform.stax.StAXResult}
	 * @return the {@link javax.xml.stream.XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring StaxResult
	 * nor a JAXP 1.4 {@link javax.xml.transform.stax.StAXResult}
	 */
	public static XMLEventWriter getXMLEventWriter(Result result) {
		if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLEventWriter();
		}
		else if (jaxp14Available) {
			return Jaxp14StaxHandler.getXMLEventWriter(result);
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * Create a SAX {@link ContentHandler} that writes to the given StAX {@link XMLStreamWriter}.
	 * @param streamWriter the StAX stream writer
	 * @return a content handler writing to the <code>streamWriter</code>
	 */
	public static ContentHandler createContentHandler(XMLStreamWriter streamWriter) {
		return new StaxStreamContentHandler(streamWriter);
	}

	/**
	 * Create a SAX {@link ContentHandler} that writes events to the given StAX {@link XMLEventWriter}.
	 * @param eventWriter the StAX event writer
	 * @return a content handler writing to the <code>eventWriter</code>
	 */
	public static ContentHandler createContentHandler(XMLEventWriter eventWriter) {
		return new StaxEventContentHandler(eventWriter);
	}

	/**
	 * Create a SAX {@link XMLReader} that reads from the given StAX {@link XMLStreamReader}.
	 * @param streamReader the StAX stream reader
	 * @return a XMLReader reading from the <code>streamWriter</code>
	 */
	public static XMLReader createXMLReader(XMLStreamReader streamReader) {
		return new StaxStreamXMLReader(streamReader);
	}

	/**
	 * Create a SAX {@link XMLReader} that reads from the given StAX {@link XMLEventReader}.
	 * @param eventReader the StAX event reader
	 * @return a XMLReader reading from the <code>eventWriter</code>
	 */
	public static XMLReader createXMLReader(XMLEventReader eventReader) {
		return new StaxEventXMLReader(eventReader);
	}

	/**
	 * Return a {@link XMLStreamReader} that reads from a {@link XMLEventReader}. Useful, because the StAX
	 * <code>XMLInputFactory</code> allows one to create a event reader from a stream reader, but not vice-versa.
	 * @return a stream reader that reads from an event reader
	 */
	public static XMLStreamReader createEventStreamReader(XMLEventReader eventReader) throws XMLStreamException {
		return new XMLEventStreamReader(eventReader);
	}


	/**
	 * Inner class to avoid a static JAXP 1.4 dependency.
	 */
	private static class Jaxp14StaxHandler {

		private static Source createStaxSource(XMLStreamReader streamReader) {
			return new StAXSource(streamReader);
		}

		private static Source createStaxSource(XMLEventReader eventReader) throws XMLStreamException {
			return new StAXSource(eventReader);
		}

		private static Result createStaxResult(XMLStreamWriter streamWriter) {
			return new StAXResult(streamWriter);
		}

		private static Result createStaxResult(XMLEventWriter eventWriter) {
			return new StAXResult(eventWriter);
		}

		private static boolean isStaxSource(Source source) {
			return source instanceof StAXSource;
		}

		private static boolean isStaxResult(Result result) {
			return result instanceof StAXResult;
		}

		private static XMLStreamReader getXMLStreamReader(Source source) {
			Assert.isInstanceOf(StAXSource.class, source);
			return ((StAXSource) source).getXMLStreamReader();
		}

		private static XMLEventReader getXMLEventReader(Source source) {
			Assert.isInstanceOf(StAXSource.class, source);
			return ((StAXSource) source).getXMLEventReader();
		}

		private static XMLStreamWriter getXMLStreamWriter(Result result) {
			Assert.isInstanceOf(StAXResult.class, result);
			return ((StAXResult) result).getXMLStreamWriter();
		}

		private static XMLEventWriter getXMLEventWriter(Result result) {
			Assert.isInstanceOf(StAXResult.class, result);
			return ((StAXResult) result).getXMLEventWriter();
		}
	}

}
