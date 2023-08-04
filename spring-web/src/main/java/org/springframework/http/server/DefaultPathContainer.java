/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.server;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author Sam Brannen
 * @since 5.0
 */
final class DefaultPathContainer implements PathContainer {

	private static final PathContainer EMPTY_PATH = new DefaultPathContainer("", Collections.emptyList());

	private static final Map<Character, DefaultSeparator> SEPARATORS = new HashMap<>(2);

	static {
		SEPARATORS.put('/', new DefaultSeparator('/', "%2F"));
		SEPARATORS.put('.', new DefaultSeparator('.', "%2E"));
	}


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
		return (this == other) || (other instanceof PathContainer that && value().equals(that.value()));
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return value();
	}


	static PathContainer createFromUrlPath(String path, Options options) {
		if (path.isEmpty()) {
			return EMPTY_PATH;
		}
		char separator = options.separator();
		DefaultSeparator separatorElement = SEPARATORS.get(separator);
		if (separatorElement == null) {
			throw new IllegalArgumentException("Unexpected separator: '" + separator + "'");
		}
		List<Element> elements = new ArrayList<>();
		int begin;
		if (path.charAt(0) == separator) {
			begin = 1;
			elements.add(separatorElement);
		}
		else {
			begin = 0;
		}
		while (begin < path.length()) {
			int end = path.indexOf(separator, begin);
			String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));
			if (!segment.isEmpty()) {
				elements.add(options.shouldDecodeAndParseSegments() ?
						decodeAndParsePathSegment(segment) :
						DefaultPathSegment.from(segment, separatorElement));
			}
			if (end == -1) {
				break;
			}
			elements.add(separatorElement);
			begin = end + 1;
		}
		return new DefaultPathContainer(path, elements);
	}

	private static PathSegment decodeAndParsePathSegment(String segment) {
		Charset charset = StandardCharsets.UTF_8;
		int index = segment.indexOf(';');
		if (index == -1) {
			String valueToMatch = StringUtils.uriDecode(segment, charset);
			return DefaultPathSegment.from(segment, valueToMatch);
		}
		else {
			String valueToMatch = StringUtils.uriDecode(segment.substring(0, index), charset);
			String pathParameterContent = segment.substring(index);
			MultiValueMap<String, String> parameters = parsePathParams(pathParameterContent, charset);
			return DefaultPathSegment.from(segment, valueToMatch, parameters);
		}
	}

	private static MultiValueMap<String, String> parsePathParams(String input, Charset charset) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		int begin = 1;
		while (begin < input.length()) {
			int end = input.indexOf(';', begin);
			String param = (end != -1 ? input.substring(begin, end) : input.substring(begin));
			parsePathParamValues(param, charset, result);
			if (end == -1) {
				break;
			}
			begin = end + 1;
		}
		return result;
	}

	private static void parsePathParamValues(String input, Charset charset, MultiValueMap<String, String> output) {
		if (StringUtils.hasText(input)) {
			int index = input.indexOf('=');
			if (index != -1) {
				String name = input.substring(0, index);
				name = StringUtils.uriDecode(name, charset);
				if (StringUtils.hasText(name)) {
					String value = input.substring(index + 1);
					for (String v : StringUtils.commaDelimitedListToStringArray(value)) {
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

		Assert.isTrue(fromIndex >= 0 && fromIndex < elements.size(), () -> "Invalid fromIndex: " + fromIndex);
		Assert.isTrue(toIndex >= 0 && toIndex <= elements.size(), () -> "Invalid toIndex: " + toIndex);
		Assert.isTrue(fromIndex < toIndex, () -> "fromIndex: " + fromIndex + " should be < toIndex " + toIndex);

		List<Element> subList = elements.subList(fromIndex, toIndex);
		String path = subList.stream().map(Element::value).collect(Collectors.joining(""));
		return new DefaultPathContainer(path, subList);
	}


	private static class DefaultSeparator implements Separator {

		private final String separator;

		private final String encodedSequence;


		DefaultSeparator(char separator, String encodedSequence) {
			this.separator = String.valueOf(separator);
			this.encodedSequence = encodedSequence;
		}


		@Override
		public String value() {
			return this.separator;
		}

		public String encodedSequence() {
			return this.encodedSequence;
		}
	}


	private static final class DefaultPathSegment implements PathSegment {

		private static final MultiValueMap<String, String> EMPTY_PARAMS =
				CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

		private final String value;

		private final String valueToMatch;

		private final MultiValueMap<String, String> parameters;

		/**
		 * Factory for segments without decoding and parsing.
		 */
		static DefaultPathSegment from(String value, DefaultSeparator separator) {
			String valueToMatch = value.contains(separator.encodedSequence()) ?
					value.replaceAll(separator.encodedSequence(), separator.value()) : value;
			return from(value, valueToMatch);
		}

		/**
		 * Factory for decoded and parsed segments.
		 */
		static DefaultPathSegment from(String value, String valueToMatch) {
			return new DefaultPathSegment(value, valueToMatch, EMPTY_PARAMS);
		}

		/**
		 * Factory for decoded and parsed segments.
		 */
		static DefaultPathSegment from(String value, String valueToMatch, MultiValueMap<String, String> params) {
			return new DefaultPathSegment(value, valueToMatch, CollectionUtils.unmodifiableMultiValueMap(params));
		}

		private DefaultPathSegment(String value, String valueToMatch, MultiValueMap<String, String> params) {
			this.value = value;
			this.valueToMatch = valueToMatch;
			this.parameters = params;
		}


		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String valueToMatch() {
			return this.valueToMatch;
		}

		@Override
		public char[] valueToMatchAsChars() {
			return this.valueToMatch.toCharArray();
		}

		@Override
		public MultiValueMap<String, String> parameters() {
			return this.parameters;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof PathSegment that && this.value.equals(that.value())));
		}

		@Override
		public int hashCode() {
			return this.value.hashCode();
		}

		@Override
		public String toString() {
			return "[value='" + this.value + "']";
		}
	}

}

