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
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HttpRequestPathHelper;


/**
 * {@code HandlerResultHandler} that performs view resolution by resolving a
 * {@link View} instance first and then rendering the response with it.
 * If the return value is a String, the configured {@link ViewResolver}s will
 * be consulted to resolve that to a {@link View} instance.
 *
 * <p>This result handler should be ordered late relative to other result
 * handlers. See {@link #setOrder(int)} for more details.
 *
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandler implements HandlerResultHandler, Ordered {

	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final ConversionService conversionService;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private final HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();


	/**
	 * Constructor with {@code ViewResolver}s tand a {@code ConversionService}.
	 * @param resolvers the resolver to use
	 * @param service for converting other reactive types (e.g. rx.Single) to Mono
	 */
	public ViewResolutionResultHandler(List<ViewResolver> resolvers, ConversionService service) {
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

	/**
	 * Set the order for this result handler relative to others.
	 * <p>By default this is set to {@link Ordered#LOWEST_PRECEDENCE} and
	 * generally needs to be used late in the order since it interprets any
	 * String return value as a view name while others may interpret the same
	 * otherwise based on annotations (e.g. for {@code @ResponseBody}).
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	// TODO: Support for Model, ModelAndView, @ModelAttribute, Object with no method annotations

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

		Mono<Object> mono;
		ResolvableType elementType;
		ResolvableType returnType = result.getReturnValueType();

		if (this.conversionService.canConvert(returnType.getRawClass(), Mono.class)) {
			Optional<Object> optionalValue = result.getReturnValue();
			if (optionalValue.isPresent()) {
				Mono<?> convertedMono = this.conversionService.convert(optionalValue.get(), Mono.class);
				mono = convertedMono.map(o -> o);
			}
			else {
				mono = Mono.empty();
			}
			elementType = returnType.getGeneric(0);
		}
		else {
			mono = Mono.justOrEmpty(result.getReturnValue());
			elementType = returnType;
		}

		mono = mono.otherwiseIfEmpty(handleMissingReturnValue(exchange, result, elementType));

		return mono.then(returnValue -> {
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
						.otherwiseIfEmpty(handleUnresolvedViewName(viewName))
						.then(view -> {
							Flux<DataBuffer> body = view.render(result, null, exchange);
							return exchange.getResponse().setBody(body);
						});
			}
			else {
				// Eventually for model-related return values (should not happen now)
				return Mono.error(new IllegalStateException("Unexpected return value"));
			}
		});
	}

	private Mono<Object> handleMissingReturnValue(ServerWebExchange exchange, HandlerResult result,
			ResolvableType elementType) {

		if (isStringOrViewReference(elementType.getRawClass())) {
			String defaultViewName = getDefaultViewName(exchange, result);
			if (defaultViewName != null) {
				return Mono.just(defaultViewName);
			}
			else {
				return Mono.error(new IllegalStateException("Handler [" + result.getHandler() + "] " +
						"neither returned a view name nor a View object"));
			}
		}
		else {
			// Eventually for model-related return values (should not happen now)
			return Mono.error(new IllegalStateException("Unexpected return value type"));
		}
	}

	/**
	 * Translate the given request into a default view name. This is useful when
	 * the application leaves the view name unspecified.
	 * <p>The default implementation strips the leading and trailing slash from
	 * the as well as any extension and uses that as the view name.
	 * @return the default view name to use; if {@code null} is returned
	 * processing will result in an IllegalStateException.
	 */
	protected String getDefaultViewName(ServerWebExchange exchange, HandlerResult result) {
		String path = this.pathHelper.getLookupPathForRequest(exchange);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return StringUtils.stripFilenameExtension(path);
	}

	private Mono<View> handleUnresolvedViewName(String viewName) {
		return Mono.error(new IllegalStateException(
				"Could not resolve view with name '" + viewName + "'."));
	}

}
