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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link PathContainer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultPathContainer implements PathContainer {

	private static final MultiValueMap<String, String> EMPTY_MAP = new LinkedMultiValueMap<>(0);

	private static final PathContainer EMPTY_PATH = new DefaultPathContainer("", Collections.emptyList());

	private static final PathContainer.Separator SEPARATOR = () -> "/";


	private final String path;

	private final List<Element> elements;


	private DefaultPathContainer(String path, List<Element> elements) {
		this.path = path;
		this.elements = Collections.unmodifiableList(elements);
	}


	@Override
	public String value() {
		return this.path;
	}

	@Override
	public List<Element> elements() {
		return this.elements;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return this.path.equals(((DefaultPathContainer) other).path);
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return "[path='" + this.path + "\']";
	}


	static PathContainer parsePath(String path, Charset charset) {
		if (path.equals("")) {
			return EMPTY_PATH;
		}
		List<Element> elements = new ArrayList<>();
		int begin;
		if (path.length() > 0 && path.charAt(0) == '/') {
			begin = 1;
			elements.add(SEPARATOR);
		}
		else {
			begin = 0;
		}
		while (begin < path.length()) {
			int end = path.indexOf('/', begin);
			String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));
			if (!segment.equals("")) {
				elements.add(parsePathSegment(segment, charset));
			}
			if (end == -1) {
				break;
			}
			elements.add(SEPARATOR);
			begin = end + 1;
		}
		return new DefaultPathContainer(path, elements);
	}

	private static PathContainer.Segment parsePathSegment(String input, Charset charset) {
		int index = input.indexOf(';');
		if (index == -1) {
			String inputDecoded = StringUtils.uriDecode(input, charset);
			return new DefaultPathSegment(input, inputDecoded, "", EMPTY_MAP);
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

	static PathContainer subPath(PathContainer container, int fromIndex, int toIndex) {
		List<Element> elements = container.elements();
		if (fromIndex == 0 && toIndex == elements.size()) {
			return container;
		}
		if (fromIndex == toIndex) {
			return EMPTY_PATH;
		}

		Assert.isTrue(fromIndex < toIndex, "fromIndex: " + fromIndex + " should be < toIndex " + toIndex);
		Assert.isTrue(fromIndex >= 0 && fromIndex < elements.size(), "Invalid fromIndex: " + fromIndex);
		Assert.isTrue(toIndex >= 0 && toIndex <= elements.size(), "Invalid toIndex: " + toIndex);

		List<Element> subList = elements.subList(fromIndex, toIndex);
		String path = subList.stream().map(Element::value).collect(Collectors.joining(""));
		return new DefaultPathContainer(path, subList);
	}


	private static class DefaultPathSegment implements PathContainer.Segment {

		private final String value;

		private final String valueDecoded;

		private final char[] valueDecodedChars;

		private final String semicolonContent;

		private final MultiValueMap<String, String> parameters;

		DefaultPathSegment(String value, String valueDecoded, String semicolonContent,
				MultiValueMap<String, String> params) {

			Assert.isTrue(!value.contains("/"), "Invalid path segment value: " + value);
			this.value = value;
			this.valueDecoded = valueDecoded;
			this.valueDecodedChars = valueDecoded.toCharArray();
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
		public char[] valueDecodedChars() {
			return this.valueDecodedChars;
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
		public boolean equals(@Nullable Object other) {
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

