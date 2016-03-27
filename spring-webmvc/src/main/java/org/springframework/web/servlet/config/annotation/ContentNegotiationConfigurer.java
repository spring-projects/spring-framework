/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;

/**
 * Creates a {@code ContentNegotiationManager} and configures it with
 * one or more {@link ContentNegotiationStrategy} instances. The following shows
 * the resulting strategy instances, the methods used to configured them, and
 * whether enabled by default:
 *
 * <table>
 * <tr>
 *     <th>Configurer Property</th>
 *     <th>Underlying Strategy</th>
 *     <th>Default Setting</th>
 * </tr>
 * <tr>
 *     <td>{@link #favorPathExtension}</td>
 *     <td>{@link PathExtensionContentNegotiationStrategy Path Extension strategy}</td>
 *     <td>On</td>
 * </tr>
 * <tr>
 *     <td>{@link #favorParameter}</td>
 *     <td>{@link ParameterContentNegotiationStrategy Parameter strategy}</td>
 *     <td>Off</td>
 * </tr>
 * <tr>
 *     <td>{@link #ignoreAcceptHeader}</td>
 *     <td>{@link HeaderContentNegotiationStrategy Header strategy}</td>
 *     <td>On</td>
 * </tr>
 * <tr>
 *     <td>{@link #defaultContentType}</td>
 *     <td>{@link FixedContentNegotiationStrategy Fixed content strategy}</td>
 *     <td>Not set</td>
 * </tr>
 * <tr>
 *     <td>{@link #defaultContentTypeStrategy}</td>
 *     <td>{@link ContentNegotiationStrategy}</td>
 *     <td>Not set</td>
 * </tr>
 * </table>
 *
 * <p>The order in which strategies are configured is fixed. You can only turn
 * them on or off.
 *
 * <p>For the path extension and parameter strategies you may explicitly add
 * {@link #mediaType MediaType mappings}. Those will be used to resolve path
 * extensions and/or a query parameter value such as "json" to a concrete media
 * type such as "application/json".
 *
 * <p>The path extension strategy will also use {@link ServletContext#getMimeType}
 * and the Java Activation framework (JAF), if available, to resolve a path
 * extension to a MediaType. You may however {@link #useJaf suppress} the use
 * of JAF.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationConfigurer {

	private final ContentNegotiationManagerFactoryBean factory =
			new ContentNegotiationManagerFactoryBean();

	private final Map<String, MediaType> mediaTypes = new HashMap<String, MediaType>();


	/**
	 * Class constructor with {@link javax.servlet.ServletContext}.
	 */
	public ContentNegotiationConfigurer(ServletContext servletContext) {
		this.factory.setServletContext(servletContext);
	}


	/**
	 * Whether the path extension in the URL path should be used to determine
	 * the requested media type.
	 * <p>By default this is set to {@code true} in which case a request
	 * for {@code /hotels.pdf} will be interpreted as a request for
	 * {@code "application/pdf"} regardless of the 'Accept' header.
	 */
	public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
		this.factory.setFavorPathExtension(favorPathExtension);
		return this;
	}

	/**
	 * Add a mapping from a key, extracted from a path extension or a query
	 * parameter, to a MediaType. This is required in order for the parameter
	 * strategy to work. Any extensions explicitly registered here are also
	 * whitelisted for the purpose of Reflected File Download attack detection
	 * (see Spring Framework reference documentation for more details on RFD
	 * attack protection).
	 * <p>The path extension strategy will also try to use
	 * {@link ServletContext#getMimeType} and JAF (if present) to resolve path
	 * extensions. To change this behavior see the {@link #useJaf} property.
	 * @param extension the key to look up
	 * @param mediaType the media type
	 * @see #mediaTypes(Map)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
		this.mediaTypes.put(extension, mediaType);
		return this;
	}

	/**
	 * An alternative to {@link #mediaType}.
	 * @see #mediaType(String, MediaType)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaTypes(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
		return this;
	}

	/**
	 * Similar to {@link #mediaType} but for replacing existing mappings.
	 * @see #mediaType(String, MediaType)
	 * @see #mediaTypes(Map)
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		this.mediaTypes.clear();
		mediaTypes(mediaTypes);
		return this;
	}

	/**
	 * Whether to ignore requests with path extension that cannot be resolved
	 * to any media type. Setting this to {@code false} will result in an
	 * {@code HttpMediaTypeNotAcceptableException} if there is no match.
	 * <p>By default this is set to {@code true}.
	 */
	public ContentNegotiationConfigurer ignoreUnknownPathExtensions(boolean ignore) {
		this.factory.setIgnoreUnknownPathExtensions(ignore);
		return this;
	}

	/**
	 * When {@link #favorPathExtension} is set, this property determines whether
	 * to allow use of JAF (Java Activation Framework) to resolve a path
	 * extension to a specific MediaType.
	 * <p>By default this is not set in which case
	 * {@code PathExtensionContentNegotiationStrategy} will use JAF if available.
	 */
	public ContentNegotiationConfigurer useJaf(boolean useJaf) {
		this.factory.setUseJaf(useJaf);
		return this;
	}

	/**
	 * Whether a request parameter ("format" by default) should be used to
	 * determine the requested media type. For this option to work you must
	 * register {@link #mediaType(String, MediaType) media type mappings}.
	 * <p>By default this is set to {@code false}.
	 * @see #parameterName(String)
	 */
	public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
		this.factory.setFavorParameter(favorParameter);
		return this;
	}

	/**
	 * Set the query parameter name to use when {@link #favorParameter} is on.
	 * <p>The default parameter name is {@code "format"}.
	 */
	public ContentNegotiationConfigurer parameterName(String parameterName) {
		this.factory.setParameterName(parameterName);
		return this;
	}

	/**
	 * Whether to disable checking the 'Accept' request header.
	 * <p>By default this value is set to {@code false}.
	 */
	public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
		return this;
	}

	/**
	 * Set the default content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @see #defaultContentTypeStrategy
	 */
	public ContentNegotiationConfigurer defaultContentType(MediaType defaultContentType) {
		this.factory.setDefaultContentType(defaultContentType);
		return this;
	}

	/**
	 * Set a custom {@link ContentNegotiationStrategy} to use to determine
	 * the content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @see #defaultContentType
	 * @since 4.1.2
	 */
	public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
		this.factory.setDefaultContentTypeStrategy(defaultStrategy);
		return this;
	}

	protected ContentNegotiationManager getContentNegotiationManager() throws Exception {
		this.factory.addMediaTypes(this.mediaTypes);
		this.factory.afterPropertiesSet();
		return this.factory.getObject();
	}

}
