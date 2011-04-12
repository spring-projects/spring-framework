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

import org.springframework.conversation.ConversationManager;
import org.springframework.conversation.ConversationType;

/**
 * The default implementation for the {@link org.springframework.conversation.ConversationManager} interface.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class DefaultConversationManager implements ConversationManager {

	/**
	 * The conversation resolver used to resolve and expose the currently used conversation id. Do not use this attribute
	 * directly, always use the {@link #getConversationResolver()} method as the getter could have been injected.
	 */
	private ConversationResolver conversationResolver;

	/**
	 * If the store was injected, this attribute holds the reference to it. Never use the attribute directly, always use
	 * the {@link #getConversationRepository()} getter as it could have been injected.
	 */
	private ConversationRepository conversationRepository;


	public MutableConversation getCurrentConversation() {
		return getCurrentConversation(true);
	}

	public MutableConversation getCurrentConversation(boolean createNewIfNotExisting) {
		ConversationResolver resolver = getConversationResolver();

		MutableConversation currentConversation = null;
		String conversationId = resolver.getCurrentConversationId();
		if (conversationId != null) {
			ConversationRepository repository = getConversationRepository();
			currentConversation = repository.getConversation(conversationId);
		}

		if (currentConversation == null && createNewIfNotExisting) {
			currentConversation = beginConversation(ConversationType.NEW);
		}

		return currentConversation;
	}

	/**
	 * The implementation uses the conversation repository to create a new root or a new child conversation depending on
	 * the conversation type specified.
	 *
	 * @param conversationType the type used to start a new conversation
	 * @return the newly created conversation
	 */
	public MutableConversation beginConversation(ConversationType conversationType) {
		ConversationRepository repository = getConversationRepository();
		ConversationResolver resolver = getConversationResolver();

		MutableConversation newConversation = null;

		switch (conversationType) {
			case NEW:
				// end the current conversation and create a new root one
				endCurrentConversation(true);
				newConversation = repository.createNewConversation();
				break;

			case NESTED:
			case ISOLATED:
				MutableConversation parentConversation = getCurrentConversation(false);

				// if a parent conversation is available, add the new conversation as its child conversation
				if (parentConversation != null) {
					newConversation = repository.createNewChildConversation(parentConversation,
							conversationType == ConversationType.ISOLATED);
				}
				else {
					// if no parent conversation found, create a new root one
					newConversation = repository.createNewConversation();
				}
				break;
		}


		// store the newly created conversation within its store and make it the current one through the resolver
		repository.storeConversation(newConversation);
		resolver.setCurrentConversationId(newConversation.getId());

		return newConversation;
	}

	/**
	 * The implementation only resolves the current conversation object using the repository, if only the given
	 * current conversation object and not the whole conversation hierarchy should be removed which can improve the
	 * removal from the underlying storage mechanism.
	 *
	 * @param root <code>true</code> to end the whole current conversation hierarchy or <code>false</code> to just
	 * remove the current conversation
	 */
	public void endCurrentConversation(boolean root) {
		// remove the conversation from the repository and clear the current conversation id through the resolver
		ConversationResolver resolver = getConversationResolver();
		ConversationRepository repository = getConversationRepository();

		String currentConversationId = resolver.getCurrentConversationId();
		if (currentConversationId == null) {
			return;
		}

		// if only the current conversation has to be removed without the full conversation hierarchy, the
		// current conversation must be set to the parent, if available
		if (!root) {
			MutableConversation currentConversation = repository.getConversation(currentConversationId);
			if (currentConversation != null && currentConversation.getParent() != null) {
				MutableConversation parentConversation = (MutableConversation)currentConversation.getParent();
				resolver.setCurrentConversationId(parentConversation.getId());
			}
		}
		
		repository.removeConversation(currentConversationId, root);
	}

	/**
	 * Returns the conversation resolver used to resolve the currently used conversation id. If the resolver itself has
	 * another scope than the manager, this method must be injected.
	 *
	 * @return the conversation resolver
	 */
	public ConversationResolver getConversationResolver() {
		return conversationResolver;
	}

	/**
	 * Inject the conversation resolver, if the method {@link #getConversationResolver()} is not injected and if the
	 * resolver has the same scope as the manager or even a more wider scope.
	 *
	 * @param conversationResolver the resolver to be injected
	 */
	public void setConversationResolver(ConversationResolver conversationResolver) {
		this.conversationResolver = conversationResolver;
	}

	/**
	 * Returns the repository where conversation objects are being registered. If the manager is in a wider scope than the
	 * repository, this method has to be injected.
	 *
	 * @return the conversation repository used to register conversation objects
	 */
	public ConversationRepository getConversationRepository() {
		return conversationRepository;
	}

	/**
	 * Inject the conversation repository to this manager which should only be done, if the method {@link
	 * #getConversationRepository()} is not injected and hence the repository has the same scope as the manager or wider.
	 *
	 * @param conversationRepository the repository to be injected
	 */
	public void setConversationRepository(ConversationRepository conversationRepository) {
		this.conversationRepository = conversationRepository;
	}
}
