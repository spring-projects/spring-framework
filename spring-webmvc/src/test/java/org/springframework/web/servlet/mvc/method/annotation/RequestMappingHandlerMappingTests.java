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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Olga Maciaszek-Sharma
 */
class RequestMappingHandlerMappingTests {

	@SuppressWarnings("unused")
	static Stream<Arguments> pathPatternsArguments() {
		StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
		StaticWebApplicationContext wac2 = new StaticWebApplicationContext();

		RequestMappingHandlerMapping mapping1 = new RequestMappingHandlerMapping();
		mapping1.setApplicationContext(wac1);

		RequestMappingHandlerMapping mapping2 = new RequestMappingHandlerMapping();
		mapping2.setPatternParser(null);
		mapping2.setApplicationContext(wac2);

		return Stream.of(
				arguments(named("PathPatternParser", mapping1), wac1),
				arguments(named("AntPathMatcher", mapping2), wac2)
			);
	}

	@Test
	void builderConfiguration() {
		RequestMappingHandlerMapping mapping = createMapping();

		RequestMappingInfo.BuilderConfiguration config = mapping.getBuilderConfiguration();
		assertThat(config).isNotNull();

		mapping.afterPropertiesSet();
		assertThat(mapping.getBuilderConfiguration()).isNotNull().isNotSameAs(config);
	}

	@Test
	@SuppressWarnings("deprecation")
	void useRegisteredSuffixPatternMatch() {
		RequestMappingHandlerMapping mapping = createMapping();

		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy =
				new org.springframework.web.accept.PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		mapping.setContentNegotiationManager(manager);
		mapping.setUseRegisteredSuffixPatternMatch(true);
		mapping.afterPropertiesSet();

		assertThat(mapping.useSuffixPatternMatch()).isTrue();
		assertThat(mapping.useRegisteredSuffixPatternMatch()).isTrue();
		assertThat(mapping.getFileExtensions()).isEqualTo(Collections.singletonList("json"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void useRegisteredSuffixPatternMatchInitialization() {
		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy =
				new org.springframework.web.accept.PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		final Set<String> extensions = new HashSet<>();

		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping() {
			@Override
			protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
				extensions.addAll(getFileExtensions());
				return super.getMappingForMethod(method, handlerType);
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("testController", ComposedAnnotationController.class);
		wac.refresh();

		mapping.setContentNegotiationManager(manager);
		mapping.setUseRegisteredSuffixPatternMatch(true);
		mapping.setApplicationContext(wac);
		mapping.afterPropertiesSet();

		assertThat(extensions).containsOnly("json");
	}

	@Test
	@SuppressWarnings("deprecation")
	void suffixPatternMatchSettings() {
		RequestMappingHandlerMapping mapping = createMapping();

		assertThat(mapping.useSuffixPatternMatch()).isFalse();
		assertThat(mapping.useRegisteredSuffixPatternMatch()).isFalse();

		mapping.setUseRegisteredSuffixPatternMatch(false);
		assertThat(mapping.useSuffixPatternMatch())
				.as("'false' registeredSuffixPatternMatch shouldn't impact suffixPatternMatch")
				.isFalse();

		mapping.setUseRegisteredSuffixPatternMatch(true);
		assertThat(mapping.useSuffixPatternMatch())
				.as("'true' registeredSuffixPatternMatch should enable suffixPatternMatch")
				.isTrue();
	}

	@PathPatternsParameterizedTest
	void resolveEmbeddedValuesInPatterns(RequestMappingHandlerMapping mapping) {
		mapping.setEmbeddedValueResolver(value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value);

		String[] patterns = { "/foo", "/${pattern}/bar" };
		String[] result = mapping.resolveEmbeddedValuesInPatterns(patterns);

		assertThat(result).containsExactly("/foo", "/foo/bar");
	}

	@PathPatternsParameterizedTest
	void pathPrefix(RequestMappingHandlerMapping mapping) throws Exception {
		mapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		mapping.setPathPrefixes(Collections.singletonMap(
				"/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));
		mapping.afterPropertiesSet();

		Method method = UserController.class.getMethod("getUser");
		RequestMappingInfo info = mapping.getMappingForMethod(method, UserController.class);

		assertThat(info).isNotNull();
		assertThat(info.getPatternValues()).containsOnly("/api/user/{id}");
	}

	@PathPatternsParameterizedTest // gh-23907
	void pathPrefixPreservesPathMatchingSettings(RequestMappingHandlerMapping mapping) throws Exception {
		mapping.setPathPrefixes(Collections.singletonMap("/api", HandlerTypePredicate.forAnyHandlerType()));
		mapping.afterPropertiesSet();

		Method method = ComposedAnnotationController.class.getMethod("get");
		RequestMappingInfo info = mapping.getMappingForMethod(method, ComposedAnnotationController.class);

		assertThat(info).isNotNull();
		assertThat(info.getActivePatternsCondition()).isNotNull();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/get");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNotNull();

		request = new MockHttpServletRequest("GET", "/api/get.pdf");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNull();
	}

	private void initRequestPath(RequestMappingHandlerMapping mapping, MockHttpServletRequest request) {
		PathPatternParser parser = mapping.getPatternParser();
		if (parser != null) {
			ServletRequestPathUtils.parseAndCache(request);
		}
		else {
			mapping.getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	@PathPatternsParameterizedTest
	void resolveRequestMappingViaComposedAnnotation(RequestMappingHandlerMapping mapping) {
		RequestMappingInfo info = assertComposedAnnotationMapping(
				mapping, "postJson", "/postJson", RequestMethod.POST);

		Set<MediaType> consumableMediaTypes = info.getConsumesCondition().getConsumableMediaTypes();
		Set<MediaType> producibleMediaTypes = info.getProducesCondition().getProducibleMediaTypes();

		assertThat(consumableMediaTypes).singleElement().hasToString(MediaType.APPLICATION_JSON_VALUE);
		assertThat(producibleMediaTypes).singleElement().hasToString(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test // SPR-14988
	void getMappingOverridesConsumesFromTypeLevelAnnotation() {
		RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(RequestMethod.POST);

		ConsumesRequestCondition condition = requestMappingInfo.getConsumesCondition();
		assertThat(condition.getConsumableMediaTypes()).containsOnly(MediaType.APPLICATION_XML);
	}

	@PathPatternsParameterizedTest // gh-22010
	void consumesWithOptionalRequestBody(RequestMappingHandlerMapping mapping, StaticWebApplicationContext wac) {
		wac.registerSingleton("testController", ComposedAnnotationController.class);
		wac.refresh();
		mapping.afterPropertiesSet();
		RequestMappingInfo result = mapping.getHandlerMethods().keySet().stream()
				.filter(info -> info.getPatternValues().equals(Collections.singleton("/post")))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No /post"));

		assertThat(result.getConsumesCondition().isBodyRequired()).isFalse();
	}

	@Test
	void getMapping() {
		assertComposedAnnotationMapping(RequestMethod.GET);
	}

	@Test
	void postMapping() {
		assertComposedAnnotationMapping(RequestMethod.POST);
	}

	@Test
	void putMapping() {
		assertComposedAnnotationMapping(RequestMethod.PUT);
	}

	@Test
	void deleteMapping() {
		assertComposedAnnotationMapping(RequestMethod.DELETE);
	}

	@Test
	void patchMapping() {
		assertComposedAnnotationMapping(RequestMethod.PATCH);
	}

	@Test  // gh-32049
	void httpExchangeWithMultipleAnnotationsAtClassLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = MultipleClassLevelAnnotationsHttpExchangeController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		assertThatIllegalStateException()
				.isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
				.withMessageContainingAll(
					"Multiple @HttpExchange annotations found on " + controllerClass,
					HttpExchange.class.getSimpleName(),
					ExtraHttpExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32049
	void httpExchangeWithMultipleAnnotationsAtMethodLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = MultipleMethodLevelAnnotationsHttpExchangeController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		assertThatIllegalStateException()
				.isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
				.withMessageContainingAll(
					"Multiple @HttpExchange annotations found on " + method,
					PostExchange.class.getSimpleName(),
					PutExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32065
	void httpExchangeWithMixedAnnotationsAtClassLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = MixedClassLevelAnnotationsController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		assertThatIllegalStateException()
				.isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
				.withMessageContainingAll(
					controllerClass.getName(),
					"is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
					RequestMapping.class.getSimpleName(),
					HttpExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32065
	void httpExchangeWithMixedAnnotationsAtMethodLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = MixedMethodLevelAnnotationsController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		assertThatIllegalStateException()
				.isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
				.withMessageContainingAll(
					method.toString(),
					"is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
					PostMapping.class.getSimpleName(),
					PostExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32065
	void httpExchangeAnnotationsOverriddenAtClassLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = ClassLevelOverriddenHttpExchangeAnnotationsController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);

		assertThat(info).isNotNull();
		assertThat(info.getActivePatternsCondition()).isNotNull();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/service/postExchange");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNull();

		request = new MockHttpServletRequest("POST", "/controller/postExchange");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNotNull();
	}

	@Test  // gh-32065
	void httpExchangeAnnotationsOverriddenAtMethodLevel() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		Class<?> controllerClass = MethodLevelOverriddenHttpExchangeAnnotationsController.class;
		Method method = controllerClass.getDeclaredMethod("post");

		RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);

		assertThat(info).isNotNull();
		assertThat(info.getActivePatternsCondition()).isNotNull();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/service/postExchange");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNull();

		request = new MockHttpServletRequest("POST", "/controller/postMapping");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNotNull();
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void httpExchangeWithDefaultValues() throws NoSuchMethodException {
		RequestMappingHandlerMapping mapping = createMapping();

		RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
				HttpExchangeController.class.getMethod("defaultValuesExchange"),
				HttpExchangeController.class);

		assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
				.extracting(PathPattern::toString)
				.containsOnly("/exchange");

		assertThat(mappingInfo.getMethodsCondition().getMethods()).isEmpty();
		assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();
		assertThat(mappingInfo.getHeadersCondition().getExpressions()).isEmpty();
		assertThat(mappingInfo.getConsumesCondition().getExpressions()).isEmpty();
		assertThat(mappingInfo.getProducesCondition().getExpressions()).isEmpty();
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void httpExchangeWithCustomValues() throws Exception {
		RequestMappingHandlerMapping mapping = createMapping();

		RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
				HttpExchangeController.class.getMethod("customValuesExchange"),
				HttpExchangeController.class);

		assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
				.extracting(PathPattern::toString)
				.containsOnly("/exchange/custom");

		assertThat(mappingInfo.getMethodsCondition().getMethods()).containsOnly(RequestMethod.POST);
		assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();
		assertThat(mappingInfo.getHeadersCondition().getExpressions()).isEmpty();

		assertThat(mappingInfo.getConsumesCondition().getExpressions())
				.extracting(MediaTypeExpression::getMediaType)
				.containsOnly(MediaType.APPLICATION_JSON);

		assertThat(mappingInfo.getProducesCondition().getExpressions())
				.extracting(MediaTypeExpression::getMediaType)
				.containsOnly(MediaType.valueOf("text/plain;charset=UTF-8"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void httpExchangeWithCustomHeaders() throws Exception {
		RequestMappingHandlerMapping mapping = createMapping();

		RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
				HttpExchangeController.class.getMethod("customHeadersExchange"),
				HttpExchangeController.class);

		assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
				.extracting(PathPattern::toString)
				.containsOnly("/exchange/headers");

		assertThat(mappingInfo.getMethodsCondition().getMethods()).containsOnly(RequestMethod.GET);
		assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();

		assertThat(mappingInfo.getHeadersCondition().getExpressions().stream().map(Object::toString))
				.containsExactly("h1=hv1", "!h2");
	}

	private static RequestMappingHandlerMapping createMapping() {
		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setApplicationContext(new StaticWebApplicationContext());
		mapping.afterPropertiesSet();
		return mapping;
	}

	private static RequestMappingInfo assertComposedAnnotationMapping(RequestMethod requestMethod) {
		RequestMappingHandlerMapping mapping = createMapping();

		String methodName = requestMethod.name().toLowerCase();
		String path = "/" + methodName;

		return assertComposedAnnotationMapping(mapping, methodName, path, requestMethod);
	}

	private static RequestMappingInfo assertComposedAnnotationMapping(
			RequestMappingHandlerMapping mapping, String methodName, String path, RequestMethod requestMethod) {

		Class<?> clazz = ComposedAnnotationController.class;
		Method method = ClassUtils.getMethod(clazz, methodName, (Class<?>[]) null);
		RequestMappingInfo info = mapping.getMappingForMethod(method, clazz);

		assertThat(info).isNotNull();

		Set<String> paths = info.getPatternValues();
		assertThat(paths).containsOnly(path);

		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		assertThat(methods).containsOnly(requestMethod);

		return info;
	}


	@Controller
	// gh-31962: The presence of multiple @RequestMappings is intentional.
	@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@ExtraRequestMapping
	static class ComposedAnnotationController {

		@RequestMapping
		public void handle() {
		}

		@PostJson("/postJson")
		public void postJson() {
		}

		@GetMapping("/get")
		public void get() {
		}

		@PostMapping(path = "/post", consumes = MediaType.APPLICATION_XML_VALUE)
		public void post(@RequestBody(required = false) Foo foo) {
		}

		// gh-31962: The presence of multiple @RequestMappings is intentional.
		@PatchMapping("/put")
		@RequestMapping(path = "/put", method = RequestMethod.PUT) // local @RequestMapping overrides meta-annotations
		@PostMapping("/put")
		public void put() {
		}

		@DeleteMapping("/delete")
		public void delete() {
		}

		@PatchMapping("/patch")
		public void patch() {
		}

	}

	@RequestMapping
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ExtraRequestMapping {
	}

	@RequestMapping(method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface PostJson {

		@AliasFor(annotation = RequestMapping.class)
		String[] value() default {};
	}


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock();
		}
	}


	@RestController
	@HttpExchange("/exchange")
	static class HttpExchangeController {

		@HttpExchange
		public void defaultValuesExchange() {}

		@PostExchange(url = "/custom", contentType = "application/json", accept = "text/plain;charset=UTF-8")
		public void customValuesExchange(){}

		@HttpExchange(method="GET", url = "/headers",
				headers = {"h1=hv1", "!h2", "Accept=application/ignored"})
		public String customHeadersExchange() {
			return "info";
		}
	}


	@HttpExchange("/exchange")
	@ExtraHttpExchange
	static class MultipleClassLevelAnnotationsHttpExchangeController {

		@PostExchange("/post")
		void post() {}
	}


	static class MultipleMethodLevelAnnotationsHttpExchangeController {

		@PostExchange("/post")
		@PutExchange("/post")
		void post() {}
	}


	@Controller
	@RequestMapping("/api")
	@HttpExchange("/api")
	static class MixedClassLevelAnnotationsController {

		@PostExchange("/post")
		void post() {}
	}


	@Controller
	@RequestMapping("/api")
	static class MixedMethodLevelAnnotationsController {

		@PostMapping("/post")
		@PostExchange("/post")
		void post() {}
	}


	@HttpExchange("/service")
	interface Service {

		@PostExchange("/postExchange")
		void post();

	}


	@Controller
	@RequestMapping("/controller")
	static class ClassLevelOverriddenHttpExchangeAnnotationsController implements Service {

		@Override
		public void post() {}
	}


	@Controller
	@RequestMapping("/controller")
	static class MethodLevelOverriddenHttpExchangeAnnotationsController implements Service {

		@PostMapping("/postMapping")
		@Override
		public void post() {}
	}


	@HttpExchange
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ExtraHttpExchange {
	}


	private static class Foo {
	}

}
