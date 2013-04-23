/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.websocket;


/**
 * A strategy for obtaining a handler instance that is scoped to external lifecycle events
 * such as the opening and closing of a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface HandlerProvider<T> {

	/**
	 * Whether the provided handler is a shared instance or not.
	 */
	boolean isSingleton();

	/**
	 * The type of handler provided.
	 */
	Class<?> getHandlerType();

	/**
	 * Obtain the handler instance, either shared or created every time.
	 */
	T getHandler();

	/**
	 * Callback to destroy a previously created handler instance if it is not shared.
	 */
	void destroy(T handler);

}
