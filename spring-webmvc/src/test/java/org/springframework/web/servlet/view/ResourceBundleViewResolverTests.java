/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ResourceBundleViewResolverTests {

	/** Comes from this package */
	private static String PROPS_FILE = "org.springframework.web.servlet.view.testviews";

	private final ResourceBundleViewResolver rb = new ResourceBundleViewResolver();

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();


	@Before
	public void setUp() throws Exception {
		rb.setBasename(PROPS_FILE);
		rb.setCache(getCache());
		rb.setDefaultParentView("testParent");

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


	@Test
	public void parentsAreAbstract() throws Exception {
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

	@Test
	public void debugViewEnglish() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.ENGLISH);
		assertThat(v, instanceOf(InternalResourceView.class));
		InternalResourceView jv = (InternalResourceView) v;
		assertEquals("debugView must have correct URL", "jsp/debug/debug.jsp", jv.getUrl());

		Map<String, Object> m = jv.getStaticAttributes();
		assertEquals("Must have 2 static attributes", 2, m.size());
		assertEquals("attribute foo", "bar", m.get("foo"));
		assertEquals("attribute postcode", "SE10 9JY", m.get("postcode"));

		assertEquals("Correct default content type", AbstractView.DEFAULT_CONTENT_TYPE, jv.getContentType());
	}

	@Test
	public void debugViewFrench() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.FRENCH);
		assertThat(v, instanceOf(InternalResourceView.class));
		InternalResourceView jv = (InternalResourceView) v;
		assertEquals("French debugView must have correct URL", "jsp/debug/deboug.jsp", jv.getUrl());
		assertEquals("Correct overridden (XML) content type", "text/xml;charset=ISO-8859-1", jv.getContentType());
	}

	@Test
	public void eagerInitialization() throws Exception {
		ResourceBundleViewResolver rb = new ResourceBundleViewResolver();
		rb.setBasename(PROPS_FILE);
		rb.setCache(getCache());
		rb.setDefaultParentView("testParent");
		rb.setLocalesToInitialize(new Locale[] {Locale.ENGLISH, Locale.FRENCH});
		rb.setApplicationContext(wac);

		View v = rb.resolveViewName("debugView", Locale.FRENCH);
		assertThat(v, instanceOf(InternalResourceView.class));
		InternalResourceView jv = (InternalResourceView) v;
		assertEquals("French debugView must have correct URL", "jsp/debug/deboug.jsp", jv.getUrl());
		assertEquals("Correct overridden (XML) content type", "text/xml;charset=ISO-8859-1", jv.getContentType());
	}

	@Test
	public void sameBundleOnlyCachedOnce() throws Exception {
		assumeTrue(rb.isCache());

		View v1 = rb.resolveViewName("debugView", Locale.ENGLISH);
		View v2 = rb.resolveViewName("debugView", Locale.UK);
		assertSame(v1, v2);
	}

	@Test
	public void noSuchViewEnglish() throws Exception {
		assertNull(rb.resolveViewName("xxxxxxweorqiwuopeir", Locale.ENGLISH));
	}

	@Test
	public void onSetContextCalledOnce() throws Exception {
		TestView tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		assertEquals("test has correct name", "test", tv.getBeanName());
		assertEquals("test should have been initialized once, not ", 1, tv.initCount);
	}

	@Test(expected = MissingResourceException.class)
	public void noSuchBasename() throws Exception {
		rb.setBasename("weoriwoierqupowiuer");
		rb.resolveViewName("debugView", Locale.ENGLISH);
	}


	static class TestView extends AbstractView {

		public int initCount;

		public void setLocation(Resource location) {
			if (!(location instanceof ServletContextResource)) {
				throw new IllegalArgumentException("Expecting ServletContextResource, not " + location.getClass().getName());
			}
		}

		@Override
		protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
				HttpServletResponse response) {
		}

		@Override
		protected void initApplicationContext() {
			++initCount;
		}
	}

}
