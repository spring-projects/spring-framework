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

package org.springframework.web.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * An abstract base class adapting a {@link WebArgumentResolver} to the
 * {@link HandlerMethodArgumentResolver} contract.
 *
 * <p><strong>Note:</strong> This class is provided for backwards compatibility.
 * However it is recommended to re-write a {@code WebArgumentResolver} as
 * {@code HandlerMethodArgumentResolver}. Since {@link #supportsParameter}
 * can only be implemented by actually resolving the value and then checking
 * the result is not {@code WebArgumentResolver#UNRESOLVED} any exceptions
 * raised must be absorbed and ignored since it's not clear whether the adapter
 * doesn't support the parameter or whether it failed for an internal reason.
 * The {@code HandlerMethodArgumentResolver} contract also provides access to
 * model attributes and to {@code WebDataBinderFactory} (for type conversion).
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractWebArgumentResolverAdapter implements HandlerMethodArgumentResolver {

	private final Log logger = LogFactory.getLog(getClass());

	private final WebArgumentResolver adaptee;


	/**
	 * Create a new instance.
	 */
	public AbstractWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		Assert.notNull(adaptee, "'adaptee' must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * Actually resolve the value and check the resolved value is not
	 * {@link WebArgumentResolver#UNRESOLVED} absorbing _any_ exceptions.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		try {
			NativeWebRequest webRequest = getWebRequest();
			Object result = this.adaptee.resolveArgument(parameter, webRequest);
			if (result == WebArgumentResolver.UNRESOLVED) {
				return false;
			}
			else {
				return ClassUtils.isAssignableValue(parameter.getParameterType(), result);
			}
		}
		catch (Exception ex) {
			// ignore (see class-level doc)
			if (logger.isDebugEnabled()) {
				logger.debug("Error in checking support for parameter [" + parameter + "]: " + ex.getMessage());
			}
			return false;
		}
	}

	/**
	 * Delegate to the {@link WebArgumentResolver} instance.
	 * @throws IllegalStateException if the resolved value is not assignable
	 * to the method parameter.
	 */
	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();
		Object result = this.adaptee.resolveArgument(parameter, webRequest);
		if (result == WebArgumentResolver.UNRESOLVED || !ClassUtils.isAssignableValue(paramType, result)) {
			throw new IllegalStateException(
					"Standard argument type [" + paramType.getName() + "] in method " + parameter.getMethod() +
					"resolved to incompatible value of type [" + (result != null ? result.getClass() : null) +
					"]. Consider declaring the argument type in a less specific fashion.");
		}
		return result;
	}


	/**
	 * Required for access to NativeWebRequest in {@link #supportsParameter}.
	 */
	protected abstract NativeWebRequest getWebRequest();

}
