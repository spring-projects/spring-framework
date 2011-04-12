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

package org.springframework.conversation;

import java.util.List;

/**
 * The interface for a conversation object being managed by the {@link ConversationManager} and created, stored and
 * removed by the {@link org.springframework.conversation.manager.ConversationRepository}.<br/>
 * The conversation object is most likely never used directly but rather indirectly through the
 * {@link org.springframework.conversation.scope.ConversationScope}. It supports fine grained access to the conversation
 * container for storing and retrieving attributes, access the conversation hierarchy or manage the timeout behavior
 * of the conversation.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
public interface Conversation {

	/**
	 * Returns the id of this conversation which must be unique within the scope it is used to identify the conversation
	 * object. The id is set by the {@link org.springframework.conversation.manager.ConversationRepository} and most
	 * likely be used by the {@link org.springframework.conversation.manager.ConversationResolver} in order to manage
	 * the current conversation.
	 *
	 * @return the id of this conversation
	 */
	String getId();

	/**
	 * Stores the given value in this conversation using the specified name. If this state already contains a value
	 * attached to the given name, it is returned, <code>null</code> otherwise.<br/> This method stores the attribute
	 * within this conversation so it will be available through this and all nested conversations.
	 *
	 * @param name the name of the value to be stored in this conversation
	 * @param value the value to be stored
	 * @return the old value attached to the same name, if any, <code>null</code> otherwise
	 */
	Object setAttribute(String name, Object value);

	/**
	 * Returns the value attached to the given name, if any previously registered, <code>null</code> otherwise.<br/>
	 * Returns the attribute stored with the given name within this conversation or any within the path through its parent
	 * to the top level root conversation. If this is a nested, isolated conversation, attributes are only being resolved
	 * within this conversation, not from its parent.
	 *
	 * @param name the name of the value to be retrieved
	 * @return the value, if available in the current state, <code>null</code> otherwise
	 */
	Object getAttribute(String name);

	/**
	 * Removes the value in the current conversation having the given name and returns it, if found and removed,
	 * <code>null</code> otherwise.<br/> Removes the attribute from this specific conversation, does not remove it, if
	 * found within its parent.
	 *
	 * @param name the name of the value to be removed from this conversation
	 * @return the removed value, if found, <code>null</code> otherwise
	 */
	Object removeAttribute(String name);

	/**
	 * Returns the top level root conversation, if this is a nested conversation or this conversation, if it is the top
	 * level root conversation. This method never returns <code>null</code>.
	 *
	 * @return the root conversation (top level conversation)
	 */
	Conversation getRoot();

	/**
	 * Returns the parent conversation, if this is a nested conversation, <code>null</code> otherwise.
	 *
	 * @return the parent conversation, if any, <code>null</code> otherwise
	 */
	Conversation getParent();

	/**
     * Returns a list of child conversations, if any, an empty list otherwise, must never return <code>null</code>.
     *
     * @return a list of child conversations (may be empty, never <code>null</code>)
     */
    List<? extends Conversation> getChildren();

	/**
	 * Returns <code>true</code>, if this is a nested conversation and hence {@link #getParent()} will returns a non-null
	 * value.
	 *
	 * @return <code>true</code>, if this is a nested conversation, <code>false</code> otherwise
	 */
	boolean isNested();

	/**
	 * Returns <code>true</code>, if this is a nested, isolated conversation so that it does not inherit the state from its
	 * parent but rather has its own state. See {@link ConversationType#ISOLATED} for more details.
	 *
	 * @return <code>true</code>, if this is a nested, isolated conversation
	 */
	boolean isIsolated();

	/**
	 * Returns the timestamp in milliseconds this conversation has been created.
	 *
	 * @return the creation timestamp in millis
	 */
	long getCreationTime();

	/**
	 * Returns the timestamp in milliseconds this conversation was last accessed (usually through a {@link
	 * #getAttribute(String)}, {@link #setAttribute(String, Object)} or {@link #removeAttribute(String)} access).
	 *
	 * @return the system time in milliseconds for the last access of this conversation
	 */
	long getLastAccessedTime();

	/**
	 * Returns the timeout of this conversation object in seconds. A value of <code>0</code> stands for no timeout.
	 * The timeout is usually managed on the root conversation object and will be returned regardless of the hierarchy
	 * of this conversation.
	 *
	 * @return the timeout in seconds if any, <code>0</code> otherwise
	 */
	int getTimeout();

	/**
	 * Set the timeout of this conversation hierarchy in seconds. A value of <code>0</code> stands for no timeout.
	 * Regardless of the hierarchy of this conversation, a timeout is always set on the top root conversation and is
	 * valid for all conversations within the same hierarchy.
	 *
	 * @param timeout the timeout in seconds to set, <code>0</code> for no timeout
	 */
	void setTimeout(int timeout);
}
