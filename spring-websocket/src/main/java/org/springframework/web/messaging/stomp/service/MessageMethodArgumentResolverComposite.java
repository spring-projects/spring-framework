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

package org.springframework.web.messaging.stomp.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.messaging.stomp.StompMessage;

/**
 * Resolves method parameters by delegating to a list of registered
 * {@link MessageMethodArgumentResolver}. Previously resolved method parameters are cached
 * for faster lookups.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageMethodArgumentResolverComposite implements MessageMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<MessageMethodArgumentResolver> argumentResolvers =
			new LinkedList<MessageMethodArgumentResolver>();

	private final Map<MethodParameter, MessageMethodArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<MethodParameter, MessageMethodArgumentResolver>(256);


	/**
	 * Return a read-only list with the contained resolvers, or an empty list.
	 */
	public List<MessageMethodArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.argumentResolvers);
	}

	/**
	 * Whether the given {@linkplain MethodParameter method parameter} is supported by any registered
	 * {@link MessageMethodArgumentResolver}.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * Iterate over registered {@link MessageMethodArgumentResolver}s and invoke the one that supports it.
	 * @exception IllegalStateException if no suitable {@link MessageMethodArgumentResolver} is found.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, StompMessage message, Object replyTo) throws Exception {

		MessageMethodArgumentResolver resolver = getArgumentResolver(parameter);
		Assert.notNull(resolver, "Unknown parameter type [" + parameter.getParameterType().getName() + "]");
		return resolver.resolveArgument(parameter, message, replyTo);
	}

	/**
	 * Find a registered {@link MessageMethodArgumentResolver} that supports the given method parameter.
	 */
	private MessageMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		MessageMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		if (result == null) {
			for (MessageMethodArgumentResolver methodArgumentResolver : this.argumentResolvers) {
				if (logger.isTraceEnabled()) {
					logger.trace("Testing if argument resolver [" + methodArgumentResolver + "] supports [" +
							parameter.getGenericParameterType() + "]");
				}
				if (methodArgumentResolver.supportsParameter(parameter)) {
					result = methodArgumentResolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Add the given {@link MessageMethodArgumentResolver}.
	 */
	public MessageMethodArgumentResolverComposite addResolver(MessageMethodArgumentResolver argumentResolver) {
		this.argumentResolvers.add(argumentResolver);
		return this;
	}

	/**
	 * Add the given {@link MessageMethodArgumentResolver}s.
	 */
	public MessageMethodArgumentResolverComposite addResolvers(
			List<? extends MessageMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers != null) {
			for (MessageMethodArgumentResolver resolver : argumentResolvers) {
				this.argumentResolvers.add(resolver);
			}
		}
		return this;
	}

}
