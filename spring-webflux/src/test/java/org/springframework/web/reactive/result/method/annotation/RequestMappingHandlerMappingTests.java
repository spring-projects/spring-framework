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

package org.springframework.web.reactive.result.method.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
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
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.HeadersRequestCondition;
import org.springframework.web.reactive.result.condition.MediaTypeExpression;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @author Sam Brannen
 */
class RequestMappingHandlerMappingTests {

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	private final RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();


	@BeforeEach
	void setup() {
		this.handlerMapping.setApplicationContext(wac);
	}


	@Test
	void resolveEmbeddedValuesInPatterns() {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value);

		String[] patterns = { "/foo", "/${pattern}/bar" };
		String[] result = this.handlerMapping.resolveEmbeddedValuesInPatterns(patterns);

		assertThat(result).containsExactly("/foo", "/foo/bar");
	}

	@Test
	void pathPrefix() {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		this.handlerMapping.setPathPrefixes(Map.of("/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));

		Method method = ReflectionUtils.findMethod(UserController.class, "getUser");
		List<RequestMappingInfo> infos = this.handlerMapping.getListMappingsForMethod(method, UserController.class);

		assertThat(infos).isNotEmpty();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.flatExtracting(PathPattern::getPatternString)
				.containsOnly("/api/user/{id}");
	}

	@Test
	void resolveRequestMappingViaComposedAnnotation() {
		RequestMappingInfo info = assertComposedAnnotationMapping("postJson", "/postJson", RequestMethod.POST);

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

	@Test // gh-22010
	void consumesWithOptionalRequestBody() {
		this.wac.registerSingleton("testController", ComposedAnnotationController.class);
		this.wac.refresh();
		this.handlerMapping.afterPropertiesSet();
		RequestMappingInfo info = this.handlerMapping.getHandlerMethods().keySet().stream()
				.filter(i -> i.getPatternsCondition().getPatterns().iterator().next().getPatternString().equals("/post"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No /post"));

		assertThat(info.getConsumesCondition().isBodyRequired()).isFalse();
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
	void httpExchangeWithMultipleAnnotationsAtClassLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = MultipleClassLevelAnnotationsHttpExchangeController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		List<RequestMappingInfo> infos = this.handlerMapping.getListMappingsForMethod(method, controllerClass);

		assertThat(infos.size()).isEqualTo(2);
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.flatExtracting(PathPattern::getPatternString)
				.containsOnly("/exchange/post", "/post");
	}

	@Test  // gh-32049
	void httpExchangeWithMultipleAnnotationsAtMethodLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = MultipleMethodLevelAnnotationsHttpExchangeController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		List<RequestMappingInfo> mappingInfos = this.handlerMapping.getListMappingsForMethod(method, controllerClass);

		assertThat(mappingInfos.size()).isEqualTo(2);
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getMethodsCondition)
				.flatExtracting(RequestMethodsRequestCondition::getMethods)
				.containsOnly(RequestMethod.POST, RequestMethod.PUT);
	}

	@Test  // gh-32065
	void httpExchangeWithMixedAnnotationsAtClassLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = MixedClassLevelAnnotationsController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		assertThatIllegalStateException()
				.isThrownBy(() -> this.handlerMapping.getListMappingsForMethod(method, controllerClass))
				.withMessageContainingAll(
					controllerClass.getName(),
					"is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
					RequestMapping.class.getSimpleName(),
					HttpExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32065
	void httpExchangeWithMixedAnnotationsAtMethodLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = MixedMethodLevelAnnotationsController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		assertThatIllegalStateException()
				.isThrownBy(() -> this.handlerMapping.getListMappingsForMethod(method, controllerClass))
				.withMessageContainingAll(
					method.toString(),
					"is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
					PostMapping.class.getSimpleName(),
					PostExchange.class.getSimpleName()
				);
	}

	@Test  // gh-32065
	void httpExchangeAnnotationsOverriddenAtClassLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = ClassLevelOverriddenHttpExchangeAnnotationsController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		List<RequestMappingInfo> infos = this.handlerMapping.getListMappingsForMethod(method, controllerClass);

		assertThat(infos).isNotEmpty();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.isNotNull();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.flatExtracting(PathPattern::getPatternString)
				.containsOnly("/controller/postExchange");
	}

	@Test  // gh-32065
	void httpExchangeAnnotationsOverriddenAtMethodLevel() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> controllerClass = MethodLevelOverriddenHttpExchangeAnnotationsController.class;
		Method method = ReflectionUtils.findMethod(controllerClass, "post");

		List<RequestMappingInfo> infos = this.handlerMapping.getListMappingsForMethod(method, controllerClass);

		assertThat(infos).isNotEmpty();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.isNotNull();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.flatExtracting(PathPattern::getPatternString)
				.containsOnly("/controller/postMapping");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void httpExchangeWithDefaultValues() {
		this.handlerMapping.afterPropertiesSet();

		Class<?> clazz = HttpExchangeController.class;
		Method method = ReflectionUtils.findMethod(clazz, "defaultValuesExchange");
		List<RequestMappingInfo> mappingInfos = this.handlerMapping.getListMappingsForMethod(method, clazz);

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.extracting(PathPattern::toString)
				.containsOnly("/exchange");

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getMethodsCondition)
				.flatExtracting(RequestMethodsRequestCondition::getMethods)
				.isEmpty();
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getParamsCondition)
				.flatExtracting(ParamsRequestCondition::getExpressions)
				.isEmpty();
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getHeadersCondition)
				.flatExtracting(HeadersRequestCondition::getExpressions)
				.isEmpty();
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getConsumesCondition)
				.flatExtracting(ConsumesRequestCondition::getExpressions)
				.isEmpty();
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getProducesCondition)
				.flatExtracting(ProducesRequestCondition::getExpressions)
				.isEmpty();
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void httpExchangeWithCustomValues() {
		this.handlerMapping.afterPropertiesSet();

		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setApplicationContext(new StaticWebApplicationContext());
		mapping.afterPropertiesSet();

		Class<HttpExchangeController> clazz = HttpExchangeController.class;
		Method method = ReflectionUtils.findMethod(clazz, "customValuesExchange");
		List<RequestMappingInfo> mappingInfos = mapping.getListMappingsForMethod(method, clazz);

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.extracting(PathPattern::toString)
				.containsOnly("/exchange/custom");

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getMethodsCondition)
				.flatExtracting(RequestMethodsRequestCondition::getMethods)
				.containsOnly(RequestMethod.POST);
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getParamsCondition)
				.flatExtracting(ParamsRequestCondition::getExpressions)
				.isEmpty();
		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getHeadersCondition)
				.flatExtracting(HeadersRequestCondition::getExpressions)
				.isEmpty();

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getConsumesCondition)
				.flatExtracting(ConsumesRequestCondition::getExpressions)
				.extracting(MediaTypeExpression::getMediaType)
				.containsOnly(MediaType.APPLICATION_JSON);

		assertThat(mappingInfos)
				.extracting(RequestMappingInfo::getProducesCondition)
				.flatExtracting(ProducesRequestCondition::getExpressions)
				.extracting(MediaTypeExpression::getMediaType)
				.containsOnly(MediaType.valueOf("text/plain;charset=UTF-8"));
	}

	private RequestMappingInfo assertComposedAnnotationMapping(RequestMethod requestMethod) {
		String methodName = requestMethod.name().toLowerCase();
		String path = "/" + methodName;
		return assertComposedAnnotationMapping(methodName, path, requestMethod);
	}

	private RequestMappingInfo assertComposedAnnotationMapping(
			String methodName, String path, RequestMethod requestMethod) {

		Class<?> clazz = ComposedAnnotationController.class;
		Method method = ClassUtils.getMethod(clazz, methodName, (Class<?>[]) null);
		List<RequestMappingInfo> infos = this.handlerMapping.getListMappingsForMethod(method, clazz);

		assertThat(infos).isNotEmpty();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.isNotEmpty();
		assertThat(infos)
				.extracting(RequestMappingInfo::getPatternsCondition)
				.flatExtracting(PatternsRequestCondition::getPatterns)
				.flatExtracting(PathPattern::getPatternString)
				.containsOnly(path);

		assertThat(infos)
				.extracting(RequestMappingInfo::getMethodsCondition)
				.flatExtracting(RequestMethodsRequestCondition::getMethods)
				.containsOnly(requestMethod);

		return infos.get(0);
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

		@RequestMapping(path = "/put", method = RequestMethod.PUT) // local @RequestMapping overrides meta-annotations
		public void put() {
		}

		@DeleteMapping("/delete")
		public void delete() {
		}

		@PatchMapping("/patch")
		public void patch() {
		}
	}

	private static class Foo {
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

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
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

}
