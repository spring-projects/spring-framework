/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.view;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
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
 * <li>{@link Void}, {@code void}, or no value -- default view name</li>
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
public class ViewResolutionResultHandler extends HandlerResultHandlerSupport implements HandlerResultHandler, Ordered {

	private static final Object NO_VALUE = new Object();

	private static final Mono<Object> NO_VALUE_MONO = Mono.just(NO_VALUE);


	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final List<View> defaultViews = new ArrayList<>(4);

	private final List<StreamHandler> streamHandlers = List.of(new SseStreamHandler());


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

		ResolvableType returnType = result.getReturnType();
		Class<?> type = returnType.toClass();

		ReactiveAdapter adapter = getAdapter(result);
		if (adapter != null) {
			if (adapter.isNoValue()) {
				return true;
			}

			type = returnType.getGeneric().toClass();
			returnType = returnType.getNested(2);

			if (adapter.isMultiValue()) {
				return Fragment.class.isAssignableFrom(type);
			}
		}

		return (CharSequence.class.isAssignableFrom(type) ||
				Rendering.class.isAssignableFrom(type) ||
				FragmentsRendering.class.isAssignableFrom(type) ||
				Model.class.isAssignableFrom(type) ||
				Map.class.isAssignableFrom(type) ||
				View.class.isAssignableFrom(type) ||
				isFragmentCollection(returnType.getNested(2)) ||
				!BeanUtils.isSimpleProperty(type));
	}

	private boolean hasModelAnnotation(MethodParameter parameter) {
		return parameter.hasMethodAnnotation(ModelAttribute.class);
	}

	private static boolean isFragmentCollection(ResolvableType returnType) {
		Class<?> clazz = returnType.resolve(Object.class);
		return (Collection.class.isAssignableFrom(clazz) && Fragment.class.equals(returnType.getNested(2).resolve()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Mono<Object> valueMono;
		ResolvableType valueType;
		ReactiveAdapter adapter = getAdapter(result);

		if (adapter != null) {
			if (adapter.isMultiValue()) {
				valueMono = (result.getReturnValue() != null ?
						Mono.just(FragmentsRendering.withPublisher(adapter.toPublisher(result.getReturnValue())).build()) :
						Mono.empty());

				valueType = ResolvableType.forClass(FragmentsRendering.class);
			}
			else {
				valueMono = (result.getReturnValue() != null ?
						Mono.from(adapter.toPublisher(result.getReturnValue())) : Mono.empty());

				valueType = (adapter.isNoValue() ? ResolvableType.forClass(Void.class) :
						result.getReturnType().getGeneric());
			}
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
					BindingContext bindingContext = result.getBindingContext();
					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());

					Class<?> clazz = valueType.toClass();
					if (clazz == Object.class) {
						clazz = returnValue.getClass();
					}

					if (Collection.class.isAssignableFrom(clazz)) {
						returnValue = FragmentsRendering.withCollection((Collection<Fragment>) returnValue).build();
						clazz = FragmentsRendering.class;
					}

					if (returnValue == NO_VALUE || ClassUtils.isVoidType(clazz)) {
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (CharSequence.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						viewsMono = resolveViews(returnValue.toString(), locale);
					}
					else if (Rendering.class.isAssignableFrom(clazz)) {
						Rendering render = (Rendering) returnValue;
						HttpStatusCode status = render.status();
						if (status != null) {
							exchange.getResponse().setStatusCode(status);
						}
						exchange.getResponse().getHeaders().putAll(render.headers());
						model.addAllAttributes(render.modelAttributes());
						Object view = render.view();
						if (view == null) {
							view = getDefaultViewName(exchange);
						}
						viewsMono = (view instanceof String viewName ? resolveViews(viewName, locale) :
								Mono.just(Collections.singletonList((View) view)));
					}
					else if (FragmentsRendering.class.isAssignableFrom(clazz)) {
						ServerHttpResponse response = exchange.getResponse();
						FragmentsRendering render = (FragmentsRendering) returnValue;
						HttpStatusCode status = render.status();
						if (status != null) {
							response.setStatusCode(status);
						}
						response.getHeaders().putAll(render.headers());
						bindingContext.updateModel(exchange);

						StreamHandler streamHandler = getStreamHandler(exchange);
						if (streamHandler != null) {
							streamHandler.updateResponse(exchange);
						}

						Flux<Flux<DataBuffer>> renderFlux = render.fragments()
								.concatMap(fragment -> renderFragment(fragment, streamHandler, locale, bindingContext, exchange))
								.doOnDiscard(DataBuffer.class, DataBufferUtils::release);

						return response.writeAndFlushWith(renderFlux);
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
					bindingContext.updateModel(exchange);
					return viewsMono.flatMap(views -> render(views, model.asMap(), null, bindingContext, exchange));
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

	private Mono<Flux<DataBuffer>> renderFragment(
			Fragment fragment, @Nullable StreamHandler streamHandler, Locale locale,
			BindingContext bindingContext, ServerWebExchange exchange) {

		// Merge attributes from top-level model
		fragment.mergeAttributes(bindingContext.getModel());

		BodySavingResponse response = new BodySavingResponse(exchange.getResponse());
		ServerWebExchange mutatedExchange = exchange.mutate().response(response).build();

		Mono<List<View>> selectedViews = (fragment.isResolved() ?
				Mono.just(List.of(fragment.view())) :
				resolveViews(fragment.viewName() != null ? fragment.viewName() : getDefaultViewName(exchange), locale));

		Map<String, Object> model = fragment.model();

		if (streamHandler != null) {
			return selectedViews.flatMap(views -> render(views, model, MediaType.TEXT_HTML, bindingContext, mutatedExchange))
					.then(Mono.fromSupplier(() -> streamHandler.format(response.getBodyFlux(), fragment, exchange)));
		}
		else {
			return selectedViews.flatMap(views -> render(views, model, null, bindingContext, mutatedExchange))
					.then(Mono.fromSupplier(response::getBodyFlux));
		}
	}

	@Nullable
	private StreamHandler getStreamHandler(ServerWebExchange exchange) {
		for (StreamHandler handler : this.streamHandlers) {
			if (handler.supports(exchange.getRequest())) {
				return handler;
			}
		}
		return null;
	}

	private String getNameForReturnValue(MethodParameter returnType) {
		return Optional.ofNullable(returnType.getMethodAnnotation(ModelAttribute.class))
				.filter(ann -> StringUtils.hasText(ann.value()))
				.map(ModelAttribute::value)
				.orElseGet(() -> Conventions.getVariableNameForParameter(returnType));
	}

	private Mono<? extends Void> render(
			List<View> views, Map<String, Object> model, @Nullable MediaType bestMediaType,
			BindingContext bindingContext, ServerWebExchange exchange) {

		for (View view : views) {
			if (view.isRedirectView()) {
				return renderWith(view, model, null, exchange, bindingContext);
			}
		}
		List<MediaType> mediaTypes = getMediaTypes(views);
		if (bestMediaType == null) {
			try {
				bestMediaType = selectMediaType(exchange, () -> mediaTypes);
			}
			catch (NotAcceptableStatusException ex) {
				HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
				if (statusCode != null && statusCode.isError()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring error response content (if any). " + ex.getReason());
					}
					return Mono.empty();
				}
				throw ex;
			}
		}
		if (bestMediaType != null) {
			for (View view : views) {
				for (MediaType mediaType : view.getSupportedMediaTypes()) {
					if (mediaType.isCompatibleWith(bestMediaType)) {
						return renderWith(view, model, mediaType, exchange, bindingContext);
					}
				}
			}
		}
		throw new NotAcceptableStatusException(mediaTypes);
	}

	private Mono<? extends Void> renderWith(View view, Map<String, Object> model,
			@Nullable MediaType mediaType, ServerWebExchange exchange, BindingContext bindingContext) {

		exchange.getAttributes().put(View.BINDING_CONTEXT_ATTRIBUTE, bindingContext);
		return view.render(model, mediaType, exchange)
				.doOnTerminate(() -> exchange.getAttributes().remove(View.BINDING_CONTEXT_ATTRIBUTE));
	}

	private List<MediaType> getMediaTypes(List<View> views) {
		return views.stream()
				.flatMap(view -> view.getSupportedMediaTypes().stream())
				.toList();
	}


	/**
	 * ServerHttpResponse that saves the body Flux and does not write.
	 */
	private static class BodySavingResponse extends ServerHttpResponseDecorator {

		@Nullable
		private Flux<DataBuffer> bodyFlux;

		@Nullable
		private HttpHeaders headers;

		BodySavingResponse(ServerHttpResponse delegate) {
			super(delegate);
		}

		@Override
		public HttpHeaders getHeaders() {
			if (!super.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
				return super.getHeaders();
			}
			// Content-type is set, ignore further updates
			if (this.headers == null) {
				this.headers = new HttpHeaders();
				this.headers.putAll(super.getHeaders());
			}
			return this.headers;
		}

		public Flux<DataBuffer> getBodyFlux() {
			Assert.state(this.bodyFlux != null, "Body not set");
			return this.bodyFlux;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			this.bodyFlux = Flux.from(body);
			return Mono.empty();
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			this.bodyFlux = Flux.from(body).flatMap(Flux::from);
			return Mono.empty();
		}
	}


	/**
	 * Strategy to render fragment with stream formatting.
	 */
	private interface StreamHandler {

		/**
		 * Whether the formatter supports the given request.
		 */
		boolean supports(ServerHttpRequest request);

		/**
		 * Update the response before streaming, e.g. to set the content-type.
		 */
		void updateResponse(ServerWebExchange exchange);

		/**
		 * Format the given fragment.
		 * @param fragmentContent the fragment serialized to data buffers
		 * @param fragment the fragment being rendered
		 * @param exchange the current exchange
		 * @return the formatted fragment
		 */
		Flux<DataBuffer> format(Flux<DataBuffer> fragmentContent, Fragment fragment, ServerWebExchange exchange);
	}


	/**
	 * Formatter for Server-Sent Events formatting.
	 */
	private static class SseStreamHandler implements StreamHandler {

		@Override
		public boolean supports(ServerHttpRequest request) {
			String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
			return (header != null && header.contains(MediaType.TEXT_EVENT_STREAM_VALUE));
		}

		@Override
		public void updateResponse(ServerWebExchange exchange) {
			MediaType mediaType = MediaType.TEXT_EVENT_STREAM;
			Charset charset = getCharset(exchange.getRequest());
			mediaType = (charset != null ? new MediaType(mediaType, charset) : mediaType);
			exchange.getResponse().getHeaders().setContentType(mediaType);
		}

		@Nullable
		private Charset getCharset(ServerHttpRequest request) {
			for (MediaType mediaType : request.getHeaders().getAccept()) {
				if (mediaType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM)) {
					if (mediaType.getCharset() != null) {
						return mediaType.getCharset();
					}
					break;
				}
			}
			return null;
		}

		@Override
		public Flux<DataBuffer> format(
				Flux<DataBuffer> fragmentFlux, Fragment fragment, ServerWebExchange exchange) {

			MediaType mediaType = exchange.getResponse().getHeaders().getContentType();
			Charset charset = (mediaType != null && mediaType.getCharset() != null ?
					mediaType.getCharset() : StandardCharsets.UTF_8);

			DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

			String eventLine = (fragment.viewName() != null ? "event:" + fragment.viewName() + "\n" : "");
			DataBuffer prefix = encodeText(eventLine + "data:", charset, bufferFactory);
			DataBuffer suffix = encodeText("\n\n", charset, bufferFactory);

			Mono<DataBuffer> content = DataBufferUtils.join(fragmentFlux)
					.map(buffer -> {
						String text;
						try {
							text = buffer.toString(charset);
						}
						finally {
							DataBufferUtils.release(buffer);
						}
						text = text.replace("\n", "\ndata:");
						return bufferFactory.wrap(text.getBytes(charset));
					});

			return Flux.concat(Flux.just(prefix), content, Flux.just(suffix));
		}

		private DataBuffer encodeText(String text, Charset charset, DataBufferFactory bufferFactory) {
			byte[] bytes = text.getBytes(charset);
			return bufferFactory.wrap(bytes);
		}
	}

}
