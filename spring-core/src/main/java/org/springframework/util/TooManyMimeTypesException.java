/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.util;

import java.util.List;

/**
 * Exception thrown from {@link MimeTypeUtils#sortBySpecificity(List)} in case of
 * the {@code mimeTypes} contains more than 50 elements.
 */
public class TooManyMimeTypesException extends IllegalArgumentException {

	private final List<? extends MimeType> mimeTypes;

	/**
	 * Create a new TooManyMimeTypesException for the given content type.
	 * @param mimeTypes the offending media types
	 */
	public TooManyMimeTypesException(List<? extends MimeType> mimeTypes) {
		super("Too many mimeTypes \"" + mimeTypes + "\"");
		this.mimeTypes = mimeTypes;
	}

	/**
	 * Return the offending content types.
	 */
	public List<? extends MimeType> getMimeTypes() {
		return this.mimeTypes;
	}

}
