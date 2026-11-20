/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.request;

import org.springframework.test.web.servlet.SmartRequestBuilder;

/**
 * An extension of {@link org.springframework.test.web.servlet.SmartRequestBuilder
 * SmartRequestBuilder} that can be configured with {@link RequestPostProcessor RequestPostProcessors}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @param <B> a self reference to the builder type
 */
public interface ConfigurableSmartRequestBuilder<B extends ConfigurableSmartRequestBuilder<B>>
		extends SmartRequestBuilder {

	/**
	 * Add the given {@code RequestPostProcessor}.
	 */
	B with(RequestPostProcessor requestPostProcessor);

}
