/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
