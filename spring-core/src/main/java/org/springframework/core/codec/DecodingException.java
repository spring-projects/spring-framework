/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.core.codec;

import org.springframework.lang.Nullable;

/**
 * Indicates an issue with decoding the input stream with a focus on content
 * related issues such as a parse failure. As opposed to more general I/O
 * errors, illegal state, or a {@link CodecException} such as a configuration
 * issue that a {@link Decoder} may choose to raise.
 *
 * <p>For example in server web application, a {@code DecodingException} would
 * translate to a response with a 400 (bad input) status while
 * {@code CodecException} would translate to 500 (server error) status.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Decoder
 */
@SuppressWarnings("serial")
public class DecodingException extends CodecException {

	/**
	 * Create a new DecodingException.
	 * @param msg the detail message
	 */
	public DecodingException(String msg) {
		super(msg);
	}

	/**
	 * Create a new DecodingException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public DecodingException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
