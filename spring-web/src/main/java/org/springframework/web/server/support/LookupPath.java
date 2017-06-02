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

package org.springframework.web.server.support;

import org.springframework.lang.Nullable;

/**
 * Lookup path information of an incoming HTTP request.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see HttpRequestPathHelper
 */
public final class LookupPath {

	public static final String LOOKUP_PATH_ATTRIBUTE = LookupPath.class.getName();

	private final String path;

	private final int fileExtStartIndex;

	private final int fileExtEndIndex;

	public LookupPath(String path, int fileExtStartIndex, int fileExtEndIndex) {
		this.path = path;
		this.fileExtStartIndex = fileExtStartIndex;
		this.fileExtEndIndex = fileExtEndIndex;
	}

	public String getPath() {
			return this.path;
	}

	public String getPathWithoutExtension() {
		if (this.fileExtStartIndex != -1) {
			return this.path.substring(0, this.fileExtStartIndex);
		}
		else {
			return this.path;
		}
	}

	@Nullable
	public String getFileExtension() {
		if (this.fileExtStartIndex == -1) {
			return null;
		}
		else if (this.fileExtEndIndex == -1) {
			return this.path.substring(this.fileExtStartIndex);
		}
		else {
			return this.path.substring(this.fileExtStartIndex, this.fileExtEndIndex);
		}
	}

}
