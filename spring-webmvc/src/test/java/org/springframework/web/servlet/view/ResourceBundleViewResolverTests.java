/*
 * Copyright 2002-2020 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class ResourceBundleViewResolverTests {

	/** Comes from this package */
	private static String PROPS_FILE = "org.springframework.web.servlet.view.testviews";

	private final ResourceBundleViewResolver rb = new ResourceBundleViewResolver();

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();


	@BeforeEach
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
		assertThatExceptionOfType(BeanIsAbstractException.class).isThrownBy(() ->
				rb.resolveViewName("debug.Parent", Locale.ENGLISH));
		assertThatExceptionOfType(BeanIsAbstractException.class).isThrownBy(() ->
				rb.resolveViewName("testParent", Locale.ENGLISH));
	}

	@Test
	public void debugViewEnglish() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.ENGLISH);
		assertThat(v).isInstanceOf(InternalResourceView.class);
		InternalResourceView jv = (InternalResourceView) v;
		assertThat(jv.getUrl()).as("debugView must have correct URL").isEqualTo("jsp/debug/debug.jsp");

		Map<String, Object> m = jv.getStaticAttributes();
		assertThat(m.size()).as("Must have 2 static attributes").isEqualTo(2);
		assertThat(m.get("foo")).as("attribute foo").isEqualTo("bar");
		assertThat(m.get("postcode")).as("attribute postcode").isEqualTo("SE10 9JY");

		assertThat(jv.getContentType()).as("Correct default content type").isEqualTo(AbstractView.DEFAULT_CONTENT_TYPE);
	}

	@Test
	public void debugViewFrench() throws Exception {
		View v = rb.resolveViewName("debugView", Locale.FRENCH);
		assertThat(v).isInstanceOf(InternalResourceView.class);
		InternalResourceView jv = (InternalResourceView) v;
		assertThat(jv.getUrl()).as("French debugView must have correct URL").isEqualTo("jsp/debug/deboug.jsp");
		assertThat(jv.getContentType()).as("Correct overridden (XML) content type").isEqualTo("text/xml;charset=ISO-8859-1");
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
		assertThat(v).isInstanceOf(InternalResourceView.class);
		InternalResourceView jv = (InternalResourceView) v;
		assertThat(jv.getUrl()).as("French debugView must have correct URL").isEqualTo("jsp/debug/deboug.jsp");
		assertThat(jv.getContentType()).as("Correct overridden (XML) content type").isEqualTo("text/xml;charset=ISO-8859-1");
	}

	@Test
	public void sameBundleOnlyCachedOnce() throws Exception {
		assumeTrue(rb.isCache());

		View v1 = rb.resolveViewName("debugView", Locale.ENGLISH);
		View v2 = rb.resolveViewName("debugView", Locale.UK);
		assertThat(v2).isSameAs(v1);
	}

	@Test
	public void noSuchViewEnglish() throws Exception {
		assertThat((Object) rb.resolveViewName("xxxxxxweorqiwuopeir", Locale.ENGLISH)).isNull();
	}

	@Test
	public void onSetContextCalledOnce() throws Exception {
		TestView tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		tv = (TestView) rb.resolveViewName("test", Locale.ENGLISH);
		assertThat(tv.getBeanName()).as("test has correct name").isEqualTo("test");
		assertThat(tv.initCount).as("test should have been initialized once, not ").isEqualTo(1);
	}

	@Test
	public void noSuchBasename() throws Exception {
		rb.setBasename("weoriwoierqupowiuer");
		assertThatExceptionOfType(MissingResourceException.class).isThrownBy(() ->
				rb.resolveViewName("debugView", Locale.ENGLISH));
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
