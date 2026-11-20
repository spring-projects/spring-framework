/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.jspecify.annotations.Nullable;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

/**
 * Convenient base class for components with a need for embedded value resolution
 * (i.e. {@link org.springframework.context.EmbeddedValueResolverAware} consumers).
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public class EmbeddedValueResolutionSupport implements EmbeddedValueResolverAware {

	private @Nullable StringValueResolver embeddedValueResolver;


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * Resolve the given embedded value through this instance's {@link StringValueResolver}.
	 * @param value the value to resolve
	 * @return the resolved value, or always the original value if no resolver is available
	 * @see #setEmbeddedValueResolver
	 */
	protected @Nullable String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


}
