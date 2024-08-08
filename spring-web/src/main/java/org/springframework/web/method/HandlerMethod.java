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

package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotatedMethod;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Encapsulates information about a handler method consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getBean() bean}.
 * Provides convenient access to method parameters, the method return value,
 * method annotations, etc.
 *
 * <p>The class may be created with a bean instance or with a bean name
 * (e.g. lazy-init bean, prototype bean). Use {@link #createWithResolvedBean()}
 * to obtain a {@code HandlerMethod} instance with a bean instance resolved
 * through the associated {@link BeanFactory}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class HandlerMethod extends AnnotatedMethod {

	/** Logger that is available to subclasses. */
	protected static final Log logger = LogFactory.getLog(HandlerMethod.class);


	private final Object bean;

	@Nullable
	private final BeanFactory beanFactory;

	@Nullable
	private final MessageSource messageSource;

	private final Class<?> beanType;

	private final boolean validateArguments;

	private final boolean validateReturnValue;

	@Nullable
	private HttpStatusCode responseStatus;

	@Nullable
	private String responseStatusReason;

	@Nullable
	private HandlerMethod resolvedFromHandlerMethod;

	private final String description;


	/**
	 * Create an instance from a bean instance and a method.
	 */
	public HandlerMethod(Object bean, Method method) {
		this(bean, method, null);
	}

	/**
	 * Variant of {@link #HandlerMethod(Object, Method)} that
	 * also accepts a {@link MessageSource} for use from subclasses.
	 * @since 5.3.10
	 */
	protected HandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		super(method);
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = messageSource;
		this.beanType = ClassUtils.getUserClass(bean);
		this.validateArguments = false;
		this.validateReturnValue = false;
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, method);
	}

	/**
	 * Create an instance from a bean instance, method name, and parameter types.
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		super(bean.getClass().getMethod(methodName, parameterTypes));
		this.bean = bean;
		this.beanFactory = null;
		this.messageSource = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.validateArguments = false;
		this.validateReturnValue = false;
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, getMethod());
	}

	/**
	 * Create an instance from a bean name, a method, and a {@code BeanFactory}.
	 * The method {@link #createWithResolvedBean()} may be used later to
	 * re-create the {@code HandlerMethod} with an initialized bean.
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		this(beanName, beanFactory, null, method);
	}

	/**
	 * Variant of {@link #HandlerMethod(String, BeanFactory, Method)} that
	 * also accepts a {@link MessageSource}.
	 */
	public HandlerMethod(
			String beanName, BeanFactory beanFactory,
			@Nullable MessageSource messageSource, Method method) {

		super(method);
		Assert.hasText(beanName, "Bean name is required");
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.messageSource = messageSource;
		Class<?> beanType = beanFactory.getType(beanName);
		if (beanType == null) {
			throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
		}
		this.beanType = ClassUtils.getUserClass(beanType);
		this.validateArguments = false;
		this.validateReturnValue = false;
		evaluateResponseStatus();
		this.description = initDescription(this.beanType, method);
	}

	/**
	 * Copy constructor for use in subclasses.
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		this(handlerMethod, null, false);
	}

	/**
	 * Re-create HandlerMethod with additional input.
	 */
	private HandlerMethod(HandlerMethod handlerMethod, @Nullable Object handler, boolean initValidateFlags) {
		super(handlerMethod);
		this.bean = (handler != null ? handler : handlerMethod.bean);
		this.beanFactory = handlerMethod.beanFactory;
		this.messageSource = handlerMethod.messageSource;
		this.beanType = handlerMethod.beanType;
		this.validateArguments = (initValidateFlags ?
				MethodValidationInitializer.checkArguments(this.beanType, getMethodParameters()) :
				handlerMethod.validateArguments);
		this.validateReturnValue = (initValidateFlags ?
				MethodValidationInitializer.checkReturnValue(this.beanType, getBridgedMethod()) :
				handlerMethod.validateReturnValue);
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.resolvedFromHandlerMethod = handlerMethod;
		this.description = handlerMethod.toString();
	}


	private void evaluateResponseStatus() {
		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
		if (annotation == null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
		}
		if (annotation != null) {
			String reason = annotation.reason();
			String resolvedReason = (StringUtils.hasText(reason) && this.messageSource != null ?
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);

			this.responseStatus = annotation.code();
			this.responseStatusReason = resolvedReason;
			if (StringUtils.hasText(this.responseStatusReason) && getMethod().getReturnType() != void.class) {
				logger.warn("Return value of [" + getMethod() + "] will be ignored since @ResponseStatus 'reason' attribute is set.");
			}
		}
	}

	private static String initDescription(Class<?> beanType, Method method) {
		StringJoiner joiner = new StringJoiner(", ", "(", ")");
		for (Class<?> paramType : method.getParameterTypes()) {
			joiner.add(paramType.getSimpleName());
		}
		return beanType.getName() + "#" + method.getName() + joiner;
	}


	/**
	 * Return the bean for this handler method.
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * This method returns the type of the handler for this handler method.
	 * <p>Note that if the bean type is a CGLIB-generated class, the original
	 * user-defined class is returned.
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	@Override
	protected Class<?> getContainingClass() {
		return this.beanType;
	}

	/**
	 * Whether the method arguments are a candidate for method validation, which
	 * is the case when there are parameter {@code jakarta.validation.Constraint}
	 * annotations.
	 * <p>The presence of {@code jakarta.validation.Valid} by itself does not
	 * trigger method validation since such parameters are already validated at
	 * the level of argument resolvers.
	 * <p><strong>Note:</strong> if the class is annotated with {@link Validated},
	 * this method returns false, deferring to method validation via AOP proxy.
	 * @since 6.1
	 */
	public boolean shouldValidateArguments() {
		return this.validateArguments;
	}

	/**
	 * Whether the method return value is a candidate for method validation, which
	 * is the case when there are method {@code jakarta.validation.Constraint}
	 * or {@code jakarta.validation.Valid} annotations.
	 * <p><strong>Note:</strong> if the class is annotated with {@link Validated},
	 * this method returns false, deferring to method validation via AOP proxy.
	 * @since 6.1
	 */
	public boolean shouldValidateReturnValue() {
		return this.validateReturnValue;
	}

	/**
	 * Return the specified response status, if any.
	 * @since 4.3.8
	 * @see ResponseStatus#code()
	 */
	@Nullable
	protected HttpStatusCode getResponseStatus() {
		return this.responseStatus;
	}

	/**
	 * Return the associated response status reason, if any.
	 * @since 4.3.8
	 * @see ResponseStatus#reason()
	 */
	@Nullable
	protected String getResponseStatusReason() {
		return this.responseStatusReason;
	}

	/**
	 * Return the HandlerMethod from which this HandlerMethod instance was
	 * resolved via {@link #createWithResolvedBean()}.
	 */
	@Nullable
	public HandlerMethod getResolvedFromHandlerMethod() {
		return this.resolvedFromHandlerMethod;
	}

	/**
	 * Re-create the HandlerMethod and initialize
	 * {@link #shouldValidateArguments()} and {@link #shouldValidateReturnValue()}.
	 * @since 6.1.3
	 */
	public HandlerMethod createWithValidateFlags() {
		return new HandlerMethod(this, null, true);
	}

	/**
	 * If the provided instance contains a bean name rather than an object instance,
	 * the bean name is resolved before a {@link HandlerMethod} is created and returned.
	 */
	public HandlerMethod createWithResolvedBean() {
		Object handler = this.bean;
		if (this.bean instanceof String beanName) {
			Assert.state(this.beanFactory != null, "Cannot resolve bean name without BeanFactory");
			handler = this.beanFactory.getBean(beanName);
		}
		Assert.notNull(handler, "No handler instance");
		return new HandlerMethod(this, handler, false);
	}

	/**
	 * Return a short representation of this handler method for log message purposes.
	 * @since 4.3
	 */
	public String getShortLogMessage() {
		return getBeanType().getName() + "#" + getMethod().getName() +
				"[" + getMethod().getParameterCount() + " args]";
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (super.equals(other) && other instanceof HandlerMethod otherMethod && this.bean.equals(otherMethod.bean)));
	}

	@Override
	public int hashCode() {
		return (this.bean.hashCode() * 31 + super.hashCode());
	}

	@Override
	public String toString() {
		return this.description;
	}


	// Support methods for use in subclass variants

	/**
	 * Assert that the target bean class is an instance of the class where the given
	 * method is declared. In some cases the actual controller instance at request-
	 * processing time may be a JDK dynamic proxy (lazy initialization, prototype
	 * beans, and others). {@code @Controller}'s that require proxying should prefer
	 * class-based proxy mechanisms.
	 */
	protected void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean class '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(formatInvokeError(text, args));
		}
	}

	protected String formatInvokeError(String text, Object[] args) {
		String formattedArgs = IntStream.range(0, args.length)
				.mapToObj(i -> (args[i] != null ?
						"[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
						"[" + i + "] [null]"))
				.collect(Collectors.joining(",\n", " ", " "));
		return text + "\n" +
				"Controller [" + getBeanType().getName() + "]\n" +
				"Method [" + getBridgedMethod().toGenericString() + "] " +
				"with argument values:\n" + formattedArgs;
	}


	/**
	 * Checks for the presence of {@code @Constraint} and {@code @Valid}
	 * annotations on the method and method parameters.
	 */
	private static class MethodValidationInitializer {

		private static final boolean BEAN_VALIDATION_PRESENT =
				ClassUtils.isPresent("jakarta.validation.Validator", HandlerMethod.class.getClassLoader());

		private static final Predicate<MergedAnnotation<? extends Annotation>> CONSTRAINT_PREDICATE =
				MergedAnnotationPredicates.typeIn("jakarta.validation.Constraint");

		private static final Predicate<MergedAnnotation<? extends Annotation>> VALID_PREDICATE =
				MergedAnnotationPredicates.typeIn("jakarta.validation.Valid");

		public static boolean checkArguments(Class<?> beanType, MethodParameter[] parameters) {
			if (BEAN_VALIDATION_PRESENT && AnnotationUtils.findAnnotation(beanType, Validated.class) == null) {
				for (MethodParameter param : parameters) {
					MergedAnnotations merged = MergedAnnotations.from(param.getParameterAnnotations());
					if (merged.stream().anyMatch(CONSTRAINT_PREDICATE)) {
						return true;
					}
					Class<?> type = param.getParameterType();
					if (merged.stream().anyMatch(VALID_PREDICATE) && isIndexOrKeyBasedContainer(type)) {
						return true;
					}
					merged = MergedAnnotations.from(getContainerElementAnnotations(param));
					if (merged.stream().anyMatch(CONSTRAINT_PREDICATE.or(VALID_PREDICATE))) {
						return true;
					}
				}
			}
			return false;
		}

		public static boolean checkReturnValue(Class<?> beanType, Method method) {
			if (BEAN_VALIDATION_PRESENT && AnnotationUtils.findAnnotation(beanType, Validated.class) == null) {
				MergedAnnotations merged = MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
				return merged.stream().anyMatch(CONSTRAINT_PREDICATE.or(VALID_PREDICATE));
			}
			return false;
		}

		private static boolean isIndexOrKeyBasedContainer(Class<?> type) {

			// Index or key-based containers only, or MethodValidationAdapter cannot access
			// the element given what is exposed in ConstraintViolation.

			return (List.class.isAssignableFrom(type) || Object[].class.isAssignableFrom(type) ||
					Map.class.isAssignableFrom(type));
		}

		/**
		 * There may be constraints on elements of a container (list, map).
		 */
		private static Annotation[] getContainerElementAnnotations(MethodParameter param) {
			List<Annotation> result = null;
			int i = param.getParameterIndex();
			Method method = param.getMethod();
			if (method != null && method.getAnnotatedParameterTypes()[i] instanceof AnnotatedParameterizedType apt) {
				for (AnnotatedType type : apt.getAnnotatedActualTypeArguments()) {
					for (Annotation annot : type.getAnnotations()) {
						result = (result != null ? result : new ArrayList<>());
						result.add(annot);
					}
				}
			}
			result = (result != null ? result : Collections.emptyList());
			return result.toArray(new Annotation[0]);
		}

	}

}
