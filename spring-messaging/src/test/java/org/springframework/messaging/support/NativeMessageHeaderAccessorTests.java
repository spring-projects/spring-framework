/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.Message;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link NativeMessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 */
public class NativeMessageHeaderAccessorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void createFromNativeHeaderMap() {
		MultiValueMap<String, String> inputNativeHeaders = new LinkedMultiValueMap<>();
		inputNativeHeaders.add("foo", "bar");
		inputNativeHeaders.add("bar", "baz");

		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor(inputNativeHeaders);
		Map<String, Object> actual = headerAccessor.toMap();

		assertEquals(actual.toString(), 1, actual.size());
		assertNotNull(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertEquals(inputNativeHeaders, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertNotSame(inputNativeHeaders, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
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

		assertEquals(2, actual.size());
		assertEquals("b", actual.get("a"));
		assertNotNull(actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertEquals(inputNativeHeaders, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
		assertNotSame(inputNativeHeaders, actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
	}

	@Test
	public void createFromMessageNull() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor((Message<?>) null);

		Map<String, Object> actual = headerAccessor.toMap();
		assertEquals(0, actual.size());

		Map<String, List<String>> actualNativeHeaders = headerAccessor.toNativeHeaderMap();
		assertEquals(Collections.emptyMap(), actualNativeHeaders);
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

		assertEquals(2, actual.size());
		assertEquals("B", actual.get("a"));

		@SuppressWarnings("unchecked")
		Map<String, List<String>> actualNativeHeaders =
				(Map<String, List<String>>) actual.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		assertNotNull(actualNativeHeaders);
		assertEquals(Arrays.asList("BAR"), actualNativeHeaders.get("foo"));
		assertEquals(Arrays.asList("baz"), actualNativeHeaders.get("bar"));
	}

	@Test
	public void setNativeHeader() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.setNativeHeader("foo", "baz");

		assertEquals(Arrays.asList("baz"), headers.getNativeHeader("foo"));
	}

	@Test
	public void setNativeHeaderNullValue() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.setNativeHeader("foo", null);

		assertNull(headers.getNativeHeader("foo"));
	}

	@Test
	public void setNativeHeaderLazyInit() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", "baz");

		assertEquals(Arrays.asList("baz"), headerAccessor.getNativeHeader("foo"));
	}

	@Test
	public void setNativeHeaderLazyInitNullValue() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", null);

		assertNull(headerAccessor.getNativeHeader("foo"));
		assertNull(headerAccessor.getMessageHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
	}

	@Test
	public void setNativeHeaderImmutable() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.setNativeHeader("foo", "bar");
		headerAccessor.setImmutable();

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Already immutable");
		headerAccessor.setNativeHeader("foo", "baz");
	}

	@Test
	public void addNativeHeader() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.addNativeHeader("foo", "baz");

		assertEquals(Arrays.asList("bar", "baz"), headers.getNativeHeader("foo"));
	}

	@Test
	public void addNativeHeaderNullValue() {
		MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<>();
		nativeHeaders.add("foo", "bar");

		NativeMessageHeaderAccessor headers = new NativeMessageHeaderAccessor(nativeHeaders);
		headers.addNativeHeader("foo", null);

		assertEquals(Arrays.asList("bar"), headers.getNativeHeader("foo"));
	}

	@Test
	public void addNativeHeaderLazyInit() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");

		assertEquals(Arrays.asList("bar"), headerAccessor.getNativeHeader("foo"));
	}

	@Test
	public void addNativeHeaderLazyInitNullValue() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", null);

		assertNull(headerAccessor.getNativeHeader("foo"));
		assertNull(headerAccessor.getMessageHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
	}

	@Test
	public void addNativeHeaderImmutable() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");
		headerAccessor.setImmutable();

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Already immutable");
		headerAccessor.addNativeHeader("foo", "baz");
	}

	@Test
	public void setImmutableIdempotent() {
		NativeMessageHeaderAccessor headerAccessor = new NativeMessageHeaderAccessor();
		headerAccessor.addNativeHeader("foo", "bar");
		headerAccessor.setImmutable();
		headerAccessor.setImmutable();
	}

}