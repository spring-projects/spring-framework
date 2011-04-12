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

package org.springframework.conversation.manager;

import org.springframework.core.NamedThreadLocal;

/**
 * An implementation of the {@link org.springframework.conversation.manager.ConversationResolver} where the currently
 * used conversation id is bound to the current thread.
 * If this implementation is used in a web environment, make sure the current conversation id is set and removed through
 * a filter accordingly as the id is bound to the current thread using a thread local.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class ThreadLocalConversationResolver implements ConversationResolver{

	/** The thread local attribute where the current conversation id is stored. */
	private final NamedThreadLocal<String> currentConversationId =
			new NamedThreadLocal<String>("Current Conversation");

	public String getCurrentConversationId() {
		return currentConversationId.get();
	}

	public void setCurrentConversationId(String conversationId) {
		currentConversationId.set(conversationId);
		if (conversationId == null) {
			currentConversationId.remove();
		}
	}
}
