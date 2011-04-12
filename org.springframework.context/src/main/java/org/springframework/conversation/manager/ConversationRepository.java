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
 * The conversation repository is responsible for creating new conversation objects, store them within its own storage
 * and finally remove the after they have been ended.<br/>
 * The repository might be transient (most likely in a web environment) but might support long running, persisted
 * conversations as well.<br/>
 * The repository is responsible for the timeout management of the conversation objects as well and might use an
 * existing mechanism to do so (in a distributed, cached storage for instance).<br/>
 * Depending on the underlying storage mechanism, the repository might support destruction callbacks within the
 * conversation objects or not. If they are not supported, make sure an appropriate warning will be logged if
 * registering a destruction callback.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
public interface ConversationRepository {

	/**
	 * Creates a new root conversation object and returns it. Be aware that this method does not store the conversation,
	 * this has to be done using the {@link #storeConversation(MutableConversation)} method. The id of the conversation
	 * will typically be set within the store method rather than the creation method.
	 *
	 * @return the newly created conversation object
	 */
	MutableConversation createNewConversation();

	/**
	 * Creates a new child conversation and attaches it as a child to the given parent conversation. Like the
	 * {@link #createNewConversation()} method, this one does not store the new child conversation object, this has to
	 * be done using the {@link #storeConversation(MutableConversation)} method. The id of the new child conversation
	 * will typically be set within the store method rather than the creation method.
	 *
	 * @param parentConversation the parent conversation to create and attach a new child conversation to
	 * @param isIsolated <code>true</code> if the new child conversation has to be isolated from its parent state,
	 * <code>false</code> if it will inherit the state from the given parent
	 * @return the newly created child conversation, attached to the given parent conversation
	 */
	MutableConversation createNewChildConversation(MutableConversation parentConversation, boolean isIsolated);

	/**
	 * Returns the conversation with the given id which has to be registered before. If no such conversation is found,
	 * <code>null</code> is returned rather than throwing an exception.
	 *
	 * @param id the id to return the conversation from this store
	 * @return the conversation, if found, <code>null</code> otherwise
	 */
	MutableConversation getConversation(String id);

	/**
	 * Stores the given conversation object within this repository. Depending on its implementation, the storage might be
	 * transient or persistent, it might relay on other mechanisms like a session (in the area of web conversations for
	 * instance). After the conversation has been stored, its id must be set hence the id of the conversation will be
	 * available only after the store method has been invoked.
	 *
	 * @param conversation the conversation to be stored within the repository
	 */
	void storeConversation(MutableConversation conversation);

	/**
	 * Removes the conversation with the given id from this store. Depending on the <code>root</code> flag, the whole
	 * conversation hierarchy is removed or just the specified conversation.
	 *
	 * @param id the id of the conversation to be removed
	 * @param root flag indicating whether the whole conversation hierarchy should be removed (<code>true</code>) or just
	 * the specified conversation (<code>false</code>)
	 */
	void removeConversation(String id, boolean root);
}
