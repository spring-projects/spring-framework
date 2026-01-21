/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.registry;

import java.util.Set;

/**
 * A group of HTTP Service interfaces that share the same
 * {@link org.springframework.web.service.invoker.HttpServiceProxyFactory} and
 * HTTP client setup.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 */
public interface HttpServiceGroup {

	/**
	 * The name of the default group to add HTTP Services to when a group is not specified.
	 */
	String DEFAULT_GROUP_NAME = "default";


	/**
	 * The name of the HTTP Service group.
	 */
	String name();

	/**
	 * The HTTP Services in the group.
	 */
	Set<Class<?>> httpServiceTypes();

	/**
	 * The client type to use for the group.
	 * <p>By default, {@link ClientType#REST_CLIENT} remains unspecified.
	 */
	ClientType clientType();


	/**
	 * Enum to specify the client type to use for an HTTP Service group.
	 */
	enum ClientType {

		/**
		 * A group backed by {@link org.springframework.web.client.RestClient}.
		 */
		REST_CLIENT,

		/**
		 * A group backed by {@link org.springframework.web.reactive.function.client.WebClient}.
		 */
		WEB_CLIENT,

		/**
		 * Not specified, falling back on a default.
		 * @see ImportHttpServices#clientType()
		 * @see AbstractHttpServiceRegistrar#setDefaultClientType
		 */
		UNSPECIFIED;


		/**
		 * Shortcut to check if this is the UNSPECIFIED enum value.
		 */
		boolean isUnspecified() {
			return (this == UNSPECIFIED);
		}
	}

}
