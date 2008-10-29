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

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import junit.framework.TestCase;

import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 */
public class TransformerUtilsTests extends TestCase {

	public void testEnableIndentingSunnyDay() throws Exception {
		Transformer transformer = new StubTransformer();
		TransformerUtils.enableIndenting(transformer);
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("yes", indent);
		String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xslt}indent-amount");
		assertNotNull(indentAmount);
		assertEquals(String.valueOf(TransformerUtils.DEFAULT_INDENT_AMOUNT), indentAmount);
	}

	public void testEnableIndentingSunnyDayWithCustomKosherIndentAmount() throws Exception {
		final String indentAmountProperty = "10";
		Transformer transformer = new StubTransformer();
		TransformerUtils.enableIndenting(transformer, Integer.valueOf(indentAmountProperty).intValue());
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("yes", indent);
		String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xslt}indent-amount");
		assertNotNull(indentAmount);
		assertEquals(indentAmountProperty, indentAmount);
	}

	public void testDisableIndentingSunnyDay() throws Exception {
		Transformer transformer = new StubTransformer();
		TransformerUtils.disableIndenting(transformer);
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("no", indent);
	}

	public void testEnableIndentingWithNullTransformer() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TransformerUtils.enableIndenting(null);
			}
		}.runTest();
	}

	public void testDisableIndentingWithNullTransformer() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TransformerUtils.disableIndenting(null);
			}
		}.runTest();
	}

	public void testEnableIndentingWithNegativeIndentAmount() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TransformerUtils.enableIndenting(new StubTransformer(), -21938);
			}
		}.runTest();
	}

	public void testEnableIndentingWithZeroIndentAmount() throws Exception {
		TransformerUtils.enableIndenting(new StubTransformer(), 0);
	}


	private static class StubTransformer extends Transformer {

		private Properties outputProperties = new Properties();

		public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
			throw new UnsupportedOperationException();
		}

		public void setParameter(String name, Object value) {
			throw new UnsupportedOperationException();
		}

		public Object getParameter(String name) {
			throw new UnsupportedOperationException();
		}

		public void clearParameters() {
			throw new UnsupportedOperationException();
		}

		public void setURIResolver(URIResolver resolver) {
			throw new UnsupportedOperationException();
		}

		public URIResolver getURIResolver() {
			throw new UnsupportedOperationException();
		}

		public void setOutputProperties(Properties oformat) {
			throw new UnsupportedOperationException();
		}

		public Properties getOutputProperties() {
			return this.outputProperties;
		}

		public void setOutputProperty(String name, String value) throws IllegalArgumentException {
			this.outputProperties.setProperty(name, value);
		}

		public String getOutputProperty(String name) throws IllegalArgumentException {
			return this.outputProperties.getProperty(name);
		}

		public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}

		public ErrorListener getErrorListener() {
			throw new UnsupportedOperationException();
		}
	}

}
