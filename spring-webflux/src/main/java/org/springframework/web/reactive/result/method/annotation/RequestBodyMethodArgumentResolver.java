/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading the
 * body of the request through a compatible {@code HttpMessageReader}.
 *
 * <p>An {@code @RequestBody} method argument is also validated if it is
 * annotated with {@code @jakarta.validation.Valid} or
 * {@link org.springframework.validation.annotation.Validated}. Validation
 * failure results in an {@link ServerWebInputException}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RequestBodyMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public RequestBodyMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter param, BindingContext bindingContext, ServerWebExchange exchange) {

		RequestBody ann = param.getParameterAnnotation(RequestBody.class);
		Assert.state(ann != null, "No RequestBody annotation");
		return readBody(param, ann.required(), bindingContext, exchange);
	}

}
