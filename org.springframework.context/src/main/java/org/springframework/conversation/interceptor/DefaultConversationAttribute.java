/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.conversation.interceptor;

import org.springframework.conversation.ConversationType;

/**
 * Default implementation of the ConversationAttribute used by the conversation system.
 *
 * @author Agim Emruli
 */
public class DefaultConversationAttribute implements ConversationAttribute {

	private final boolean shouldStartConversation;

	private final boolean shouldEndConversation;

	private final ConversationType conversationType;

	private final boolean shouldEndRoot;

	private int timeout = DEFAULT_TIMEOUT;

	private DefaultConversationAttribute(boolean startConversation,
			boolean endConversation,
			ConversationType conversationType,
			int timeout, boolean shouldEndRootConversation) {
		shouldStartConversation = startConversation;
		shouldEndConversation = endConversation;
		this.conversationType = conversationType;
		this.timeout = timeout;
		this.shouldEndRoot = shouldEndRootConversation;
	}

	public DefaultConversationAttribute(ConversationType conversationType, int timeout) {
	    this(true,false,conversationType,timeout, false);
	}

	public DefaultConversationAttribute(boolean shouldEndRoot) {
		this(false, true, null, DEFAULT_TIMEOUT, shouldEndRoot);
	}

	public boolean shouldStartConversation() {
		return shouldStartConversation;
	}

	public boolean shouldEndConversation() {
		return shouldEndConversation;
	}

	public boolean shouldEndRoot() {
		return shouldEndRoot;
	}

	public ConversationType getConversationType() {
		return conversationType;
	}

	public int getTimeout() {
		return timeout;
	}
}
