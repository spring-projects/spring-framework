/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestsmultipart;

import java.nio.charset.StandardCharsets;

import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipartControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new MultipartController());

	void multiPart() {
		// tag::snippet[]
		assertThat(mockMvc.post().uri("/upload").multipart()
				.file("file1.txt", "Hello".getBytes(StandardCharsets.UTF_8))
				.file("file2.txt", "World".getBytes(StandardCharsets.UTF_8)))
			. // ...
			// end::snippet[]
			hasStatusOk();
	}

	static class MultipartController {}
}
