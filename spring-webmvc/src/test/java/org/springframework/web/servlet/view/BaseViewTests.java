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

package org.springframework.web.servlet.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Base tests for {@link AbstractView}.
 *
 * <p>Not called {@code AbstractViewTests} since doing so would cause it
 * to be ignored in the Gradle build.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 */
public class BaseViewTests {

	@Test
	public void renderWithoutStaticAttributes() throws Exception {
		WebApplicationContext wac = mock();
		given(wac.getServletContext()).willReturn(new MockServletContext());

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);

		// Check superclass handles duplicate init
		tv.setApplicationContext(wac);
		tv.setApplicationContext(wac);

		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar");
		model.put("something", new Object());
		tv.render(model, request, response);

		checkContainsAll(model, tv.model);

		assertThat(tv.initialized).isTrue();
	}

	/**
	 * Test attribute passing, NOT CSV parsing.
	 */
	@Test
	public void renderWithStaticAttributesNoCollision() throws Exception {
		WebApplicationContext wac = mock();
		given(wac.getServletContext()).willReturn(new MockServletContext());

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);

		tv.setApplicationContext(wac);
		Properties p = new Properties();
		p.setProperty("foo", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);

		Map<String, Object> model = new HashMap<>();
		model.put("one", new HashMap<>());
		model.put("two", new Object());
		tv.render(model, request, response);

		checkContainsAll(model, tv.model);
		checkContainsAll(p, tv.model);

		assertThat(tv.initialized).isTrue();
	}

	@Test
	public void pathVarsOverrideStaticAttributes() throws Exception {
		WebApplicationContext wac = mock();
		given(wac.getServletContext()).willReturn(new MockServletContext());

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();

		TestView tv = new TestView(wac);
		tv.setApplicationContext(wac);

		Properties p = new Properties();
		p.setProperty("one", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);

		Map<String, Object> pathVars = new HashMap<>();
		pathVars.put("one", new HashMap<>());
		pathVars.put("two", new Object());
		request.setAttribute(View.PATH_VARIABLES, pathVars);

		tv.render(new HashMap<>(), request, response);

		checkContainsAll(pathVars, tv.model);

		assertThat(tv.model).hasSize(3);
		assertThat(tv.model.get("something")).isEqualTo("else");
		assertThat(tv.initialized).isTrue();
	}

	@Test
	public void dynamicModelOverridesStaticAttributesIfCollision() throws Exception {
		WebApplicationContext wac = mock();
		given(wac.getServletContext()).willReturn(new MockServletContext());

		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		TestView tv = new TestView(wac);

		tv.setApplicationContext(wac);
		Properties p = new Properties();
		p.setProperty("one", "bar");
		p.setProperty("something", "else");
		tv.setAttributes(p);

		Map<String, Object> model = new HashMap<>();
		model.put("one", new HashMap<>());
		model.put("two", new Object());
		tv.render(model, request, response);

		// Check it contains all
		checkContainsAll(model, tv.model);

		assertThat(tv.model).hasSize(3);
		assertThat(tv.model.get("something")).isEqualTo("else");
		assertThat(tv.initialized).isTrue();
	}

	@Test
	public void dynamicModelOverridesPathVariables() throws Exception {
		WebApplicationContext wac = mock();
		given(wac.getServletContext()).willReturn(new MockServletContext());

		TestView tv = new TestView(wac);
		tv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, Object> pathVars = new HashMap<>();
		pathVars.put("one", "bar");
		pathVars.put("something", "else");
		request.setAttribute(View.PATH_VARIABLES, pathVars);

		Map<String, Object> model = new HashMap<>();
		model.put("one", new HashMap<>());
		model.put("two", new Object());

		tv.render(model, request, response);

		checkContainsAll(model, tv.model);
		assertThat(tv.model).hasSize(3);
		assertThat(tv.model.get("something")).isEqualTo("else");
		assertThat(tv.initialized).isTrue();
	}

	@Test
	public void ignoresNullAttributes() {
		AbstractView v = new ConcreteView();
		v.setAttributes(null);
		assertThat(v.getStaticAttributes()).isEmpty();
	}

	/**
	 * Test only the CSV parsing implementation.
	 */
	@Test
	public void attributeCSVParsingIgnoresNull() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV(null);
		assertThat(v.getStaticAttributes()).isEmpty();
	}

	@Test
	public void attributeCSVParsingIgnoresEmptyString() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("");
		assertThat(v.getStaticAttributes()).isEmpty();
	}

	/**
	 * Format is attname0={value1},attname1={value1}
	 */
	@Test
	public void attributeCSVParsingValid() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("foo=[bar],king=[kong]");
		assertThat(v.getStaticAttributes()).hasSize(2);
		assertThat(v.getStaticAttributes().get("foo").equals("bar")).isTrue();
		assertThat(v.getStaticAttributes().get("king").equals("kong")).isTrue();
	}

	@Test
	public void attributeCSVParsingValidWithWeirdCharacters() {
		AbstractView v = new ConcreteView();
		String fooval = "owfie   fue&3[][[[2 \n\n \r  \t 8\ufffd3";
		// Also tests empty value
		String kingval = "";
		v.setAttributesCSV("foo=(" + fooval + "),king={" + kingval + "},f1=[we]");
		assertThat(v.getStaticAttributes()).hasSize(3);
		assertThat(v.getStaticAttributes().get("foo").equals(fooval)).isTrue();
		assertThat(v.getStaticAttributes().get("king").equals(kingval)).isTrue();
	}

	@Test
	public void attributeCSVParsingInvalid() {
		AbstractView v = new ConcreteView();
		// No equals
		assertThatIllegalArgumentException().isThrownBy(() ->
			v.setAttributesCSV("fweoiruiu"));

		// No value
		assertThatIllegalArgumentException().isThrownBy(() ->
				v.setAttributesCSV("fweoiruiu="));

		// No closing ]
		assertThatIllegalArgumentException().isThrownBy(() ->
				v.setAttributesCSV("fweoiruiu=["));

		// Second one is bogus
		assertThatIllegalArgumentException().isThrownBy(() ->
				v.setAttributesCSV("fweoiruiu=[de],="));
	}

	@Test
	public void attributeCSVParsingIgnoresTrailingComma() {
		AbstractView v = new ConcreteView();
		v.setAttributesCSV("foo=[de],");
		assertThat(v.getStaticAttributes()).hasSize(1);
	}

	/**
	 * Check that all keys in expected have same values in actual.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void checkContainsAll(Map expected, Map<String, Object> actual) {
		expected.forEach((k, v) -> assertThat(actual.get(k)).as("Values for model key '" + k
						+ "' must match").isEqualTo(expected.get(k)));
	}


	/**
	 * Trivial concrete subclass we can use when we're interested only
	 * in CSV parsing, which doesn't require lifecycle management
	 */
	private static class ConcreteView extends AbstractView {
		// Do-nothing concrete subclass
		@Override
		protected void renderMergedOutputModel(Map<String, Object> model,
				HttpServletRequest request, HttpServletResponse response)

			throws ServletException, IOException {
			throw new UnsupportedOperationException();
		}
	}


	/**
	 * Single threaded subclass of AbstractView to check superclass behavior.
	 */
	private static class TestView extends AbstractView {

		private final WebApplicationContext wac;

		boolean initialized;

		/** Captured model in render */
		Map<String, Object> model;

		TestView(WebApplicationContext wac) {
			this.wac = wac;
		}

		@Override
		protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			this.model = model;
		}

		/**
		 * @see org.springframework.context.support.ApplicationObjectSupport#initApplicationContext()
		 */
		@Override
		protected void initApplicationContext() throws ApplicationContextException {
			if (initialized) {
				throw new RuntimeException("Already initialized");
			}
			this.initialized = true;
			assertThat(getApplicationContext()).isSameAs(wac);
		}
	}

}
