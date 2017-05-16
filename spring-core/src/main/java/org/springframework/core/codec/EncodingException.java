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

/**
 * Indicates an issue with encoding the input Object stream with a focus on
 * not being able to encode Objects. As opposed to a more general I/O errors
 * or a {@link CodecException} such as a configuration issue that an
 * {@link Encoder} may also choose to raise.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Encoder
 */
@SuppressWarnings("serial")
public class EncodingException extends CodecException {

	/**
	 * Create a new EncodingException.
	 * @param msg the detail message
	 */
	public EncodingException(String msg) {
		super(msg);
	}

	/**
	 * Create a new EncodingException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public EncodingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
