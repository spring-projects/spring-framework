/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ResourceBundleViewResolverTests extends TestCase {

	/** Comes from this package */
	private static String PROPS_FILE = "org.springframework.web.servlet.view.testviews";

	private ResourceBundleViewResolver rb;

	private StaticWebApplicationContext wac;


	@Override
	protected void setUp() throws Exception {
		rb = new ResourceBundleViewResolver();
		rb.setBasename(PROPS_FILE);
		rb.setCache(getCache());
		rb.setDefaultParentView("testParent");

		wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();

		// This will be propagated to views, so we need it.
		rb.setApplicationContext(wac);
	}

	/**
	 * Not a constant: allows overrides.
	 * Controls whether to cache views.
	 */
	protected boolean getCache() {
		return true;
	}


	public void testParentsAreAbstract() throws Exception {
		try {
			rb.resolveViewName("debug.Parent", Locale.ENGLISH);
			fail("Should have thrown BeanIsAbstractException");
		}
		catch (BeanIsAbstractException ex) {
			// expected
		}
		try {
			rb.resolveViewName("testParent", Locale.ENGLISH);
			fail("Should have thrown BeanIsAbstractException");
		}
		catch (BeanIsAbstractException ex) {
			// expected
		}
	}

	public void testDebugViewEnglish() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.ENGLISH);
		assertTrue("debugView must be of type InternalResourceView", v instanceof InternalResourceView);
		InternalResourceView jv = (InternalResourceView) v;
		assertTrue("debugView must have correct URL", "jsp/debug/debug.jsp".equals(jv.getUrl()));

		Map m = jv.getStaticAttributes();
		assertTrue("Must have 2 static attributes, not " + m.size(), m.size() == 2);
		assertTrue("attribute foo = bar, not '" + m.get("foo") + "'", m.get("foo").equals("bar"));
		assertTrue("attribute postcode = SE10 9JY", m.get("postcode").equals("SE10 9JY"));

		assertTrue("Correct default content type", jv.getContentType().equals(AbstractView.DEFAULT_CONTENT_TYPE));
	}

	public void testDebugViewFrench() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.FRENCH);
		assertTrue("French debugView must be of type InternalResourceView", v instanceof InternalResourceView);
		InternalResourceView jv = (InternalResourceView) v;
		assertTrue("French debugView must have correct URL", "jsp/debug/deboug.jsp".equals(jv.getUrl()));
		assertTrue(
			"Correct overridden (XML) content type, not '" + jv.getContentType() + "'",
			jv.getContentType().equals("text/xml;charset=ISO-8859-1"));
	}

	public void testEagerInitialization() throws Exception {
		ResourceBundleViewResolver rb = new ResourceBundleViewResolver();
		rb.setBasename(PROPS_FILE);
		rb.setCache(getCache());
		rb.setDefaultParentView("testParent");
		rb.setLocalesToInitialize(new Locale[] {Locale.ENGLISH, Locale.FRENCH});
		rb.setApplicationContext(wac);

		View v = rb.resolveViewName("debugView", Locale.FRENCH);
		assertTrue("French debugView must be of type InternalResourceView", v instanceof InternalResourceView);
		InternalResourceView jv = (InternalResourceView) v;
		assertTrue("French debugView must have correct URL", "jsp/debug/deboug.jsp".equals(jv.getUrl()));
		assertTrue(
			"Correct overridden (XML) content type, not '" + jv.getContentType() + "'",
			jv.getContentType().equals("text/xml;charset=ISO-8859-1"));
	}

	public void testSameBundleOnlyCachedOnce() throws Exception {
		if (rb.isCache()) {
			View v1 = rb.resolveViewName("debugView", Locale.ENGLISH);
			View v2 = rb.resolveViewName("debugView", Locale.UK);
			assertSame(v1, v2);
		}
	}

	public void testNoSuchViewEnglish() throws Exception {
		View v = rb.resolveViewName("xxxxxxweorqiwuopeir", Locale.ENGLISH);
		assertTrue(v == null);
	}

	public void testOnSetContextCalledOnce() throws Exception {
		TestView tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		assertTrue("test has correct name", "test".equals(tv.getBeanName()));
		assertTrue("test should have been initialized once, not " + tv.initCount + " times", tv.initCount == 1);
	}

	public void testNoSuchBasename() throws Exception {
		try {
			rb.setBasename("weoriwoierqupowiuer");
			rb.resolveViewName("debugView", Locale.ENGLISH);
			fail("No such basename: all requests should fail with exception");
		}
		catch (MissingResourceException ex) {
			// OK
		}
	}


	public static class TestView extends AbstractView {

		public int initCount;

		public void setLocation(Resource location) {
			if (!(location instanceof ServletContextResource)) {
				throw new IllegalArgumentException("Expecting ServletContextResource, not " + location.getClass().getName());
			}
		}

		@Override
		protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) {
		}

		@Override
		protected void initApplicationContext() {
			++initCount;
		}
	}

}
