/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.HandlerResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler} that encapsulates the view resolution algorithm
 * supporting the following return types:
 * <ul>
 * <li>{@link Void} or no value -- default view name</li>
 * <li>{@link String} -- view name unless {@code @ModelAttribute}-annotated
 * <li>{@link View} -- View to render with
 * <li>{@link Model} -- attributes to add to the model
 * <li>{@link Map} -- attributes to add to the model
 * <li>{@link Rendering} -- use case driven API for view resolution</li>
 * <li>{@link ModelAttribute @ModelAttribute} -- attribute for the model
 * <li>Non-simple value -- attribute for the model
 * </ul>
 *
 * <p>A String-based view name is resolved through the configured
 * {@link ViewResolver} instances into a {@link View} to use for rendering.
 * If a view is left unspecified (e.g. by returning {@code null} or a
 * model-related return value), a default view name is selected.
 *
 * <p>By default this resolver is ordered at {@link Ordered#LOWEST_PRECEDENCE}
 * and generally needs to be late in the order since it interprets any String
 * return value as a view name or any non-simple value type as a model attribute
 * while other result handlers may interpret the same otherwise based on the
 * presence of annotations, e.g. for {@code @ResponseBody}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ViewResolutionResultHandler extends HandlerResultHandlerSupport
		implements HandlerResultHandler, Ordered {

	private static final Object NO_VALUE = new Object();

	private static final Mono<Object> NO_VALUE_MONO = Mono.just(NO_VALUE);


	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final List<View> defaultViews = new ArrayList<>(4);


	/**
	 * Basic constructor with a default {@link ReactiveAdapterRegistry}.
	 * @param viewResolvers the resolver to use
	 * @param contentTypeResolver to determine the requested content type
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers,
			RequestedContentTypeResolver contentTypeResolver) {

		this(viewResolvers, contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Constructor with an {@link ReactiveAdapterRegistry} instance.
	 * @param viewResolvers the view resolver to use
	 * @param contentTypeResolver to determine the requested content type
	 * @param registry for adaptation to reactive types
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers,
			RequestedContentTypeResolver contentTypeResolver, ReactiveAdapterRegistry registry) {

		super(contentTypeResolver, registry);
		this.viewResolvers.addAll(viewResolvers);
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
	public void setDefaultViews(@Nullable List<View> defaultViews) {
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
		if (hasModelAnnotation(result.getReturnTypeSource())) {
			return true;
		}

		Class<?> type = result.getReturnType().getRawClass();
		ReactiveAdapter adapter = getAdapter(result);
		if (adapter != null) {
			if (adapter.isNoValue()) {
				return true;
			}
			type = result.getReturnType().getGeneric().resolve(Object.class);
		}

		return (type != null &&
				(CharSequence.class.isAssignableFrom(type) || Rendering.class.isAssignableFrom(type) ||
						Model.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) ||
						void.class.equals(type) || View.class.isAssignableFrom(type) ||
						!BeanUtils.isSimpleProperty(type)));
	}

	private boolean hasModelAnnotation(MethodParameter parameter) {
		return parameter.hasMethodAnnotation(ModelAttribute.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Mono<Object> valueMono;
		ResolvableType valueType;
		ReactiveAdapter adapter = getAdapter(result);

		if (adapter != null) {
			if (adapter.isMultiValue()) {
				throw new IllegalArgumentException(
						"Multi-value reactive types not supported in view resolution: " + result.getReturnType());
			}

			valueMono = (result.getReturnValue() != null ?
					Mono.from(adapter.toPublisher(result.getReturnValue())) : Mono.empty());

			valueType = (adapter.isNoValue() ? ResolvableType.forClass(Void.class) :
					result.getReturnType().getGeneric());
		}
		else {
			valueMono = Mono.justOrEmpty(result.getReturnValue());
			valueType = result.getReturnType();
		}

		return valueMono
				.switchIfEmpty(exchange.isNotModified() ? Mono.empty() : NO_VALUE_MONO)
				.flatMap(returnValue -> {

					Mono<List<View>> viewsMono;
					Model model = result.getModel();
					MethodParameter parameter = result.getReturnTypeSource();
					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());

					Class<?> clazz = valueType.getRawClass();
					if (clazz == null) {
						clazz = returnValue.getClass();
					}

					if (returnValue == NO_VALUE || Void.class.equals(clazz) || void.class.equals(clazz)) {
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (CharSequence.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						viewsMono = resolveViews(returnValue.toString(), locale);
					}
					else if (Rendering.class.isAssignableFrom(clazz)) {
						Rendering render = (Rendering) returnValue;
						HttpStatus status = render.status();
						if (status != null) {
							exchange.getResponse().setStatusCode(status);
						}
						exchange.getResponse().getHeaders().putAll(render.headers());
						model.addAllAttributes(render.modelAttributes());
						Object view = render.view();
						if (view == null) {
							view = getDefaultViewName(exchange);
						}
						viewsMono = (view instanceof String ? resolveViews((String) view, locale) :
								Mono.just(Collections.singletonList((View) view)));
					}
					else if (Model.class.isAssignableFrom(clazz)) {
						model.addAllAttributes(((Model) returnValue).asMap());
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (Map.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						model.addAllAttributes((Map<String, ?>) returnValue);
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (View.class.isAssignableFrom(clazz)) {
						viewsMono = Mono.just(Collections.singletonList((View) returnValue));
					}
					else {
						String name = getNameForReturnValue(parameter);
						model.addAttribute(name, returnValue);
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}

					updateBindingContext(result.getBindingContext(), exchange);

					return viewsMono.flatMap(views -> render(views, model.asMap(), exchange));
				});
	}

	/**
	 * Select a default view name when a controller did not specify it.
	 * Use the request path the leading and trailing slash stripped.
	 */
	private String getDefaultViewName(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return StringUtils.stripFilenameExtension(path);
	}

	private Mono<List<View>> resolveViews(String viewName, Locale locale) {
		return Flux.fromIterable(getViewResolvers())
				.concatMap(resolver -> resolver.resolveViewName(viewName, locale))
				.collectList()
				.map(views -> {
					if (views.isEmpty()) {
						throw new IllegalStateException(
								"Could not resolve view with name '" + viewName + "'.");
					}
					views.addAll(getDefaultViews());
					return views;
				});
	}

	private String getNameForReturnValue(MethodParameter returnType) {
		return Optional.ofNullable(returnType.getMethodAnnotation(ModelAttribute.class))
				.filter(ann -> StringUtils.hasText(ann.value()))
				.map(ModelAttribute::value)
				.orElse(Conventions.getVariableNameForParameter(returnType));
	}

	private void updateBindingContext(BindingContext context, ServerWebExchange exchange) {
		Map<String, Object> model = context.getModel().asMap();
		model.keySet().stream()
				.filter(name -> isBindingCandidate(name, model.get(name)))
				.filter(name -> !model.containsKey(BindingResult.MODEL_KEY_PREFIX + name))
				.forEach(name -> {
					WebExchangeDataBinder binder = context.createDataBinder(exchange, model.get(name), name);
					model.put(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
				});
	}

	private boolean isBindingCandidate(String name, @Nullable Object value) {
		return (!name.startsWith(BindingResult.MODEL_KEY_PREFIX) && value != null &&
				!value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}

	private Mono<? extends Void> render(List<View> views, Map<String, Object> model,
			ServerWebExchange exchange) {

		for (View view : views) {
			if (view.isRedirectView()) {
				return view.render(model, null, exchange);
			}
		}

		List<MediaType> mediaTypes = getMediaTypes(views);
		MediaType bestMediaType = selectMediaType(exchange, () -> mediaTypes);
		if (bestMediaType != null) {
			for (View view : views) {
				for (MediaType mediaType : view.getSupportedMediaTypes()) {
					if (mediaType.isCompatibleWith(bestMediaType)) {
						return view.render(model, mediaType, exchange);
					}
				}
			}
		}
		throw new NotAcceptableStatusException(mediaTypes);
	}

	private List<MediaType> getMediaTypes(List<View> views) {
		return views.stream()
				.flatMap(view -> view.getSupportedMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
