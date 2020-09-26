/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.validation.Validator;

/**
 * A resolver to extract and convert the payload of a message using a
 * {@link MessageConverter}. It also validates the payload using a
 * {@link Validator} if the argument is annotated with a Validation annotation.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 * @deprecated as of 5.2, in favor of {@link PayloadMethodArgumentResolver}
 */
@Deprecated
public class PayloadArgumentResolver extends PayloadMethodArgumentResolver {

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @since 4.0.9
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator) {
		this(messageConverter, validator, true);
	}

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 * @param useDefaultResolution if "true" (the default) this resolver supports
	 * all parameters; if "false" then only arguments with the {@code @Payload}
	 * annotation are supported.
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator,
			boolean useDefaultResolution) {


		super(messageConverter, validator, useDefaultResolution);
	}

}
