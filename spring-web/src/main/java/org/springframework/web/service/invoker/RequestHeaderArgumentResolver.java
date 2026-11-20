/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestHeader @RequestHeader}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, ?&gt;}
 * with multiple headers and value(s).
 * <li>{@code Collection} or an array of header values.
 * <li>An individual header value.
 * </ul>
 *
 * <p>Individual header values may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestHeader#required()} is set to {@code false}
 * <li>{@link RequestHeader#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class RequestHeaderArgumentResolver extends AbstractNamedValueArgumentResolver {


	public RequestHeaderArgumentResolver(ConversionService conversionService) {
		super(conversionService);
	}


	@Override
	protected @Nullable NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader annot = parameter.getParameterAnnotation(RequestHeader.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "request header", true));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.addHeader(name, (String) value);
	}

}
