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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HttpRequestPathHelper;


/**
 * {@code HandlerResultHandler} that encapsulates the view resolution algorithm
 * supporting the following return types:
 * <ul>
 *     <li>String-based view name
 *     <li>Reference to a {@link View}
 *     <li>{@link Model}
 *     <li>{@link Map}
 *     <li>Return types annotated with {@code @ModelAttribute}
 *     <li>{@link BeanUtils#isSimpleProperty Non-simple} return types are
 *     treated as a model attribute
 * </ul>
 *
 * <p>A String-based view name is resolved through the configured
 * {@link ViewResolver} instances into a {@link View} to use for rendering.
 * If a view is left unspecified (e.g. by returning {@code null} or a
 * model-related return value), a default view name is selected.
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

	@Override
	public boolean supports(HandlerResult result) {
		Class<?> clazz = result.getReturnValueType().getRawClass();
		if (hasModelAttributeAnnotation(result)) {
			return true;
		}
		if (isSupportedType(clazz)) {
			return true;
		}
		if (this.conversionService.canConvert(clazz, Mono.class)) {
			clazz = result.getReturnValueType().getGeneric(0).getRawClass();
			return isSupportedType(clazz);
		}
		return false;
	}

	private boolean hasModelAttributeAnnotation(HandlerResult result) {
		if (result.getHandler() instanceof HandlerMethod) {
			MethodParameter returnType = ((HandlerMethod) result.getHandler()).getReturnType();
			if (returnType.hasMethodAnnotation(ModelAttribute.class)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSupportedType(Class<?> clazz) {
		return (CharSequence.class.isAssignableFrom(clazz) || View.class.isAssignableFrom(clazz) ||
				Model.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) ||
				!BeanUtils.isSimpleProperty(clazz));
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Mono<Object> valueMono;
		ResolvableType elementType;
		ResolvableType returnType = result.getReturnValueType();

		if (this.conversionService.canConvert(returnType.getRawClass(), Mono.class)) {
			Optional<Object> optionalValue = result.getReturnValue();
			if (optionalValue.isPresent()) {
				Mono<?> converted = this.conversionService.convert(optionalValue.get(), Mono.class);
				valueMono = converted.map(o -> o);
			}
			else {
				valueMono = Mono.empty();
			}
			elementType = returnType.getGeneric(0);
		}
		else {
			valueMono = Mono.justOrEmpty(result.getReturnValue());
			elementType = returnType;
		}

		Mono<Object> viewMono;
		if (isViewReturnType(result, elementType)) {
			viewMono = valueMono.otherwiseIfEmpty(selectDefaultViewName(exchange, result));
		}
		else {
			viewMono = valueMono.map(value -> updateModel(result, value))
					.defaultIfEmpty(result.getModel())
					.then(model -> selectDefaultViewName(exchange, result));
		}

		return viewMono.then(returnValue -> {
			if (returnValue instanceof View) {
				Flux<DataBuffer> body = ((View) returnValue).render(result, null, exchange);
				return exchange.getResponse().writeWith(body);
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
							return exchange.getResponse().writeWith(body);
						});
			}
			else {
				// Should not happen
				return Mono.error(new IllegalStateException("Unexpected return value"));
			}
		});
	}

	private boolean isViewReturnType(HandlerResult result, ResolvableType elementType) {
		Class<?> clazz = elementType.getRawClass();
		return (View.class.isAssignableFrom(clazz) ||
				(CharSequence.class.isAssignableFrom(clazz) && !hasModelAttributeAnnotation(result)));
	}

	private Mono<Object> selectDefaultViewName(ServerWebExchange exchange, HandlerResult result) {
		String defaultViewName = getDefaultViewName(exchange, result);
		if (defaultViewName != null) {
			return Mono.just(defaultViewName);
		}
		else {
			return Mono.error(new IllegalStateException("Handler [" + result.getHandler() + "] " +
					"neither returned a view name nor a View object"));
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
	@SuppressWarnings("UnusedParameters")
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

	private Object updateModel(HandlerResult result, Object value) {
		if (value instanceof Model) {
			result.getModel().addAllAttributes(((Model) value).asMap());
		}
		else if (value instanceof Map) {
			//noinspection unchecked
			result.getModel().addAllAttributes((Map<String, ?>) value);
		}
		else if (result.getHandler() instanceof HandlerMethod) {
			MethodParameter returnType = ((HandlerMethod) result.getHandler()).getReturnType();
			String name = getNameForReturnValue(value, returnType);
			result.getModel().addAttribute(name, value);
		}
		else {
			result.getModel().addAttribute(value);
		}
		return value;
	}

	/**
	 * Derive the model attribute name for the given return value using one of:
	 * <ol>
	 * <li>The method {@code ModelAttribute} annotation value
	 * <li>The declared return type if it is more specific than {@code Object}
	 * <li>The actual return value type
	 * </ol>
	 * @param returnValue the value returned from a method invocation
	 * @param returnType the return type of the method
	 * @return the model name, never {@code null} nor empty
	 */
	private static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
		ModelAttribute annotation = returnType.getMethodAnnotation(ModelAttribute.class);
		if (annotation != null && StringUtils.hasText(annotation.value())) {
			return annotation.value();
		}
		else {
			Method method = returnType.getMethod();
			Class<?> containingClass = returnType.getContainingClass();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}

	private Mono<View> handleUnresolvedViewName(String viewName) {
		return Mono.error(new IllegalStateException(
				"Could not resolve view with name '" + viewName + "'."));
	}

}
