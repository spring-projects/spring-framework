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
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see HttpRequestPathHelper
 */
public final class LookupPath {

	/**
	 * Name of request attribute under which the LookupPath is stored via
	 * {@link #getOrCreate} and accessed via {@link #getCurrent}.
	 */
	public static final String LOOKUP_PATH_ATTRIBUTE_NAME = LookupPath.class.getName();


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


	/**
	 * Get the LookupPath for the current request from the request attribute
	 * {@link #LOOKUP_PATH_ATTRIBUTE_NAME} or otherwise create and stored it
	 * under that attribute for subsequent use.
	 * @param exchange the current exchange
	 * @param pathHelper the pathHelper to create the LookupPath with
	 * @return the LookupPath for the current request
	 */
	public static LookupPath getOrCreate(ServerWebExchange exchange, HttpRequestPathHelper pathHelper) {
		return exchange.<LookupPath>getAttribute(LookupPath.LOOKUP_PATH_ATTRIBUTE_NAME)
				.orElseGet(() -> {
					LookupPath lookupPath = pathHelper.getLookupPathForRequest(exchange);
					exchange.getAttributes().put(LookupPath.LOOKUP_PATH_ATTRIBUTE_NAME, lookupPath);
					return lookupPath;
				});
	}

	/**
	 * Get the LookupPath for the current request from the request attribute
	 * {@link #LOOKUP_PATH_ATTRIBUTE_NAME} or raise an {@link IllegalStateException}
	 * if not found.
	 * @param exchange the current exchange
	 * @return the LookupPath, never {@code null}
	 */
	public static LookupPath getCurrent(ServerWebExchange exchange) {
		return exchange.<LookupPath>getAttribute(LookupPath.LOOKUP_PATH_ATTRIBUTE_NAME)
				.orElseThrow(() -> new IllegalStateException("No LookupPath attribute."));
	}

}
