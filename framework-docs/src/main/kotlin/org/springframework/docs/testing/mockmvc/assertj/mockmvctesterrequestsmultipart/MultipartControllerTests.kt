/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestsmultipart

import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.nio.charset.StandardCharsets

/**
 *
 * @author Stephane Nicoll
 */
class MultipartControllerTests {

	private val mockMvc = MockMvcTester.of(MultipartController())

	fun multiPart() {
		// tag::snippet[]
		assertThat(mockMvc.post().uri("/upload").multipart()
				.file("file1.txt", "Hello".toByteArray(StandardCharsets.UTF_8))
				.file("file2.txt", "World".toByteArray(StandardCharsets.UTF_8)))
			. // ...
			// end::snippet[]
			hasStatusOk()
	}

	class MultipartController
}