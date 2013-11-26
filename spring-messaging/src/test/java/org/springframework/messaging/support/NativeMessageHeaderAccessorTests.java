/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link NativeMessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 */
public class NativeMessageHeaderAccessorTests {


	@Test
	public void originalNativeHeaders() {
		MultiValueMap<String, String> original = new LinkedMultiValueMap<>();
		original.add("foo", "bar");
		original.add("bar", "baz");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(original);
		Map<String, Object> actual = headers.toMap();

		assertEquals(1, actual.size());
		assertNotNull(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertEquals(original, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
	}

	@Test
	public void wrapMessage() {

		MultiValueMap<String, String> originalNativeHeaders = new LinkedMultiValueMap<>();
		originalNativeHeaders.add("foo", "bar");
		originalNativeHeaders.add("bar", "baz");

		Map<String, Object> original = new HashMap<String, Object>();
		original.put("a", "b");
		original.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, originalNativeHeaders);

		GenericMessage<String> message = new GenericMessage<>("p", original);

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(message);
		Map<String, Object> actual = headers.toMap();

		assertEquals(4, actual.size());
		assertNotNull(actual.get(MessageHeaders.ID));
		assertNotNull(actual.get(MessageHeaders.TIMESTAMP));
		assertEquals("b", actual.get("a"));
		assertNotNull(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertEquals(originalNativeHeaders, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
	}

	@Test
	public void wrapNullMessage() {
		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor((Message<?>) null);
		Map<String, Object> actual = headers.toMap();

		assertEquals(1, actual.size());

		@SuppressWarnings("unchecked")
		Map<String, List<String>> actualNativeHeaders =
				(Map<String, List<String>>) actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		assertEquals(Collections.emptyMap(), actualNativeHeaders);
	}

	@Test
	public void wrapMessageAndModifyHeaders() {

		MultiValueMap<String, String> originalNativeHeaders = new LinkedMultiValueMap<>();
		originalNativeHeaders.add("foo", "bar");
		originalNativeHeaders.add("bar", "baz");

		Map<String, Object> original = new HashMap<String, Object>();
		original.put("a", "b");
		original.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, originalNativeHeaders);

		GenericMessage<String> message = new GenericMessage<>("p", original);

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(message);
		headers.setHeader("a", "B");
		headers.setNativeHeader("foo", "BAR");

		Map<String, Object> actual = headers.toMap();

		assertEquals(4, actual.size());
		assertNotNull(actual.get(MessageHeaders.ID));
		assertNotNull(actual.get(MessageHeaders.TIMESTAMP));
		assertEquals("B", actual.get("a"));

		@SuppressWarnings("unchecked")
		Map<String, List<String>> actualNativeHeaders =
				(Map<String, List<String>>) actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		assertNotNull(actualNativeHeaders);
		assertEquals(Arrays.asList("BAR"), actualNativeHeaders.get("foo"));
		assertEquals(Arrays.asList("baz"), actualNativeHeaders.get("bar"));
	}

}
