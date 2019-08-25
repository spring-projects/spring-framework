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

package org.springframework.messaging.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link NativeMessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 */
public class NativeMessageHeaderAccessorTests {

	@Test
	public void createFromNativeHeaderMap() {
		MultiValueMap<String, String> inputNativeHeaders = new LinkedMultiValueMap<>();
		inputNativeHeaders.add("foo", "bar");
		inputNativeHeaders.add("bar", "baz");

		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor(inputNativeHeaders);
		Map<String, Object> actual = headerAccessor.toMap();

		assertThat(actual.size()).as(actual.toString()).isEqualTo(1);
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNotNull();
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isEqualTo(inputNativeHeaders);
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNotSameAs(inputNativeHeaders);
	}

	@Test
	public void createFromMessage() {
		MultiValueMap<String, String> inputNativeHeaders = new LinkedMultiValueMap<>();
		inputNativeHeaders.add("foo", "bar");
		inputNativeHeaders.add("bar", "baz");

		Map<String, Object> inputHeaders = new HashMap<>();
		inputHeaders.put("a", "b");
		inputHeaders.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, inputNativeHeaders);

		GenericMessage<String> message = new GenericMessage<>("p", inputHeaders);
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor(message);
		Map<String, Object> actual = headerAccessor.toMap();

		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get("a")).isEqualTo("b");
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNotNull();
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isEqualTo(inputNativeHeaders);
		assertThat(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNotSameAs(inputNativeHeaders);
	}

	@Test
	public void createFromMessageNull() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor((Message<?>) null);

		Map<String, Object> actual = headerAccessor.toMap();
		assertThat(actual.size()).isEqualTo(0);

		Map<String, List<String>> actualNativeHeaders = headerAccessor.toNativeHeaderMap();
		assertThat(actualNativeHeaders).isEqualTo(Collections.emptyMap());
	}

	@Test
	public void createFromMessageAndModify() {

		MultiValueMap<String, String> inputNativeHeaders = new LinkedMultiValueMap<>();
		inputNativeHeaders.add("foo", "bar");
		inputNativeHeaders.add("bar", "baz");

		Map<String, Object> nativeHeaders = new HashMap<>();
		nativeHeaders.put("a", "b");
		nativeHeaders.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, inputNativeHeaders);

		GenericMessage<String> message = new GenericMessage<>("p", nativeHeaders);

		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor(message);
		headerAccessor.setHeader("a", "B");
		headerAccessor.setNativeHeader("foo", "BAR");

		Map<String, Object> actual = headerAccessor.toMap();

		assertThat(actual.size()).isEqualTo(2);
		assertThat(actual.get("a")).isEqualTo("B");

		@SuppressWarnings("unchecked")
		Map<String, List<String>> actualNativeHeaders =
				(Map<String, List<String>>) actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		assertThat(actualNativeHeaders).isNotNull();
		assertThat(actualNativeHeaders.get("foo")).isEqualTo(Arrays.asList("BAR"));
		assertThat(actualNativeHeaders.get("bar")).isEqualTo(Arrays.asList("baz"));
	}

	@Test
	public void setNativeHeader() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.setNativeHeader("foo", "baz");

		assertThat(headers.getNativeHeader("foo")).isEqualTo(Arrays.asList("baz"));
	}

	@Test
	public void setNativeHeaderNullValue() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.setNativeHeader("foo", null);

		assertThat(headers.getNativeHeader("foo")).isNull();
	}

	@Test
	public void setNativeHeaderLazyInit() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", "baz");

		assertThat(headerAccessor.getNativeHeader("foo")).isEqualTo(Arrays.asList("baz"));
	}

	@Test
	public void setNativeHeaderLazyInitNullValue() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", null);

		assertThat(headerAccessor.getNativeHeader("foo")).isNull();
		assertThat(headerAccessor.getMessageHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNull();
	}

	@Test
	public void setNativeHeaderImmutable() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", "bar");
		headerAccessor.setImmutable();

		assertThatIllegalStateException().isThrownBy(() ->
				headerAccessor.setNativeHeader("foo", "baz"))
			.withMessageContaining("Already immutable");
	}

	@Test
	public void addNativeHeader() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.addNativeHeader("foo", "baz");

		assertThat(headers.getNativeHeader("foo")).isEqualTo(Arrays.asList("bar", "baz"));
	}

	@Test
	public void addNativeHeaderNullValue() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.addNativeHeader("foo", null);

		assertThat(headers.getNativeHeader("foo")).isEqualTo(Arrays.asList("bar"));
	}

	@Test
	public void addNativeHeaderLazyInit() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");

		assertThat(headerAccessor.getNativeHeader("foo")).isEqualTo(Arrays.asList("bar"));
	}

	@Test
	public void addNativeHeaderLazyInitNullValue() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", null);

		assertThat(headerAccessor.getNativeHeader("foo")).isNull();
		assertThat(headerAccessor.getMessageHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS)).isNull();
	}

	@Test
	public void addNativeHeaderImmutable() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");
		headerAccessor.setImmutable();

		assertThatIllegalStateException().isThrownBy(() ->
				headerAccessor.addNativeHeader("foo", "baz"))
			.withMessageContaining("Already immutable");
	}

	@Test
	public void setImmutableIdempotent() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");
		headerAccessor.setImmutable();
		headerAccessor.setImmutable();
	}

}
