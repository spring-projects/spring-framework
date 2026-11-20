/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.web.bind.support.WebExchangeDataBinder;

/**
 * Extended variant of {@link WebExchangeDataBinder} that adds URI path variables
 * and request headers to the bind values map.
 *
 * <p>Note: This class has existed since 5.0, but only as a private class within
 * {@link org.springframework.web.reactive.BindingContext}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2.1
 * @deprecated in favor of the relocated
 * {@link org.springframework.web.reactive.result.ExtendedWebExchangeDataBinder}
 */
@Deprecated(since = "7.0.2", forRemoval = true)
public class ExtendedWebExchangeDataBinder extends org.springframework.web.reactive.result.ExtendedWebExchangeDataBinder {

	public ExtendedWebExchangeDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}

}
