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
 * Codec exception suitable for internal errors, like those not related to invalid data. It can be used to make sure
 * such error will produce a 5xx status code and not a 4xx one when reading HTTP messages for example.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@SuppressWarnings("serial")
public class InternalCodecException extends CodecException {

	public InternalCodecException(String msg) {
		super(msg);
	}

	public InternalCodecException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
