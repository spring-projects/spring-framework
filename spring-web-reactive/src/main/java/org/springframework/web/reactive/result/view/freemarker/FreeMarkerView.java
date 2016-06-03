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
package org.springframework.web.reactive.result.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.Version;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@code View} implementation that uses the FreeMarker template engine.
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as
 * {@link FreeMarkerConfigurer} being accessible in the application context.
 * Alternatively set the FreeMarker configuration can be set directly on this
 * class via {@link #setConfiguration}.
 *
 * <p>The {@link #setUrl(String) url} property is the location of the FreeMarker
 * template relative to the FreeMarkerConfigurer's
 * {@link FreeMarkerConfigurer#setTemplateLoaderPath templateLoaderPath}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Rossen Stoyanchev
 */
public class FreeMarkerView extends AbstractUrlBasedView {

	private Configuration configuration;

	private String encoding;


	/**
	 * Set the FreeMarker Configuration to be used by this view.
	 * <p>Typically this property is not set directly. Instead a single
	 * {@link FreeMarkerConfig} is expected in the Spring application context
	 * which is used to obtain the FreeMarker configuration.
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Return the FreeMarker configuration used by this view.
	 */
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Set the encoding of the FreeMarker template file.
	 * <p>By default {@link FreeMarkerConfigurer} sets the default encoding in
	 * the FreeMarker configuration to "UTF-8". It's recommended to specify the
	 * encoding in the FreeMarker Configuration rather than per template if all
	 * your templates share a common encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Return the encoding for the FreeMarker template.
	 */
	protected String getEncoding() {
		return this.encoding;
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
	 * Autodetect a {@link FreeMarkerConfig} object via the ApplicationContext.
	 * @return the Configuration instance to use for FreeMarkerViews
	 * @throws BeansException if no Configuration instance could be found
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					getApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
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
			if (logger.isDebugEnabled()) {
				logger.debug("No FreeMarker view found for URL: " + getUrl());
			}
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

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> renderAttributes, ServerWebExchange exchange) {
		// Expose all standard FreeMarker hash models.
		SimpleHash freeMarkerModel = getTemplateModel(renderAttributes, exchange);
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering FreeMarker template [" + getUrl() + "].");
		}
		Locale locale = Locale.getDefault(); // TODO
		DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
		try {
			// TODO: pass charset
			Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream());
			getTemplate(locale).process(freeMarkerModel, writer);
		}
		catch (IOException ex) {
			String message = "Could not load FreeMarker template for URL [" + getUrl() + "]";
			return Mono.error(new IllegalStateException(message, ex));
		}
		catch (Throwable ex) {
			return Mono.error(ex);
		}
		return exchange.getResponse().writeWith(Flux.just(dataBuffer));
	}

	/**
	 * Build a FreeMarker template model for the given model Map.
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
	 * Return the configured FreeMarker {@link ObjectWrapper}, or the
	 * {@link ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = getConfiguration().getObjectWrapper();
		Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
		return (ow != null ? ow : new DefaultObjectWrapperBuilder(version).build());
	}

	/**
	 * Retrieve the FreeMarker template for the given locale,
	 * to be rendering by this view.
	 * <p>By default, the template specified by the "url" bean property
	 * will be retrieved.
	 * @param locale the current locale
	 * @return the FreeMarker template to render
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return (getEncoding() != null ?
				getConfiguration().getTemplate(getUrl(), locale, getEncoding()) :
				getConfiguration().getTemplate(getUrl(), locale));
	}

}
