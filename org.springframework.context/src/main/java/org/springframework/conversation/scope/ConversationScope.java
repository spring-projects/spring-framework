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

package org.springframework.conversation.scope;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.conversation.Conversation;
import org.springframework.conversation.ConversationManager;
import org.springframework.conversation.manager.MutableConversation;

/**
 * The default implementation of a conversation scope most likely being exposed as <code>"conversation"</code> scope.
 * It uses the {@link ConversationManager} to get access to the current conversation being used as the container for
 * storing and retrieving attributes and beans.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
public class ConversationScope implements Scope {

	/** Holds the conversation manager reference, if statically injected. */
	private ConversationManager conversationManager;

	/** The name of the current conversation object, made available through {@link #resolveContextualObject(String)}. */
	public static final String CURRENT_CONVERSATION_ATTRIBUTE_NAME = "currentConversation";


	public Object get(String name, ObjectFactory<?> objectFactory) {
		Conversation conversation = getConversationManager().getCurrentConversation(true);
		Object attribute = conversation.getAttribute(name);
		if (attribute == null) {
			attribute = objectFactory.getObject();
			conversation.setAttribute(name, attribute);
		}

		return attribute;
	}

	/**
	 * Will return <code>null</code> if there is no current conversation. It will not implicitly start a new one, if
	 * no current conversation object in place.
	 */
	public String getConversationId() {
		Conversation conversation = getConversationManager().getCurrentConversation(false);
		if (conversation != null) {
			return conversation.getId();
		}

		return null;
	}

	/**
	 * Registering a destruction callback is only possible, if supported by the underlying
	 * {@link org.springframework.conversation.manager.ConversationRepository}.
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		Conversation conversation = getConversationManager().getCurrentConversation(false);
		if (conversation instanceof MutableConversation) {
			((MutableConversation) conversation).registerDestructionCallback(name, callback);
		}
	}

	public Object remove(String name) {
		Conversation conversation = getConversationManager().getCurrentConversation(false);
		if (conversation != null) {
			return conversation.removeAttribute(name);
		}

		return null;
	}

	/**
	 * Supports the following contextual objects:
	 * <ul>
	 * <li><code>"currentConversation"</code>, returns the current {@link org.springframework.conversation.Conversation}</li>
	 * </ul>
	 *
	 * @see org.springframework.beans.factory.config.Scope#resolveContextualObject(String)
	 */
	public Object resolveContextualObject(String key) {
		if (CURRENT_CONVERSATION_ATTRIBUTE_NAME.equals(key)) {
			return getConversationManager().getCurrentConversation(true);
		}

		return null;
	}

	public void setConversationManager(ConversationManager conversationManager) {
		this.conversationManager = conversationManager;
	}

	public ConversationManager getConversationManager() {
		return conversationManager;
	}
}
