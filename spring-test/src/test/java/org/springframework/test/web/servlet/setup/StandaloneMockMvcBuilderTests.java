/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.EnumSet;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.impl.UnknownSerializer;

import org.springframework.http.support.JacksonHandlerInstantiator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StandaloneMockMvcBuilder}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sebastien Deleuze
 */
class StandaloneMockMvcBuilderTests {

	@Test  // SPR-10825
	void placeHoldersInRequestMapping() throws Exception {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerExecutionChain chain = hm.getHandler(request);

		assertThat(chain).isNotNull();
		assertThat(((HandlerMethod) chain.getHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
	}

	@Test
	void apiVersionStrategySet() {
		ApiVersionStrategy versionStrategy = mock(ApiVersionStrategy.class);

		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder();
		builder.setApiVersionStrategy(versionStrategy);
		builder.build();

		assertThat(builder.wac.getBean(RequestMappingHandlerMapping.class).getApiVersionStrategy())
				.isSameAs(versionStrategy);
	}

	@Test  // SPR-12553
	void applicationContextAttribute() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		WebApplicationContext wac = builder.initWebAppContext();
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(wac.getServletContext())).isEqualTo(wac);
	}

	@Test
	void addFiltersFiltersNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters((Filter[]) null));
	}

	@Test
	void addFiltersFiltersContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters(new ContinueFilter(), null));
	}

	@Test
	void addFilterPatternsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String[]) null));
	}

	@Test
	void addFilterPatternContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String) null));
	}

	@Test
	void addFilterWithInitParams() throws ServletException {
		Filter filter = mock(Filter.class);
		ArgumentCaptor<FilterConfig> captor = ArgumentCaptor.forClass(FilterConfig.class);

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PersonController())
				.addFilter(filter, "testFilter", Map.of("p", "v"), EnumSet.of(DispatcherType.REQUEST), "/")
				.build();

		verify(filter, times(1)).init(captor.capture());
		assertThat(captor.getValue().getInitParameter("p")).isEqualTo("v");

		// gh-33252

		assertThat(mockMvc.getDispatcherServlet().getServletContext().getFilterRegistrations())
				.hasSize(1).containsKey("testFilter");
	}

	@Test  // SPR-13375
	@SuppressWarnings("rawtypes")
	void springHandlerInstantiator() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
		builder.build();
		JacksonHandlerInstantiator instantiator = new JacksonHandlerInstantiator(builder.wac.getAutowireCapableBeanFactory());
		ValueSerializer serializer = instantiator.serializerInstance(null, null, UnknownSerializer.class);
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


	private static class ContinueFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(request, response);
		}
	}

}
