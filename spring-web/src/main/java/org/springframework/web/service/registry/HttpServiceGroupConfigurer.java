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

import java.util.function.Predicate;

import org.springframework.core.Ordered;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Callback to configure the set of declared {@link HttpServiceGroup}s.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @param <CB> the type of client builder, i.e. {@code RestClient} or {@code WebClient} builder.
 */
@FunctionalInterface
public interface HttpServiceGroupConfigurer<CB> extends Ordered {

	/**
	 * Configure the underlying infrastructure for all group.
	 */
	void configureGroups(Groups<CB> groups);

	/**
	 * Determine the order of this configurer relative to others.
	 * <p>By default, this is {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	@Override
	default int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}


	/**
	 * Contract to help iterate and configure the set of groups.
	 * @param <CB> the type of client builder, i.e. {@code RestClient} or {@code WebClient} builder.
	 */
	interface Groups<CB> {

		/**
		 * Select groups to configure by name.
		 */
		Groups<CB> filterByName(String... groupNames);

		/**
		 * Select groups to configure through a {@link Predicate}.
		 */
		Groups<CB> filter(Predicate<HttpServiceGroup> predicate);

		/**
		 * Configure the client builder for each
		 * {@link #filter(Predicate) filtered} group.
		 */
		void forEachClient(ClientCallback<CB> callback);

		/**
		 * Configure the {@code HttpServiceProxyFactory} for each
		 * {@link #filter(Predicate) filtered} group.
		 */
		void forEachProxyFactory(ProxyFactoryCallback callback);

		/**
		 * Configure the client builder and {@code HttpServiceProxyFactory} for each
		 * {@link #filter(Predicate) filtered} group.
		 */
		void forEachGroup(GroupCallback<CB> callback);
	}


	/**
	 * Callback to configure the client for a given group.
	 * @param <CB> the type of client builder, i.e. {@code RestClient} or {@code WebClient} builder.
	 */
	@FunctionalInterface
	interface ClientCallback<CB> {

		void withClient(HttpServiceGroup group, CB clientBuilder);
	}


	/**
	 * Callback to configure the {@code HttpServiceProxyFactory} for a given group.
	 */
	@FunctionalInterface
	interface ProxyFactoryCallback {

		void withProxyFactory(HttpServiceGroup group, HttpServiceProxyFactory.Builder factoryBuilder);
	}


	/**
	 * Callback to configure the client and {@code HttpServiceProxyFactory} for a given group.
	 * @param <CB> the type of client builder, i.e. {@code RestClient} or {@code WebClient} builder.
	 */
	@FunctionalInterface
	interface GroupCallback<CB> {

		void withGroup(HttpServiceGroup group,
				CB clientBuilder, HttpServiceProxyFactory.Builder factoryBuilder);
	}

}
