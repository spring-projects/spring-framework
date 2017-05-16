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
import org.springframework.web.server.ServerWebExchange;

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

	private final int fileExtensionIndex;

	private final int pathParametersIndex;

	public LookupPath(String path, int fileExtensionIndex, int pathParametersIndex) {
		this.path = path;
		this.fileExtensionIndex = fileExtensionIndex;
		this.pathParametersIndex = pathParametersIndex;
	}

	public String getPath() {
		if (this.pathParametersIndex != -1) {
			// TODO: variant without the path parameter information?
			//return this.path.substring(0, this.pathParametersIndex);
			return this.path;
		}
		else {
			return this.path;
		}
	}

	public String getPathWithoutExtension() {
		if (this.fileExtensionIndex != -1) {
			return this.path.substring(0, this.fileExtensionIndex);
		}
		else {
			return this.path;
		}
	}

	@Nullable
	public String getFileExtension() {
		if (this.fileExtensionIndex == -1) {
			return null;
		}
		else if (this.pathParametersIndex == -1) {
			return this.path.substring(this.fileExtensionIndex);
		}
		else {
			return this.path.substring(this.fileExtensionIndex, this.pathParametersIndex);
		}
	}

	@Nullable
	public String getPathParameters() {
		return this.pathParametersIndex == -1 ?
				null : this.path.substring(this.pathParametersIndex + 1);
	}

}
