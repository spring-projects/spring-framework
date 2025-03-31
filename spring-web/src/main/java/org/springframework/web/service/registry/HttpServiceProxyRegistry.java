/*
 * Copyright 2002-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * A registry that contains HTTP Service client proxies.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @see ImportHttpServices
 * @see HttpServiceProxyRegistryFactoryBean
 */
public interface HttpServiceProxyRegistry {

	/**
	 * Return an HTTP service client proxy from any group as long as there is
	 * only one client proxy of the given type across all groups.
	 * @param httpServiceType the type of client proxy
	 * @return the proxy, or {@code null} if not found
	 * @param <P> the type of HTTP Interface client proxy
	 * @throws IllegalArgumentException if more than one client proxy of the
	 * given type exists across groups
	 */
	<P> @Nullable P getClient(Class<P> httpServiceType);

	/**
	 * Return an HTTP service client proxy from the given group.
	 * @param groupName the name of the group
	 * @param httpServiceType the type of client proxy
	 * @return the proxy, or {@code null} if not found
	 * @param <P> the type of HTTP Interface client proxy
	 */
	<P> @Nullable P getClient(String groupName, Class<P> httpServiceType);

}
