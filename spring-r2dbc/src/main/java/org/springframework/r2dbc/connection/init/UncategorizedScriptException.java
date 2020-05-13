/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

/**
 * Thrown when we cannot determine anything more specific than "something went wrong while
 * processing an SQL script": for example, a {@link io.r2dbc.spi.R2dbcException} from
 * R2DBC that we cannot pinpoint more precisely.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class UncategorizedScriptException extends ScriptException {

	/**
	 * Create a new {@code UncategorizedScriptException}.
	 * @param message detailed message
	 */
	public UncategorizedScriptException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code UncategorizedScriptException}.
	 * @param message detailed message
	 * @param cause the root cause
	 */
	public UncategorizedScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
