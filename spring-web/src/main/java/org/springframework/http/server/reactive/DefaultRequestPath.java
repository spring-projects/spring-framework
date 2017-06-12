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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRequestPath implements RequestPath {

	private static final MultiValueMap<String, String> EMPTY_MAP = new LinkedMultiValueMap<>(0);

	private static final PathSegment EMPTY_PATH_SEGMENT = new DefaultPathSegment("", "", "", EMPTY_MAP);

	private static final PathSegmentContainer EMPTY_PATH =
			new DefaultPathSegmentContainer("", Collections.emptyList());

	private static final PathSegmentContainer ROOT_PATH =
			new DefaultPathSegmentContainer("/", Collections.singletonList(EMPTY_PATH_SEGMENT));


	private final PathSegmentContainer fullPath;

	private final PathSegmentContainer contextPath;

	private final PathSegmentContainer pathWithinApplication;


	DefaultRequestPath(URI uri, String contextPath, Charset charset) {
		this.fullPath = parsePath(uri.getRawPath(), charset);
		this.contextPath = initContextPath(this.fullPath, contextPath);
		this.pathWithinApplication = initPathWithinApplication(this.fullPath, this.contextPath);
	}

	DefaultRequestPath(RequestPath requestPath, String contextPath, Charset charset) {
		this.fullPath = new DefaultPathSegmentContainer(requestPath.value(), requestPath.pathSegments());
		this.contextPath = initContextPath(this.fullPath, contextPath);
		this.pathWithinApplication = initPathWithinApplication(this.fullPath, this.contextPath);
	}

	private static PathSegmentContainer parsePath(String path, Charset charset) {
		path = StringUtils.hasText(path) ? path : "";
		if ("".equals(path)) {
			return EMPTY_PATH;
		}
		if ("/".equals(path)) {
			return ROOT_PATH;
		}
		List<PathSegment> result = new ArrayList<>();
		int begin = 1;
		while (true) {
			int end = path.indexOf('/', begin);
			String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));
			result.add(parsePathSegment(segment, charset));
			if (end == -1) {
				break;
			}
			begin = end + 1;
			if (begin == path.length()) {
				// trailing slash
				result.add(EMPTY_PATH_SEGMENT);
				break;
			}
		}
		return new DefaultPathSegmentContainer(path, result);
	}

	private static PathSegment parsePathSegment(String input, Charset charset) {
		if ("".equals(input)) {
			return EMPTY_PATH_SEGMENT;
		}
		int index = input.indexOf(';');
		if (index == -1) {
			return new DefaultPathSegment(input, StringUtils.uriDecode(input, charset), "", EMPTY_MAP);
		}
		String value = input.substring(0, index);
		String valueDecoded = StringUtils.uriDecode(value, charset);
		String semicolonContent = input.substring(index);
		MultiValueMap<String, String> parameters = parseParams(semicolonContent, charset);
		return new DefaultPathSegment(value, valueDecoded, semicolonContent, parameters);
	}

	private static MultiValueMap<String, String> parseParams(String input, Charset charset) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		int begin = 1;
		while (begin < input.length()) {
			int end = input.indexOf(';', begin);
			String param = (end != -1 ? input.substring(begin, end) : input.substring(begin));
			parseParamValues(param, charset, result);
			if (end == -1) {
				break;
			}
			begin = end + 1;
		}
		return result;
	}

	private static void parseParamValues(String input, Charset charset, MultiValueMap<String, String> output) {
		if (StringUtils.hasText(input)) {
			int index = input.indexOf("=");
			if (index != -1) {
				String name = input.substring(0, index);
				String value = input.substring(index + 1);
				for (String v : StringUtils.commaDelimitedListToStringArray(value)) {
					name = StringUtils.uriDecode(name, charset);
					if (StringUtils.hasText(name)) {
						output.add(name, StringUtils.uriDecode(v, charset));
					}
				}
			}
			else {
				String name = StringUtils.uriDecode(input, charset);
				if (StringUtils.hasText(name)) {
					output.add(input, "");
				}
			}
		}
	}

	private static PathSegmentContainer initContextPath(PathSegmentContainer path, String contextPath) {
		if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
			return EMPTY_PATH;
		}

		Assert.isTrue(contextPath.startsWith("/") && !contextPath.endsWith("/") &&
				path.value().startsWith(contextPath), "Invalid contextPath: " + contextPath);

		int length = contextPath.length();
		int counter = 0;

		List<PathSegment> result = new ArrayList<>();
		for (PathSegment pathSegment : path.pathSegments()) {
			result.add(pathSegment);
			counter += 1; // for '/' separators
			counter += pathSegment.value().length();
			counter += pathSegment.semicolonContent().length();
			if (length == counter) {
				return new DefaultPathSegmentContainer(contextPath, result);
			}
		}

		// Should not happen..
		throw new IllegalStateException("Failed to initialize contextPath='" + contextPath + "'" +
				" given path='" + path.value() + "'");
	}

	private static PathSegmentContainer initPathWithinApplication(PathSegmentContainer path,
			PathSegmentContainer contextPath) {

		String value = path.value().substring(contextPath.value().length());
		List<PathSegment> pathSegments = new ArrayList<>(path.pathSegments());
		pathSegments.removeAll(contextPath.pathSegments());
		return new DefaultPathSegmentContainer(value, pathSegments);
	}


	@Override
	public String value() {
		return this.fullPath.value();
	}

	@Override
	public List<PathSegment> pathSegments() {
		return this.fullPath.pathSegments();
	}

	@Override
	public PathSegmentContainer contextPath() {
		return this.contextPath;
	}

	@Override
	public PathSegmentContainer pathWithinApplication() {
		return this.pathWithinApplication;
	}


	private static class DefaultPathSegmentContainer implements PathSegmentContainer {

		private final String path;

		private final List<PathSegment> pathSegments;


		DefaultPathSegmentContainer(String path, List<PathSegment> pathSegments) {
			this.path = path;
			this.pathSegments = Collections.unmodifiableList(pathSegments);
		}


		@Override
		public String value() {
			return this.path;
		}

		@Override
		public List<PathSegment> pathSegments() {
			return this.pathSegments;
		}


		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			return this.path.equals(((DefaultPathSegmentContainer) other).path);
		}

		@Override
		public int hashCode() {
			return this.path.hashCode();
		}

		@Override
		public String toString() {
			return "[path='" + this.path + "\']";
		}
	}


	private static class DefaultPathSegment implements PathSegment {

		private final String value;

		private final String valueDecoded;

		private final String semicolonContent;

		private final MultiValueMap<String, String> parameters;


		DefaultPathSegment(String value, String valueDecoded, String semicolonContent,
				MultiValueMap<String, String> params) {

			this.value = value;
			this.valueDecoded = valueDecoded;
			this.semicolonContent = semicolonContent;
			this.parameters = CollectionUtils.unmodifiableMultiValueMap(params);
		}


		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String valueDecoded() {
			return this.valueDecoded;
		}

		@Override
		public String semicolonContent() {
			return this.semicolonContent;
		}

		@Override
		public MultiValueMap<String, String> parameters() {
			return this.parameters;
		}


		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}

			DefaultPathSegment segment = (DefaultPathSegment) other;
			return (this.value.equals(segment.value) &&
					this.semicolonContent.equals(segment.semicolonContent) &&
					this.parameters.equals(segment.parameters));
		}

		@Override
		public int hashCode() {
			int result = this.value.hashCode();
			result = 31 * result + this.semicolonContent.hashCode();
			result = 31 * result + this.parameters.hashCode();
			return result;
		}

		public String toString() {
			return "[value='" + this.value + "\', " +
					"semicolonContent='" + this.semicolonContent + "\', " +
					"parameters=" + this.parameters + "']";
		}
	}

}
