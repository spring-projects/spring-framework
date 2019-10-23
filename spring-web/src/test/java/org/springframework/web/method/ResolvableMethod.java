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

package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import static java.util.stream.Collectors.joining;

/**
 * Convenience class to resolve to a Method and method parameters.
 *
 * <p>Note that a replica of this class also exists in spring-messaging.
 *
 * <h1>Background</h1>
 *
 * <p>When testing annotated methods we create test classes such as
 * "TestController" with a diverse range of method signatures representing
 * supported annotations and argument types. It becomes challenging to use
 * naming strategies to keep track of methods and arguments especially in
 * combination with variables for reflection metadata.
 *
 * <p>The idea with {@link ResolvableMethod} is NOT to rely on naming techniques
 * but to use hints to zero in on method parameters. Such hints can be strongly
 * typed and explicit about what is being tested.
 *
 * <h2>1. Declared Return Type</h2>
 *
 * When testing return types it's likely to have many methods with a unique
 * return type, possibly with or without an annotation.
 *
 * <pre>
 * import static org.springframework.web.method.ResolvableMethod.on;
 * import static org.springframework.web.method.MvcAnnotationPredicates.requestMapping;
 *
 * // Return type
 * on(TestController.class).resolveReturnType(Foo.class);
 * on(TestController.class).resolveReturnType(List.class, Foo.class);
 * on(TestController.class).resolveReturnType(Mono.class, responseEntity(Foo.class));
 *
 * // Annotation + return type
 * on(TestController.class).annotPresent(RequestMapping.class).resolveReturnType(Bar.class);
 *
 * // Annotation not present
 * on(TestController.class).annotNotPresent(RequestMapping.class).resolveReturnType();
 *
 * // Annotation with attributes
 * on(TestController.class).annot(requestMapping("/foo").params("p")).resolveReturnType();
 * </pre>
 *
 * <h2>2. Method Arguments</h2>
 *
 * When testing method arguments it's more likely to have one or a small number
 * of methods with a wide array of argument types and parameter annotations.
 *
 * <pre>
 * import static org.springframework.web.method.MvcAnnotationPredicates.requestParam;
 *
 * ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();
 *
 * testMethod.arg(Foo.class);
 * testMethod.annotPresent(RequestParam.class).arg(Integer.class);
 * testMethod.annotNotPresent(RequestParam.class)).arg(Integer.class);
 * testMethod.annot(requestParam().name("c").notRequired()).arg(Integer.class);
 * </pre>
 *
 * <h3>3. Mock Handler Method Invocation</h3>
 *
 * Locate a method by invoking it through a proxy of the target handler:
 *
 * <pre>
 * ResolvableMethod.on(TestController.class).mockCall(o -> o.handle(null)).method();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResolvableMethod {

	private static final Log logger = LogFactory.getLog(ResolvableMethod.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	// Matches ValueConstants.DEFAULT_NONE (spring-web and spring-messaging)
	private static final String DEFAULT_VALUE_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";


	private final Method method;


	private ResolvableMethod(Method method) {
		Assert.notNull(method, "'method' is required");
		this.method = method;
	}


	/**
	 * Return the resolved method.
	 */
	public Method method() {
		return this.method;
	}

	/**
	 * Return the declared return type of the resolved method.
	 */
	public MethodParameter returnType() {
		return new SynthesizingMethodParameter(this.method, -1);
	}

	/**
	 * Find a unique argument matching the given type.
	 * @param type the expected type
	 * @param generics optional array of generic types
	 */
	public MethodParameter arg(Class<?> type, Class<?>... generics) {
		return new ArgResolver().arg(type, generics);
	}

	/**
	 * Find a unique argument matching the given type.
	 * @param type the expected type
	 * @param generic at least one generic type
	 * @param generics optional array of generic types
	 */
	public MethodParameter arg(Class<?> type, ResolvableType generic, ResolvableType... generics) {
		return new ArgResolver().arg(type, generic, generics);
	}

	/**
	 * Find a unique argument matching the given type.
	 * @param type the expected type
	 */
	public MethodParameter arg(ResolvableType type) {
		return new ArgResolver().arg(type);
	}

	/**
	 * Filter on method arguments with annotation.
	 * See {@link org.springframework.web.method.MvcAnnotationPredicates}.
	 */
	@SafeVarargs
	public final ArgResolver annot(Predicate<MethodParameter>... filter) {
		return new ArgResolver(filter);
	}

	@SafeVarargs
	public final ArgResolver annotPresent(Class<? extends Annotation>... annotationTypes) {
		return new ArgResolver().annotPresent(annotationTypes);
	}

	/**
	 * Filter on method arguments that don't have the given annotation type(s).
	 * @param annotationTypes the annotation types
	 */
	@SafeVarargs
	public final ArgResolver annotNotPresent(Class<? extends Annotation>... annotationTypes) {
		return new ArgResolver().annotNotPresent(annotationTypes);
	}


	@Override
	public String toString() {
		return "ResolvableMethod=" + formatMethod();
	}


	private String formatMethod() {
		return (method().getName() +
				Arrays.stream(this.method.getParameters())
						.map(this::formatParameter)
						.collect(joining(",\n\t", "(\n\t", "\n)")));
	}

	private String formatParameter(Parameter param) {
		Annotation[] anns = param.getAnnotations();
		return (anns.length > 0 ?
				Arrays.stream(anns).map(this::formatAnnotation).collect(joining(",", "[", "]")) + " " + param :
				param.toString());
	}

	private String formatAnnotation(Annotation annotation) {
		Map<String, Object> map = AnnotationUtils.getAnnotationAttributes(annotation);
		map.forEach((key, value) -> {
			if (value.equals(DEFAULT_VALUE_NONE)) {
				map.put(key, "NONE");
			}
		});
		return annotation.annotationType().getName() + map;
	}

	private static ResolvableType toResolvableType(Class<?> type, Class<?>... generics) {
		return (ObjectUtils.isEmpty(generics) ? ResolvableType.forClass(type) :
				ResolvableType.forClassWithGenerics(type, generics));
	}

	private static ResolvableType toResolvableType(Class<?> type, ResolvableType generic, ResolvableType... generics) {
		ResolvableType[] genericTypes = new ResolvableType[generics.length + 1];
		genericTypes[0] = generic;
		System.arraycopy(generics, 0, genericTypes, 1, generics.length);
		return ResolvableType.forClassWithGenerics(type, genericTypes);
	}


	/**
	 * Create a {@code ResolvableMethod} builder for the given handler class.
	 */
	public static <T> Builder<T> on(Class<T> objectClass) {
		return new Builder<>(objectClass);
	}


	/**
	 * Builder for {@code ResolvableMethod}.
	 */
	public static class Builder<T> {

		private final Class<?> objectClass;

		private final List<Predicate<Method>> filters = new ArrayList<>(4);


		private Builder(Class<?> objectClass) {
			Assert.notNull(objectClass, "Class must not be null");
			this.objectClass = objectClass;
		}


		private void addFilter(String message, Predicate<Method> filter) {
			this.filters.add(new LabeledPredicate<>(message, filter));
		}

		/**
		 * Filter on methods with the given name.
		 */
		public Builder<T> named(String methodName) {
			addFilter("methodName=" + methodName, method -> method.getName().equals(methodName));
			return this;
		}

		/**
		 * Filter on methods with the given parameter types.
		 */
		public Builder<T> argTypes(Class<?>... argTypes) {
			addFilter("argTypes=" + Arrays.toString(argTypes), method ->
					ObjectUtils.isEmpty(argTypes) ? method.getParameterCount() == 0 :
							Arrays.equals(method.getParameterTypes(), argTypes));
			return this;
		}

		/**
		 * Filter on annotated methods.
		 * See {@link org.springframework.web.method.MvcAnnotationPredicates}.
		 */
		@SafeVarargs
		public final Builder<T> annot(Predicate<Method>... filters) {
			this.filters.addAll(Arrays.asList(filters));
			return this;
		}

		/**
		 * Filter on methods annotated with the given annotation type.
		 * @see #annot(Predicate[])
		 * See {@link org.springframework.web.method.MvcAnnotationPredicates}.
		 */
		@SafeVarargs
		public final Builder<T> annotPresent(Class<? extends Annotation>... annotationTypes) {
			String message = "annotationPresent=" + Arrays.toString(annotationTypes);
			addFilter(message, method ->
					Arrays.stream(annotationTypes).allMatch(annotType ->
							AnnotatedElementUtils.findMergedAnnotation(method, annotType) != null));
			return this;
		}

		/**
		 * Filter on methods not annotated with the given annotation type.
		 */
		@SafeVarargs
		public final Builder<T> annotNotPresent(Class<? extends Annotation>... annotationTypes) {
			String message = "annotationNotPresent=" + Arrays.toString(annotationTypes);
			addFilter(message, method -> {
				if (annotationTypes.length != 0) {
					return Arrays.stream(annotationTypes).noneMatch(annotType ->
							AnnotatedElementUtils.findMergedAnnotation(method, annotType) != null);
				}
				else {
					return method.getAnnotations().length == 0;
				}
			});
			return this;
		}

		/**
		 * Filter on methods returning the given type.
		 * @param returnType the return type
		 * @param generics optional array of generic types
		 */
		public Builder<T> returning(Class<?> returnType, Class<?>... generics) {
			return returning(toResolvableType(returnType, generics));
		}

		/**
		 * Filter on methods returning the given type with generics.
		 * @param returnType the return type
		 * @param generic at least one generic type
		 * @param generics optional extra generic types
		 */
		public Builder<T> returning(Class<?> returnType, ResolvableType generic, ResolvableType... generics) {
			return returning(toResolvableType(returnType, generic, generics));
		}

		/**
		 * Filter on methods returning the given type.
		 * @param returnType the return type
		 */
		public Builder<T> returning(ResolvableType returnType) {
			String expected = returnType.toString();
			String message = "returnType=" + expected;
			addFilter(message, m -> expected.equals(ResolvableType.forMethodReturnType(m).toString()));
			return this;
		}

		/**
		 * Build a {@code ResolvableMethod} from the provided filters which must
		 * resolve to a unique, single method.
		 * <p>See additional resolveXxx shortcut methods going directly to
		 * {@link Method} or return type parameter.
		 * @throws IllegalStateException for no match or multiple matches
		 */
		public ResolvableMethod build() {
			Set<Method> methods = MethodIntrospector.selectMethods(this.objectClass, this::isMatch);
			Assert.state(!methods.isEmpty(), () -> "No matching method: " + this);
			Assert.state(methods.size() == 1, () -> "Multiple matching methods: " + this + formatMethods(methods));
			return new ResolvableMethod(methods.iterator().next());
		}

		private boolean isMatch(Method method) {
			return this.filters.stream().allMatch(p -> p.test(method));
		}

		private String formatMethods(Set<Method> methods) {
			return "\nMatched:\n" + methods.stream()
					.map(Method::toGenericString).collect(joining(",\n\t", "[\n\t", "\n]"));
		}

		public ResolvableMethod mockCall(Consumer<T> invoker) {
			MethodInvocationInterceptor interceptor = new MethodInvocationInterceptor();
			T proxy = initProxy(this.objectClass, interceptor);
			invoker.accept(proxy);
			Method method = interceptor.getInvokedMethod();
			return new ResolvableMethod(method);
		}


		// Build & resolve shortcuts...

		/**
		 * Resolve and return the {@code Method} equivalent to:
		 * <p>{@code build().method()}
		 */
		public final Method resolveMethod() {
			return build().method();
		}

		/**
		 * Resolve and return the {@code Method} equivalent to:
		 * <p>{@code named(methodName).build().method()}
		 */
		public Method resolveMethod(String methodName) {
			return named(methodName).build().method();
		}

		/**
		 * Resolve and return the declared return type equivalent to:
		 * <p>{@code build().returnType()}
		 */
		public final MethodParameter resolveReturnType() {
			return build().returnType();
		}

		/**
		 * Shortcut to the unique return type equivalent to:
		 * <p>{@code returning(returnType).build().returnType()}
		 * @param returnType the return type
		 * @param generics optional array of generic types
		 */
		public MethodParameter resolveReturnType(Class<?> returnType, Class<?>... generics) {
			return returning(returnType, generics).build().returnType();
		}

		/**
		 * Shortcut to the unique return type equivalent to:
		 * <p>{@code returning(returnType).build().returnType()}
		 * @param returnType the return type
		 * @param generic at least one generic type
		 * @param generics optional extra generic types
		 */
		public MethodParameter resolveReturnType(Class<?> returnType, ResolvableType generic,
				ResolvableType... generics) {

			return returning(returnType, generic, generics).build().returnType();
		}

		public MethodParameter resolveReturnType(ResolvableType returnType) {
			return returning(returnType).build().returnType();
		}


		@Override
		public String toString() {
			return "ResolvableMethod.Builder[\n" +
					"\tobjectClass = " + this.objectClass.getName() + ",\n" +
					"\tfilters = " + formatFilters() + "\n]";
		}

		private String formatFilters() {
			return this.filters.stream().map(Object::toString)
					.collect(joining(",\n\t\t", "[\n\t\t", "\n\t]"));
		}
	}


	/**
	 * Predicate with a descriptive label.
	 */
	private static class LabeledPredicate<T> implements Predicate<T> {

		private final String label;

		private final Predicate<T> delegate;


		private LabeledPredicate(String label, Predicate<T> delegate) {
			this.label = label;
			this.delegate = delegate;
		}


		@Override
		public boolean test(T method) {
			return this.delegate.test(method);
		}

		@Override
		public Predicate<T> and(Predicate<? super T> other) {
			return this.delegate.and(other);
		}

		@Override
		public Predicate<T> negate() {
			return this.delegate.negate();
		}

		@Override
		public Predicate<T> or(Predicate<? super T> other) {
			return this.delegate.or(other);
		}

		@Override
		public String toString() {
			return this.label;
		}
	}


	/**
	 * Resolver for method arguments.
	 */
	public class ArgResolver {

		private final List<Predicate<MethodParameter>> filters = new ArrayList<>(4);


		@SafeVarargs
		private ArgResolver(Predicate<MethodParameter>... filter) {
			this.filters.addAll(Arrays.asList(filter));
		}

		/**
		 * Filter on method arguments with annotations.
		 * See {@link org.springframework.web.method.MvcAnnotationPredicates}.
		 */
		@SafeVarargs
		public final ArgResolver annot(Predicate<MethodParameter>... filters) {
			this.filters.addAll(Arrays.asList(filters));
			return this;
		}

		/**
		 * Filter on method arguments that have the given annotations.
		 * @param annotationTypes the annotation types
		 * @see #annot(Predicate[])
		 * See {@link org.springframework.web.method.MvcAnnotationPredicates}.
		 */
		@SafeVarargs
		public final ArgResolver annotPresent(Class<? extends Annotation>... annotationTypes) {
			this.filters.add(param -> Arrays.stream(annotationTypes).allMatch(param::hasParameterAnnotation));
			return this;
		}

		/**
		 * Filter on method arguments that don't have the given annotations.
		 * @param annotationTypes the annotation types
		 */
		@SafeVarargs
		public final ArgResolver annotNotPresent(Class<? extends Annotation>... annotationTypes) {
			this.filters.add(param ->
					(annotationTypes.length > 0 ?
							Arrays.stream(annotationTypes).noneMatch(param::hasParameterAnnotation) :
							param.getParameterAnnotations().length == 0));
			return this;
		}

		/**
		 * Resolve the argument also matching to the given type.
		 * @param type the expected type
		 */
		public MethodParameter arg(Class<?> type, Class<?>... generics) {
			return arg(toResolvableType(type, generics));
		}

		/**
		 * Resolve the argument also matching to the given type.
		 * @param type the expected type
		 */
		public MethodParameter arg(Class<?> type, ResolvableType generic, ResolvableType... generics) {
			return arg(toResolvableType(type, generic, generics));
		}

		/**
		 * Resolve the argument also matching to the given type.
		 * @param type the expected type
		 */
		public MethodParameter arg(ResolvableType type) {
			this.filters.add(p -> type.toString().equals(ResolvableType.forMethodParameter(p).toString()));
			return arg();
		}

		/**
		 * Resolve the argument.
		 */
		public final MethodParameter arg() {
			List<MethodParameter> matches = applyFilters();
			Assert.state(!matches.isEmpty(), () ->
					"No matching arg in method\n" + formatMethod());
			Assert.state(matches.size() == 1, () ->
					"Multiple matching args in method\n" + formatMethod() + "\nMatches:\n\t" + matches);
			return matches.get(0);
		}


		private List<MethodParameter> applyFilters() {
			List<MethodParameter> matches = new ArrayList<>();
			for (int i = 0; i < method.getParameterCount(); i++) {
				MethodParameter param = new SynthesizingMethodParameter(method, i);
				param.initParameterNameDiscovery(nameDiscoverer);
				if (this.filters.stream().allMatch(p -> p.test(param))) {
					matches.add(param);
				}
			}
			return matches;
		}
	}


	private static class MethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor {

		private Method invokedMethod;


		Method getInvokedMethod() {
			return this.invokedMethod;
		}

		@Override
		@Nullable
		public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy) {
			if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, object, args);
			}
			else {
				this.invokedMethod = method;
				return null;
			}
		}

		@Override
		@Nullable
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T initProxy(Class<?> type, MethodInvocationInterceptor interceptor) {
		Assert.notNull(type, "'type' must not be null");
		if (type.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(type);
			factory.addInterface(Supplier.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}

		else {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(type);
			enhancer.setInterfaces(new Class<?>[] {Supplier.class});
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

			Class<?> proxyClass = enhancer.createClass();
			Object proxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Objenesis failed, falling back to default constructor", ex);
				}
			}

			if (proxy == null) {
				try {
					proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate proxy " +
							"via both Objenesis and default constructor fails as well", ex);
				}
			}

			((Factory) proxy).setCallbacks(new Callback[] {interceptor});
			return (T) proxy;
		}
	}

}
