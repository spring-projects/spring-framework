/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.support.converter;

/**
 * Constants that indicate a target message type to convert to: a
 * {@link jakarta.jms.TextMessage}, a {@link jakarta.jms.BytesMessage},
 * a {@link jakarta.jms.MapMessage} or an {@link jakarta.jms.ObjectMessage}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingMessageConverter#setTargetType
 */
public enum MessageType {

	/**
	 * A {@link jakarta.jms.TextMessage}.
	 */
	TEXT,

	/**
	 * A {@link jakarta.jms.BytesMessage}.
	 */
	BYTES,

	/**
	 * A {@link jakarta.jms.MapMessage}.
	 */
	MAP,

	/**
	 * A {@link jakarta.jms.ObjectMessage}.
	 */
	OBJECT

}
