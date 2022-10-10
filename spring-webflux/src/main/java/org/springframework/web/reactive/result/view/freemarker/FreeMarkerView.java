/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.Version;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.RequestContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@code View} implementation that uses the FreeMarker template engine.
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as
 * {@link FreeMarkerConfigurer} being accessible in the application context.
 * Alternatively the FreeMarker {@link Configuration} can be set directly on this
 * class via {@link #setConfiguration}.
 *
 * <p>The {@link #setUrl(String) url} property is the location of the FreeMarker
 * template relative to the FreeMarkerConfigurer's
 * {@link FreeMarkerConfigurer#setTemplateLoaderPath templateLoaderPath}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class FreeMarkerView extends AbstractUrlBasedView {

	/**
	 * Attribute name of the {@link RequestContext} instance in the template model,
	 * available to Spring's macros &mdash; for example, for creating
	 * {@link org.springframework.web.reactive.result.view.BindStatus BindStatus}
	 * objects.
	 * @since 5.2
	 * @see #setExposeSpringMacroHelpers(boolean)
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";


	@Nullable
	private Configuration configuration;

	@Nullable
	private String encoding;

	private boolean exposeSpringMacroHelpers = true;


	/**
	 * Set the FreeMarker {@link Configuration} to be used by this view.
	 * <p>Typically this property is not set directly. Instead a single
	 * {@link FreeMarkerConfig} is expected in the Spring application context
	 * which is used to obtain the FreeMarker configuration.
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Get the FreeMarker {@link Configuration} used by this view.
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Obtain the FreeMarker {@link Configuration} for actual use.
	 * @return the FreeMarker configuration (never {@code null})
	 * @throws IllegalStateException in case of no {@code Configuration} object set
	 * @see #getConfiguration()
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}

	/**
	 * Set the encoding of the FreeMarker template file.
	 * <p>By default {@link FreeMarkerConfigurer} sets the default encoding in
	 * the FreeMarker configuration to "UTF-8". It's recommended to specify the
	 * encoding in the FreeMarker {@link Configuration} rather than per template
	 * if all your templates share a common encoding.
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get the encoding for the FreeMarker template.
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * Set whether to expose a {@link RequestContext} for use by Spring's macro
	 * library, under the name {@value #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE}.
	 * <p>Default is {@code true}.
	 * <p>Needed for Spring's FreeMarker default macros. Note that this is
	 * <i>not</i> required for templates that use HTML forms <i>unless</i> you
	 * wish to take advantage of the Spring helper macros.
	 * @since 5.2
	 * @see #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (getConfiguration() == null) {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
		}
	}

	/**
	 * Autodetect a {@link FreeMarkerConfig} object in the {@code ApplicationContext}.
	 * @return the {@code FreeMarkerConfig} instance to use for this view
	 * @throws BeansException if no {@code FreeMarkerConfig} instance could be found
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this application context " +
							"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
							"This bean may be given any name.", ex);
		}
	}


	/**
	 * Check that the FreeMarker template used for this view exists and is valid.
	 * <p>Can be overridden to customize the behavior, for example in case of
	 * multiple templates to be rendered into a single view.
	 */
	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			// Allow for ViewResolver chaining...
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException(
					"Failed to parse FreeMarker template for URL [" +  getUrl() + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load FreeMarker template for URL [" + getUrl() + "]", ex);
		}
	}

	/**
	 * Prepare the model to use for rendering by potentially exposing a
	 * {@link RequestContext} for use in Spring FreeMarker macros and then
	 * delegating to the inherited implementation of this method.
	 * @since 5.2
	 * @see #setExposeSpringMacroHelpers(boolean)
	 * @see org.springframework.web.reactive.result.view.AbstractView#getModelAttributes(Map, ServerWebExchange)
	 */
	@Override
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		if (this.exposeSpringMacroHelpers) {
			if (model != null && model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new IllegalStateException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
						"' because of an existing model object of the same name");
			}
			// Make a defensive copy of the model.
			Map<String, Object> attributes = (model != null ? new HashMap<>(model) : new HashMap<>());
			// Expose RequestContext instance for Spring macros.
			attributes.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, new RequestContext(
					exchange, attributes, obtainApplicationContext(), getRequestDataValueProcessor()));
			return super.getModelAttributes(attributes, exchange);
		}
		return super.getModelAttributes(model, exchange);
	}

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			@Nullable MediaType contentType, ServerWebExchange exchange) {

		return exchange.getResponse().writeWith(Mono
				.fromCallable(() -> {
					// Expose all standard FreeMarker hash models.
					SimpleHash freeMarkerModel = getTemplateModel(renderAttributes, exchange);

					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "Rendering [" + getUrl() + "]");
					}

					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
					FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
					try {
						Charset charset = getCharset(contentType);
						Writer writer = new OutputStreamWriter(bos, charset);
						getTemplate(locale).process(freeMarkerModel, writer);

						byte[] bytes = bos.toByteArrayUnsafe();
						return exchange.getResponse().bufferFactory().wrap(bytes);
					}
					catch (IOException ex) {
						String message = "Could not load FreeMarker template for URL [" + getUrl() + "]";
						throw new IllegalStateException(message, ex);
					}
				})
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release));
	}

	private Charset getCharset(@Nullable MediaType mediaType) {
		return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(getDefaultCharset());
	}

	/**
	 * Build a FreeMarker template model for the given model map.
	 * <p>The default implementation builds a {@link SimpleHash}.
	 * @param model the model to use for rendering
	 * @param exchange current exchange
	 * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
	 */
	protected SimpleHash getTemplateModel(Map<String, Object> model, ServerWebExchange exchange) {
		SimpleHash fmModel = new SimpleHash(getObjectWrapper());
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * Get the configured FreeMarker {@link ObjectWrapper}, or the
	 * {@linkplain ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
		return (ow != null ? ow : new DefaultObjectWrapperBuilder(version).build());
	}

	/**
	 * Get the FreeMarker template for the given locale, to be rendered by this view.
	 * <p>By default, the template specified by the "url" bean property will be retrieved.
	 * @param locale the current locale
	 * @return the FreeMarker template to render
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(getUrl(), locale, getEncoding()) :
				obtainConfiguration().getTemplate(getUrl(), locale));
	}

}
