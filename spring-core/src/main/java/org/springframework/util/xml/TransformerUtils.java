/*
 * Copyright 2002-2023 the original author or authors.
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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.util.Assert;

/**
 * Contains common behavior relating to {@link javax.xml.transform.Transformer Transformers}
 * and the {@code javax.xml.transform} package in general.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class TransformerUtils {

	/**
	 * The indent amount of characters if {@link #enableIndenting indenting is enabled}.
	 * <p>Defaults to "2".
	 */
	public static final int DEFAULT_INDENT_AMOUNT = 2;


	/**
	 * Enable indenting for the supplied {@link javax.xml.transform.Transformer}.
	 * <p>If the underlying XSLT engine is Xalan, then the special output key {@code indent-amount}
	 * will be also be set to a value of {@link #DEFAULT_INDENT_AMOUNT} characters.
	 * @param transformer the target transformer
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer) {
		enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
	}

	/**
	 * Enable indenting for the supplied {@link javax.xml.transform.Transformer}.
	 * <p>If the underlying XSLT engine is Xalan, then the special output key {@code indent-amount}
	 * will be also be set to a value of {@link #DEFAULT_INDENT_AMOUNT} characters.
	 * @param transformer the target transformer
	 * @param indentAmount the size of the indent (2 characters, 3 characters, etc.)
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer, int indentAmount) {
		Assert.notNull(transformer, "Transformer must not be null");
		if (indentAmount < 0) {
			throw new IllegalArgumentException("Invalid indent amount (must not be less than zero): " + indentAmount);
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			// Xalan-specific, but this is the most common XSLT engine in any case
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", String.valueOf(indentAmount));
		}
		catch (IllegalArgumentException ignored) {
		}
	}

	/**
	 * Disable indenting for the supplied {@link javax.xml.transform.Transformer}.
	 * @param transformer the target transformer
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void disableIndenting(Transformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
	}

}
