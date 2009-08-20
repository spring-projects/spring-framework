/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.context.message;

/**
 * A factory for a default message to return if no message could be resolved.
 * Allows the message String to be created lazily, only when it is needed.
 * @author Keith Donald
 * @since 3.0
 * @see MessageBuilder
 */
public interface DefaultMessageFactory {
	
	/**
	 * Create the default message.
	 */
	String createDefaultMessage();
}
