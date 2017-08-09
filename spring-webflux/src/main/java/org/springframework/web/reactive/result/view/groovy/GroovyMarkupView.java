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

package org.springframework.web.reactive.result.view.groovy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import org.springframework.web.util.NestedServletException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

/**
 * An {@link AbstractUrlBasedView} subclass based on Groovy XML/XHTML markup templates.
 *
 * <p>Spring's Groovy Markup Template support requires Groovy 2.3.1 and higher.
 *
 * @author Jason Yu
 * @since 5.0
 * @see GroovyMarkupViewResolver
 * @see GroovyMarkupConfigurer
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy Markup Template engine documentation</a>
 */
public class GroovyMarkupView extends AbstractUrlBasedView {

	@Nullable
	private MarkupTemplateEngine engine;

	/**
	 * Set the MarkupTemplateEngine to use in this view.
	 * <p>If not set, the engine is auto-detected by looking up a single
	 * {@link GroovyMarkupConfig} bean in the web application context and using
	 * it to obtain the configured {@code MarkupTemplateEngine} instance.
	 * @see GroovyMarkupConfig
	 */
	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.engine = templateEngine;
	}

	/**
	 * Autodetect a MarkupTemplateEngine via the ApplicationContext.
	 * Called if a MarkupTemplateEngine has not been manually configured.
	 */
	protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
		try {
			return BeanFactoryUtils
					.beanOfTypeIncludingAncestors(
							obtainApplicationContext(),
							GroovyMarkupConfig.class, true, false)
					.getTemplateEngine();
		}
		catch (NoSuchBeanDefinitionException e) {
			throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
					"Servlet web application context or the parent root context: GroovyMarkupConfigurer is " +
					"the usual implementation. This bean may have any name.", e);
		}
	}

	/**
	 * Invoked at startup.
	 * If no {@link #setTemplateEngine(MarkupTemplateEngine) templateEngine} has
	 * been manually set, this method looks up a {@link GroovyMarkupConfig} bean
	 * by type and uses it to obtain the Groovy Markup template engine.
	 * @see GroovyMarkupConfig
	 * @see #setTemplateEngine(groovy.text.markup.MarkupTemplateEngine)
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (this.engine == null) {
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			this.engine.resolveTemplate(getUrl());
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> renderAttributes,
			@Nullable MediaType contentType,
			ServerWebExchange exchange) {

		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");

		DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
		try {
			Template template = this.engine.createTemplateByPath(url);
			Charset charset = getCharset(contentType);
			Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
			template.make(renderAttributes).writeTo(new BufferedWriter(writer));
		}
		catch (IOException e) {
			String message = "Could not load Groovy template for URL : " + getUrl();
			return Mono.error(new IllegalStateException(message, e));
		}
		catch (ClassNotFoundException e) {
			Throwable cause = (e.getCause() != null ? e.getCause() : e);
			return Mono.error(new NestedServletException(
				"Could not find class while rendering Groovy Markup view with name '" +
				getUrl() + "':" + e.getMessage() + "'", cause));
		}
		catch (Throwable e) {
			return Mono.error(e);
		}
		return exchange.getResponse().writeWith(Flux.just(dataBuffer));
	}

	/**
	 * Transform {@code MediaType} to {@code Charset}. If {@code MediaType} is null, then get
	 * default charset from {@code AbstractView} class.
	 */
	private Charset getCharset(@Nullable MediaType mediaType) {
		return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(getDefaultCharset());
	}
}
