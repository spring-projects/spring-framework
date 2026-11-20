/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.multipart;

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.Part;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipartTests {

	RestTestClient client;

	@Test
	void multipart() {
		// tag::multipart[]
		client.get().uri("/upload")
				.accept(MediaType.MULTIPART_FORM_DATA)
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {})
				.value(result -> {
					Part field = result.getFirst("fieldPart");
					assertThat(field).isInstanceOfSatisfying(FormFieldPart.class,
							formField -> assertThat(formField.value()).isEqualTo("fieldValue"));
					Part file = result.getFirst("filePart");
					assertThat(file).isInstanceOfSatisfying(FilePart.class,
							filePart -> assertThat(filePart.filename()).isEqualTo("logo.png"));
				});
		// end::multipart[]
	}

}
