/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying {@code ModelAttribute} method inter-dependencies.
 *
 * @author Kevin Binswanger
 */

public class ModelInitializerOrderingTests {

	private static final Log logger = LogFactory.getLog(ModelInitializerOrderingTests.class);

	private Model model;


	@Test
	public void straightLineDependency() throws Exception {
		runTest(new ModelInitializerOrderingTests.StraightLineDependencyController());
		assertInvokedBefore("getA", "getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
		assertInvokedBefore("getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
		assertInvokedBefore("getB2", "getC1", "getC2", "getC3", "getC4");
		assertInvokedBefore("getC1", "getC2", "getC3", "getC4");
		assertInvokedBefore("getC2", "getC3", "getC4");
		assertInvokedBefore("getC3", "getC4");
	}

	@Test
	public void treeDependency() throws Exception {
		runTest(new ModelInitializerOrderingTests.TreeDependencyController());
		assertInvokedBefore("getA", "getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
		assertInvokedBefore("getB1", "getC1", "getC2");
		assertInvokedBefore("getB2", "getC3", "getC4");
	}

	@Test
	public void invertedTreeDependency() throws Exception {
		runTest(new ModelInitializerOrderingTests.InvertedTreeDependencyController());
		assertInvokedBefore("getC1", "getA", "getB1");
		assertInvokedBefore("getC2", "getA", "getB1");
		assertInvokedBefore("getC3", "getA", "getB2");
		assertInvokedBefore("getC4", "getA", "getB2");
		assertInvokedBefore("getB1", "getA");
		assertInvokedBefore("getB2", "getA");
	}

	@Test
	public void unresolvedDependency() throws Exception {
		runTest(new ModelInitializerOrderingTests.UnresolvedDependencyController());
		assertInvokedBefore("getA", "getC1", "getC2", "getC3", "getC4");

		// No other order guarantees for methods with unresolvable dependencies (and methods that depend on them),
		// Required dependencies will be created via default constructor.
	}

	private void runTest(Object controller) throws Exception {
		InitBinderBindingContext context = getBindingContext(controller);
		this.model = context.getModel();
		this.model.addAttribute("methods", new ArrayList<String>());

		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));

		ReactiveAdapterRegistry adapterRegistry = new ReactiveAdapterRegistry();

		ArgumentResolverConfigurer resolverConfigurer = new ArgumentResolverConfigurer();
		resolverConfigurer.addCustomResolver(new ModelArgumentResolver(adapterRegistry));

		ControllerMethodResolver methodResolver = new ControllerMethodResolver(
				resolverConfigurer, adapterRegistry, new StaticApplicationContext(), Collections.emptyList());

		ModelInitializer modelInitializer = new ModelInitializer(methodResolver, adapterRegistry);
		modelInitializer.initModel(new HandlerMethod(controller, "handle"), context, exchange)
				.block(Duration.ofMillis(5000));

		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (String name : getInvokedMethods()) {
				sb.append(" >> ").append(name);
			}
			logger.debug(sb);
		}
	}

	private void assertInvokedBefore(String beforeMethod, String... afterMethods) {
		List<String> actual = getInvokedMethods();
		for (String afterMethod : afterMethods) {
			assertTrue(beforeMethod + " should be before " + afterMethod + ". Actual order: " +
					actual.toString(), actual.indexOf(beforeMethod) < actual.indexOf(afterMethod));
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> getInvokedMethods() {
		return (List<String>) this.model.asMap().get("methods");
	}

	private InitBinderBindingContext getBindingContext(Object controller) {

		List<SyncInvocableHandlerMethod> binderMethods =
				MethodIntrospector.selectMethods(controller.getClass(), BINDER_METHODS)
						.stream()
						.map(method -> new SyncInvocableHandlerMethod(controller, method))
						.collect(Collectors.toList());

		WebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		return new InitBinderBindingContext(bindingInitializer, binderMethods);
	}


	private static class AbstractController {

		@RequestMapping
		public void handle() {
		}

		@SuppressWarnings("unchecked")
		<T> T updateAndReturn(Model model, String methodName, T returnValue) throws IOException {
			((List<String>) model.asMap().get("methods")).add(methodName);
			return returnValue;
		}
	}

	private static class StraightLineDependencyController extends ModelInitializerOrderingTests.AbstractController {

		@ModelAttribute
		public ModelInitializerOrderingTests.A getA(Model model) throws IOException {
			return updateAndReturn(model, "getA", new ModelInitializerOrderingTests.A());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B1 getB1(@ModelAttribute ModelInitializerOrderingTests.A a, Model model) throws IOException {
			return updateAndReturn(model, "getB1", new ModelInitializerOrderingTests.B1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B2 getB2(@ModelAttribute ModelInitializerOrderingTests.B1 b1, Model model) throws IOException {
			return updateAndReturn(model, "getB2", new ModelInitializerOrderingTests.B2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C1 getC1(@ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getC1", new ModelInitializerOrderingTests.C1());
		}


		@ModelAttribute
		public ModelInitializerOrderingTests.C2 getC2(@ModelAttribute ModelInitializerOrderingTests.C1 c1, Model model) throws IOException {
			return updateAndReturn(model, "getC2", new ModelInitializerOrderingTests.C2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C3 getC3(@ModelAttribute ModelInitializerOrderingTests.C2 c2, Model model) throws IOException {
			return updateAndReturn(model, "getC3", new ModelInitializerOrderingTests.C3());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C4 getC4(@ModelAttribute ModelInitializerOrderingTests.C3 c3, Model model) throws IOException {
			return updateAndReturn(model, "getC4", new ModelInitializerOrderingTests.C4());
		}
	}

	private static class TreeDependencyController extends ModelInitializerOrderingTests.AbstractController {

		@ModelAttribute
		public ModelInitializerOrderingTests.A getA(Model model) throws IOException {
			return updateAndReturn(model, "getA", new ModelInitializerOrderingTests.A());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B1 getB1(@ModelAttribute ModelInitializerOrderingTests.A a, Model model) throws IOException {
			return updateAndReturn(model, "getB1", new ModelInitializerOrderingTests.B1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B2 getB2(@ModelAttribute ModelInitializerOrderingTests.A a, Model model) throws IOException {
			return updateAndReturn(model, "getB2", new ModelInitializerOrderingTests.B2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C1 getC1(@ModelAttribute ModelInitializerOrderingTests.B1 b1, Model model) throws IOException {
			return updateAndReturn(model, "getC1", new ModelInitializerOrderingTests.C1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C2 getC2(@ModelAttribute ModelInitializerOrderingTests.B1 b1, Model model) throws IOException {
			return updateAndReturn(model, "getC2", new ModelInitializerOrderingTests.C2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C3 getC3(@ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getC3", new ModelInitializerOrderingTests.C3());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C4 getC4(@ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getC4", new ModelInitializerOrderingTests.C4());
		}
	}

	private static class InvertedTreeDependencyController extends ModelInitializerOrderingTests.AbstractController {

		@ModelAttribute
		public ModelInitializerOrderingTests.C1 getC1(Model model) throws IOException {
			return updateAndReturn(model, "getC1", new ModelInitializerOrderingTests.C1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C2 getC2(Model model) throws IOException {
			return updateAndReturn(model, "getC2", new ModelInitializerOrderingTests.C2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C3 getC3(Model model) throws IOException {
			return updateAndReturn(model, "getC3", new ModelInitializerOrderingTests.C3());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C4 getC4(Model model) throws IOException {
			return updateAndReturn(model, "getC4", new ModelInitializerOrderingTests.C4());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B1 getB1(@ModelAttribute ModelInitializerOrderingTests.C1 c1, @ModelAttribute ModelInitializerOrderingTests.C2 c2, Model model) throws IOException {
			return updateAndReturn(model, "getB1", new ModelInitializerOrderingTests.B1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.B2 getB2(@ModelAttribute ModelInitializerOrderingTests.C3 c3, @ModelAttribute ModelInitializerOrderingTests.C4 c4, Model model) throws IOException {
			return updateAndReturn(model, "getB2", new ModelInitializerOrderingTests.B2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.A getA(@ModelAttribute ModelInitializerOrderingTests.B1 b1, @ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getA", new ModelInitializerOrderingTests.A());
		}

	}

	private static class UnresolvedDependencyController extends ModelInitializerOrderingTests.AbstractController {

		@ModelAttribute
		public ModelInitializerOrderingTests.A getA(Model model) throws IOException {
			return updateAndReturn(model, "getA", new ModelInitializerOrderingTests.A());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C1 getC1(@ModelAttribute ModelInitializerOrderingTests.B1 b1, Model model) throws IOException {
			return updateAndReturn(model, "getC1", new ModelInitializerOrderingTests.C1());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C2 getC2(@ModelAttribute ModelInitializerOrderingTests.B1 b1, Model model) throws IOException {
			return updateAndReturn(model, "getC2", new ModelInitializerOrderingTests.C2());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C3 getC3(@ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getC3", new ModelInitializerOrderingTests.C3());
		}

		@ModelAttribute
		public ModelInitializerOrderingTests.C4 getC4(@ModelAttribute ModelInitializerOrderingTests.B2 b2, Model model) throws IOException {
			return updateAndReturn(model, "getC4", new ModelInitializerOrderingTests.C4());
		}
	}

	private static class A { }
	private static class B1 { }
	private static class B2 { }
	private static class C1 { }
	private static class C2 { }
	private static class C3 { }
	private static class C4 { }

	private static final ReflectionUtils.MethodFilter BINDER_METHODS = method ->
			AnnotationUtils.findAnnotation(method, InitBinder.class) != null;
}
