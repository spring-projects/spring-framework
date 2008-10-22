/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Editor for <code>java.net.URI</code>, to directly populate a URI property
 * instead of using a String property as bridge.
 *
 * <p>Supports Spring-style URI notation: any fully qualified standard URI
 * ("file:", "http:", etc) and Spring's special "classpath:" pseudo-URL,
 * which will be resolved to a corresponding URI.
 *
 * <p>Note: A URI is more relaxed than a URL in that it does not require
 * a valid protocol to be specified. Any scheme within a valid URI syntax
 * is allowed, even without a matching protocol handler being registered.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see java.net.URI
 * @see URLEditor
 */
public class URIEditor extends PropertyEditorSupport {

	private final ClassLoader classLoader;


	/**
	 * Create a new URIEditor, converting "classpath:" locations into
	 * standard URIs (not trying to resolve them into physical resources).
	 */
	public URIEditor() {
		this.classLoader = null;
	}

	/**
	 * Create a new URIEditor, using the given ClassLoader to resolve
	 * "classpath:" locations into physical resource URLs.
	 * @param classLoader the ClassLoader to use for resolving "classpath:" locations
	 * (may be <code>null</code> to indicate the default ClassLoader)
	 */
	public URIEditor(ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String uri = text.trim();
			if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
				ClassPathResource resource =
						new ClassPathResource(uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
				try {
					String url = resource.getURL().toString();
					setValue(createURI(url));
				}
				catch (IOException ex) {
					throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
				}
				catch (URISyntaxException ex) {
					throw new IllegalArgumentException("Invalid URI syntax: " + ex);
				}
			}
			else {
				try {
					setValue(createURI(uri));
				}
				catch (URISyntaxException ex) {
					throw new IllegalArgumentException("Invalid URI syntax: " + ex);
				}
			}
		}
		else {
			setValue(null);
		}
	}

	/**
	 * Create a URI instance for the given (resolved) String value.
	 * <p>The default implementation uses the <code>URI(String)</code>
	 * constructor, replacing spaces with "%20" quotes first.
	 * @param value the value to convert into a URI instance
	 * @return the URI instance
	 * @throws URISyntaxException if URI conversion failed
	 */
	protected URI createURI(String value) throws URISyntaxException {
		return new URI(StringUtils.replace(value, " ", "%20"));
	}


	public String getAsText() {
		URI value = (URI) getValue();
		return (value != null ? value.toString() : "");
	}

}
