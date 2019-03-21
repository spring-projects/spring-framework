/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.support;

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

	private StringValueResolver embeddedValueResolver;


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
	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


}
