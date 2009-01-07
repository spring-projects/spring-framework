/*
 * Copyright 2002-2008 the original author or authors.
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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.springframework.util.Assert;

/**
 * Contains common behavior relating to {@link javax.xml.transform.Transformer Transformers}, and the
 * <code>javax.xml.transform</code> package in general.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5.5
 */
public abstract class TransformerUtils {

	/**
	 * The indent amount of characters if {@link #enableIndenting(javax.xml.transform.Transformer) indenting is enabled}.
	 * <p>Defaults to "2".
	 */
	public static final int DEFAULT_INDENT_AMOUNT = 2;

	/**
	 * Enable indenting for the supplied {@link javax.xml.transform.Transformer}. <p>If the underlying XSLT engine is
	 * Xalan, then the special output key <code>indent-amount</code> will be also be set to a value of {@link
	 * #DEFAULT_INDENT_AMOUNT} characters.
	 *
	 * @param transformer the target transformer
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer) {
		enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
	}

	/**
	 * Enable indenting for the supplied {@link javax.xml.transform.Transformer}. <p>If the underlying XSLT engine is
	 * Xalan, then the special output key <code>indent-amount</code> will be also be set to a value of {@link
	 * #DEFAULT_INDENT_AMOUNT} characters.
	 *
	 * @param transformer  the target transformer
	 * @param indentAmount the size of the indent (2 characters, 3 characters, etc.)
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer, int indentAmount) {
		Assert.notNull(transformer, "Transformer must not be null");
		Assert.isTrue(indentAmount > -1, "The indent amount cannot be less than zero : got " + indentAmount);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			// Xalan-specific, but this is the most common XSLT engine in any case
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
		}
		catch (IllegalArgumentException ignored) {
		}
	}

	/**
	 * Disable indenting for the supplied {@link javax.xml.transform.Transformer}.
	 *
	 * @param transformer the target transformer
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void disableIndenting(Transformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
	}

	/**
	 * Indicates whether the given {@link Source} is a StAX Source.
	 *
	 * @return <code>true</code> if <code>source</code> is a Spring {@link StaxSource} or JAXP 1.4 {@link StAXSource};
	 *         <code>false</code> otherwise.
	 */
	public static boolean isStaxSource(Source source) {
		if (source instanceof StaxSource) {
			return true;
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.isStaxSource(source);
		}
		else {
			return false;
		}
	}

	/**
	 * Indicates whether the given {@link Result} is a StAX Result.
	 *
	 * @return <code>true</code> if <code>result</code> is a Spring {@link StaxResult} or JAXP 1.4 {@link StAXResult};
	 *         <code>false</code> otherwise.
	 */
	public static boolean isStaxResult(Result result) {
		if (result instanceof StaxResult) {
			return true;
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.isStaxResult(result);
		}
		else {
			return false;
		}
	}

	/**
	 * Returns the {@link XMLStreamReader} for the given StAX Source.
	 *
	 * @param source a Spring {@link StaxSource} or {@link StAXSource}
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring-WS {@link StaxSource} or {@link
	 *                                  StAXSource}
	 */
	public static XMLStreamReader getXMLStreamReader(Source source) {
		if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLStreamReader();
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.getXMLStreamReader(source);
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * Returns the {@link XMLEventReader} for the given StAX Source.
	 *
	 * @param source a Spring {@link StaxSource} or {@link StAXSource}
	 * @return the {@link XMLEventReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring {@link StaxSource} or {@link
	 *                                  StAXSource}
	 */
	public static XMLEventReader getXMLEventReader(Source source) {
		if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLEventReader();
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.getXMLEventReader(source);
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * Returns the {@link XMLStreamWriter} for the given StAX Result.
	 *
	 * @param result a Spring {@link StaxResult} or {@link StAXResult}
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring {@link StaxResult} or {@link
	 *                                  StAXResult}
	 */
	public static XMLStreamWriter getXMLStreamWriter(Result result) {
		if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLStreamWriter();
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.getXMLStreamWriter(result);
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * Returns the {@link XMLEventWriter} for the given StAX Result.
	 *
	 * @param result a Spring {@link StaxResult} or {@link StAXResult}
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException if <code>source</code> is neither a Spring {@link StaxResult} or {@link
	 *                                  StAXResult}
	 */
	public static XMLEventWriter getXMLEventWriter(Result result) {
		if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLEventWriter();
		}
		else if (JaxpVersion.isAtLeastJaxp14()) {
			return Jaxp14StaxHandler.getXMLEventWriter(result);
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/** Inner class to avoid a static JAXP 1.4 dependency. */
	private static class Jaxp14StaxHandler {

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
