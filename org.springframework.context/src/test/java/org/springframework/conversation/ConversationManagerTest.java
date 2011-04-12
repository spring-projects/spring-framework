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

import org.junit.Test;

import org.springframework.conversation.manager.DefaultConversationManager;
import org.springframework.conversation.manager.LocalTransientConversationRepository;
import org.springframework.conversation.manager.ThreadLocalConversationResolver;

import static org.junit.Assert.*;

/**
 * @author Micha Kiener
 */
public class ConversationManagerTest {

	@Test
	public void testNewConversationCreation() {
		ConversationManager manager = createConversationManager();
		assertNotNull("manager must not be null", manager);

		Conversation conversation = manager.getCurrentConversation();
		assertNotNull("implicit creation of a current conversation must not return null", conversation);

		Conversation conversation2 = manager.getCurrentConversation();
		assertSame("subsequent invocation of current conversation must return the same object", conversation,
				conversation2);

		conversation2 = manager.getCurrentConversation(false);
		assertSame("subsequent invocation of current conversation must return the same object", conversation,
				conversation2);

		conversation2 = manager.getCurrentConversation(true);
		assertSame("subsequent invocation of current conversation must return the same object", conversation,
				conversation2);
	}

	@Test
	public void testNewConversationCreationAndRemoval() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		manager.endCurrentConversation(true);
		Conversation conversation2 = manager.getCurrentConversation();

		assertNotSame("an ended conversation must not be reused", conversation, conversation2);
		manager.endCurrentConversation(true);

		conversation = manager.getCurrentConversation(false);
		assertNull("Requesting the current conversation without implicit creation must not create one", conversation);
	}

	@Test
	public void testNewNestedConversation() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		Conversation childConversation = manager.beginConversation(ConversationType.NESTED);
		assertNotSame("child conversation must be different from its parent", conversation, childConversation);
		assertSame("child conversation must be linked to its parent", conversation, childConversation.getParent());
		assertSame("child conversation must be the current one", childConversation, manager.getCurrentConversation());
		assertTrue("child conversation must be nested", childConversation.isNested());
		assertFalse("child conversation must not be isolated", childConversation.isIsolated());
	}

	@Test
	public void testNewIsolatedConversation() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		Conversation childConversation = manager.beginConversation(ConversationType.ISOLATED);
		assertNotSame("child conversation must be different from its parent", conversation, childConversation);
		assertSame("child conversation must be linked to its parent", conversation, childConversation.getParent());
		assertSame("child conversation must be the current one", childConversation, manager.getCurrentConversation());
		assertTrue("child conversation must be nested", childConversation.isNested());
		assertTrue("child conversation must be isolated", childConversation.isIsolated());
	}

	@Test
	public void testAndEndNewNestedConversation() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		Conversation childConversation = manager.beginConversation(ConversationType.NESTED);
		assertSame("child conversation must be the current one", childConversation, manager.getCurrentConversation());

		manager.endCurrentConversation(false);
		assertSame("after ending a child conversation, its parent has to be the current one", conversation,
				manager.getCurrentConversation());
	}

	@Test
	public void testAndRootEndNewNestedConversation() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		Conversation childConversation = manager.beginConversation(ConversationType.NESTED);
		assertSame("child conversation must be the current one", childConversation, manager.getCurrentConversation());

		manager.endCurrentConversation(true);
		assertNull("ending a conversation with root mode, will end in having no current conversation",
				manager.getCurrentConversation(false));
	}

	@Test
	public void testImplicitEnding() {
		ConversationManager manager = createConversationManager();
		Conversation conversation = manager.getCurrentConversation();

		Conversation newConversation = manager.beginConversation(ConversationType.NEW);
		assertSame("new conversation must be the current one", newConversation, manager.getCurrentConversation());

		manager.endCurrentConversation(true);
		assertNull("implicit ending must not reactivate previous conversation", manager.getCurrentConversation(false));
	}

	protected ConversationManager createConversationManager() {
		DefaultConversationManager manager = new DefaultConversationManager();
		LocalTransientConversationRepository repository = new LocalTransientConversationRepository();
		ThreadLocalConversationResolver resolver = new ThreadLocalConversationResolver();

		manager.setConversationRepository(repository);
		manager.setConversationResolver(resolver);
				
		return manager;
	}
}
