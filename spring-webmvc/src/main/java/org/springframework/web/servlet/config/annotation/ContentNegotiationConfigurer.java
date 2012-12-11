/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.servlet.config.annotation;

import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;

/**
 * Helps with configuring a {@link ContentNegotiationManager}.
 *
 * <p>By default strategies for checking the extension of the request path and
 * the {@code Accept} header are registered. The path extension check will perform
 * lookups through the {@link ServletContext} and the Java Activation Framework
 * (if present) unless {@linkplain #mediaTypes(Map) media types} are configured.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationConfigurer {

	private ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();


	/**
	 * Class constructor with {@link javax.servlet.ServletContext}.
	 */
	public ContentNegotiationConfigurer(ServletContext servletContext) {
		this.factoryBean.setServletContext(servletContext);
	}

	/**
	 * Indicate whether the extension of the request path should be used to determine
	 * the requested media type with the <em>highest priority</em>.
	 * <p>By default this value is set to {@code true} in which case a request
	 * for {@code /hotels.pdf} will be interpreted as a request for
	 * {@code "application/pdf"} regardless of the {@code Accept} header.
	 */
	public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
		this.factoryBean.setFavorPathExtension(favorPathExtension);
		return this;
	}

	/**
	 * Add mappings from file extensions to media types.
	 * <p>If this property is not set, the Java Action Framework, if available, may
	 * still be used in conjunction with {@link #favorPathExtension(boolean)}.
	 */
	public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
		this.factoryBean.getMediaTypes().put(extension, mediaType);
		return this;
	}

	/**
	 * Add mappings from file extensions to media types.
	 * <p>If this property is not set, the Java Action Framework, if available, may
	 * still be used in conjunction with {@link #favorPathExtension(boolean)}.
	 */
	public ContentNegotiationConfigurer mediaTypes(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.factoryBean.getMediaTypes().putAll(mediaTypes);
		}
		return this;
	}

	/**
	 * Add mappings from file extensions to media types replacing any previous mappings.
	 * <p>If this property is not set, the Java Action Framework, if available, may
	 * still be used in conjunction with {@link #favorPathExtension(boolean)}.
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		this.factoryBean.getMediaTypes().clear();
		mediaTypes(mediaTypes);
		return this;
	}

	/**
	 * Indicate whether to use the Java Activation Framework as a fallback option
	 * to map from file extensions to media types. This is used only when
	 * {@link #favorPathExtension(boolean)} is set to {@code true}.
	 * <p>The default value is {@code true}.
	 * @see #parameterName
	 * @see #mediaTypes(Map)
	 */
	public ContentNegotiationConfigurer useJaf(boolean useJaf) {
		this.factoryBean.setUseJaf(useJaf);
		return this;
	}

	/**
	 * Indicate whether a request parameter should be used to determine the
	 * requested media type with the <em>2nd highest priority</em>, i.e.
	 * after path extensions but before the {@code Accept} header.
	 * <p>The default value is {@code false}. If set to to {@code true}, a request
	 * for {@code /hotels?format=pdf} will be interpreted as a request for
	 * {@code "application/pdf"} regardless of the {@code Accept} header.
	 * <p>To use this option effectively you must also configure the MediaType
	 * type mappings via {@link #mediaTypes(Map)}.
	 * @see #parameterName(String)
	 */
	public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
		this.factoryBean.setFavorParameter(favorParameter);
		return this;
	}

	/**
	 * Set the parameter name that can be used to determine the requested media type
	 * if the {@link #favorParameter(boolean)} property is {@code true}.
	 * <p>The default parameter name is {@code "format"}.
	 */
	public ContentNegotiationConfigurer parameterName(String parameterName) {
		this.factoryBean.setParameterName(parameterName);
		return this;
	}

	/**
	 * Indicate whether the HTTP {@code Accept} header should be ignored altogether.
	 * If set the {@code Accept} header is checked at the
	 * <em>3rd highest priority</em>, i.e. after the request path extension and
	 * possibly a request parameter if configured.
	 * <p>By default this value is set to {@code false}.
	 */
	public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.factoryBean.setIgnoreAcceptHeader(ignoreAcceptHeader);
		return this;
	}

	/**
	 * Set the default content type.
	 * <p>This content type will be used when neither the request path extension,
	 * nor a request parameter, nor the {@code Accept} header could help determine
	 * the requested content type.
	 */
	public ContentNegotiationConfigurer defaultContentType(MediaType defaultContentType) {
		this.factoryBean.setDefaultContentType(defaultContentType);
		return this;
	}

	/**
	 * Return the configured {@link ContentNegotiationManager} instance
	 */
	protected ContentNegotiationManager getContentNegotiationManager() throws Exception {
		this.factoryBean.afterPropertiesSet();
		return this.factoryBean.getObject();
	}

}
