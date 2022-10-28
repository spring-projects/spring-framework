/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

/**
 * Base class for {@link ServletRequestBindingException} exceptions that could
 * not bind because the request value is required but is either missing or
 * otherwise resolves to {@code null} after conversion.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.6
 */
@SuppressWarnings("serial")
public class MissingRequestValueException extends ServletRequestBindingException {

	private final boolean missingAfterConversion;


	/**
	 * Constructor with a message only.
	 */
	public MissingRequestValueException(String msg) {
		this(msg, false);
	}

	/**
	 * Constructor with a message and a flag that indicates whether a value
	 * was present but became {@code null} after conversion.
	 */
	public MissingRequestValueException(String msg, boolean missingAfterConversion) {
		super(msg);
		this.missingAfterConversion = missingAfterConversion;
	}

	/**
	 * Constructor with a given {@link ProblemDetail}, and a
	 * {@link org.springframework.context.MessageSource} code and arguments to
	 * resolve the detail message with.
	 * @since 6.0
	 */
	protected MissingRequestValueException(String msg, boolean missingAfterConversion,
			@Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

		super(msg, messageDetailCode, messageDetailArguments);
		this.missingAfterConversion = missingAfterConversion;
	}


	/**
	 * Whether the request value was present but converted to {@code null}, e.g. via
	 * {@code org.springframework.core.convert.support.IdToEntityConverter}.
	 */
	public boolean isMissingAfterConversion() {
		return this.missingAfterConversion;
	}

}
