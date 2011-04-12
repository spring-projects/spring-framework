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

/**
 * The current conversation resolver is another extension point for the conversation manager to easily change the
 * default behavior of storing the currently used conversation id.
 *
 * In a web environment, the current conversation id would most likely be bound to the current window / tab and hence be
 * based on the window management. This makes it possible to run different conversations per browser window and
 * isolating them from each other by default.
 *
 * In a unit-test or batch environment the current conversation could be bound to the current thread to make
 * conversations be available in a non-web environment as well.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public interface ConversationResolver {

	/**
	 * Returns the id of the currently used conversation, if any, <code>null</code> otherwise.
	 *
	 * @return the id of the current conversation, if any, <code>null</code> otherwise
	 */
	String getCurrentConversationId();

	/**
	 * Set the given conversation id to be the currently used one. Replaces the current one, if any, but is not removing
	 * the current conversation.
	 *
	 * @param conversationId the id of the conversation to be made the current one
	 */
	void setCurrentConversationId(String conversationId);
}
