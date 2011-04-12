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
 * The conversation type is used while starting a new conversation and declares how the conversation manager
 * should create and start it as well how to end the current conversation, if any.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public enum ConversationType {
	/**
	 * The type NEW creates a new root conversation and will end a current one, if any.
	 */
	NEW,

	/**
	 * The type NESTED will create a new conversation and add it as a child conversation to the current one,
	 * if available. If there is no current conversation, this type is the same as NEW.
	 * A nested conversation will inherit the state from its parent.
	 */
	NESTED,

	/**
	 * The type ISOLATED is basically the same as NESTED but will isolate the state from its parent. While a
	 * nested conversation will inherit the state from its parent, an isolated one does not but rather has its
	 * own state.
	 */
	ISOLATED
}
