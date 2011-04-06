/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.annotation.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Abstract base class for argument resolvers that resolve named values.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ConfigurableBeanFactory beanFactory;

	private final BeanExpressionContext expressionContext;

	private Map<MethodParameter, NamedValueInfo> namedValueInfoCache =
			new ConcurrentHashMap<MethodParameter, NamedValueInfo>();

	public AbstractNamedValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.expressionContext = (beanFactory != null) ? new BeanExpressionContext(beanFactory, new RequestScope()) : null;
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public final Object resolveArgument(MethodParameter parameter,
										ModelMap model,
										NativeWebRequest webRequest, 
										WebDataBinderFactory binderFactory) throws Exception {
		Class<?> paramType = parameter.getParameterType();

		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);

		Object arg = resolveNamedValueArgument(webRequest, parameter, namedValueInfo.name);

		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveDefaultValue(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required) {
				handleMissingValue(namedValueInfo.name, parameter);
			}
			arg = checkForNull(namedValueInfo.name, arg, paramType);
		}

		if (binderFactory != null) {
			WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
			return binder.convertIfNecessary(arg, paramType, parameter);
		}
		else {
			return arg;
		}
	}

	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo result = namedValueInfoCache.get(parameter);
		if (result == null) {
			NamedValueInfo info = createNamedValueInfo(parameter);
			String name = info.name;
			if (name.length() == 0) {
				name = parameter.getParameterName();
				if (name == null) {
					throw new IllegalStateException("No parameter name specified for argument of type [" +
							parameter.getParameterType().getName() +
							"], and no parameter name information found in class file either.");
				}
			}
			boolean required = info.required;
			String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);

			result = new NamedValueInfo(name, required, defaultValue);
			namedValueInfoCache.put(parameter, result);
		}
		return result;
	}

	/**
	 * Creates a new {@link NamedValueInfo} object for the given method parameter.
	 *
	 * <p>Implementations typically retrieve the method annotation by means of {@link
	 * MethodParameter#getParameterAnnotation(Class)}.
	 *
	 * @param parameter the method parameter
	 * @return the named value information
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * Resolves the given parameter into a method argument.
	 *
	 * @param webRequest the current web request, allowing access to the native request as well
	 * @param parameter the parameter to resolve to an argument. This parameter must have previously been passed to the
	 * {@link #supportsParameter(org.springframework.core.MethodParameter)} method of this interface, which must have
	 * returned {@code true}.
	 * @param name the name
	 * @return the resolved argument. May be {@code null}.
	 * @throws Exception in case of errors
	 */
	protected abstract Object resolveNamedValueArgument(NativeWebRequest webRequest,
														MethodParameter parameter,
														String name) throws Exception;

	private Object resolveDefaultValue(String value) {
		if (beanFactory == null) {
			return value;
		}
		String placeholdersResolved = beanFactory.resolveEmbeddedValue(value);
		BeanExpressionResolver exprResolver = beanFactory.getBeanExpressionResolver();
		if (exprResolver == null) {
			return value;
		}
		return exprResolver.evaluate(placeholdersResolved, expressionContext);
	}

	/**
	 * Invoked when a named value is required, but 
	 * {@link #resolveNamedValueArgument(NativeWebRequest, MethodParameter, String)} returned {@code null} 
	 * and there is no default value set.
	 *
	 * <p>Concrete subclasses typically throw an exception in this scenario.
	 *
	 * @param name the name
	 * @param parameter the method parameter
	 */
	protected abstract void handleMissingValue(String name, MethodParameter parameter) throws ServletException;

	private Object checkForNull(String name, Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being declared as a " +
						"primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		return value;
	}

	/**
	 * Represents the information about a named value, including name, whether it's required and a default value.
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		private final String defaultValue;

		protected NamedValueInfo(String name, boolean required, String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}

	}

}
