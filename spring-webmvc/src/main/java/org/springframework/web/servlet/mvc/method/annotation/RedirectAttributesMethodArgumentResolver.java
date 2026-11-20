/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * Resolves method arguments of type {@link RedirectAttributes}.
 *
 * <p>This resolver must be listed ahead of
 * {@link org.springframework.web.method.annotation.ModelMethodProcessor} and
 * {@link org.springframework.web.method.annotation.MapMethodProcessor},
 * which support {@link Map} and {@link Model} arguments both of which are
 * "super" types of {@code RedirectAttributes} and would also attempt to
 * resolve a {@code RedirectAttributes} argument.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RedirectAttributesMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RedirectAttributes.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "RedirectAttributes argument only supported on regular handler methods");

		ModelMap redirectAttributes;
		if (binderFactory != null) {
			DataBinder dataBinder = binderFactory.createBinder(webRequest, null, DataBinder.DEFAULT_OBJECT_NAME);
			redirectAttributes = new RedirectAttributesModelMap(dataBinder);
		}
		else {
			redirectAttributes = new RedirectAttributesModelMap();
		}
		mavContainer.setRedirectModel(redirectAttributes);
		return redirectAttributes;
	}

}
