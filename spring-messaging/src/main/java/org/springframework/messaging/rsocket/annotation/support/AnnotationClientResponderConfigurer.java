/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.rsocket.annotation.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.rsocket.RSocketFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.ClientRSocketFactoryConfigurer;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;

/**
 * {@link ClientRSocketFactoryConfigurer} to configure and plug in a responder
 * that handles requests via annotated handler methods. Effectively a thin layer over
 * {@link RSocketMessageHandler} that provides a programmatic way to configure
 * it and obtain a responder via {@link RSocketMessageHandler#clientResponder()
 * clientResponder()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public final class AnnotationClientResponderConfigurer implements ClientRSocketFactoryConfigurer {

	private final List<Object> handlers = new ArrayList<>();

	@Nullable
	private RSocketStrategies strategies;


	private AnnotationClientResponderConfigurer(List<Object> handlers) {
		for (Object obj : handlers) {
			this.handlers.add(obj instanceof Class ? BeanUtils.instantiateClass((Class<?>) obj) : obj);
		}
	}


	/**
	 * Configure handlers to detect {@code @MessasgeMapping} handler methods on.
	 * This is used to set {@link RSocketMessageHandler#setHandlers(List)}.
	 */
	public AnnotationClientResponderConfigurer handlers(Object... handlers) {
		this.handlers.addAll(Arrays.asList(handlers));
		return this;
	}


	// Implementation of ClientRSocketFactoryConfigurer

	@Override
	public void configureWithStrategies(RSocketStrategies strategies) {
		this.strategies = strategies;
	}

	@Override
	public void configure(RSocketFactory.ClientRSocketFactory factory) {
		Assert.notEmpty(this.handlers, "No handlers");
		RSocketMessageHandler messageHandler = new RSocketMessageHandler();
		messageHandler.setHandlers(this.handlers);
		messageHandler.setRSocketStrategies(this.strategies);
		messageHandler.afterPropertiesSet();
		factory.acceptor(messageHandler.clientResponder());
	}


	// Static factory methods

	/**
	 * Create an {@code AnnotationClientResponderConfigurer} with the given handlers
	 * to check for {@code @MessasgeMapping} handler methods.
	 */
	public static AnnotationClientResponderConfigurer withHandlers(Object... handlers) {
		return new AnnotationClientResponderConfigurer(Arrays.asList(handlers));
	}

	/**
	 * Create an {@code AnnotationClientResponderConfigurer} to set up further.
	 */
	public static AnnotationClientResponderConfigurer create() {
		return new AnnotationClientResponderConfigurer(Collections.emptyList());
	}

}
