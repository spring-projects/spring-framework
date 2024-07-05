/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

/**
 * Various ways to encode collections in URL parameters.
 *
 * <p>
 * These specific cases are inspired by the <a href="http://swagger.io/specification/">OpenAPI
 * specification</a>.
 * </p>
 *
 * @author Seokjae Lee
 * @since 6.1
 */
public enum CollectionFormat {

	/**
	 * Comma separated values, eg foo=bar,baz
	 */
	CSV(","),

	/**
	 * Space separated values, eg foo=bar baz
	 */
	SSV(" "),

	/**
	 * Tab separated values, eg foo=bar[tab]baz
	 */
	TSV("\t"),

	/**
	 * Values separated with the pipe (|) character, eg foo=bar|baz
	 */
	PIPES("|"),
	;

	public String separator;

	CollectionFormat(String separator) {
		this.separator = separator;
	}
}
