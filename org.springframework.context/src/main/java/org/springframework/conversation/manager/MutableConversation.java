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

import org.springframework.conversation.Conversation;

/**
 * <p>
 * This interface extends the {@link Conversation} interface and is most likely used internally to modify the
 * conversation object. Should never be used outside of the conversation management infrastructure.
 * </p>
 *
 * @author Micha Kiener
 * @since 3.1
 */
public interface MutableConversation extends Conversation {

	/**
	 * Set the id for this conversation which must be unique within the scope the conversation objects are being stored.
	 * The id of the conversation objects is usually managed by the {@link ConversationRepository}.
	 * 
	 * @param id the id of the conversation
	 */
	void setId(String id);

	/**
	 * Method being invoked to add the given conversation as a child conversation to this parent conversation. If
	 * <code>isIsolated</code> is <code>true</code>, the state of the child conversation is isolated from its parent
	 * state, if it is set to <code>false</code>, the child conversation will inherit the state from its parent.
	 *
	 * @param conversation the conversation to be added as a child to this parent conversation
	 * @param isIsolated flag indicating whether this conversation should be isolated from the given parent conversation
	 */
	void addChildConversation(MutableConversation conversation, boolean isIsolated);

	/**
	 * Removes the given child conversation from this parent conversation.
	 *
	 * @param conversation the conversation to be removed from this one
	 */
	void removeChildConversation(MutableConversation conversation);

	/**
	 * Reset the last access timestamp using the current time in milliseconds from the system. This is usually done if a
	 * conversation is used behind a scope and beans are being accessed or added to it.
	 */
	void touch();

	/**
	 * Clears the state of this conversation by removing all of its attributes. It will, however, not invalidate the
	 * conversation. All attributes having a destruction callback being registered will fire, if the underlying
	 * {@link ConversationRepository} supports destruction callbacks.
	 */
	void clear();

	/**
	 * Returns <code>true</code> if this conversation has been expired. The expiration time (timeout) is only managed
	 * on the root conversation and is valid for the whole conversation hierarchy.
	 *
	 * @return <code>true</code> if this conversation has been expired, <code>false</code> otherwise
	 */
	boolean isExpired();

	/**
	 * Invalidates this conversation object. An invalidated conversation will throw an {@link IllegalStateException},
	 * if it is accessed or modified.
	 */
	void invalidate();

	/**
	 * Registers the given callback to be invoked if the attribute having the specified name is being removed from this
	 * conversation. Supporting destruction callbacks is dependant of the underlying {@link ConversationRepository}, so
	 * this operation is optional and might not be supported.
	 *
	 * @param attributeName the name of the attribute to register the destruction callback for
	 * @param callback the callback to be invoked if the specified attribute is removed from this conversation
	 */
	void registerDestructionCallback(String attributeName, Runnable callback);
}
