/*
 * Copyright 2025-present the original author or authors.
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
