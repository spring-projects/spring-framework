/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import rx.Single;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ModelInitializer}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelInitializerTests {

	private ModelInitializer modelInitializer;

	private final ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));


	@Before
	public void setUp() throws Exception {

		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		ArgumentResolverConfigurer resolverConfigurer = new ArgumentResolverConfigurer();
		resolverConfigurer.addCustomResolver(new ModelArgumentResolver(adapterRegistry));

		ControllerMethodResolver methodResolver = new ControllerMethodResolver(
				resolverConfigurer, adapterRegistry, new StaticApplicationContext(), Collections.emptyList());

		this.modelInitializer = new ModelInitializer(methodResolver, adapterRegistry);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void initBinderMethod() throws Exception {

		Validator validator = mock(Validator.class);

		TestController controller = new TestController();
		controller.setValidator(validator);
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(GetMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));

		WebExchangeDataBinder binder = context.createDataBinder(this.exchange, "name");
		assertEquals(Collections.singletonList(validator), binder.getValidators());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void modelAttributeMethods() throws Exception {
		TestController controller = new TestController();
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(GetMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));

		Map<String, Object> model = context.getModel().asMap();
		assertEquals(5, model.size());

		Object value = model.get("bean");
		assertEquals("Bean", ((TestBean) value).getName());

		value = model.get("monoBean");
		assertEquals("Mono Bean", ((Mono<TestBean>) value).block(Duration.ofMillis(5000)).getName());

		value = model.get("singleBean");
		assertEquals("Single Bean", ((Single<TestBean>) value).toBlocking().value().getName());

		value = model.get("voidMethodBean");
		assertEquals("Void Method Bean", ((TestBean) value).getName());

		value = model.get("voidMonoMethodBean");
		assertEquals("Void Mono Method Bean", ((TestBean) value).getName());
	}

	@Test
	public void saveModelAttributeToSession() throws Exception {
		TestController controller = new TestController();
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(GetMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));

		WebSession session = this.exchange.getSession().block(Duration.ZERO);
		assertNotNull(session);
		assertEquals(0, session.getAttributes().size());

		context.saveModel();
		assertEquals(1, session.getAttributes().size());
		assertEquals("Bean", ((TestBean) session.getRequiredAttribute("bean")).getName());
	}

	@Test
	public void retrieveModelAttributeFromSession() throws Exception {
		WebSession session = this.exchange.getSession().block(Duration.ZERO);
		assertNotNull(session);

		TestBean testBean = new TestBean("Session Bean");
		session.getAttributes().put("bean", testBean);

		TestController controller = new TestController();
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(GetMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));

		context.saveModel();
		assertEquals(1, session.getAttributes().size());
		assertEquals("Session Bean", ((TestBean) session.getRequiredAttribute("bean")).getName());
	}

	@Test
	public void requiredSessionAttributeMissing() throws Exception {
		TestController controller = new TestController();
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(PostMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		try {
			this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));
			fail();
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Required attribute 'missing-bean' is missing.", ex.getMessage());
		}
	}

	@Test
	public void clearModelAttributeFromSession() throws Exception {
		WebSession session = this.exchange.getSession().block(Duration.ZERO);
		assertNotNull(session);

		TestBean testBean = new TestBean("Session Bean");
		session.getAttributes().put("bean", testBean);

		TestController controller = new TestController();
		InitBinderBindingContext context = getBindingContext(controller);

		Method method = ResolvableMethod.on(TestController.class).annotPresent(GetMapping.class).resolveMethod();
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(Duration.ofMillis(5000));

		context.getSessionStatus().setComplete();
		context.saveModel();

		assertEquals(0, session.getAttributes().size());
	}


	private InitBinderBindingContext getBindingContext(Object controller) {

		List<SyncInvocableHandlerMethod> binderMethods =
				MethodIntrospector.selectMethods(controller.getClass(), BINDER_METHODS)
						.stream()
						.map(method -> new SyncInvocableHandlerMethod(controller, method))
						.collect(Collectors.toList());;

		WebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		return new InitBinderBindingContext(bindingInitializer, binderMethods);
	}


	@SuppressWarnings("unused")
	@SessionAttributes({"bean", "missing-bean"})
	private static class TestController {

		@Nullable
		private Validator validator;


		void setValidator(Validator validator) {
			this.validator = validator;
		}


		@InitBinder
		public void initDataBinder(WebDataBinder dataBinder) {
			if (this.validator != null) {
				dataBinder.addValidators(this.validator);
			}
		}

		@ModelAttribute("bean")
		public TestBean returnValue() {
			return new TestBean("Bean");
		}

		@ModelAttribute("monoBean")
		public Mono<TestBean> returnValueMono() {
			return Mono.just(new TestBean("Mono Bean"));
		}

		@ModelAttribute("singleBean")
		public Single<TestBean> returnValueSingle() {
			return Single.just(new TestBean("Single Bean"));
		}

		@ModelAttribute
		public void voidMethodBean(Model model) {
			model.addAttribute("voidMethodBean", new TestBean("Void Method Bean"));
		}

		@ModelAttribute
		public Mono<Void> voidMonoMethodBean(Model model) {
			return Mono.just("Void Mono Method Bean")
					.doOnNext(name -> model.addAttribute("voidMonoMethodBean", new TestBean(name)))
					.then();
		}

		@GetMapping
		public void handleGet() {}

		@PostMapping
		public void handlePost(@ModelAttribute("missing-bean") TestBean testBean) {}

	}


	private static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "TestBean[name=" + this.name + "]";
		}
	}

	private static final ReflectionUtils.MethodFilter BINDER_METHODS = method ->
			AnnotationUtils.findAnnotation(method, InitBinder.class) != null;

}
