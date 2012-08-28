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

package org.springframework.web.servlet.view;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContextException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * Tests for AbstractView. Not called AbstractViewTests as
 * would otherwise be excluded by Ant build script wildcard.
 *
 * @author Rod Johnson
 */
public class BaseViewTests extends TestCase {

	public void testRenderWithoutStaticAttributes() throws Exception {
		
		WebApplicationContext wac = createMock(WebApplicationContext.class);
		wac.getServletContext();
		expectLastCall().andReturn(new MockServletContext());
		replay(wac);

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);
		
		// Check superclass handles duplicate init
		tv.setApplicationContext(wac);		
		tv.setApplicationContext(wac);
		
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", "bar");
		model.put("something", new Object());
		tv.render(model, request, response);

		// check it contains all
		checkContainsAll(model, tv.model);
		
		assertTrue(tv.inited);
		verify(wac);
	}
	
	/**
	 * Test attribute passing, NOT CSV parsing.
	 */
	public void testRenderWithStaticAttributesNoCollision() throws Exception {
		WebApplicationContext wac = createMock(WebApplicationContext.class);
		wac.getServletContext();
		expectLastCall().andReturn(new MockServletContext());
		replay(wac);

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);
	
		tv.setApplicationContext(wac);
		Properties p = new Properties();	
		p.setProperty("foo", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);	
	
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("one", new HashMap<Object, Object>());
		model.put("two", new Object());
		tv.render(model, request, response);

		// Check it contains all
		checkContainsAll(model, tv.model);
		checkContainsAll(p, tv.model);
	
		assertTrue(tv.inited);
		verify(wac);
	}

	public void testPathVarsOverrideStaticAttributes() throws Exception {
		WebApplicationContext wac = createMock(WebApplicationContext.class);
		wac.getServletContext();
		expectLastCall().andReturn(new MockServletContext());
		replay(wac);

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();

		TestView tv = new TestView(wac);
		tv.setApplicationContext(wac);
		
		Properties p = new Properties();	
		p.setProperty("one", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);	
		
		Map<String, Object> pathVars = new HashMap<String, Object>();
		pathVars.put("one", new HashMap<Object, Object>());
		pathVars.put("two", new Object());
		request.setAttribute(View.PATH_VARIABLES, pathVars);
		
		tv.render(new HashMap<String, Object>(), request, response);

		// Check it contains all
		checkContainsAll(pathVars, tv.model);
		assertTrue(tv.model.size() == 3);
		// will have old something from properties
		assertTrue(tv.model.get("something").equals("else"));

		assertTrue(tv.inited);
		verify(wac);
	}
	
	public void testDynamicModelOverridesStaticAttributesIfCollision() throws Exception {
		WebApplicationContext wac = createMock(WebApplicationContext.class);
		wac.getServletContext();
		expectLastCall().andReturn(new MockServletContext());
		replay(wac);

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);

		tv.setApplicationContext(wac);
		Properties p = new Properties();	
		p.setProperty("one", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);	

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("one", new HashMap<Object, Object>());
		model.put("two", new Object());
		tv.render(model, request, response);

		// Check it contains all
		checkContainsAll(model, tv.model);
		assertTrue(tv.model.size() == 3);
		// will have old something from properties
		assertTrue(tv.model.get("something").equals("else"));

		assertTrue(tv.inited);
		verify(wac);
	}

	public void testDynamicModelOverridesPathVariables() throws Exception {
		WebApplicationContext wac = createMock(WebApplicationContext.class);
		wac.getServletContext();
		expectLastCall().andReturn(new MockServletContext());
		replay(wac);

		TestView tv = new TestView(wac);
		tv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, Object> pathVars = new HashMap<String, Object>();
		pathVars.put("one", "bar");
		pathVars.put("something", "else");
		request.setAttribute(View.PATH_VARIABLES, pathVars);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("one", new HashMap<Object, Object>());
		model.put("two", new Object());

		tv.render(model, request, response);

		// Check it contains all
		checkContainsAll(model, tv.model);
		assertEquals(3, tv.model.size());
		// will have old something from path variables
		assertTrue(tv.model.get("something").equals("else"));

		assertTrue(tv.inited);
		verify(wac);
	}
	
	public void testIgnoresNullAttributes() {
		AbstractView v = new ConcreteView();
		v.setAttributes(null);
		assertTrue(v.getStaticAttributes().size() == 0);
	}
	
	/**
	 * Test only the CSV parsing implementation.
	 */
	public void testAttributeCSVParsingIgnoresNull() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV(null);
		assertTrue(v.getStaticAttributes().size() == 0);
	}
	
	public void testAttributeCSVParsingIgnoresEmptyString() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("");
		assertTrue(v.getStaticAttributes().size() == 0);
	}
	
	/**
	 * Format is attname0={value1},attname1={value1}
	 */
	public void testAttributeCSVParsingValid() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("foo=[bar],king=[kong]");
		assertTrue(v.getStaticAttributes().size() == 2);
		assertTrue(v.getStaticAttributes().get("foo").equals("bar"));
		assertTrue(v.getStaticAttributes().get("king").equals("kong"));
	}
	
	public void testAttributeCSVParsingValidWithWeirdCharacters() {
		AbstractView v = new ConcreteView();
		String fooval = "owfie   fue&3[][[[2 \n\n \r  \t 8\ufffd3";
		// Also tests empty value
		String kingval = "";
		v.setAttributesCSV("foo=(" + fooval + "),king={" + kingval + "},f1=[we]");
		assertTrue(v.getStaticAttributes().size() == 3);
		assertTrue(v.getStaticAttributes().get("foo").equals(fooval));
		assertTrue(v.getStaticAttributes().get("king").equals(kingval));
	}
	
	public void testAttributeCSVParsingInvalid() {
		AbstractView v = new ConcreteView();
		try {
			// No equals
			v.setAttributesCSV("fweoiruiu");
			fail();
		}
		catch (IllegalArgumentException ex) {
		}
		
		try {
			// No value
			v.setAttributesCSV("fweoiruiu=");
			fail();
		}
		catch (IllegalArgumentException ex) {
		}
		
		try {
			// No closing ]
			v.setAttributesCSV("fweoiruiu=[");
			fail();
		}
		catch (IllegalArgumentException ex) {
		}
		try {
			// Second one is bogus
			v.setAttributesCSV("fweoiruiu=[de],=");
			fail();
		}
		catch (IllegalArgumentException ex) {
		}
	}
	
	public void testAttributeCSVParsingIgoresTrailingComma() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("foo=[de],");
		assertTrue(v.getStaticAttributes().size() == 1);
	}
	
	/**
	 * Check that all keys in expected have same values in actual
	 * @param expected
	 * @param actual
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void checkContainsAll(Map expected, Map<String, Object> actual) {
		Set<String> keys = expected.keySet();
		for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			//System.out.println("Checking model key " + key);
			assertTrue("Value for model key '" + key + "' must match", actual.get(key) == expected.get(key));			
		}
	}
	
	/**
	 * Trivial concrete subclass we can use when we're interested only
	 * in CSV parsing, which doesn't require lifecycle management
	 */
	private class ConcreteView extends AbstractView {
		// Do-nothing concrete subclass
		protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Single threaded subclass of AbstractView to check superclass
	 * behaviour
	 */
	private class TestView extends AbstractView {
		private WebApplicationContext wac;
		public boolean inited;
		
		/** Captured model in render */
		public Map<String, Object> model;
		
		public TestView(WebApplicationContext wac) {
			this.wac = wac;
			
		}
		protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
				// do nothing
				this.model = model;
			}
		/**
		 * @see org.springframework.context.support.ApplicationObjectSupport#initApplicationContext()
		 */
		protected void initApplicationContext() throws ApplicationContextException {
			if (inited)
				throw new RuntimeException("Already initialized");
			this.inited = true;
			assertTrue(getApplicationContext() == wac);
		}

	}

}
