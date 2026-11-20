/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpStatusCodeException}.
 *
 * @author Chris Beams
 */
class HttpStatusCodeExceptionTests {

	/**
	 * Corners bug SPR-9273, which reported the fact that following the changes made in
	 * SPR-7591, {@link HttpStatusCodeException} and subtypes became no longer
	 * serializable due to the addition of a non-serializable {@code Charset} field.
	 */
	@Test
	void serializability() throws IOException, ClassNotFoundException {
		HttpStatusCodeException ex1 = new HttpClientErrorException(
				HttpStatus.BAD_REQUEST, null, null, StandardCharsets.US_ASCII);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ObjectOutputStream(out).writeObject(ex1);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		HttpStatusCodeException ex2 =
				(HttpStatusCodeException) new ObjectInputStream(in).readObject();
		assertThat(ex2.getResponseBodyAsString()).isEqualTo(ex1.getResponseBodyAsString());
	}

	@Test
	void emptyStatusText() {
		HttpStatusCodeException ex = new HttpClientErrorException(HttpStatus.NOT_FOUND, "");

		assertThat(ex.getMessage()).isEqualTo("404 Not Found");
	}

}
