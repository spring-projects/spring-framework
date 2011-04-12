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
 * An abstract implementation for a conversation repository. Its implementation is based on the
 * {@link org.springframework.conversation.manager.DefaultConversation} and manages its initial timeout and provides
 * easy removal functionality. Internally, there is no explicit check for the conversation object implementing the
 * {@link MutableConversation} interface, it is assumed to be implemented as the abstract repository also creates the
 * conversation objects.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
public abstract class AbstractConversationRepository implements ConversationRepository {

	/**
	 * The default timeout in seconds for new conversations, can be setup using the Spring configuration of the
	 * repository. A value of <code>0</code> means there is no timeout.
	 */
	private int defaultTimeout = 0;

	/**
	 * Creates a new conversation object and initializes its timeout by using the default timeout being set on this
	 * repository.
	 */
	public MutableConversation createNewConversation() {
		MutableConversation conversation = new DefaultConversation();
		conversation.setTimeout(getDefaultConversationTimeout());
		return conversation;
	}

	/**
	 * Creates a new conversation, attaches it to the parent and initializes its timeout as being set on the parent.
	 */
	public MutableConversation createNewChildConversation(MutableConversation parentConversation, boolean isIsolated) {
		MutableConversation childConversation = createNewConversation();
		parentConversation.addChildConversation(childConversation, isIsolated);
		childConversation.setTimeout(parentConversation.getTimeout());
		return childConversation;
	}

	/**
	 * Generic implementation of the remove method of a repository, handling the <code>root</code> flag automatically
	 * by invoking the {@link #removeConversation(org.springframework.conversation.Conversation)} by either passing in
	 * the root conversation or just the given conversation.<br/>
	 * Concrete repository implementations can overwrite the
	 * {@link #removeConversation(org.springframework.conversation.Conversation)} method to finally remove the
	 * conversation object or they might provide their own custom implementation for the remove operation by overwriting
	 * this method completely.
	 *
	 * @param id the id of the conversation to be removed
	 * @param root flag indicating whether the whole conversation hierarchy should be removed (<code>true</code>) or just
	 * the specified conversation
	 */
	public void removeConversation(String id, boolean root) {
		MutableConversation conversation = getConversation(id);
		if (conversation == null) {
			return;
		}

		if (root) {
			removeConversation((MutableConversation)conversation.getRoot());
		}
		else {
			removeConversation(conversation);
		}
	}

	/**
	 * Internal, final method recursively invoking this method for all children of the given conversation.
	 *
	 * @param conversation the conversation to be removed, including its children, if any
	 */
	protected final void removeConversation(MutableConversation conversation) {
		for (Conversation child : conversation.getChildren()) {
			// remove the child from its parent and recursively invoke this method to remove the children of the
			// current conversation
			conversation.removeChildConversation((MutableConversation)child);
			removeConversation((MutableConversation)child);
		}

		// end the conversation (will internally clear the attributes, invoke destruction callbacks, if any, and
		// invalidates the conversation
		conversation.clear();
		conversation.invalidate();

		// finally, remove the single object from the repository
		removeSingleConversationObject((MutableConversation)conversation);
	}

	/**
	 * Abstract removal method to be implemented by concrete repository implementations to remove the given, single
	 * conversation object. Any parent and child relations must not be handled within this method, just the removal of
	 * the given object.
	 *
	 * @param conversation the single conversation object to be removed from this repository
	 */
	protected abstract void removeSingleConversationObject(MutableConversation conversation);

	
	public void setDefaultConversationTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public int getDefaultConversationTimeout() {
		return defaultTimeout;
	}
}
