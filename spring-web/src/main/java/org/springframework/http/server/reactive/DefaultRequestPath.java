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
package org.springframework.http.server.reactive;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RequestPath}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRequestPath implements RequestPath {

	private final PathSegmentContainer fullPath;

	private final PathSegmentContainer contextPath;

	private final PathSegmentContainer pathWithinApplication;


	DefaultRequestPath(URI uri, String contextPath, Charset charset) {
		this.fullPath = PathSegmentContainer.parse(uri.getRawPath(), charset);
		this.contextPath = initContextPath(this.fullPath, contextPath);
		this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
	}

	DefaultRequestPath(RequestPath requestPath, String contextPath) {
		this.fullPath = requestPath;
		this.contextPath = initContextPath(this.fullPath, contextPath);
		this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
	}

	private static PathSegmentContainer initContextPath(PathSegmentContainer path, String contextPath) {
		if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
			return DefaultPathSegmentContainer.EMPTY_PATH;
		}

		Assert.isTrue(contextPath.startsWith("/") && !contextPath.endsWith("/") &&
				path.value().startsWith(contextPath), "Invalid contextPath: " + contextPath);

		int length = contextPath.length();
		int counter = 0;

		for (int i=0; i < path.pathSegments().size(); i++) {
			PathSegment pathSegment = path.pathSegments().get(i);
			counter += 1; // for slash separators
			counter += pathSegment.value().length();
			counter += pathSegment.semicolonContent().length();
			if (length == counter) {
				return DefaultPathSegmentContainer.subPath(path, 0, i + 1);
			}
		}

		// Should not happen..
		throw new IllegalStateException("Failed to initialize contextPath='" + contextPath + "'" +
				" given path='" + path.value() + "'");
	}

	private static PathSegmentContainer extractPathWithinApplication(PathSegmentContainer fullPath,
			PathSegmentContainer contextPath) {

		return PathSegmentContainer.subPath(fullPath, contextPath.pathSegments().size());
	}


	// PathSegmentContainer methods..


	@Override
	public boolean isEmpty() {
		return this.contextPath.isEmpty() && this.pathWithinApplication.isEmpty();
	}

	@Override
	public String value() {
		return this.fullPath.value();
	}

	@Override
	public boolean isAbsolute() {
		return !this.contextPath.isEmpty() && this.contextPath.isAbsolute() ||
				this.pathWithinApplication.isAbsolute();
	}

	@Override
	public List<PathSegment> pathSegments() {
		return this.fullPath.pathSegments();
	}

	@Override
	public boolean hasTrailingSlash() {
		return this.pathWithinApplication.hasTrailingSlash();
	}


	// RequestPath methods..

	@Override
	public PathSegmentContainer contextPath() {
		return this.contextPath;
	}

	@Override
	public PathSegmentContainer pathWithinApplication() {
		return this.pathWithinApplication;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		DefaultRequestPath that = (DefaultRequestPath) other;
		return (this.fullPath.equals(that.fullPath) &&
				this.contextPath.equals(that.contextPath) &&
				this.pathWithinApplication.equals(that.pathWithinApplication));
	}

	@Override
	public int hashCode() {
		int result = this.fullPath.hashCode();
		result = 31 * result + this.contextPath.hashCode();
		result = 31 * result + this.pathWithinApplication.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "DefaultRequestPath[fullPath='" + this.fullPath + "', " +
				"contextPath='" + this.contextPath.value() + "', " +
				"pathWithinApplication='" + this.pathWithinApplication.value() + "']";
	}

}
