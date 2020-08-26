/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StandaloneMockMvcBuilder}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sebastien Deleuze
 */
public class StandaloneMockMvcBuilderTests {

	@Test  // SPR-10825
	public void placeHoldersInRequestMapping() throws Exception {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerExecutionChain chain = hm.getHandler(request);

		assertThat(chain).isNotNull();
		assertThat(((HandlerMethod) chain.getHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
	}

	@Test  // SPR-13637
	@SuppressWarnings("deprecation")
	public void suffixPatternMatch() throws Exception {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
		builder.setUseSuffixPatternMatch(false);
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
		HandlerExecutionChain chain = hm.getHandler(request);
		assertThat(chain).isNotNull();
		assertThat(((HandlerMethod) chain.getHandler()).getMethod().getName()).isEqualTo("persons");

		request = new MockHttpServletRequest("GET", "/persons.xml");
		chain = hm.getHandler(request);
		assertThat(chain).isNull();
	}

	@Test  // SPR-12553
	public void applicationContextAttribute() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		WebApplicationContext  wac = builder.initWebAppContext();
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(wac.getServletContext())).isEqualTo(wac);
	}

	@Test
	public void addFiltersFiltersNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters((Filter[]) null));
	}

	@Test
	public void addFiltersFiltersContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters(new ContinueFilter(), (Filter) null));
	}

	@Test
	public void addFilterPatternsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String[]) null));
	}

	@Test
	public void addFilterPatternContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String) null));
	}

	@Test  // SPR-13375
	@SuppressWarnings("rawtypes")
	public void springHandlerInstantiator() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
		builder.build();
		SpringHandlerInstantiator instantiator = new SpringHandlerInstantiator(builder.wac.getAutowireCapableBeanFactory());
		JsonSerializer serializer = instantiator.serializerInstance(null, null, UnknownSerializer.class);
		assertThat(serializer).isNotNull();
	}


	@Controller
	private static class PlaceholderController {

		@RequestMapping(value = "${sys.login.ajax}")
		private void handleWithPlaceholders() { }
	}


	private static class TestStandaloneMockMvcBuilder extends StandaloneMockMvcBuilder {

		private WebApplicationContext wac;

		private TestStandaloneMockMvcBuilder(Object... controllers) {
			super(controllers);
		}

		@Override
		protected WebApplicationContext initWebAppContext() {
			this.wac = super.initWebAppContext();
			return this.wac;
		}
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/persons")
		public String persons() {
			return null;
		}

		@RequestMapping(value="/forward")
		public String forward() {
			return "forward:/persons";
		}
	}


	private class ContinueFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(request, response);
		}
	}

}
