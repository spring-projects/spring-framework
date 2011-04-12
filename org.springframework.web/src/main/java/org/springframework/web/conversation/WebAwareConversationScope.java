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

import org.springframework.conversation.scope.ConversationScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * The extension of the default conversation scope ( {@link ConversationScope}) by supporting contextual web
 * objects returned by overwriting {@link #resolveContextualObject(String)}.
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class WebAwareConversationScope extends ConversationScope {

	/** @see org.springframework.conversation.scope.ConversationScope#resolveContextualObject(String) */
	@Override
	public Object resolveContextualObject(String key) {
		// invoke super to support the conversation context objects
		Object object = super.resolveContextualObject(key);
		if (object != null) {
			return object;
		}

		// support web context objects
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		return attributes.resolveReference(key);
	}
}
