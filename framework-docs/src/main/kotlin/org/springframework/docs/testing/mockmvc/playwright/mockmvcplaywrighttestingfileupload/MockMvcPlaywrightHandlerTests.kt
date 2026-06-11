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

package org.springframework.docs.testing.mockmvc.playwright.mockmvcplaywrighttestingfileupload

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.FilePayload
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.charset.StandardCharsets

@SpringJUnitWebConfig
class MockMvcPlaywrightHandlerTests {

	private lateinit var page: Page

	// tag::test[]
	@Test
	@Throws(IOException::class)
	fun testFileUpload() {
		val file = FilePayload("playwright-single.txt", MediaType.TEXT_PLAIN_VALUE, "single-content".toByteArray(StandardCharsets.UTF_8))
		page.locator("#singleFile").setInputFiles(file)
		page.locator("#singleFileForm button[type='submit']").click()

		assertThat(page.locator("body")).hasText("file=playwright-single.txt,content=single-content")
	}
	// end::test[]

	// tag::controller[]
	@PostMapping(path = ["/upload-single"], produces = [MediaType.TEXT_PLAIN_VALUE])
	@Throws(IOException::class)
	fun uploadSingle(@RequestParam("file") file: MultipartFile): String {
		return "file=${file.originalFilename},content=${StreamUtils.copyToString(file.inputStream,
			StandardCharsets.UTF_8)}"
	}
	// end::controller[]
}
