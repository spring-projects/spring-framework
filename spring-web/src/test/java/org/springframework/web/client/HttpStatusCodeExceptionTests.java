/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

import org.junit.Test;

import org.springframework.http.HttpStatus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link HttpStatusCodeException} and subclasses.
 *
 * @author Chris Beams
 */
public class HttpStatusCodeExceptionTests {

	/**
	 * Corners bug SPR-9273, which reported the fact that following the changes made in
	 * SPR-7591, {@link HttpStatusCodeException} and subtypes became no longer
	 * serializable due to the addition of a non-serializable {@link Charset} field.
	 */
	@Test
	public void testSerializability() throws IOException, ClassNotFoundException {
		HttpStatusCodeException ex1 = new HttpClientErrorException(
				HttpStatus.BAD_REQUEST, null, null, Charset.forName("US-ASCII"));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ObjectOutputStream(out).writeObject(ex1);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		HttpStatusCodeException ex2 =
				(HttpStatusCodeException) new ObjectInputStream(in).readObject();
		assertThat(ex2.getResponseBodyAsString(), equalTo(ex1.getResponseBodyAsString()));
	}

}
