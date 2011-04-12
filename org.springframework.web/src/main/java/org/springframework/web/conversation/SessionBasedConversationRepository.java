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

package org.springframework.web.conversation;

import java.io.Serializable;

import org.springframework.conversation.Conversation;
import org.springframework.conversation.manager.AbstractConversationRepository;
import org.springframework.conversation.manager.MutableConversation;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * A conversation store implementation ({@link org.springframework.conversation.manager.ConversationRepository}) based
 * on a web environment where the conversations are most likely to be stored in the current session.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class SessionBasedConversationRepository extends AbstractConversationRepository {

	/** The name of the attribute for the conversation map within the session. */
	public static final String CONVERSATION_STORE_ATTR_NAME = SessionBasedConversationRepository.class.getName();


	public MutableConversation getConversation(String id) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		Object conversation = attributes
				.getAttribute(getSessionAttributeNameForConversationId(id), RequestAttributes.SCOPE_GLOBAL_SESSION);
		Assert.isInstanceOf(Conversation.class, conversation);
		return (MutableConversation) conversation;
	}

	public void storeConversation(MutableConversation conversation) {
		conversation.setId(createNextConversationId());
		
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		String conversationSessionAttributeName = getSessionAttributeNameForConversationId(conversation.getId());

		attributes.setAttribute(conversationSessionAttributeName, conversation, RequestAttributes.SCOPE_GLOBAL_SESSION);
		attributes.registerDestructionCallback(conversationSessionAttributeName,
				new ConversationDestructionCallback(conversation), RequestAttributes.SCOPE_GLOBAL_SESSION);
	}

	@Override
	protected void removeSingleConversationObject(MutableConversation conversation) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		attributes.removeAttribute(getSessionAttributeNameForConversationId(conversation.getId()),
				RequestAttributes.SCOPE_GLOBAL_SESSION);
	}

	protected String getSessionAttributeNameForConversationId(String conversationId) {
		return CONVERSATION_STORE_ATTR_NAME + conversationId;
	}

	protected String createNextConversationId() {
		int nextAvailableConversationId = 1;

		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		synchronized (attributes.getSessionMutex()) {
			String[] attributeNames = attributes.getAttributeNames(RequestAttributes.SCOPE_GLOBAL_SESSION);
			for (String attributeName : attributeNames) {
				if (attributeName.startsWith(CONVERSATION_STORE_ATTR_NAME)) {

					MutableConversation conversation = (MutableConversation) attributes
							.getAttribute(attributeName, RequestAttributes.SCOPE_GLOBAL_SESSION);
					if (conversation.isExpired()) {
						conversation.clear();
						attributes.removeAttribute(attributeName, RequestAttributes.SCOPE_GLOBAL_SESSION);
					}

					String conversationId = conversation.getId();
					int currentConversationId = Integer.parseInt(conversationId);
					if (currentConversationId > nextAvailableConversationId) {
						nextAvailableConversationId = currentConversationId;
					}
				}
			}
		}
		return Integer.toString(nextAvailableConversationId);
	}

	
	private static final class ConversationDestructionCallback implements Runnable, Serializable {

		private final MutableConversation conversation;

		private ConversationDestructionCallback(MutableConversation conversation) {
			this.conversation = conversation;
		}

		public void run() {
			conversation.clear();
		}
	}
}
