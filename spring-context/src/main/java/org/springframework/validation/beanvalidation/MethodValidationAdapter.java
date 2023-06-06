/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Conventions;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.annotation.Validated;

/**
 * Assist with applying method-level validation via
 * {@link jakarta.validation.Validator}, adapt each resulting
 * {@link ConstraintViolation} to {@link ParameterValidationResult}, and
 * raise {@link MethodValidationException}.
 *
 * <p>Used by {@link MethodValidationInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class MethodValidationAdapter {

	private static final Comparator<ParameterValidationResult> RESULT_COMPARATOR = new ResultComparator();


	private final Supplier<Validator> validator;

	private final Supplier<SpringValidatorAdapter> validatorAdapter;

	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	/**
	 * Create an instance using a default JSR-303 validator underneath.
	 */
	@SuppressWarnings("DataFlowIssue")
	public MethodValidationAdapter() {
		this.validator = SingletonSupplier.of(() -> Validation.buildDefaultValidatorFactory().getValidator());
		this.validatorAdapter = SingletonSupplier.of(() -> new SpringValidatorAdapter(this.validator.get()));
	}

	/**
	 * Create an instance using the given JSR-303 ValidatorFactory.
	 * @param validatorFactory the JSR-303 ValidatorFactory to use
	 */
	@SuppressWarnings("DataFlowIssue")
	public MethodValidationAdapter(ValidatorFactory validatorFactory) {
		this.validator = SingletonSupplier.of(validatorFactory::getValidator);
		this.validatorAdapter = SingletonSupplier.of(() -> new SpringValidatorAdapter(this.validator.get()));
	}

	/**
	 * Create an instance using the given JSR-303 Validator.
	 * @param validator the JSR-303 Validator to use
	 */
	public MethodValidationAdapter(Validator validator) {
		this.validator = () -> validator;
		this.validatorAdapter = () -> new SpringValidatorAdapter(validator);
	}

	/**
	 * Create an instance for the supplied (potentially lazily initialized) Validator.
	 * @param validator a Supplier for the Validator to use
	 */
	public MethodValidationAdapter(Supplier<Validator> validator) {
		this.validator = validator;
		this.validatorAdapter = () -> new SpringValidatorAdapter(this.validator.get());
	}


	/**
	 * Set the strategy to use to determine message codes for violations.
	 * <p>Default is a DefaultMessageCodesResolver.
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * Return the {@link #setMessageCodesResolver(MessageCodesResolver) configured}
	 * {@code MessageCodesResolver}.
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * Set the ParameterNameDiscoverer to use to resolve method parameter names
	 * that is in turn used to create error codes for {@link MessageSourceResolvable}.
	 * <p>Default is {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the {@link #setParameterNameDiscoverer(ParameterNameDiscoverer) configured}
	 * {@code ParameterNameDiscoverer}.
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}


	/**
	 * Use this method determine the validation groups to pass into
	 * {@link #validateMethodArguments(Object, Method, Object[], Class[])} and
	 * {@link #validateMethodReturnValue(Object, Method, Object, Class[])}.
	 * <p>Default are the validation groups as specified in the {@link Validated}
	 * annotation on the method, or on the containing target class of the method,
	 * or for an AOP proxy without a target (with all behavior in advisors), also
	 * check on proxied interfaces.
	 * @param target the target Object
	 * @param method the target method
	 * @return the applicable validation groups as a {@code Class} array
	 */
	public static Class<?>[] determineValidationGroups(Object target, Method method) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(method, Validated.class);
		if (validatedAnn == null) {
			if (AopUtils.isAopProxy(target)) {
				for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(target)) {
					validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
					if (validatedAnn != null) {
						break;
					}
				}
			}
			else {
				validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
			}
		}
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}

	/**
	 * Validate the given method arguments and raise {@link ConstraintViolation}
	 * in case of any errors.
	 * @param target the target Object
	 * @param method the target method
	 * @param arguments candidate arguments for a method invocation
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 */
	public void validateMethodArguments(Object target, Method method, Object[] arguments, Class<?>[] groups) {
		ExecutableValidator execVal = this.validator.get().forExecutables();
		Set<ConstraintViolation<Object>> result;
		try {
			result = execVal.validateParameters(target, method, arguments, groups);
		}
		catch (IllegalArgumentException ex) {
			// Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
			// Let's try to find the bridged method on the implementation class...
			Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, target.getClass());
			Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(mostSpecificMethod);
			result = execVal.validateParameters(target, bridgedMethod, arguments, groups);
		}
		if (!result.isEmpty()) {
			throw createException(target, method, result, i -> arguments[i]);
		}
	}

	/**
	 * Validate the given return value and raise {@link ConstraintViolation}
	 * in case of any errors.
	 * @param target the target Object
	 * @param method the target method
	 * @param returnValue value returned from invoking the target method
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 */
	public void validateMethodReturnValue(
			Object target, Method method, @Nullable Object returnValue, Class<?>[] groups) {

		ExecutableValidator execVal = this.validator.get().forExecutables();
		Set<ConstraintViolation<Object>> result = execVal.validateReturnValue(target, method, returnValue, groups);
		if (!result.isEmpty()) {
			throw createException(target, method, result, i -> returnValue);
		}
	}

	private MethodValidationException createException(
			Object target, Method method, Set<ConstraintViolation<Object>> violations,
			Function<Integer, Object> argumentFunction) {

		Map<MethodParameter, ValueResultBuilder> parameterViolations = new LinkedHashMap<>();
		Map<Path.Node, BeanResultBuilder> cascadedViolations = new LinkedHashMap<>();

		for (ConstraintViolation<Object> violation : violations) {
			Iterator<Path.Node> itr = violation.getPropertyPath().iterator();
			while (itr.hasNext()) {
				Path.Node node = itr.next();

				MethodParameter parameter;
				if (node.getKind().equals(ElementKind.PARAMETER)) {
					parameter = new MethodParameter(method, node.as(Path.ParameterNode.class).getParameterIndex());
				}
				else if (node.getKind().equals(ElementKind.RETURN_VALUE)) {
					parameter = new MethodParameter(method, -1);
				}
				else {
					continue;
				}
				parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);

				Object argument = argumentFunction.apply(parameter.getParameterIndex());
				if (!itr.hasNext()) {
					parameterViolations
							.computeIfAbsent(parameter, p -> new ValueResultBuilder(target, parameter, argument))
							.addViolation(violation);
				}
				else {
					cascadedViolations
							.computeIfAbsent(node, n -> new BeanResultBuilder(parameter, argument, itr.next()))
							.addViolation(violation);
				}
				break;
			}
		}

		List<ParameterValidationResult> validatonResultList = new ArrayList<>();
		parameterViolations.forEach((parameter, builder) -> validatonResultList.add(builder.build()));
		cascadedViolations.forEach((node, builder) -> validatonResultList.add(builder.build()));
		validatonResultList.sort(RESULT_COMPARATOR);

		return new MethodValidationException(target, method, validatonResultList, violations);
	}

	/**
	 * Create a {@link MessageSourceResolvable} for the given violation.
	 * @param target target of the method invocation to which validation was applied
	 * @param parameter the method parameter associated with the violation
	 * @param violation the violation
	 * @return the created {@code MessageSourceResolvable}
	 */
	private MessageSourceResolvable createMessageSourceResolvable(
			Object target, MethodParameter parameter, ConstraintViolation<Object> violation) {

		String objectName = Conventions.getVariableName(target) + "#" + parameter.getExecutable().getName();
		String paramName = (parameter.getParameterName() != null ? parameter.getParameterName() : "");
		Class<?> parameterType = parameter.getParameterType();

		ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
		String code = descriptor.getAnnotation().annotationType().getSimpleName();
		String[] codes = this.messageCodesResolver.resolveMessageCodes(code, objectName, paramName, parameterType);
		Object[] arguments = this.validatorAdapter.get().getArgumentsForConstraint(objectName, paramName, descriptor);

		return new DefaultMessageSourceResolvable(codes, arguments, violation.getMessage());
	}

	/**
	 * Select an object name and create a {@link BindingResult} for the argument.
	 * <p>By default, the name is based on the parameter name, or for a return type on
	 * {@link Conventions#getVariableNameForReturnType(Method, Class, Object)}.
	 * <p>If a name cannot be determined for any reason, e.g. a return value with
	 * insufficient type information, then {@code "{methodName}.arg{index}"} is used.
	 * @param parameter the method parameter
	 * @param argument the argument value
	 * @return the determined name
	 */
	private BindingResult createBindingResult(MethodParameter parameter, @Nullable Object argument) {
		// TODO: allow external customization via Function (e.g. from @ModelAttribute + Conventions based on type)
		String objectName = parameter.getParameterName();
		int index = parameter.getParameterIndex();
		if (index == -1) {
			try {
				Method method = parameter.getMethod();
				if (method != null) {
					Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, parameter.getContainingClass());
					objectName = Conventions.getVariableNameForReturnType(method, resolvedType, argument);
				}
			}
			catch (IllegalArgumentException ex) {
				// insufficient type information
			}
		}
		if (objectName == null) {
			objectName = (parameter.getExecutable().getName() + (index != -1 ? ".arg" + index : ""));
		}
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(argument, objectName);
		result.setMessageCodesResolver(this.messageCodesResolver);
		return result;
	}


	/**
	 * Builds a validation result for a value method parameter with constraints
	 * declared directly on it.
	 */
	private final class ValueResultBuilder {

		private final Object target;

		private final MethodParameter parameter;

		@Nullable
		private final Object argument;

		private final List<MessageSourceResolvable> resolvableErrors = new ArrayList<>();

		private final Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();

		public ValueResultBuilder(Object target, MethodParameter parameter, @Nullable Object argument) {
			this.target = target;
			this.parameter = parameter;
			this.argument = argument;
		}

		public void addViolation(ConstraintViolation<Object> violation) {
			this.violations.add(violation);
			this.resolvableErrors.add(createMessageSourceResolvable(this.target, this.parameter, violation));
		}

		public ParameterValidationResult build() {
			return new ParameterValidationResult(
					this.parameter, this.argument, this.resolvableErrors, this.violations);
		}

	}


	/**
	 * Builds a validation result for an {@link jakarta.validation.Valid @Valid}
	 * annotated bean method parameter with cascaded constraints.
	 */
	private final class BeanResultBuilder {

		private final MethodParameter parameter;

		@Nullable
		private final Object argument;

		@Nullable
		private final Object container;

		@Nullable
		private final Integer containerIndex;

		@Nullable
		private final Object containerKey;

		private final Errors errors;

		private final Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();

		public BeanResultBuilder(MethodParameter parameter, @Nullable Object argument, Path.Node node) {
			this.parameter = parameter;

			this.containerIndex = node.getIndex();
			this.containerKey = node.getKey();
			if (argument instanceof List<?> list && this.containerIndex != null) {
				this.container = list;
				argument = list.get(this.containerIndex);
			}
			else if (argument instanceof Map<?, ?> map && this.containerKey != null) {
				this.container = map;
				argument = map.get(this.containerKey);
			}
			else {
				this.container = null;
			}

			this.argument = argument;
			this.errors = createBindingResult(parameter, argument);
		}

		public void addViolation(ConstraintViolation<Object> violation) {
			this.violations.add(violation);
		}

		public ParameterErrors build() {
			validatorAdapter.get().processConstraintViolations(this.violations, this.errors);
			return new ParameterErrors(
					this.parameter, this.argument, this.errors, this.violations,
					this.container, this.containerIndex, this.containerKey);
		}
	}


	/**
	 * Comparator for validation results, sorted by method parameter index first,
	 * also falling back on container indexes if necessary for cascaded
	 * constraints on a List container.
	 */
	private final static class ResultComparator implements Comparator<ParameterValidationResult> {

		@Override
		public int compare(ParameterValidationResult result1, ParameterValidationResult result2) {
			int index1 = result1.getMethodParameter().getParameterIndex();
			int index2 = result2.getMethodParameter().getParameterIndex();
			int i = Integer.compare(index1, index2);
			if (i != 0) {
				return i;
			}
			if (result1 instanceof ParameterErrors errors1 && result2 instanceof ParameterErrors errors2) {
				Integer containerIndex1 = errors1.getContainerIndex();
				Integer containerIndex2 = errors2.getContainerIndex();
				if (containerIndex1 != null && containerIndex2 != null) {
					i = Integer.compare(containerIndex1, containerIndex2);
					return i;
				}
			}
			return 0;
		}
	}

}
