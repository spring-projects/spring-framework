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

package org.springframework.core.codec;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * General error that indicates a problem while encoding and decoding to and
 * from an Object stream.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CodecException extends NestedRuntimeException {

	/**
	 * Create a new CodecException.
	 * @param msg the detail message
	 */
	public CodecException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Create a new CodecException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public CodecException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
