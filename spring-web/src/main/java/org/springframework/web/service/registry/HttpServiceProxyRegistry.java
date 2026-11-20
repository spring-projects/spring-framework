/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.registry;

import java.util.Set;

/**
 * A registry for HTTP service clients organized by {@link HttpServiceGroup}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @see ImportHttpServices
 * @see HttpServiceProxyRegistryFactoryBean
 */
public interface HttpServiceProxyRegistry {

	/**
	 * Return an HTTP service client from any group as long as there is only one
	 * client of this type across all groups.
	 * @param httpServiceType the type of client
	 * @param <P> the type of HTTP service client
	 * @return the matched client
	 * @throws IllegalArgumentException if there is no client of the given type,
	 * or there is more than one client of the given type.
	 */
	<P> P getClient(Class<P> httpServiceType);

	/**
	 * Return an HTTP service client from the named group.
	 * @param groupName the name of the group
	 * @param httpServiceType the type of client
	 * @param <P> the type of HTTP service client
	 * @return the matched client
	 * @throws IllegalArgumentException if there is no matching client.
	 */
	<P> P getClient(String groupName, Class<P> httpServiceType);

	/**
	 * Return the names of all groups in the registry.
	 */
	Set<String> getGroupNames();

	/**
	 * Return all HTTP service client types in the named group.
	 * @param groupName the name of the group
	 * @return the HTTP service types
	 * @throws IllegalArgumentException if there is no matching group.
	 */
	Set<Class<?>> getClientTypesInGroup(String groupName);

}
