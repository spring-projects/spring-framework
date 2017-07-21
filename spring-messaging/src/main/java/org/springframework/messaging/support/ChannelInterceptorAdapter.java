/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link ChannelInterceptor} base class with empty method implementations
 * as a convenience.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class ChannelInterceptorAdapter implements ChannelInterceptor {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		return message;
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
	}

	@Override
	public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
	}

	public boolean preReceive(MessageChannel channel) {
		return true;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return message;
	}

	@Override
	public void afterReceiveCompletion(@Nullable Message<?> message, MessageChannel channel, @Nullable Exception ex) {
	}

}
