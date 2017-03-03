/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Convenience class to resolve a method and its parameters based on hints.
 *
 * <h1>Background</h1>
 *
 * <p>When testing annotated methods we create test classes such as
 * "TestController" with a diverse range of method signatures representing
 * supported annotations and argument types. It becomes challenging to use
 * naming strategies to keep track of methods and arguments especially in
 * combination variables for reflection metadata.
 *
 * <p>The idea with {@link ResolvableMethod} is NOT to rely on naming techniques
 * but to use hints to zero in on method parameters. Especially in combination
 * with {@link ResolvableType} such hints can be strongly typed and make tests
 * more readable by being explicit about what is being tested and more robust
 * since the provided hints have to match.
 *
 * <p>Common use cases:
 *
 * <h2>1. Declared Return Type</h2>
 *
 * When testing return types it's common to have many methods with a unique
 * return type, possibly with or without an annotation.
 *
 * <pre>
 *
 * import static org.springframework.web.method.ResolvableMethod.on;
 *
 * // Return type
 * on(TestController.class).resolveReturnType(Foo.class);
 *
 * // Annotation + return type
 * on(TestController.class).annotated(ResponseBody.class).resolveReturnType(Bar.class);
 *
 * // Annotation not present
 * on(TestController.class).isNotAnnotated(ResponseBody.class).resolveReturnType();
 *
 * // Annotation properties
 * on(TestController.class)
 *         .annotated(RequestMapping.class, patterns("/foo"), params("p"))
 *         .annotated(ResponseBody.class)
 *         .resolveReturnType();
 * </pre>
 *
 * <h2>2. Method Arguments</h2>
 *
 * When testing method arguments it's more likely to have one or a small number
 * of methods with a wide array of argument types and parameter annotations.
 *
 * <pre>
 *
 * ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();
 *
 * testMethod.arg(Foo.class);
 * testMethod.annotated(RequestBody.class)).arg(Bar.class);
 * testMethod.annotated(RequestBody.class), required()).arg(Bar.class);
 * testMethod.notAnnotated(RequestBody.class)).arg(Bar.class);
 * </pre>
 *
 * <h3>3. Mock Handler Method Invocation</h3>
 *
 * Locate a method by invoking it through a proxy of the target handler:
 *
 * <pre>
 *
 * ResolvableMethod.on(TestController.class).mockCall(o -> o.handle(null)).method();
 * </pre>
 *
 * @author Rossen Stoyanchev
 */
public class ResolvableMethod {

	private static final Log logger = LogFactory.getLog(ResolvableMethod.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();


	private final Method method;


	private ResolvableMethod(Method method) {
		Assert.notNull(method, "method is required");
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
		return new MethodParameter(this.method, -1);
	}

	/**
	 * Find a unique argument matching the given type.
	 * @param type the expected type
	 */
	public MethodParameter arg(Class<?> type) {
		return new ArgResolver().arg(type);
	}

	/**
	 * Find a unique argument matching the given type.
	 * @param type the expected type
	 */
	public MethodParameter arg(ResolvableType type) {
		return new ArgResolver().arg(type);
	}

	/**
	 * Filter on method arguments that have the given annotation.
	 * @param annotationType the annotation type
	 * @param filter optional filters on the annotation
	 */
	@SafeVarargs
	public final <A extends Annotation> ArgResolver annotated(Class<A> annotationType, Predicate<A>... filter) {
		return new ArgResolver().annotated(annotationType, filter);
	}

	/**
	 * Filter on method arguments that don't have the given annotation type(s).
	 * @param annotationTypes the annotation types
	 */
	@SafeVarargs
	public final ArgResolver notAnnotated(Class<? extends Annotation>... annotationTypes) {
		return new ArgResolver().notAnnotated(annotationTypes);
	}

	/**
	 * Filter on method arguments using customer predicates.
	 */
	@SafeVarargs
	public final ArgResolver filtered(Predicate<MethodParameter>... filter) {
		return new ArgResolver().filtered(filter);
	}


	@Override
	public String toString() {
		return "ResolvableMethod=" + formatMethod();
	}

	private String formatMethod() {
		return this.method().getName() +
				Arrays.stream(this.method.getParameters())
						.map(p -> {
							Annotation[] annots = p.getAnnotations();
							return (annots.length != 0 ? Arrays.toString(annots) : "") + " " + p;
						})
						.collect(Collectors.joining(",\n\t", "(\n\t", "\n)"));
	}


	/**
	 * Main entry point providing access to a {@code ResolvableMethod} builder.
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
		public Builder named(String methodName) {
			addFilter("methodName=" + methodName, m -> m.getName().equals(methodName));
			return this;
		}

		/**
		 * Filter on methods with the given annotation type.
		 * @param annotationType the expected annotation type
		 * @param filter optional filters on the actual annotation
		 */
		@SafeVarargs
		public final <A extends Annotation> Builder annotated(Class<A> annotationType, Predicate<A>... filter) {
			String message = "annotated=" + annotationType.getName();
			addFilter(message, m -> {
				A annot = AnnotatedElementUtils.findMergedAnnotation(m, annotationType);
				return (annot != null && Arrays.stream(filter).allMatch(f -> f.test(annot)));
			});
			return this;
		}

		/**
		 * Filter on methods not annotated with the given annotation type.
		 */
		public final <A extends Annotation> Builder isNotAnnotated(Class<A> annotationType) {
			String message = "notAnnotated=" + annotationType.getName();
			addFilter(message, m -> AnnotationUtils.findAnnotation(m, annotationType) == null);
			return this;
		}

		/**
		 * Filter on methods returning the given type.
		 */
		public Builder returning(Class<?> returnType) {
			return returning(ResolvableType.forClass(returnType));
		}

		/**
		 * Filter on methods returning the given type.
		 */
		public Builder returning(ResolvableType resolvableType) {
			String expected = resolvableType.toString();
			String message = "returnType=" + expected;
			addFilter(message, m -> expected.equals(ResolvableType.forMethodReturnType(m).toString()));
			return this;
		}

		/**
		 * Add custom filters for matching methods.
		 */
		@SafeVarargs
		public final Builder filtered(Predicate<Method>... filters) {
			this.filters.addAll(Arrays.asList(filters));
			return this;
		}

		/**
		 * Build a {@code ResolvableMethod} from the provided filters which must
		 * resolve to a unique, single method.
		 *
		 * <p>See additional resolveXxx shortcut methods going directly to
		 * {@link Method} or return type parameter.
		 *
		 * @throws IllegalStateException for no match or multiple matches
		 */
		public ResolvableMethod build() {
			Set<Method> methods = MethodIntrospector.selectMethods(this.objectClass, this::isMatch);
			Assert.state(!methods.isEmpty(), "No matching method: " + this);
			Assert.state(methods.size() == 1, "Multiple matching methods: " + this + formatMethods(methods));
			return new ResolvableMethod(methods.iterator().next());
		}

		private boolean isMatch(Method method) {
			return this.filters.stream().allMatch(p -> p.test(method));
		}

		private String formatMethods(Set<Method> methods) {
			return "\nMatched:\n" + methods.stream()
					.map(Method::toGenericString).collect(Collectors.joining(",\n\t", "[\n\t", "\n]"));
		}

		public ResolvableMethod mockCall(Consumer<T> invoker) {
			MethodInvocationInterceptor interceptor = new MethodInvocationInterceptor();
			T proxy = initProxy(this.objectClass, interceptor);
			invoker.accept(proxy);
			Method method = interceptor.getInvokedMethod();
			return new ResolvableMethod(method);
		}


		// Build & Resolve shortcuts...

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
		 */
		public MethodParameter resolveReturnType(Class<?> returnType) {
			return returning(returnType).build().returnType();
		}

		/**
		 * Shortcut to the unique return type equivalent to:
		 * <p>{@code returning(returnType).build().returnType()}
		 */
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
					.collect(Collectors.joining(",\n\t\t", "[\n\t\t", "\n\t]"));
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
		 * Filter on method arguments that have the given annotation.
		 * @param annotationType the annotation type
		 * @param filter optional filters on the annotation
		 */
		@SafeVarargs
		public final <A extends Annotation> ArgResolver annotated(Class<A> annotationType, Predicate<A>... filter) {
			this.filters.add(param -> {
				A annot = param.getParameterAnnotation(annotationType);
				return (annot != null && Arrays.stream(filter).allMatch(f -> f.test(annot)));
			});
			return this;
		}

		/**
		 * Filter on method arguments that don't have the given annotations.
		 * @param annotationTypes the annotation types
		 */
		@SafeVarargs
		public final ArgResolver notAnnotated(Class<? extends Annotation>... annotationTypes) {
			this.filters.add(p -> Arrays.stream(annotationTypes).noneMatch(p::hasParameterAnnotation));
			return this;
		}

		/**
		 * Filter on method arguments using customer predicates.
		 */
		@SafeVarargs
		public final ArgResolver filtered(Predicate<MethodParameter>... filter) {
			this.filters.addAll(Arrays.asList(filter));
			return this;
		}

		/**
		 * Resolve the argument also matching to the given type.
		 * @param type the expected type
		 */
		public MethodParameter arg(Class<?> type) {
			this.filters.add(p -> type.equals(p.getParameterType()));
			return arg(ResolvableType.forClass(type));
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
			Assert.state(!matches.isEmpty(), () -> "No matching arg in method\n" + formatMethod());
			Assert.state(matches.size() == 1, () -> "Multiple matching args in method\n" + formatMethod());
			return matches.get(0);
		}


		private List<MethodParameter> applyFilters() {
			List<MethodParameter> matches = new ArrayList<>();
			for (int i = 0; i < method.getParameterCount(); i++) {
				MethodParameter param = new MethodParameter(method, i);
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
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}
	}

}
