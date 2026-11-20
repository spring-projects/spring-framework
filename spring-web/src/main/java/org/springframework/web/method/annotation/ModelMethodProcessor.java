/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Model} arguments and handles {@link Model} return values.
 *
 * <p>A {@link Model} return type has a set purpose. Therefore this handler
 * should be configured ahead of handlers that support any return value type
 * annotated with {@code @ModelAttribute} or {@code @ResponseBody} to ensure
 * they don't take over.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Model.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAndViewContainer is required for model exposure");
		return mavContainer.getModel();
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return Model.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			return;
		}

		if (returnValue instanceof Model model) {
			mavContainer.addAllAttributes(model.asMap());
		}
		else {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type [" +
					returnType.getParameterType().getName() + "] in method: " + returnType.getMethod());
		}
	}

}
