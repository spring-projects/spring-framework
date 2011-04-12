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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.conversation.Conversation;

/**
 * A {@link ConversationRepository} storing the conversations within an internal map and hence assuming the conversation
 * objects being transient.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class LocalTransientConversationRepository extends AbstractConversationRepository {

	/** The map for the conversation storage. */
	private final ConcurrentMap<String, MutableConversation> conversationMap = new ConcurrentHashMap<String, MutableConversation>();

	/** Using an atomic integer, there is no need for synchronization while increasing the number. */
	private final AtomicInteger nextAvailableConversationId = new AtomicInteger(0);


	public MutableConversation getConversation(String id) {
		return conversationMap.get(id);
	}

	public void storeConversation(MutableConversation conversation) {
		conversation.setId(createNextConversationId());
		conversationMap.put(conversation.getId(), conversation);
	}

	@Override
	protected void removeSingleConversationObject(MutableConversation conversation) {
		conversationMap.remove(conversation.getId());
	}

	public List<Conversation> getConversations() {
		return new ArrayList<Conversation>(conversationMap.values());
	}

	public int size() {
		return conversationMap.size();
	}

	protected String createNextConversationId(){
		return Integer.toString(nextAvailableConversationId.incrementAndGet());
	}

}
