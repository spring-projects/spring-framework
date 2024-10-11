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

package org.springframework.web.cors;

/**
 * Used to enumerate the CORS request result.
 *
 * @author Igor Durbek
 * @since 6.2
 */
public enum IsCorsRequestResult {

	/**
	 * Is CORS request.
	 */
	IS_CORS_REQUEST,

	/**
	 * Is not a CORS request.
	 */
	IS_NOT_CORS_REQUEST,

	/**
	 * Invalid origin - reject the request.
	 * See test for an example of a malformed origin request.
	 */
	MALFORMED_ORIGIN
}
