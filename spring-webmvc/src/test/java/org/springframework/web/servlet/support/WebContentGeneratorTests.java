/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.servlet.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link WebContentGenerator}.
 * @author Rossen Stoyanchev
 */
public class WebContentGeneratorTests {


	@Test
	public void getAllowHeaderWithConstructorTrue() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator(true);
		assertEquals("GET,HEAD,POST,OPTIONS", generator.getAllowHeader());
	}

	@Test
	public void getAllowHeaderWithConstructorFalse() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator(false);
		assertEquals("GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS", generator.getAllowHeader());
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsConstructor() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator("POST");
		assertEquals("POST,OPTIONS", generator.getAllowHeader());
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsSetter() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods("POST");
		assertEquals("POST,OPTIONS", generator.getAllowHeader());
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsSetterEmpty() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods();
		assertEquals("Effectively \"no restriction\" on supported methods",
				"GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS", generator.getAllowHeader());
	}


	private static class TestWebContentGenerator extends WebContentGenerator {

		public TestWebContentGenerator() {
		}

		public TestWebContentGenerator(boolean restrictDefaultSupportedMethods) {
			super(restrictDefaultSupportedMethods);
		}

		public TestWebContentGenerator(String... supportedMethods) {
			super(supportedMethods);
		}
	}
}
