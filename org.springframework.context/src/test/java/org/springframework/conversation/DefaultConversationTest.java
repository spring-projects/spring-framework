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

import org.springframework.conversation.manager.DefaultConversation;
import org.springframework.conversation.manager.MutableConversation;

import static org.junit.Assert.*;

/**
 * @author Micha Kiener
 */
public class DefaultConversationTest {

	@Test
	public void	testConversationAttribute() {
		Conversation conversation = new DefaultConversation();

		String test = new String("Test");
		conversation.setAttribute("test", test);

		assertSame("attribute must be accessible through conversation object", test, conversation.getAttribute("test"));
	}

	@Test
	public void	testConversationAttributeInheritance() {
		MutableConversation conversation = new DefaultConversation();

		String test = new String("Test");
		conversation.setAttribute("test", test);

		MutableConversation childConversation = new DefaultConversation();
		conversation.addChildConversation(childConversation, false);

		assertSame("attribute must be accessible through child conversation object", test,
				childConversation.getAttribute("test"));
	}

	@Test
	public void	testConversationAttributeIsolation() {
		MutableConversation conversation = new DefaultConversation();

		String test = new String("Test");
		conversation.setAttribute("test", test);

		MutableConversation childConversation = new DefaultConversation();
		conversation.addChildConversation(childConversation, true);

		assertNull("attribute must not be accessible from within an isolated conversation",
				childConversation.getAttribute("test"));
	}

	@Test
	public void	testNewConversationAttributeIsolation() {
		MutableConversation conversation = new DefaultConversation();

		String test = new String("Test");
		conversation.setAttribute("test", test);

		MutableConversation childConversation = new DefaultConversation();
		conversation.addChildConversation(childConversation, true);

		String test2 = new String("Test2");
		childConversation.setAttribute("test2", test2);

		assertNull("newly added attribute on isolated conversation must not be accessible within its parent",
				conversation.getAttribute("test2"));
	}

	@Test
	public void	testConversationAttributeRemoval() {
		Conversation conversation = new DefaultConversation();

		String test = new String("Test");
		conversation.setAttribute("test", test);

		String test2 = (String)conversation.removeAttribute("test");
		assertNotNull("removed attribute must be returned", test2);
		assertSame("removed attribute must be the same as being added", test, test2);

		assertNull("removed attribute must not be accessible after removal", conversation.getAttribute("test"));
	}
}
