
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

/**
 * <p>
 * A conversation manager is used to manage conversations, most of all, the current conversation. It is used by
 * the advice behind the conversation annotations {@link org.springframework.conversation.annotation.BeginConversation}
 * and {@link org.springframework.conversation.annotation.EndConversation} in order to start and end conversations.
 * </p>
 * <p>
 * A conversation manager uses a {@link org.springframework.conversation.manager.ConversationRepository} to create,
 * store and remove conversation objects and a {@link org.springframework.conversation.manager.ConversationResolver}
 * to set and remove the current conversation id.
 * </p>
 * <p>
 * A conversation manager might be used manually in order to start and end conversations manually.
 * </p>
 * <p>
 * Conversations are a good way to scope beans and attributes depending on business logic boundary rather than a
 * technical boundary of a scope like session, request etc. Usually a conversation boundary is defined by the starting
 * point of a use case and ended accordingly or in other words a conversation defines the boundary for a unit of
 * work.<br/><br/>
 *
 * A conversation is either implicitly started upon the first request of a conversation scoped bean or it is
 * explicitly started by using the conversation manager manually or by placing the begin conversation on a method.<br/>
 * The same applies for ending conversations as they are either implicitly ended by starting a new one or if the
 * timeout of a conversation is reached or they are ended explicitly by placing the end conversation annotation or
 * using the conversation manager manually.
 * </p>
 * <p>
 * Conversations might have child conversations which are either nested and hence will inherit the state of their
 * parent or they are isolated by having its own state and hence being independent from its parent.
 * </p>
 * <p>
 * <b>Extending the conversation management</b><br/>
 * The conversation management ships with different out-of-the box implementations but is easy to extend.
 * To extend the storage mechanism of conversations, the {@link org.springframework.conversation.manager.ConversationRepository}
 * and maybe the {@link org.springframework.conversation.manager.DefaultConversation} have to be extended or
 * overwritten to support the desired behavior.<br/>
 * To change the behavior where the current conversation is stored, either overwrite the
 * {@link org.springframework.conversation.manager.ConversationResolver} or make sure the current conversation id
 * is being resolved, stored and removed within the default {@link org.springframework.conversation.manager.ThreadLocalConversationResolver}.
 * </p>
 *
 * @author Micha Kiener
 * @since 3.1
 */
public interface ConversationManager {

	/**
	 * Returns the current conversation and creates a new one, if there is currently no active conversation yet.
	 * Internally, the manager will use the {@link org.springframework.conversation.manager.ConversationResolver}
	 * to resolve the current conversation id and the {@link org.springframework.conversation.manager.ConversationRepository}
	 * to load the conversation object being returned.
	 *
	 * @return the current conversation, never <code>null</code>, will create a new conversation, if no one existing
	 */
	Conversation getCurrentConversation();

	/**
	 * Returns the current conversation, if existing or creates a new one, if currently no active conversation available
	 * and <code>createIfNotExisting</code> is specified as <code>true</code>.
	 * 
	 * @param createNewIfNotExisting <code>true</code>, if a new conversation should be created, if there is currently
	 * no active conversation in place, <code>false</code> to return <code>null</code>, if no current conversation active
	 * @return the current conversation or <code>null</code>, if no current conversation available and
	 * <code>createIfNotExisting</code> is set as <code>false</code>
	 */
	Conversation getCurrentConversation(boolean createNewIfNotExisting);

	/**
	 * Creates a new conversation according the given <code>conversationType</code> and makes it the current active
	 * conversation. See {@link ConversationType} for more detailed information about the different conversation
	 * creation types available.<br/>
	 * If {@link ConversationType#NEW} is specified, the current conversation will automatically be ended
	 *
	 * @param conversationType the type used to start a new conversation
	 * @return the newly created conversation
	 */
	Conversation beginConversation(ConversationType conversationType);

	/**
	 * Ends the current conversation, if any. If <code>root</code> is <code>true</code>, the whole conversation
	 * hierarchy is ended and there will no current conversation be active afterwards. If <code>root</code> is
	 * <code>false</code>, the current conversation is ended and if it is a nested one, its parent is made the
	 * current conversation.
	 *
	 * @param root <code>true</code> to end the whole current conversation hierarchy or <code>false</code> to just
	 * end the current conversation
	 */
	void endCurrentConversation(boolean root);

}
