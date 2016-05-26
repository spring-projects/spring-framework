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
package org.springframework.web.reactive.result.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;


/**
 * {@code HandlerResultHandler} that resolves a String return value from a
 * handler to a {@link View} which is then used to render the response.
 * A handler may also return a {@code View} instance and/or async variants that
 * provide a String view name or a {@code View}.
 *
 * <p>This result handler should be ordered after others that may also interpret
 * a String return value for example in combination with {@code @ResponseBody}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewResolverResultHandler implements HandlerResultHandler, Ordered {

	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final ConversionService conversionService;

	private int order = Ordered.LOWEST_PRECEDENCE;


	public ViewResolverResultHandler(List<ViewResolver> resolvers, ConversionService service) {
		Assert.notEmpty(resolvers, "At least one ViewResolver is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.viewResolvers.addAll(resolvers);
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
		this.conversionService = service;
	}


	/**
	 * Return a read-only list of view resolvers.
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	// TODO: Add support for model-related return value (Model, ModelAndView, @ModelAttribute)

	@Override
	public boolean supports(HandlerResult result) {
		Class<?> clazz = result.getReturnValueType().getRawClass();
		if (isStringOrViewReference(clazz)) {
			return true;
		}
		if (this.conversionService.canConvert(clazz, Mono.class)) {
			clazz = result.getReturnValueType().getGeneric(0).getRawClass();
			return isStringOrViewReference(clazz);
		}
		return false;
	}

	private boolean isStringOrViewReference(Class<?> clazz) {
		return (CharSequence.class.isAssignableFrom(clazz) || View.class.isAssignableFrom(clazz));
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Mono<?> returnValueMono;
		if (this.conversionService.canConvert(result.getReturnValueType().getRawClass(), Mono.class)) {
			returnValueMono = this.conversionService.convert(result.getReturnValue().get(), Mono.class);
		}
		else if (result.getReturnValue().isPresent()) {
			returnValueMono = Mono.just(result.getReturnValue().get());
		}
		else {
			Optional<String> viewName = getDefaultViewName(result, exchange);
			if (viewName.isPresent()) {
				returnValueMono = Mono.just(viewName.get());
			}
			else {
				returnValueMono = Mono.error(new IllegalStateException("Handler [" + result.getHandler() + "] " +
						"neither returned a view name nor a View object"));
			}
		}

		return returnValueMono.then(returnValue -> {
			if (returnValue instanceof View) {
				Flux<DataBuffer> body = ((View) returnValue).render(result, null, exchange);
				return exchange.getResponse().setBody(body);
			}
			else if (returnValue instanceof CharSequence) {
				String viewName = returnValue.toString();
				Locale locale = Locale.getDefault(); // TODO
				return Flux.fromIterable(getViewResolvers())
						.concatMap(resolver -> resolver.resolveViewName(viewName, locale))
						.next()
						.then(view -> {
							Flux<DataBuffer> body = view.render(result, null, exchange);
							return exchange.getResponse().setBody(body);
						});
			}
			else {
				// Should not happen
				return Mono.error(new IllegalStateException(
						"Unexpected return value: " + returnValue.getClass()));
			}
		});
	}

	protected Optional<String> getDefaultViewName(HandlerResult result, ServerWebExchange exchange) {
		return Optional.empty();
	}

}
