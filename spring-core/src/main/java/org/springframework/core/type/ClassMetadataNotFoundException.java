/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.type;

import org.springframework.lang.Nullable;

/**
 * Exception equivalent for to {@link ClassNotFoundException} indicating that class
 * metadata could not be loaded by
 * {@link org.springframework.core.type.classreading.MetadataReader}.
 * @author Danny Thomas
 * @since 6.x
 */
public class ClassMetadataNotFoundException extends RuntimeException {

	static final long serialVersionUID = 4007711655486657517L;

	@Nullable
	private final Throwable ex;

	public ClassMetadataNotFoundException(String s) {
		super(s, null); // Disallow initCause
		this.ex = null;
	}

	public ClassMetadataNotFoundException(String s, Throwable e) {
		super(s, null); // Disallow initCause
		this.ex = e;
	}

	public Throwable getException() {
		return this.ex;
	}

	public Throwable getCause() {
		return this.ex;
	}

}
