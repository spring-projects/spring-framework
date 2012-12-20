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

import java.util.Properties;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Rick Evans
 * @author Arjen Poutsma
 */
public class TransformerUtilsTests {

	@Test
	public void enableIndentingSunnyDay() throws Exception {
		Transformer transformer = new StubTransformer();
		TransformerUtils.enableIndenting(transformer);
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("yes", indent);
		String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xslt}indent-amount");
		assertNotNull(indentAmount);
		assertEquals(String.valueOf(TransformerUtils.DEFAULT_INDENT_AMOUNT), indentAmount);
	}

	@Test
	public void enableIndentingSunnyDayWithCustomKosherIndentAmount() throws Exception {
		final String indentAmountProperty = "10";
		Transformer transformer = new StubTransformer();
		TransformerUtils.enableIndenting(transformer, Integer.valueOf(indentAmountProperty));
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("yes", indent);
		String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xslt}indent-amount");
		assertNotNull(indentAmount);
		assertEquals(indentAmountProperty, indentAmount);
	}

	@Test
	public void disableIndentingSunnyDay() throws Exception {
		Transformer transformer = new StubTransformer();
		TransformerUtils.disableIndenting(transformer);
		String indent = transformer.getOutputProperty(OutputKeys.INDENT);
		assertNotNull(indent);
		assertEquals("no", indent);
	}

	@Test(expected = IllegalArgumentException.class)
	public void enableIndentingWithNullTransformer() throws Exception {
		TransformerUtils.enableIndenting(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void disableIndentingWithNullTransformer() throws Exception {
		TransformerUtils.disableIndenting(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void enableIndentingWithNegativeIndentAmount() throws Exception {
		TransformerUtils.enableIndenting(new StubTransformer(), -21938);
	}

	@Test
	public void enableIndentingWithZeroIndentAmount() throws Exception {
		TransformerUtils.enableIndenting(new StubTransformer(), 0);
	}

	private static class StubTransformer extends Transformer {

		private Properties outputProperties = new Properties();

		@Override
		public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setParameter(String name, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getParameter(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clearParameters() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setURIResolver(URIResolver resolver) {
			throw new UnsupportedOperationException();
		}

		@Override
		public URIResolver getURIResolver() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setOutputProperties(Properties oformat) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Properties getOutputProperties() {
			return this.outputProperties;
		}

		@Override
		public void setOutputProperty(String name, String value) throws IllegalArgumentException {
			this.outputProperties.setProperty(name, value);
		}

		@Override
		public String getOutputProperty(String name) throws IllegalArgumentException {
			return this.outputProperties.getProperty(name);
		}

		@Override
		public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ErrorListener getErrorListener() {
			throw new UnsupportedOperationException();
		}
	}

}
