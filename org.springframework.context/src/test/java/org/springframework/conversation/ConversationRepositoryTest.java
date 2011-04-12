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

import org.junit.Test;

import org.springframework.conversation.manager.ConversationRepository;
import org.springframework.conversation.manager.LocalTransientConversationRepository;
import org.springframework.conversation.manager.MutableConversation;

import static org.junit.Assert.*;

/**
 * @author Micha Kiener
 */
public class ConversationRepositoryTest {

	@Test
	public void testConversationCreation() {
		ConversationRepository repository = createRepository();
		assertNotNull("repository must not be null", repository);

		MutableConversation conversation = repository.createNewConversation();
		assertNotNull("conversation object must not be null", conversation);
		assertNull("Id must not be set after creation", conversation.getId());
		assertNotNull("List of children must never be null", conversation.getChildren());
		assertEquals("Children list must be empty", 0, conversation.getChildren().size());
	}

	@Test
	public void testConversationCreationAndStorage() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String conversationId = conversation.getId();
		assertNotNull("ID must not be null after storage of a conversation", conversationId);

		MutableConversation conversationRequested = repository.getConversation(conversationId);
		assertNotNull("Conversation must be available after storage", conversationRequested);
		assertSame("Conversation objects must be the same in a transient repository", conversation,
				conversationRequested);
	}

	@Test
	public void testConversationCreationStorageAndRemoval() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String conversationId = conversation.getId();

		repository.removeConversation(conversationId, true);
		MutableConversation conversationRequested = repository.getConversation(conversationId);
		assertNull("Conversation must no longer be available after removal", conversationRequested);
	}

	@Test
	public void testConversationHierarchyCreation() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String rootConversationId = conversation.getId();

		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		assertNotNull("child conversation must not be null", childConversation);
		assertNotNull("parent conversation must be available", childConversation.getParent());
		assertSame("parent conversation and root conversation must be the same", conversation,
				childConversation.getParent());
		assertFalse("nested conversation must not be isolated", childConversation.isIsolated());

		List<? extends Conversation> children = conversation.getChildren();
		assertNotNull("children list must never be null", children);
		assertEquals("children size must be one", 1, children.size());
		assertSame("child conversation must be available through the children list of the root conversation",
				childConversation, children.get(0));

		repository.storeConversation(childConversation);
		String childConversationId = childConversation.getId();
		assertNotNull("child conversation id must be set after storage", childConversationId);
		assertNotSame("root and child conversation id must be different", rootConversationId, childConversationId);

		assertNotNull("root conversation must be retrievable through repository",
				repository.getConversation(rootConversationId));
		assertNotNull("child conversation must be retrievable through repository",
				repository.getConversation(childConversationId));

		assertSame("root conversation must be the same, if getRoot is invoked", conversation, conversation.getRoot());
		assertSame("root conversation must be the same, if getRoot is invoked", conversation,
				childConversation.getRoot());
	}

	@Test
	public void testConversationHierarchyCreationAndSingleRemoval() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String rootConversationId = conversation.getId();

		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		repository.storeConversation(childConversation);
		String childConversationId = childConversation.getId();

		repository.removeConversation(childConversationId, false);

		assertNull("parent must not be available after removal", childConversation.getParent());
		assertEquals("list of children must be empty after removal", 0, conversation.getChildren().size());

		assertNull("child conversation must not be available through the repository after removal",
				repository.getConversation(childConversationId));
		assertNotNull("root conversation must still be available after removal of its child conversation",
				repository.getConversation(rootConversationId));
	}

	@Test
	public void testConversationHierarchyCreationAndRootThroughChildRemoval() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String rootConversationId = conversation.getId();

		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		repository.storeConversation(childConversation);
		String childConversationId = childConversation.getId();

		repository.removeConversation(childConversationId, true);

		assertNull("parent must not be available after removal", childConversation.getParent());
		assertEquals("list of children must be empty after removal", 0, conversation.getChildren().size());

		assertNull("child conversation must not be available through the repository after removal",
				repository.getConversation(childConversationId));
		assertNull("root conversation must not be available after root removal",
				repository.getConversation(rootConversationId));
	}

	@Test
	public void testConversationHierarchyCreationAndRootThroughParentRemoval() {
		ConversationRepository repository = createRepository();

		MutableConversation conversation = repository.createNewConversation();
		repository.storeConversation(conversation);
		String rootConversationId = conversation.getId();

		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		repository.storeConversation(childConversation);
		String childConversationId = childConversation.getId();

		repository.removeConversation(rootConversationId, false);

		assertNull("parent must not be available after removal", childConversation.getParent());
		assertEquals("list of children must be empty after removal", 0, conversation.getChildren().size());

		assertNull("child conversation must not be available through the repository after removal",
				repository.getConversation(childConversationId));
		assertNull("root conversation must not be available after root removal",
				repository.getConversation(rootConversationId));
	}

	@Test
	public void testRepositoryDefaultTimeout() {
		LocalTransientConversationRepository repository = new LocalTransientConversationRepository();
		repository.setDefaultConversationTimeout(100);

		MutableConversation conversation = repository.createNewConversation();
		assertEquals("default timeout of repository has to be set on newly created conversations", 100,
				conversation.getTimeout());

		MutableConversation conversation2 = repository.createNewConversation();
		conversation2.setTimeout(50);
		assertEquals("setting a timeout on a conversation must not change the default one", 100,
				conversation.getTimeout());
	}

	@Test
	public void testRepositoryTimeoutInheritance() {
		LocalTransientConversationRepository repository = new LocalTransientConversationRepository();
		repository.setDefaultConversationTimeout(100);

		MutableConversation conversation = repository.createNewConversation();
		conversation.setTimeout(50);
		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);

		assertEquals("timeout of a child conversation must be inherited from its parent", 50,
				childConversation.getTimeout());
	}

	@Test
	public void testRepositoryCustomTimeoutChildParentPropagation() {
		LocalTransientConversationRepository repository = new LocalTransientConversationRepository();
		repository.setDefaultConversationTimeout(100);

		MutableConversation conversation = repository.createNewConversation();
		conversation.setTimeout(50);
		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		childConversation.setTimeout(70);

		assertEquals("the custom timeout being set on a child conversation must be propagated to its parent", 70,
				conversation.getTimeout());
	}

	@Test
	public void testRepositoryCustomTimeoutParentChildPropagation() {
		LocalTransientConversationRepository repository = new LocalTransientConversationRepository();
		repository.setDefaultConversationTimeout(100);

		MutableConversation conversation = repository.createNewConversation();
		MutableConversation childConversation = repository.createNewChildConversation(conversation, false);
		conversation.setTimeout(50);

		assertEquals("the custom timeout being set on a parent conversation must be propagated to its children", 50,
				childConversation.getTimeout());
	}
	
	protected ConversationRepository createRepository() {
		return new LocalTransientConversationRepository();
	}
}
