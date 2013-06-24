/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.Map;

/**
 * A message implementation that accepts a {@link Throwable} payload.
 * Once created this object is immutable.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 4.0
 */
public class ErrorMessage extends GenericMessage<Throwable> {

	private static final long serialVersionUID = -5470210965279837728L;

	public ErrorMessage(Throwable payload) {
		super(payload);
	}

	public ErrorMessage(Throwable payload, Map<String, Object> headers) {
		super(payload, headers);
	}

}
