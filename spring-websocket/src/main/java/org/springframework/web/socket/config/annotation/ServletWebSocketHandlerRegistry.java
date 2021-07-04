/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link WebSocketHandlerRegistry} with Spring MVC handler mappings for the
 * handshake requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletWebSocketHandlerRegistry implements WebSocketHandlerRegistry {

	private final List<ServletWebSocketHandlerRegistration> registrations = new ArrayList<>(4);

	private int order = 1;

	@Nullable
	private UrlPathHelper urlPathHelper;


	public ServletWebSocketHandlerRegistry() {
	}


	@Override
	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		ServletWebSocketHandlerRegistration registration = new ServletWebSocketHandlerRegistration();
		registration.addHandler(handler, paths);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Set the order for the resulting {@link SimpleUrlHandlerMapping} relative to
	 * other handler mappings configured in Spring MVC.
	 * <p>The default value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the UrlPathHelper to configure on the {@code SimpleUrlHandlerMapping}
	 * used to map handshake requests.
	 */
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	/**
	 * Whether there are any endpoint SockJS registrations without a TaskScheduler.
	 * This method should be invoked just before {@link #getHandlerMapping()} to
	 * allow for registrations to be made first.
	 */
	protected boolean requiresTaskScheduler() {
		return this.registrations.stream()
				.anyMatch(r -> r.getSockJsServiceRegistration() != null &&
						r.getSockJsServiceRegistration().getTaskScheduler() == null);
	}

	/**
	 * Provide the TaskScheduler to use for SockJS endpoints for which a task
	 * scheduler has not been explicitly registered. This method must be called
	 * prior to {@link #getHandlerMapping()}.
	 */
	protected void setTaskScheduler(TaskScheduler scheduler) {
		this.registrations.stream()
				.map(ServletWebSocketHandlerRegistration::getSockJsServiceRegistration)
				.filter(Objects::nonNull)
				.filter(r -> r.getTaskScheduler() == null)
				.forEach(registration -> registration.setTaskScheduler(scheduler));
	}

	public AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<>();
		for (ServletWebSocketHandlerRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			mappings.forEach((httpHandler, patterns) -> {
				for (String pattern : patterns) {
					urlMap.put(pattern, httpHandler);
				}
			});
		}
		WebSocketHandlerMapping hm = new WebSocketHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(this.order);
		if (this.urlPathHelper != null) {
			hm.setUrlPathHelper(this.urlPathHelper);
		}
		return hm;
	}

}
