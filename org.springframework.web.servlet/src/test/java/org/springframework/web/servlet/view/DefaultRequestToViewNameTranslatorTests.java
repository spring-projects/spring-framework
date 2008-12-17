/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.servlet.view;

import junit.framework.TestCase;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for the DefaultRequestToViewNameTranslator class.
 *
 * @author Rick Evans
 */
public final class DefaultRequestToViewNameTranslatorTests extends TestCase {

	private static final String VIEW_NAME = "apple";
	private static final String EXTENSION = ".html";
	private static final String CONTEXT_PATH = "/sundays";

	private DefaultRequestToViewNameTranslator translator;
	private MockHttpServletRequest request;


	protected void setUp() throws Exception {
		this.translator = new DefaultRequestToViewNameTranslator();
		this.request = new MockHttpServletRequest();
		this.request.setContextPath(CONTEXT_PATH);
	}


	public void TODO_testGetViewNameLeavesLeadingSlashIfSoConfigured() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		this.translator.setStripLeadingSlash(false);
		assertViewName("/" + VIEW_NAME);
	}

	public void testGetViewNameLeavesExtensionIfSoConfigured() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME + EXTENSION);
		this.translator.setStripExtension(false);
		assertViewName(VIEW_NAME + EXTENSION);
	}

	public void testGetViewNameWithDefaultConfiguration() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME + EXTENSION);
		assertViewName(VIEW_NAME);
	}

	public void testGetViewNameWithCustomSeparator() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME + "/fiona" + EXTENSION);
		this.translator.setSeparator("_");
		assertViewName(VIEW_NAME + "_fiona");
	}

	public void testGetViewNameWithNoExtension() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		assertViewName(VIEW_NAME);
	}

	public void testGetViewNameWithPrefix() throws Exception {
		final String prefix = "fiona_";
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		this.translator.setPrefix(prefix);
		assertViewName(prefix + VIEW_NAME);
	}

	public void testGetViewNameWithNullPrefix() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		this.translator.setPrefix(null);
		assertViewName(VIEW_NAME);
	}

	public void testGetViewNameWithSuffix() throws Exception {
		final String suffix = ".fiona";
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		this.translator.setSuffix(suffix);
		assertViewName(VIEW_NAME + suffix);
	}

	public void testGetViewNameWithNullSuffix() throws Exception {
		request.setRequestURI(CONTEXT_PATH + VIEW_NAME);
		this.translator.setSuffix(null);
		assertViewName(VIEW_NAME);
	}

	public void testTrySetUrlPathHelperToNull() throws Exception {
		try {
			this.translator.setUrlPathHelper(null);
		}
		catch (IllegalArgumentException expected) {
		}
	}


	private void assertViewName(String expectedViewName) {
		String actualViewName = this.translator.getViewName(this.request);
		assertNotNull(actualViewName);
		assertEquals("Did not get the expected viewName from the DefaultRequestToViewNameTranslator.getViewName(..)",
				expectedViewName, actualViewName);
	}

}
