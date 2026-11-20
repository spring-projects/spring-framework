/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.standard;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link jakarta.websocket.server.ServerEndpointConfig.Configurator} for initializing
 * {@link ServerEndpoint}-annotated classes through Spring.
 *
 * <p>
 * <pre class="code">
 * &#064;ServerEndpoint(value = "/echo", configurator = SpringConfigurator.class)
 * public class EchoEndpoint {
 *     // ...
 * }
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see ServerEndpointExporter
 */
public class SpringConfigurator extends Configurator {

	private static final String NO_VALUE = ObjectUtils.identityToString(new Object());

	private static final Log logger = LogFactory.getLog(SpringConfigurator.class);

	private static final Map<String, Map<Class<?>, String>> cache = new ConcurrentHashMap<>();


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			String message = "Failed to find the root WebApplicationContext. Was ContextLoaderListener not used?";
			logger.error(message);
			throw new IllegalStateException(message);
		}

		String beanName = ClassUtils.getShortNameAsProperty(endpointClass);
		if (wac.containsBean(beanName)) {
			T endpoint = wac.getBean(beanName, endpointClass);
			if (logger.isTraceEnabled()) {
				logger.trace("Using @ServerEndpoint singleton " + endpoint);
			}
			return endpoint;
		}

		Component ann = AnnotationUtils.findAnnotation(endpointClass, Component.class);
		if (ann != null && wac.containsBean(ann.value())) {
			T endpoint = wac.getBean(ann.value(), endpointClass);
			if (logger.isTraceEnabled()) {
				logger.trace("Using @ServerEndpoint singleton " + endpoint);
			}
			return endpoint;
		}

		beanName = getBeanNameByType(wac, endpointClass);
		if (beanName != null) {
			return (T) wac.getBean(beanName);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Creating new @ServerEndpoint instance of type " + endpointClass);
		}
		return wac.getAutowireCapableBeanFactory().createBean(endpointClass);
	}

	private @Nullable String getBeanNameByType(WebApplicationContext wac, Class<?> endpointClass) {
		String wacId = wac.getId();
		Map<Class<?>, String> beanNamesByType = cache.computeIfAbsent(wacId, key -> new ConcurrentHashMap<>());

		String beanName = beanNamesByType.get(endpointClass);
		if (beanName == null) {
			String[] names = wac.getBeanNamesForType(endpointClass);
			beanName = (names.length == 1 ? names[0] : NO_VALUE);
			beanNamesByType.put(endpointClass, beanName);
			if (names.length > 1) {
				throw new IllegalStateException("Found multiple @ServerEndpoint's of type [" +
						endpointClass.getName() + "]: bean names " + Arrays.toString(names));
			}
		}

		return (NO_VALUE.equals(beanName) ? null : beanName);
	}

}
