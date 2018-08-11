/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * A strategy interface for formatting message codes.
 *
 * @author Chris Beams
 * @since 3.2
 * @see DefaultMessageCodesResolver
 * @see DefaultMessageCodesResolver.Format
 */
public interface MessageCodeFormatter {

	/**
	 * Build and return a message code consisting of the given fields,
	 * usually delimited by {@link DefaultMessageCodesResolver#CODE_SEPARATOR}.
	 * @param errorCode e.g.: "typeMismatch"
	 * @param objectName e.g.: "user"
	 * @param field e.g. "age"
	 * @return concatenated message code, e.g.: "typeMismatch.user.age"
	 * @see DefaultMessageCodesResolver.Format
	 */
	String format(String errorCode, @Nullable String objectName, @Nullable String field);

}
