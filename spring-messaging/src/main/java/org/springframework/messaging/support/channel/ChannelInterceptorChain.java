/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support.channel;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;


/**
 * A convenience wrapper class for invoking a list of {@link ChannelInterceptor}s.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class ChannelInterceptorChain {

	private static final Log logger = LogFactory.getLog(ChannelInterceptorChain.class);

	private final List<ChannelInterceptor> interceptors = new CopyOnWriteArrayList<ChannelInterceptor>();


	public boolean set(List<ChannelInterceptor> interceptors) {
		synchronized (this.interceptors) {
			this.interceptors.clear();
			return this.interceptors.addAll(interceptors);
		}
	}

	public boolean add(ChannelInterceptor interceptor) {
		return this.interceptors.add(interceptor);
	}

	public List<ChannelInterceptor> getInterceptors() {
		return Collections.unmodifiableList(this.interceptors);
	}


	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		UUID originalId = message.getHeaders().getId();
		if (logger.isTraceEnabled()) {
			logger.trace("preSend message id " + originalId);
		}
		for (ChannelInterceptor interceptor : this.interceptors) {
			message = interceptor.preSend(message, channel);
			if (message == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("preSend returned null (precluding the send)");
				}
				return null;
			}
		}
		if (logger.isTraceEnabled()) {
			if (!message.getHeaders().getId().equals(originalId)) {
				logger.trace("preSend returned modified message " + message);
			}
		}
		return message;
	}

	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		if (logger.isTraceEnabled()) {
			logger.trace("postSend (sent=" + sent + ") message id " + message.getHeaders().getId());
		}
		for (ChannelInterceptor interceptor : this.interceptors) {
			interceptor.postSend(message, channel, sent);
		}
	}

	public boolean preReceive(MessageChannel channel) {
		if (logger.isTraceEnabled()) {
			logger.trace("preReceive on channel '" + channel + "'");
		}
		for (ChannelInterceptor interceptor : this.interceptors) {
			if (!interceptor.preReceive(channel)) {
				return false;
			}
		}
		return true;
	}

	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		if (message != null && logger.isTraceEnabled()) {
			logger.trace("postReceive on channel '" + channel + "', message: " + message);
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("postReceive on channel '" + channel + "', message is null");
		}
		for (ChannelInterceptor interceptor : this.interceptors) {
			message = interceptor.postReceive(message, channel);
			if (message == null) {
				return null;
			}
		}
		return message;
	}


}
