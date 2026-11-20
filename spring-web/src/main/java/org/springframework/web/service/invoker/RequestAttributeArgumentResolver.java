/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestAttribute;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestAttribute @RequestAttribute}
 * annotated arguments.
 *
 * <p>The argument may be a single variable value or a {@code Map} with multiple
 * variables and values.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestAttribute#required()} is set to {@code false}
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class RequestAttributeArgumentResolver extends AbstractNamedValueArgumentResolver {


	@Override
	protected @Nullable NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute annot = parameter.getParameterAnnotation(RequestAttribute.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), null, "request attribute", false));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.addAttribute(name, value);
	}

}
