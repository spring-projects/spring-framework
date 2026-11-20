/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves an argument of type {@link Principal}, similar to
 * {@link ServletRequestMethodArgumentResolver} but irrespective of whether the
 * argument is annotated or not. This is done to enable custom argument
 * resolution of a {@link Principal} argument (with a custom annotation).
 *
 * @author Rossen Stoyanchev
 * @since 5.3.1
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Principal.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new IllegalStateException("Current request is not of type HttpServletRequest: " + webRequest);
		}

		Principal principal = request.getUserPrincipal();
		if (principal != null && !parameter.getParameterType().isInstance(principal)) {
			throw new IllegalStateException("Current user principal is not of type [" +
					parameter.getParameterType().getName() + "]: " + principal);
		}

		return principal;
	}

}
