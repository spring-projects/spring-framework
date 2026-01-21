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

import org.springframework.web.service.invoker.HttpExchangeAdapter;

/**
 * Adapter that helps to configure a group independent of its client builder type.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 * @param <CB> the type of client builder, i.e. {@code RestClient} or {@code WebClient} builder.
 */
public interface HttpServiceGroupAdapter<CB> {

	/**
	 * Create a client builder instance.
	 */
	CB createClientBuilder();

	/**
	 * Return the type of configurer that is compatible with this group.
	 */
	Class<? extends HttpServiceGroupConfigurer<CB>> getConfigurerType();

	/**
	 * Use the client builder to create an {@link HttpExchangeAdapter} to use to
	 * initialize the {@link org.springframework.web.service.invoker.HttpServiceProxyFactory}
	 * for the group.
	 */
	HttpExchangeAdapter createExchangeAdapter(CB clientBuilder);

}
