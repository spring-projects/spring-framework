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
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.MethodValidator;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

/**
 * {@link MethodValidator} that uses a Bean Validation
 * {@link jakarta.validation.Validator} for validation, and adapts
 * {@link ConstraintViolation}s to {@link MethodValidationResult}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class MethodValidationAdapter implements MethodValidator {

	private static final MethodValidationResult emptyValidationResult = MethodValidationResult.emptyResult();

	private static final ObjectNameResolver defaultObjectNameResolver = new DefaultObjectNameResolver();

	private static final Comparator<ParameterValidationResult> resultComparator = new ResultComparator();


	private final Supplier<Validator> validator;

	private final Supplier<SpringValidatorAdapter> validatorAdapter;

	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private ObjectNameResolver objectNameResolver = defaultObjectNameResolver;


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
	 * Set the {@code ParameterNameDiscoverer} to discover method parameter names
	 * with to create error codes for {@link MessageSourceResolvable}. Used only
	 * when {@link MethodParameter}s are not passed into
	 * {@link #validateArguments} or {@link #validateReturnValue}.
	 * <p>Default is {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the {@link #setParameterNameDiscoverer configured}
	 * {@code ParameterNameDiscoverer}.
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Configure a resolver to determine the name of an {@code @Valid} method
	 * parameter to use for its {@link BindingResult}. This allows aligning with
	 * a higher level programming model such as to resolve the name of an
	 * {@code @ModelAttribute} method parameter in Spring MVC.
	 * <p>By default, the object name is resolved through:
	 * <ul>
	 * <li>{@link MethodParameter#getParameterName()} for input parameters
	 * <li>{@link Conventions#getVariableNameForReturnType(Method, Class, Object)}
	 * for a return type
	 * </ul>
	 * If a name cannot be determined, e.g. a return value with insufficient
	 * type information, then it defaults to one of:
	 * <ul>
	 * <li>{@code "{methodName}.arg{index}"} for input parameters
	 * <li>{@code "{methodName}.returnValue"} for a return type
	 * </ul>
	 */
	public void setObjectNameResolver(ObjectNameResolver nameResolver) {
		this.objectNameResolver = nameResolver;
	}


	/**
	 * {@inheritDoc}.
	 * <p>Default are the validation groups as specified in the {@link Validated}
	 * annotation on the method, or on the containing target class of the method,
	 * or for an AOP proxy without a target (with all behavior in advisors), also
	 * check on proxied interfaces.
	 */
	@Override
	public Class<?>[] determineValidationGroups(Object target, Method method) {
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

	@Override
	public final MethodValidationResult validateArguments(
			Object target, Method method, @Nullable MethodParameter[] parameters, Object[] arguments,
			Class<?>[] groups) {

		Set<ConstraintViolation<Object>> violations =
				invokeValidatorForArguments(target, method, arguments, groups);

		if (violations.isEmpty()) {
			return emptyValidationResult;
		}

		return adaptViolations(target, method, violations,
				i -> parameters != null ? parameters[i] : initMethodParameter(method, i),
				i -> arguments[i]);
	}

	/**
	 * Invoke the validator, and return the resulting violations.
	 */
	public final Set<ConstraintViolation<Object>> invokeValidatorForArguments(
			Object target, Method method, Object[] arguments, Class<?>[] groups) {

		ExecutableValidator execVal = this.validator.get().forExecutables();
		Set<ConstraintViolation<Object>> violations;
		try {
			violations = execVal.validateParameters(target, method, arguments, groups);
		}
		catch (IllegalArgumentException ex) {
			// Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
			// Let's try to find the bridged method on the implementation class...
			Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, target.getClass());
			Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(mostSpecificMethod);
			violations = execVal.validateParameters(target, bridgedMethod, arguments, groups);
		}
		return violations;
	}

	@Override
	public final MethodValidationResult validateReturnValue(
			Object target, Method method, @Nullable MethodParameter returnType, @Nullable Object returnValue,
			Class<?>[] groups) {

		Set<ConstraintViolation<Object>> violations =
				invokeValidatorForReturnValue(target, method, returnValue, groups);

		if (violations.isEmpty()) {
			return emptyValidationResult;
		}

		return adaptViolations(target, method, violations,
				i -> returnType != null ? returnType : initMethodParameter(method, -1),
				i -> returnValue);
	}

	/**
	 * Invoke the validator, and return the resulting violations.
	 */
	public final Set<ConstraintViolation<Object>> invokeValidatorForReturnValue(
			Object target, Method method, @Nullable Object returnValue, Class<?>[] groups) {

		ExecutableValidator execVal = this.validator.get().forExecutables();
		return execVal.validateReturnValue(target, method, returnValue, groups);
	}

	private MethodValidationResult adaptViolations(
			Object target, Method method, Set<ConstraintViolation<Object>> violations,
			Function<Integer, MethodParameter> parameterFunction,
			Function<Integer, Object> argumentFunction) {

		Map<MethodParameter, ValueResultBuilder> parameterViolations = new LinkedHashMap<>();
		Map<Path.Node, BeanResultBuilder> cascadedViolations = new LinkedHashMap<>();

		for (ConstraintViolation<Object> violation : violations) {
			Iterator<Path.Node> itr = violation.getPropertyPath().iterator();
			while (itr.hasNext()) {
				Path.Node node = itr.next();

				MethodParameter parameter;
				if (node.getKind().equals(ElementKind.PARAMETER)) {
					int index = node.as(Path.ParameterNode.class).getParameterIndex();
					parameter = parameterFunction.apply(index);
				}
				else if (node.getKind().equals(ElementKind.RETURN_VALUE)) {
					parameter = parameterFunction.apply(-1);
				}
				else {
					continue;
				}

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
		validatonResultList.sort(resultComparator);

		return MethodValidationResult.create(target, method, validatonResultList);
	}

	private MethodParameter initMethodParameter(Method method, int index) {
		MethodParameter	parameter = new MethodParameter(method, index);
		parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
		return parameter;
	}

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

	private BindingResult createBindingResult(MethodParameter parameter, @Nullable Object argument) {
		String objectName = this.objectNameResolver.resolveName(parameter, argument);
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(argument, objectName);
		result.setMessageCodesResolver(this.messageCodesResolver);
		return result;
	}


	/**
	 * Strategy to resolve the name of an {@code @Valid} method parameter to
	 * use for its {@link BindingResult}.
	 */
	public interface ObjectNameResolver {

		/**
		 * Determine the name for the given method argument.
		 * @param parameter the method parameter
		 * @param value the argument value or return value
		 * @return the name to use
		 */
		String resolveName(MethodParameter parameter, @Nullable Object value);

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

		public ValueResultBuilder(Object target, MethodParameter parameter, @Nullable Object argument) {
			this.target = target;
			this.parameter = parameter;
			this.argument = argument;
		}

		public void addViolation(ConstraintViolation<Object> violation) {
			this.resolvableErrors.add(createMessageSourceResolvable(this.target, this.parameter, violation));
		}

		public ParameterValidationResult build() {
			return new ParameterValidationResult(this.parameter, this.argument, this.resolvableErrors);
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
					this.parameter, this.argument, this.errors, this.container,
					this.containerIndex, this.containerKey);
		}
	}


	/**
	 * Default algorithm to select an object name, as described in
	 * {@link #setObjectNameResolver(ObjectNameResolver)}.
	 */
	private static class DefaultObjectNameResolver implements ObjectNameResolver {

		@Override
		public String resolveName(MethodParameter parameter, @Nullable Object value) {
			String objectName = null;
			if (parameter.getParameterIndex() != -1) {
				objectName = parameter.getParameterName();
			}
			else {
				try {
					Method method = parameter.getMethod();
					if (method != null) {
						Class<?> containingClass = parameter.getContainingClass();
						Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
						objectName = Conventions.getVariableNameForReturnType(method, resolvedType, value);
					}
				}
				catch (IllegalArgumentException ex) {
					// insufficient type information
				}
			}
			if (objectName == null) {
				int index = parameter.getParameterIndex();
				objectName = (parameter.getExecutable().getName() + (index != -1 ? ".arg" + index : ".returnValue"));
			}
			return objectName;
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
					if (i != 0) {
						return i;
					}
				}
				i = compareKeys(errors1, errors2);
				return i;
			}
			return 0;
		}

		@SuppressWarnings("unchecked")
		private <E> int compareKeys(ParameterErrors errors1, ParameterErrors errors2) {
			Object key1 = errors1.getContainerKey();
			Object key2 = errors2.getContainerKey();
			if (key1 instanceof Comparable<?> && key2 instanceof Comparable<?>) {
				return ((Comparable<E>) key1).compareTo((E) key2);
			}
			return 0;
		}
	}

}
