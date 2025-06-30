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
