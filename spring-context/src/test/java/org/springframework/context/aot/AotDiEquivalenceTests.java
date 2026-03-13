/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.aot;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Provider;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalence tests verifying that Spring DI produces the same observable bean
 * state in both regular and AOT modes.
 *
 * <p>The core invariant: for any valid DI configuration, {@code getBean()} in
 * regular mode and AOT mode should produce beans with the same observable state.
 *
 * <p>Tests are organized by scenario groups covering key combinations of DI
 * dimensions: injection style, scope, retrieval method, target type, lifecycle,
 * and lazy resolution. Each fixture bean implements {@link Verifiable} to
 * produce a comparable snapshot of its state.
 *
 * @author Spring Framework Contributors
 * @since 7.0
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/35871">gh-35871</a>
 */
class AotDiEquivalenceTests {

	private static final String TARGET = "target";

	private static final String[] ALL_SCOPES = {
			BeanDefinition.SCOPE_SINGLETON,
			BeanDefinition.SCOPE_PROTOTYPE };

	@TestFactory
	Stream<DynamicTest> regularAndAotModesProduceSameBeanState() {
		return Stream.of(
			explicitArgsScenarios(),
			injectionStyleScenarios(),
			targetTypeScenarios(),
			lifecycleScenarios(),
			lazyScenarios(),
			crossCuttingScenarios()
		).flatMap(Function.identity());
	}


	// --- Scenario Groups ---

	/**
	 * getBean(name, args) with various injection styles (prototype scope).
	 * This scenario group catches gh-35871-class regressions where
	 * InstanceSupplier.andThen() post-processing was not applied when
	 * the supplier was bypassed due to explicit constructor args.
	 */
	private Stream<DynamicTest> explicitArgsScenarios() {
		List<Class<? extends Verifiable>> beanClasses = List.of(
				SetterInjectedWithArg.class,
				FieldInjectedWithArg.class,
				MethodInjectedWithArg.class);
		return beanClasses.stream().map(beanClass -> scenario(
				"[prototype] %s, getBean(name, args)".formatted(beanClass.getSimpleName()),
				ctx -> {
					registerDependency(ctx);
					registerBean(ctx, TARGET, beanClass, BeanDefinition.SCOPE_PROTOTYPE);
				},
				ctx -> ((Verifiable) ctx.getBean(TARGET, "testValue")).snapshot()));
	}

	/**
	 * Standard getBean(name) with each injection style x scope.
	 */
	private Stream<DynamicTest> injectionStyleScenarios() {
		record Case(String style, Class<? extends Verifiable> beanClass) {}
		List<Case> cases = List.of(
				new Case("constructor", ConstructorInjectedBean.class),
				new Case("setter", SetterInjectedBean.class),
				new Case("field", FieldInjectedBean.class),
				new Case("method", MethodInjectedBean.class));
		return cases.stream().flatMap(c ->
				Arrays.stream(ALL_SCOPES).map(scope ->
						scenario("[%s] %s injection".formatted(scope, c.style),
								ctx -> {
									registerDependency(ctx);
									registerBean(ctx, TARGET, c.beanClass, scope);
								},
								ctx -> ((Verifiable) ctx.getBean(TARGET)).snapshot())));
	}

	/**
	 * Injection target type variations x scope.
	 */
	private Stream<DynamicTest> targetTypeScenarios() {
		record Case(String type, Class<? extends Verifiable> beanClass) {}
		List<Case> cases = List.of(
				new Case("Optional<T>", OptionalInjectedBean.class),
				new Case("ObjectProvider<T>", ObjectProviderInjectedBean.class),
				new Case("Provider<T>", ProviderInjectedBean.class),
				new Case("List<T>", ListInjectedBean.class));
		return cases.stream().flatMap(c ->
				Arrays.stream(ALL_SCOPES).map(scope ->
						scenario("[%s] %s field injection".formatted(scope, c.type),
								ctx -> {
									registerDependency(ctx);
									registerBean(ctx, TARGET, c.beanClass, scope);
								},
								ctx -> ((Verifiable) ctx.getBean(TARGET)).snapshot())));
	}

	/**
	 * Lifecycle callbacks x scope.
	 */
	private Stream<DynamicTest> lifecycleScenarios() {
		record Case(String label, Class<? extends Verifiable> beanClass) {}
		List<Case> cases = List.of(
				new Case("@PostConstruct + BeanNameAware", PostConstructBean.class),
				new Case("InitializingBean", InitializingBeanImpl.class));
		return cases.stream().flatMap(c ->
				Arrays.stream(ALL_SCOPES).map(scope ->
						scenario("[%s] %s".formatted(scope, c.label),
								ctx -> {
									registerDependency(ctx);
									registerBean(ctx, TARGET, c.beanClass, scope);
								},
								ctx -> ((Verifiable) ctx.getBean(TARGET)).snapshot())));
	}

	/**
	 * @Lazy injection x scope.
	 */
	private Stream<DynamicTest> lazyScenarios() {
		record Case(String style, Class<? extends Verifiable> beanClass) {}
		List<Case> cases = List.of(
				new Case("@Lazy field", LazyFieldInjectedBean.class),
				new Case("@Lazy setter", LazySetterInjectedBean.class));
		return cases.stream().flatMap(c ->
				Arrays.stream(ALL_SCOPES).map(scope ->
						scenario("[%s] %s injection".formatted(scope, c.style),
								ctx -> {
									registerDependency(ctx);
									registerBean(ctx, TARGET, c.beanClass, scope);
								},
								ctx -> ((Verifiable) ctx.getBean(TARGET)).snapshot())));
	}


	/**
	 * Cross-cutting scenarios combining multiple DI features on the same bean.
	 * Bugs like gh-35871 hide at the intersection of dimensions, so these
	 * tests exercise feature combinations rather than individual axes.
	 */
	private Stream<DynamicTest> crossCuttingScenarios() {
		// Feature combinations with explicit constructor args
		List<Class<? extends Verifiable>> withArgBeans = List.of(
				FullFeaturedWithArg.class,
				LazyAndEagerWithArg.class,
				OptionalSetterWithArg.class);
		Stream<DynamicTest> withArgs = withArgBeans.stream().map(beanClass -> scenario(
				"[prototype] %s, getBean(name, args)".formatted(beanClass.getSimpleName()),
				ctx -> {
					registerDependency(ctx);
					registerBean(ctx, TARGET, beanClass, BeanDefinition.SCOPE_PROTOTYPE);
				},
				ctx -> ((Verifiable) ctx.getBean(TARGET, "testValue")).snapshot()));

		// Feature combinations with standard retrieval x scope
		record Case(String label, Class<? extends Verifiable> beanClass) {}
		List<Case> standardCases = List.of(
				new Case("setter+field+@PostConstruct+BeanNameAware",
						MultiInjectionWithLifecycle.class),
				new Case("@Lazy field+setter+@PostConstruct",
						LazyWithLifecycle.class),
				new Case("Optional+Provider+@PostConstruct",
						TargetTypeMixWithLifecycle.class));
		Stream<DynamicTest> standard = standardCases.stream().flatMap(c ->
				Arrays.stream(ALL_SCOPES).map(scope ->
						scenario("[%s] %s".formatted(scope, c.label),
								ctx -> {
									registerDependency(ctx);
									registerBean(ctx, TARGET, c.beanClass, scope);
								},
								ctx -> ((Verifiable) ctx.getBean(TARGET)).snapshot())));

		return Stream.concat(withArgs, standard);
	}


	// --- Test infrastructure ---

	private static DynamicTest scenario(String name,
			Consumer<GenericApplicationContext> setup,
			Function<GenericApplicationContext, Object> stateExtractor) {

		return DynamicTest.dynamicTest(name,
				() -> assertAotEquivalent(setup, stateExtractor));
	}

	/**
	 * Assert that the given context setup produces beans with the same
	 * observable state in regular mode and AOT-compiled mode.
	 */
	@SuppressWarnings("unchecked")
	private static void assertAotEquivalent(
			Consumer<GenericApplicationContext> setup,
			Function<GenericApplicationContext, Object> stateExtractor) {

		// Regular mode
		GenericApplicationContext regular = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(regular);
		setup.accept(regular);
		regular.refresh();
		Object regularState = stateExtractor.apply(regular);
		regular.close();

		// AOT mode
		GenericApplicationContext aotSource = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(aotSource);
		setup.accept(aotSource);
		TestGenerationContext genCtx = processAheadOfTime(aotSource);
		TestCompiler.forSystem().with(genCtx).compile(compiled -> {
			ApplicationContextInitializer<GenericApplicationContext> initializer =
					compiled.getInstance(ApplicationContextInitializer.class);
			GenericApplicationContext aot = new GenericApplicationContext();
			initializer.initialize(aot);
			aot.refresh();
			Object aotState = stateExtractor.apply(aot);
			assertThat(aotState)
					.as("AOT mode should produce the same bean state as regular mode")
					.isEqualTo(regularState);
			aot.close();
		});
	}

	private static TestGenerationContext processAheadOfTime(
			GenericApplicationContext applicationContext) {

		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		return generationContext;
	}

	private static void registerDependency(GenericApplicationContext ctx) {
		ctx.registerBeanDefinition("service",
				new RootBeanDefinition(ServiceBean.class));
	}

	private static void registerBean(GenericApplicationContext ctx,
			String name, Class<?> beanClass, String scope) {

		RootBeanDefinition bd = new RootBeanDefinition(beanClass);
		if (!BeanDefinition.SCOPE_SINGLETON.equals(scope)) {
			bd.setScope(scope);
		}
		ctx.registerBeanDefinition(name, bd);
	}


	// --- Verifiable contract ---

	interface Verifiable {
		Map<String, Object> snapshot();
	}


	// --- Fixture beans ---

	public static class ServiceBean {
	}

	// Injection with explicit constructor args (for getBean(name, args) scenarios)

	public static class SetterInjectedWithArg implements Verifiable {

		private final String name;

		private ServiceBean service;

		public SetterInjectedWithArg(String name) {
			this.name = name;
		}

		@Autowired
		public void setService(ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("name", name, "serviceInjected", service != null);
		}
	}

	public static class FieldInjectedWithArg implements Verifiable {

		private final String name;

		@Autowired
		private ServiceBean service;

		public FieldInjectedWithArg(String name) {
			this.name = name;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("name", name, "serviceInjected", service != null);
		}
	}

	public static class MethodInjectedWithArg implements Verifiable {

		private final String name;

		private ServiceBean service;

		public MethodInjectedWithArg(String name) {
			this.name = name;
		}

		@Autowired
		public void configure(ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("name", name, "serviceInjected", service != null);
		}
	}

	// Standard injection styles (for getBean(name) scenarios)

	public static class ConstructorInjectedBean implements Verifiable {

		private final ServiceBean service;

		public ConstructorInjectedBean(ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	public static class SetterInjectedBean implements Verifiable {

		private ServiceBean service;

		@Autowired
		public void setService(ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	public static class FieldInjectedBean implements Verifiable {

		@Autowired
		private ServiceBean service;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	public static class MethodInjectedBean implements Verifiable {

		private ServiceBean service;

		@Autowired
		public void configure(ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	// Target type variations

	public static class OptionalInjectedBean implements Verifiable {

		@Autowired
		private Optional<ServiceBean> service;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("servicePresent", service != null && service.isPresent());
		}
	}

	public static class ObjectProviderInjectedBean implements Verifiable {

		@Autowired
		private ObjectProvider<ServiceBean> service;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceAvailable",
					service != null && service.getIfAvailable() != null);
		}
	}

	public static class ProviderInjectedBean implements Verifiable {

		@Autowired
		private Provider<ServiceBean> service;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceAvailable",
					service != null && service.get() != null);
		}
	}

	public static class ListInjectedBean implements Verifiable {

		@Autowired
		private List<ServiceBean> services;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("servicesCount", services != null ? services.size() : 0);
		}
	}

	// Lifecycle

	public static class PostConstructBean implements BeanNameAware, Verifiable {

		@Autowired
		private ServiceBean service;

		private String beanName;

		private boolean initialized;

		@PostConstruct
		public void init() {
			this.initialized = true;
		}

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"serviceInjected", service != null,
					"beanName", (beanName != null ? beanName : ""),
					"initialized", initialized);
		}
	}

	public static class InitializingBeanImpl implements InitializingBean, Verifiable {

		@Autowired
		private ServiceBean service;

		private boolean initialized;

		@Override
		public void afterPropertiesSet() {
			this.initialized = true;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"serviceInjected", service != null,
					"initialized", initialized);
		}
	}

	// Lazy

	public static class LazyFieldInjectedBean implements Verifiable {

		@Lazy
		@Autowired
		private ServiceBean service;

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	public static class LazySetterInjectedBean implements Verifiable {

		private ServiceBean service;

		@Autowired
		public void setService(@Lazy ServiceBean service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of("serviceInjected", service != null);
		}
	}

	// Cross-cutting: explicit args + multiple features

	public static class FullFeaturedWithArg implements BeanNameAware, Verifiable {

		private final String name;

		@Autowired
		private ServiceBean fieldService;

		private ServiceBean setterService;

		private String beanName;

		private boolean initialized;

		public FullFeaturedWithArg(String name) {
			this.name = name;
		}

		@Autowired
		public void setSetterService(ServiceBean service) {
			this.setterService = service;
		}

		@PostConstruct
		public void init() {
			this.initialized = true;
		}

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"name", name,
					"fieldInjected", fieldService != null,
					"setterInjected", setterService != null,
					"beanName", (beanName != null ? beanName : ""),
					"initialized", initialized);
		}
	}

	public static class LazyAndEagerWithArg implements Verifiable {

		private final String name;

		@Lazy
		@Autowired
		private ServiceBean lazyService;

		private ServiceBean eagerService;

		public LazyAndEagerWithArg(String name) {
			this.name = name;
		}

		@Autowired
		public void setEagerService(ServiceBean service) {
			this.eagerService = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"name", name,
					"lazyInjected", lazyService != null,
					"eagerInjected", eagerService != null);
		}
	}

	public static class OptionalSetterWithArg implements Verifiable {

		private final String name;

		private Optional<ServiceBean> service = Optional.empty();

		public OptionalSetterWithArg(String name) {
			this.name = name;
		}

		@Autowired
		public void setService(Optional<ServiceBean> service) {
			this.service = service;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"name", name,
					"servicePresent", service.isPresent());
		}
	}

	// Cross-cutting: multiple features without explicit args

	public static class MultiInjectionWithLifecycle
			implements BeanNameAware, Verifiable {

		@Autowired
		private ServiceBean fieldService;

		private ServiceBean setterService;

		private String beanName;

		private boolean initialized;

		@Autowired
		public void setSetterService(ServiceBean service) {
			this.setterService = service;
		}

		@PostConstruct
		public void init() {
			this.initialized = true;
		}

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"fieldInjected", fieldService != null,
					"setterInjected", setterService != null,
					"beanName", (beanName != null ? beanName : ""),
					"initialized", initialized);
		}
	}

	public static class LazyWithLifecycle implements Verifiable {

		@Lazy
		@Autowired
		private ServiceBean lazyService;

		private ServiceBean eagerService;

		private boolean initialized;

		@Autowired
		public void setEagerService(ServiceBean service) {
			this.eagerService = service;
		}

		@PostConstruct
		public void init() {
			this.initialized = true;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"lazyInjected", lazyService != null,
					"eagerInjected", eagerService != null,
					"initialized", initialized);
		}
	}

	public static class TargetTypeMixWithLifecycle implements Verifiable {

		@Autowired
		private Optional<ServiceBean> optionalService;

		private Provider<ServiceBean> providerService;

		private boolean initialized;

		@Autowired
		public void setProviderService(Provider<ServiceBean> service) {
			this.providerService = service;
		}

		@PostConstruct
		public void init() {
			this.initialized = true;
		}

		@Override
		public Map<String, Object> snapshot() {
			return Map.of(
					"optionalPresent", optionalService != null && optionalService.isPresent(),
					"providerAvailable", providerService != null && providerService.get() != null,
					"initialized", initialized);
		}
	}
}
