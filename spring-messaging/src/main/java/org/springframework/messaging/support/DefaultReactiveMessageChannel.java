/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.support;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.ReactiveSubscribableChannel;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link ReactiveSubscribableChannel}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class DefaultReactiveMessageChannel implements ReactiveSubscribableChannel, BeanNameAware {

	private static final Mono<Boolean> SUCCESS_RESULT = Mono.just(true);

	private static Log logger = LogFactory.getLog(DefaultReactiveMessageChannel.class);


	private final Set<ReactiveMessageHandler> handlers = new CopyOnWriteArraySet<>();

	private String beanName;


	public DefaultReactiveMessageChannel() {
		this.beanName = getClass().getSimpleName() + "@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * A message channel uses the bean name primarily for logging purposes.
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Return the bean name for this message channel.
	 */
	public String getBeanName() {
		return this.beanName;
	}


	@Override
	public boolean subscribe(ReactiveMessageHandler handler) {
		boolean result = this.handlers.add(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug(getBeanName() + " added " + handler);
			}
		}
		return result;
	}


	@Override
	public boolean unsubscribe(ReactiveMessageHandler handler) {
		boolean result = this.handlers.remove(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug(getBeanName() + " removed " + handler);
			}
		}
		return result;
	}


	@Override
	public Mono<Boolean> send(Message<?> message) {
		return Flux.fromIterable(this.handlers)
				.concatMap(handler -> handler.handleMessage(message))
				.then(SUCCESS_RESULT);
	}

}
