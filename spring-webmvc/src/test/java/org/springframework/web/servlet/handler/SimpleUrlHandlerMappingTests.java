/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

/**
 * Tests for {@link SimpleUrlHandlerMapping}.
 *
 * @author Brian Clozel
 */
class SimpleUrlHandlerMappingTests {

	@Test
	void shouldFailWhenHandlerBeanNotFound() {
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping(Map.of("/welcome.html", "mainController"));
		assertThatThrownBy(() -> handlerMapping.setApplicationContext(new StaticApplicationContext()))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
	}

	@Test
	void newlineInRequestShouldMatch() throws Exception {
		Object controller = new Object();
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		urlPathHelper.setUrlDecode(false);
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping(Collections.singletonMap("/*/baz", controller));
		mapping.setUrlPathHelper(urlPathHelper);
		mapping.setApplicationContext(new StaticApplicationContext());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo%0a%0dbar/baz");

		HandlerExecutionChain hec = mapping.getHandler(request);
		assertThat(hec).isNotNull();
		assertThat(hec.getHandler()).isSameAs(controller);
	}

	@HandlerMappingsTest
	void resolveFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setUrlMap(Map.of("/welcome.html", "mainController"));
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/welcome.html", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.html");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolvePatternFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setUrlMap(Map.of("/welcome*", "mainController"));
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/welcome.x", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome.x");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolvePathWithParamFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setUrlMap(Map.of("/welcome.x", "mainController"));
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/welcome.x;jsessionid=123", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.x");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolvePathWithContextFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setUrlMap(Map.of("/welcome.x", "mainController"));
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/app", "/welcome.x", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.x");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolvePathWithIncludeFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setUrlMap(Map.of("/welcome.html", "mainController"));
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/original.html", usePathPatterns);
		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/welcome.html");
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.html");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolveDefaultPathFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("mainController", Object.class);
		Object mainController = applicationContext.getBean("mainController");
		handlerMapping.setDefaultHandler(mainController);
		handlerMapping.setApplicationContext(applicationContext);

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(mainController);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(mainController);
	}

	@HandlerMappingsTest
	void resolveParameterizedControllerFromMap(SimpleUrlHandlerMapping handlerMapping) throws Exception {
		ParameterizableViewController viewController = new ParameterizableViewController();
		viewController.setView(new RedirectView("/after/{variable}"));
		viewController.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
		handlerMapping.setUrlMap(Map.of("/before/{variable}", viewController));
		handlerMapping.setApplicationContext(new StaticApplicationContext());

		boolean usePathPatterns = handlerMapping.getPatternParser() != null;
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/before/test", usePathPatterns);
		HandlerExecutionChain chain = getHandler(handlerMapping, request);

		assertThat(chain.getHandler()).isSameAs(viewController);
		@SuppressWarnings("unchecked")
		Map<String, String> variables = (Map<String, String>) request.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		assertThat(variables).containsEntry("variable", "test");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(viewController);
	}

	private HandlerExecutionChain getHandler(HandlerMapping mapping, MockHttpServletRequest request) throws Exception {
		HandlerExecutionChain chain = mapping.getHandler(request);
		Assert.notNull(chain, "No handler found for request: " + request.getRequestURI());
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			interceptor.preHandle(request, null, chain.getHandler());
		}
		return chain;
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ParameterizedTest(name="[{index}] {0}")
	@MethodSource("handlerMappings")
	@interface HandlerMappingsTest {
	}

	static Stream<Arguments> handlerMappings() {
		SimpleUrlHandlerMapping defaultConfig = new SimpleUrlHandlerMapping();
		SimpleUrlHandlerMapping antPatternConfig = new SimpleUrlHandlerMapping();
		antPatternConfig.setPatternParser(null);
		return Stream.of(
				arguments(named("with PathPattern", defaultConfig)),
				arguments(named("with AntPathMatcher", antPatternConfig))
			);
	}

}
