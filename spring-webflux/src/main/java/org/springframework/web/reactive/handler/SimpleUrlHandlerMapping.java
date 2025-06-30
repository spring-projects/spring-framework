/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of the {@link org.springframework.web.reactive.HandlerMapping}
 * interface to map from URLs to request handler beans. Supports both mapping
 * to bean instances and mapping to bean names; the latter is required for
 * non-singleton handlers.
 *
 * <p>The "urlMap" property is suitable for populating the handler map with
 * bean instances. Mappings to bean names can be set via the "mappings"
 * property, in a form accepted by the {@code java.util.Properties} class,
 * as follows:
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>The syntax is {@code PATH=HANDLER_BEAN_NAME}. If the path doesn't begin
 * with a slash, one is prepended.
 *
 * <p>Supports direct matches, for example, a registered "/test" matches "/test", and
 * various Ant-style pattern matches, for example, a registered "/t*" pattern matches
 * both "/test" and "/team", "/test/*" matches all paths under "/test",
 * "/test/**" matches all paths below "/test". For details, see the
 * {@link org.springframework.web.util.pattern.PathPattern} javadoc.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * Create a {@code SimpleUrlHandlerMapping} with default settings.
	 */
	public SimpleUrlHandlerMapping() {
	}

	/**
	 * Create a {@code SimpleUrlHandlerMapping} using the supplied URL map.
	 * @param urlMap map with URL paths as keys and handler beans (or handler
	 * bean names) as values
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
		setUrlMap(urlMap);
	}

	/**
	 * Create a {@code SimpleUrlHandlerMapping} using the supplied URL map and order.
	 * @param urlMap map with URL paths as keys and handler beans (or handler
	 * bean names) as values
	 * @param order the order value for this {@code SimpleUrlHandlerMapping}
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 * @see #setOrder(int)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
		setUrlMap(urlMap);
		setOrder(order);
	}


	/**
	 * Map URL paths to handler bean names.
	 * This is the typical way of configuring this HandlerMapping.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax details,
	 * see the {@link org.springframework.web.util.pattern.PathPattern} javadoc.
	 * @param mappings properties with URLs as keys and bean names as values
	 * @see #setUrlMap
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * Set a Map with URL paths as keys and handler beans (or handler bean names)
	 * as values. Convenient for population with bean references.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax details,
	 * see the {@link org.springframework.web.util.pattern.PathPattern} javadoc.
	 * @param urlMap map with URLs as keys and beans as values
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * Allow {@code Map} access to the URL path mappings, with the option to add or
	 * override specific entries.
	 * <p>Useful for specifying entries directly, for example via "urlMap[myKey]".
	 * This is particularly useful for adding or overriding entries in child
	 * bean definitions.
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * Calls the {@link #registerHandlers} method in addition to the
	 * superclass's initialization.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * Register all handlers specified in the URL map for the corresponding paths.
	 * @param urlMap a Map with URL paths as keys and handler beans or bean names as values
	 * @throws BeansException if a handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		}
		else {
			for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
				String url = entry.getKey();
				Object handler = entry.getValue();
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String handlerName) {
					handler = handlerName.trim();
				}
				registerHandler(url, handler);
			}
			logMappings();
		}
	}

	private void logMappings() {
		if (mappingsLogger.isDebugEnabled()) {
			mappingsLogger.debug(formatMappingName() + " " + getHandlerMap());
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Patterns " + getHandlerMap().keySet() + " in " + formatMappingName());
		}
	}

}
