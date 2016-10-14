package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 * @author Rossen Stoyanchev
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
		return createSingleBean(WebReactiveConfigurer::createRequestMappingHandlerMapping,
				RequestMappingHandlerMapping.class);
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
		return createSingleBean(WebReactiveConfigurer::createRequestMappingHandlerAdapter,
				RequestMappingHandlerAdapter.class);
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
		return createSingleBean(WebReactiveConfigurer::getValidator, Validator.class);
	}

	@Override
	public Optional<MessageCodesResolver> getMessageCodesResolver() {
		return createSingleBean(WebReactiveConfigurer::getMessageCodesResolver, MessageCodesResolver.class);
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

	private <T> Optional<T> createSingleBean(Function<WebReactiveConfigurer, Optional<T>> factory,
			Class<T> beanType) {

		List<Optional<T>> result = this.delegates.stream()
				.map(factory).filter(Optional::isPresent).collect(Collectors.toList());

		if (result.isEmpty()) {
			return Optional.empty();
		}
		else if (result.size() == 1) {
			return result.get(1);
		}
		else {
			throw new IllegalStateException("More than one WebReactiveConfigurer implements " +
					beanType.getSimpleName() + " factory method.");
		}
	}

}
