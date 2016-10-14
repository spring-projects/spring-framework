package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * A {@link WebReactiveConfigurer} that delegates to one or more others.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class WebReactiveConfigurerComposite implements WebReactiveConfigurer {

	private final List<WebReactiveConfigurer> delegates = new ArrayList<>();

	public void addWebReactiveConfigurers(List<WebReactiveConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.delegates.addAll(configurers);
		}
	}

	@Override
	public Optional<RequestMappingHandlerMapping> createRequestMappingHandlerMapping() {
		Optional<RequestMappingHandlerMapping> selected = Optional.empty();
		for (WebReactiveConfigurer configurer : this.delegates) {
			Optional<RequestMappingHandlerMapping> handlerMapping = configurer.createRequestMappingHandlerMapping();
			if (handlerMapping.isPresent()) {
				if (selected != null) {
					throw new IllegalStateException("No unique RequestMappingHandlerMapping found: {" +
							selected.get() + ", " + handlerMapping.get() + "}");
				}
				selected = handlerMapping;
			}
		}
		return selected;
	}

	@Override
	public void configureRequestedContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.delegates.stream().forEach(delegate -> delegate.configureRequestedContentTypeResolver(builder));
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addCorsMappings(registry));
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.delegates.stream().forEach(delegate -> delegate.configurePathMatching(configurer));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addResourceHandlers(registry));
	}

	@Override
	public Optional<RequestMappingHandlerAdapter> createRequestMappingHandlerAdapter() {
		Optional<RequestMappingHandlerAdapter> selected = Optional.empty();
		for (WebReactiveConfigurer configurer : this.delegates) {
			Optional<RequestMappingHandlerAdapter> handlerAdapter = configurer.createRequestMappingHandlerAdapter();
			if (handlerAdapter.isPresent()) {
				if (selected != null) {
					throw new IllegalStateException("No unique RequestMappingHandlerAdapter found: {" +
							selected.get() + ", " + handlerAdapter.get() + "}");
				}
				selected = handlerAdapter;
			}
		}
		return selected;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.delegates.stream().forEach(delegate -> delegate.addArgumentResolvers(resolvers));
	}

	@Override
	public void configureMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.delegates.stream().forEach(delegate -> delegate.configureMessageReaders(messageReaders));
	}

	@Override
	public void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.delegates.stream().forEach(delegate -> delegate.extendMessageReaders(messageReaders));
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addFormatters(registry));
	}

	@Override
	public Optional<Validator> getValidator() {
		Optional<Validator> selected = Optional.empty();
		for (WebReactiveConfigurer configurer : this.delegates) {
			Optional<Validator> validator = configurer.getValidator();
			if (validator.isPresent()) {
				if (selected != null) {
					throw new IllegalStateException("No unique Validator found: {" +
							selected.get() + ", " + validator.get() + "}");
				}
				selected = validator;
			}
		}
		return selected;
	}

	@Override
	public Optional<MessageCodesResolver> getMessageCodesResolver() {
		Optional<MessageCodesResolver> selected = Optional.empty();
		for (WebReactiveConfigurer configurer : this.delegates) {
			Optional<MessageCodesResolver> messageCodesResolver = configurer.getMessageCodesResolver();
			if (messageCodesResolver.isPresent()) {
				if (selected != null) {
					throw new IllegalStateException("No unique MessageCodesResolver found: {" +
							selected.get() + ", " + messageCodesResolver.get() + "}");
				}
				selected = messageCodesResolver;
			}
		}
		return selected;
	}

	@Override
	public void configureMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		this.delegates.stream().forEach(delegate -> delegate.configureMessageWriters(messageWriters));
	}

	@Override
	public void extendMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		this.delegates.stream().forEach(delegate -> delegate.extendMessageWriters(messageWriters));
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.configureViewResolvers(registry));
	}
}
