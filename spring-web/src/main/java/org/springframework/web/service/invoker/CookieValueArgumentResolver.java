/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.CookieValue;

/**
 * {@link HttpServiceArgumentResolver} for {@link CookieValue @CookieValue}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, ?&gt;} with
 * multiple cookies and value(s).
 * <li>{@code Collection} or an array of cookie values.
 * <li>An individual cookie value.
 * </ul>
 *
 * <p>Individual cookie values may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link CookieValue#required()} is set to {@code false}
 * <li>{@link CookieValue#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class CookieValueArgumentResolver extends AbstractNamedValueArgumentResolver {


	public CookieValueArgumentResolver(ConversionService conversionService) {
		super(conversionService);
	}


	@Override
	protected @Nullable NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue annot = parameter.getParameterAnnotation(CookieValue.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "cookie value", true));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.addCookie(name, (String) value);
	}

}
