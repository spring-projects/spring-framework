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

package org.springframework.messaging.handler.annotation.reactive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class to resolve method arguments from a named value, e.g.
 * message headers or destination variables. Named values could have one or more
 * of a name, a required flag, and a default value.
 *
 * <p>Subclasses only need to define specific steps such as how to obtain named
 * value details from a method parameter, how to resolve to argument values, or
 * how to handle missing values.
 *
 *  <p>A default value string can contain ${...} placeholders and Spring
 * Expression Language {@code #{...}} expressions which will be resolved if a
 * {@link ConfigurableBeanFactory} is supplied to the class constructor.
 *
 * <p>A {@link ConversionService} is used to convert a resolved String argument
 * value to the expected target method parameter type.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	private final ConversionService conversionService;

	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	@Nullable
	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	/**
	 * Constructor with a {@link ConversionService} and a {@link BeanFactory}.
	 * @param conversionService conversion service for converting String values
	 * to the target method parameter type
	 * @param beanFactory a bean factory for resolving {@code ${...}}
	 * placeholders and {@code #{...}} SpEL expressions in default values
	 */
	protected AbstractNamedValueMethodArgumentResolver(ConversionService conversionService,
			@Nullable ConfigurableBeanFactory beanFactory) {

		this.conversionService = conversionService;
		this.configurableBeanFactory = beanFactory;
		this.expressionContext = (beanFactory != null ? new BeanExpressionContext(beanFactory, null) : null);
	}


	@Override
	public Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {

		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		Object arg = resolveArgumentInternal(nestedParameter, message, resolvedName.toString());
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, message);
			}
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
		}

		if (parameter != nestedParameter || !ClassUtils.isAssignableValue(parameter.getParameterType(), arg)) {
			arg = this.conversionService.convert(arg, TypeDescriptor.forObject(arg), new TypeDescriptor(parameter));
		}

		return arg;
	}

	/**
	 * Obtain the named value for the given method parameter.
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			namedValueInfo = createNamedValueInfo(parameter);
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		return namedValueInfo;
	}

	/**
	 * Create the {@link NamedValueInfo} object for the given method parameter.
	 * Implementations typically retrieve the method annotation by means of
	 * {@link MethodParameter#getParameterAnnotation(Class)}.
	 * @param parameter the method parameter
	 * @return the named value information
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * Fall back on the parameter name from the class file if necessary and
	 * replace {@link ValueConstants#DEFAULT_NONE} with null.
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		if (info.name.isEmpty()) {
			name = parameter.getParameterName();
			if (name == null) {
				Class<?> type = parameter.getParameterType();
				throw new IllegalArgumentException(
						"Name for argument of type [" + type.getName() + "] not specified, " +
								"and parameter name information not found in class file either.");
			}
		}
		return new NamedValueInfo(name, info.required,
				ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
	}

	/**
	 * Resolve the given annotation-specified value,
	 * potentially containing placeholders and expressions.
	 */
	@Nullable
	private Object resolveEmbeddedValuesAndExpressions(String value) {
		if (this.configurableBeanFactory == null || this.expressionContext == null) {
			return value;
		}
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
		if (exprResolver == null) {
			return value;
		}
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * Resolves the given parameter type and value name into an argument value.
	 * @param parameter the method parameter to resolve to an argument value
	 * @param message the current request
	 * @param name the name of the value being resolved
	 * @return the resolved argument. May be {@code null}
	 */
	@Nullable
	protected abstract Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name);

	/**
	 * Invoked when a value is required, but {@link #resolveArgumentInternal}
	 * returned {@code null} and there is no default value. Sub-classes can
	 * throw an appropriate exception for this case.
	 * @param name the name for the value
	 * @param parameter the target method parameter
	 * @param message the message being processed
	 */
	protected abstract void handleMissingValue(String name, MethodParameter parameter, Message<?> message);

	/**
	 * One last chance to handle a possible null value.
	 * Specifically for booleans method parameters, use {@link Boolean#FALSE}.
	 * Also raise an ISE for primitive types.
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being " +
						"declared as a primitive type. Consider declaring it as object wrapper " +
						"for the corresponding primitive type.");
			}
		}
		return value;
	}


	/**
	 * Represents a named value declaration.
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		@Nullable
		private final String defaultValue;

		protected NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
