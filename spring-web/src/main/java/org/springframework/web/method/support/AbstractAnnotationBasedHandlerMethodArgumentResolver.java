/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.support;

import java.util.Iterator;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Annotation-based {@link HandlerMethodArgumentResolver} structure, respecting
 * inheritance of method parameter annotations.
 *
 * @author Matt Benson
 * @since 4.3.0
 */
public abstract class AbstractAnnotationBasedHandlerMethodArgumentResolver
		implements HandlerMethodArgumentResolver {

	@Override
	public final boolean supportsParameter(MethodParameter parameter) {
		return supportedMethodParameter(parameter) != null;
	}

	/**
	 * Determine whether the specified {@link MethodParameter} from some level of the
	 * original method's override hierarchy is supported by this resolver.
	 *
	 * @param parameter the method parameter to check
	 * @return {@code true} if this resolver supports the supplied parameter;
	 *         {@code false} otherwise
	 */
	protected abstract boolean supportsLocalParameter(MethodParameter parameter);

	@Override
	public final Object resolveArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		MethodParameter mp = supportedMethodParameter(parameter);
		return (mp == null ? null
				: resolveLocalArgument(mp, mavContainer, webRequest, binderFactory));
	}

	/**
	 * Resolve a {@link MethodParameter} from some level of the original method's
	 * override hierarchy into an argument value from a given message.
	 *
	 * @param parameter the method parameter to resolve. This parameter must have
	 *        previously been passed to
	 *        {@link #supportsParameter(org.springframework.core.MethodParameter)} which
	 *        must have returned {@code true}.
	 * @param message the currently processed message
	 * @return the resolved argument value, or {@code null}
	 * @throws Exception in case of errors with the preparation of argument values
	 */
	protected abstract Object resolveLocalArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception;

	private MethodParameter supportedMethodParameter(MethodParameter parameter) {
		for (Iterator<MethodParameter> parameters = MethodParameterUtils.parameterHierarchy(
				parameter).iterator(); parameters.hasNext();) {
			MethodParameter p = parameters.next();
			if (supportsLocalParameter(p)) {
				return p;
			}
		}
		return null;
	}
}
