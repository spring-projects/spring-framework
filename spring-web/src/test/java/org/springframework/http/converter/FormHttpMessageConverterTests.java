/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.MULTIPART_MIXED;
import static org.springframework.http.MediaType.MULTIPART_RELATED;

/**
 * Tests for {@link FormHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
class FormHttpMessageConverterTests {

	private static final ResolvableType LINKED_MULTI_VALUE_MAP =
			ResolvableType.forClassWithGenerics(LinkedMultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTI_VALUE_MAP =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MAP =
			ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);

	private final FormHttpMessageConverter converter = new FormHttpMessageConverter();

	@Test
	void cannotReadToMapsWhenMediaTypeMissing() {
		assertCannotRead(MAP, null);
		assertCannotRead(MULTI_VALUE_MAP, null);
		assertCannotRead(LINKED_MULTI_VALUE_MAP, null);
		// without generics
		assertCannotRead(ResolvableType.forClass(Map.class), null);
		assertCannotRead(ResolvableType.forClass(MultiValueMap.class), null);
		assertCannotRead(ResolvableType.forClass(LinkedMultiValueMap.class), null);
	}

	@Test
	void canReadToMapTypes() {
		assertCanRead(MAP, APPLICATION_FORM_URLENCODED);
		assertCanRead(MULTI_VALUE_MAP, APPLICATION_FORM_URLENCODED);
		assertCanRead(LINKED_MULTI_VALUE_MAP, APPLICATION_FORM_URLENCODED);
		// without generics
		assertCanRead(ResolvableType.forClass(Map.class), APPLICATION_FORM_URLENCODED);
		assertCanRead(ResolvableType.forClass(MultiValueMap.class), APPLICATION_FORM_URLENCODED);
		assertCanRead(ResolvableType.forClass(LinkedMultiValueMap.class), APPLICATION_FORM_URLENCODED);
	}

	@Test
	void cannotReadMultipart() {
		// Without custom multipart types supported
		assertCannotReadMultipart();

		// Should still be the case with custom multipart types supported
		assertCannotReadMultipart();
	}

	@Test
	void canWrite() {
		assertCanWrite(APPLICATION_FORM_URLENCODED);
		assertCannotWrite(MediaType.ALL);
	}


	@Test
	void canWriteMapTypes() {
		assertCanWrite(MAP, APPLICATION_FORM_URLENCODED);
		assertCanWrite(MULTI_VALUE_MAP, APPLICATION_FORM_URLENCODED);
		assertCanWrite(LINKED_MULTI_VALUE_MAP, APPLICATION_FORM_URLENCODED);
		// without generics
		assertCanWrite(ResolvableType.forClass(Map.class), APPLICATION_FORM_URLENCODED);
		assertCanWrite(ResolvableType.forClass(MultiValueMap.class), APPLICATION_FORM_URLENCODED);
		assertCanWrite(ResolvableType.forClass(LinkedMultiValueMap.class), APPLICATION_FORM_URLENCODED);
	}

	@Test
	void cannotWriteMultipart() {
		assertCannotWrite(MULTIPART_FORM_DATA);
		assertCannotWrite(MULTIPART_MIXED);
		assertCannotWrite(MULTIPART_RELATED);
		assertCannotWrite(new MediaType("multipart", "form-data", UTF_8));
		assertCannotWrite(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	void readFormAsMultiValueMap() throws Exception {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.ISO_8859_1));
		inputMessage.getHeaders().setContentType(
				new MediaType("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1));
		Object result = this.converter.read(ResolvableType.forClass(MultiValueMap.class), inputMessage, null);

		assertThat(result).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, String> form = (MultiValueMap<String, String>) result;
		assertThat(form).as("Invalid result").hasSize(3);
		assertThat(form.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
		List<String> values = form.get("name 2");
		assertThat(values).as("Invalid result").containsExactly("value 2+1", "value 2+2");
		assertThat(form.getFirst("name 3")).as("Invalid result").isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void readFormAsMap() throws Exception {
		String body = "name+1=value+1&name+2=value+2&name+3";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.ISO_8859_1));
		inputMessage.getHeaders().setContentType(
				new MediaType("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1));
		Object result = this.converter.read(ResolvableType.forClass(Map.class), inputMessage, null);

		assertThat(result).isInstanceOf(Map.class);
		Map<String, String> form = (Map<String, String>) result;
		assertThat(form).as("Invalid result").hasSize(3);
		assertThat(form.get("name 1")).as("Invalid result").isEqualTo("value 1");
		assertThat(form.get("name 2")).as("Invalid result").isEqualTo("value 2");
		assertThat(form.get("name 3")).as("Invalid result").isNull();
	}

	@Test
	void readInvalidFormWithValueThatWontUrlDecode() {
		//java.net.URLDecoder doesn't like negative integer values after a % character
		String body = "name+1=value+1&name+2=value+2%" + ((char)-1);
		assertInvalidFormIsRejectedWithSpecificException(body);
	}

	@Test
	void readInvalidFormWithNameThatWontUrlDecode() {
		//java.net.URLDecoder doesn't like negative integer values after a % character
		String body = "name+1=value+1&name+2%" + ((char)-1) + "=value+2";
		assertInvalidFormIsRejectedWithSpecificException(body);
	}

	@Test
	void readInvalidFormWithNameWithNoValueThatWontUrlDecode() {
		//java.net.URLDecoder doesn't like negative integer values after a % character
		String body = "name+1=value+1&name+2%" + ((char)-1);
		assertInvalidFormIsRejectedWithSpecificException(body);
	}

	@Test
	void writeFormFromMultiValueMap() throws IOException {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(body, APPLICATION_FORM_URLENCODED, outputMessage);

		assertThat(outputMessage.getBodyAsString(UTF_8))
				.as("Invalid result").isEqualTo("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3");
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(APPLICATION_FORM_URLENCODED);
		assertThat(outputMessage.getHeaders().getContentLength())
				.as("Invalid content-length").isEqualTo(outputMessage.getBodyAsBytes().length);
	}

	@Test
	void writeFormFromMap() throws IOException {
		Map<String, String> body = new HashMap<>();
		body.put("name 1", "value 1");
		body.put("name 2", "value 2");
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(body, APPLICATION_FORM_URLENCODED, outputMessage);

		assertThat(outputMessage.getBodyAsString(UTF_8))
				.as("Invalid result").isEqualTo("name+2=value+2&name+1=value+1");
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(APPLICATION_FORM_URLENCODED);
		assertThat(outputMessage.getHeaders().getContentLength())
				.as("Invalid content-length").isEqualTo(outputMessage.getBodyAsBytes().length);
	}

	private void assertCanRead(MediaType mediaType) {
		assertCanRead(MULTI_VALUE_MAP, mediaType);
	}

	private void assertCanRead(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canRead(type, mediaType)).as(type.toClass().getSimpleName() + " : " + mediaType).isTrue();
	}

	private void assertCannotReadMultipart() {
		assertCannotRead(new MediaType("multipart", "*"));
		assertCannotRead(MULTIPART_FORM_DATA);
		assertCannotRead(MULTIPART_MIXED);
		assertCannotRead(MULTIPART_RELATED);
	}

	private void assertCannotRead(MediaType mediaType) {
		assertCannotRead(MULTI_VALUE_MAP, mediaType);
	}

	private void assertCannotRead(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canRead(type, mediaType)).as(type + " : " + mediaType).isFalse();
	}

	private void assertCanWrite(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canWrite(type, LinkedMultiValueMap.class, mediaType))
				.as(type + " : " + mediaType).isTrue();
	}

	private void assertCanWrite(MediaType mediaType) {
		assertCanWrite(MULTI_VALUE_MAP, mediaType);
	}

	private void assertCannotWrite(MediaType mediaType) {
		assertThat(this.converter.canWrite(MULTI_VALUE_MAP, MultiValueMap.class, mediaType))
				.as(MultiValueMap.class.getSimpleName() + " : " + mediaType).isFalse();
	}

	private void assertInvalidFormIsRejectedWithSpecificException(String body) {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.ISO_8859_1));
		inputMessage.getHeaders().setContentType(
				new MediaType("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1));

		assertThatThrownBy(() -> this.converter.read(MULTI_VALUE_MAP, inputMessage, null))
				.isInstanceOf(HttpMessageNotReadableException.class)
				.hasCauseInstanceOf(IllegalArgumentException.class)
				.hasMessage("Could not decode HTTP form payload");
	}

}
