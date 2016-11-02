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
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.AbstractHandlerResultHandler;
import org.springframework.web.server.NotAcceptableStatusException;
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
 * <p>By default this resolver is ordered at {@link Ordered#LOWEST_PRECEDENCE}
 * and generally needs to be late in the order since it interprets any String
 * return value as a view name while others may interpret the same otherwise
 * based on annotations (e.g. for {@code @ResponseBody}).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ViewResolutionResultHandler extends AbstractHandlerResultHandler
		implements HandlerResultHandler, Ordered {

	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final List<View> defaultViews = new ArrayList<>(4);

	private final HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();


	/**
	 * Constructor with {@link ViewResolver}s and a {@link RequestedContentTypeResolver}.
	 * @param resolvers the resolver to use
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ViewResolutionResultHandler(List<ViewResolver> resolvers,
			RequestedContentTypeResolver contentTypeResolver) {

		this(resolvers, contentTypeResolver, new ReactiveAdapterRegistry());
	}

	/**
	 * Constructor with {@code ViewResolver}s tand a {@code ConversionService}.
	 * @param resolvers the resolver to use
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting from other reactive types (e.g.
	 * rx.Single) to Mono
	 */
	public ViewResolutionResultHandler(List<ViewResolver> resolvers,
			RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		super(contentTypeResolver, adapterRegistry);
		this.viewResolvers.addAll(resolvers);
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
	}


	/**
	 * Return a read-only list of view resolvers.
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	/**
	 * Set the default views to consider always when resolving view names and
	 * trying to satisfy the best matching content type.
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews.clear();
		if (defaultViews != null) {
			this.defaultViews.addAll(defaultViews);
		}
	}

	/**
	 * Return the configured default {@code View}'s.
	 */
	public List<View> getDefaultViews() {
		return this.defaultViews;
	}

	@Override
	public boolean supports(HandlerResult result) {
		Class<?> clazz = result.getReturnType().getRawClass();
		if (hasModelAttributeAnnotation(result)) {
			return true;
		}
		Optional<Object> optional = result.getReturnValue();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(clazz, optional);
		if (adapter != null) {
			if (adapter.getDescriptor().isNoValue()) {
				return true;
			}
			else {
				clazz = result.getReturnType().getGeneric(0).getRawClass();
				return isSupportedType(clazz);
			}
		}
		else if (isSupportedType(clazz)) {
			return true;
		}
		return false;
	}

	private boolean hasModelAttributeAnnotation(HandlerResult result) {
		MethodParameter returnType = result.getReturnTypeSource();
		return returnType.hasMethodAnnotation(ModelAttribute.class);
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
		ResolvableType returnType = result.getReturnType();

		Optional<Object> optional = result.getReturnValue();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(returnType.getRawClass(), optional);
		if (adapter != null) {
			if (optional.isPresent()) {
				Mono<?> converted = adapter.toMono(optional);
				valueMono = converted.map(o -> o);
			}
			else {
				valueMono = Mono.empty();
			}
			elementType = adapter.getDescriptor().isNoValue() ?
					ResolvableType.forClass(Void.class) : returnType.getGeneric(0);
		}
		else {
			valueMono = Mono.justOrEmpty(result.getReturnValue());
			elementType = returnType;
		}

		Mono<Object> viewMono;
		if (isViewNameOrReference(elementType, result)) {
			Mono<Object> viewName = getDefaultViewNameMono(exchange, result);
			viewMono = valueMono.otherwiseIfEmpty(viewName);
		}
		else {
			viewMono = valueMono.map(value -> updateModel(value, result))
					.defaultIfEmpty(result.getModel())
					.then(model -> getDefaultViewNameMono(exchange, result));
		}
		Map<String, ?> model = result.getModel().asMap();
		return viewMono.then(view -> {
			updateResponseStatus(result.getReturnTypeSource(), exchange);
			if (view instanceof View) {
				return ((View) view).render(model, null, exchange);
			}
			else if (view instanceof CharSequence) {
				String viewName = view.toString();
				Locale locale = Locale.getDefault(); // TODO
				return resolveAndRender(viewName, locale, model, exchange);

			}
			else {
				// Should not happen
				return Mono.error(new IllegalStateException("Unexpected view type"));
			}
		});
	}

	private boolean isViewNameOrReference(ResolvableType elementType, HandlerResult result) {
		Class<?> clazz = elementType.getRawClass();
		return (View.class.isAssignableFrom(clazz) ||
				(CharSequence.class.isAssignableFrom(clazz) && !hasModelAttributeAnnotation(result)));
	}

	private Mono<Object> getDefaultViewNameMono(ServerWebExchange exchange, HandlerResult result) {
		if (exchange.isNotModified()) {
			return Mono.empty();
		}
		String defaultViewName = getDefaultViewName(result, exchange);
		if (defaultViewName != null) {
			return Mono.just(defaultViewName);
		}
		else {
			return Mono.error(new IllegalStateException(
					"Handler [" + result.getHandler() + "] " +
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
	protected String getDefaultViewName(HandlerResult result, ServerWebExchange exchange) {
		String path = this.pathHelper.getLookupPathForRequest(exchange);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return StringUtils.stripFilenameExtension(path);
	}

	@SuppressWarnings("unchecked")
	private Object updateModel(Object value, HandlerResult result) {
		if (value instanceof Model) {
			result.getModel().addAllAttributes(((Model) value).asMap());
		}
		else if (value instanceof Map) {
			result.getModel().addAllAttributes((Map<String, ?>) value);
		}
		else {
			MethodParameter returnType = result.getReturnTypeSource();
			String name = getNameForReturnValue(value, returnType);
			result.getModel().addAttribute(name, value);
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

	private Mono<? extends Void> resolveAndRender(String viewName, Locale locale,
			Map<String, ?> model, ServerWebExchange exchange) {

		return Flux.fromIterable(getViewResolvers())
				.concatMap(resolver -> resolver.resolveViewName(viewName, locale))
				.switchIfEmpty(Mono.error(
						new IllegalStateException(
								"Could not resolve view with name '" + viewName + "'.")))
				.collectList()
				.then(views -> {
					views.addAll(getDefaultViews());

					List<MediaType> producibleTypes = getProducibleMediaTypes(views);
					MediaType bestMediaType = selectMediaType(exchange, () -> producibleTypes);

					if (bestMediaType != null) {
						for (View view : views) {
							for (MediaType supported : view.getSupportedMediaTypes()) {
								if (supported.isCompatibleWith(bestMediaType)) {
									return view.render(model, bestMediaType, exchange);
								}
							}
						}
					}

					return Mono.error(new NotAcceptableStatusException(producibleTypes));
				});
	}

	private List<MediaType> getProducibleMediaTypes(List<View> views) {
		List<MediaType> result = new ArrayList<>();
		views.forEach(view -> result.addAll(view.getSupportedMediaTypes()));
		return result;
	}

}
