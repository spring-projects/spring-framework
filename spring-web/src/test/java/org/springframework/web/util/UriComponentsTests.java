/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/** @author Arjen Poutsma */
public class UriComponentsTests {

	@Test
	public void encode() {
		UriComponents uriComponents = UriComponentsBuilder.fromPath("/hotel list").build();
		UriComponents encoded = uriComponents.encode();
		assertEquals("/hotel%20list", encoded.getPath());
	}

	@Test
	public void toUriEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/hotel list/Z\u00fcrich").build();
		UriComponents encoded = uriComponents.encode();
		assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), encoded.toUri());
	}

	@Test
	public void toUriNotEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/hotel list/Z\u00fcrich").build();
		assertEquals(new URI("http://example.com/hotel%20list/Z\u00fcrich"), uriComponents.toUri());
	}

	@Test
	public void expand() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com").path("/{foo} {bar}").build();
		uriComponents = uriComponents.expand("1 2", "3 4");
		assertEquals("/1 2 3 4", uriComponents.getPath());
		assertEquals("http://example.com/1 2 3 4", uriComponents.toUriString());
	}

	@Test(expected = IllegalStateException.class)
	public void expandEncoded() {
		UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidCharacters() {
		UriComponentsBuilder.fromPath("/{foo}").build(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidEncodedSequence() {
		UriComponentsBuilder.fromPath("/fo%2o").build(true);
	}

	@Test
	public void normalize() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo/../bar").build();
		assertEquals("http://example.com/bar", uriComponents.normalize().toString());
	}

	@Test
	public void serializable() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com").path("/{foo}").query("bar={baz}").build();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(uriComponents);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		UriComponents readObject = (UriComponents) ois.readObject();
		assertThat(uriComponents.toString(), equalTo(readObject.toString()));
	}

}
