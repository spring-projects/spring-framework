/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.message;

import java.util.List;
import java.util.Map;

/**
 * A context for recording and retrieving messages for display in a user interface.
 */
public interface MessageContext {

	/**
	 * Return all messages in this context indexed by the UI element they are associated with.
	 * @return the message map
	 */
	public Map<String, List<Message>> getMessages();
	
	/**
	 * Get all messages on the UI element provided.
	 * Returns an empty list if no messages have been recorded against the element.
	 * Messages are returned in the order they were added.
	 * @param element the id of the element to lookup messages against
	 */
	public List<Message> getMessages(String element);

	/**
	 * Add a new message to this context.
	 * @param element the id of the UI element the message should be associated with
	 * @param messageResolver the resolver that will resolve the message to be added
	 */
	public void addMessage(String element, MessageResolver messageResolver);

}
