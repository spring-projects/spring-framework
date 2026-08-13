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

package org.springframework.docs.integration.restmessageconversion

import com.fasterxml.jackson.annotation.JsonView
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.multipart.FilePart
import org.springframework.http.converter.multipart.FormFieldPart
import org.springframework.http.converter.multipart.Part
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import java.nio.file.Path

class RestClientMessageConversion {

	private val restClient = RestClient.create()

	private val myBean = Any()

	fun useJsonView() {
		// tag::jsonview[]
		val user = User("eric", "7!jd#h23")

		val response: ResponseEntity<Void> = restClient.post()
			.contentType(APPLICATION_JSON)
			.body(user)
			.hint(JsonView::class.java.name, User.WithoutPasswordView::class.java)
			.retrieve()
			.toBodilessEntity()
		// end::jsonview[]
	}

	fun sendUrlEncodedForm() {
		// tag::urlencodedform[]
		val form: MultiValueMap<String, String> = LinkedMultiValueMap()
		form.add("project", "Spring Framework")
		form.add("module", "spring-web")
		val response: ResponseEntity<Void> = this.restClient.post()
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.toBodilessEntity()
		// end::urlencodedform[]
	}

	fun sendMultipartData() {
		// tag::multipartrequest[]
		val parts: MultiValueMap<String, Any> = LinkedMultiValueMap()

		parts.add("fieldPart", "fieldValue")
		parts.add("filePart", FileSystemResource("...logo.png"))
		parts.add("jsonPart", Person("Jason"))

		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_XML
		parts.add("xmlPart", HttpEntity(myBean, headers))

		val response: ResponseEntity<Void> = this.restClient.post()
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(parts)
			.retrieve()
			.toBodilessEntity()
		// end::multipartrequest[]
	}

	fun receiveMultipartData() {
		// tag::multipartresponse[]
		val result = this.restClient.get()
			.uri("https://example.com/upload")
			.accept(MediaType.MULTIPART_FORM_DATA)
			.retrieve()
			.body(object : ParameterizedTypeReference<MultiValueMap<String, Part>>() {})

		val field = result?.getFirst("fieldPart")
		if (field is FormFieldPart) {
			val fieldValue = field.value()
		}
		val file = result?.getFirst("filePart")
		if (file is FilePart) {
			file.transferTo(Path.of("/tmp/" + file.filename()))
		}
		// end::multipartresponse[]
	}

	class User(
		@JsonView(WithoutPasswordView::class) val username: String,
		@JsonView(WithPasswordView::class) val password: String) {

		interface WithoutPasswordView
		interface WithPasswordView : WithoutPasswordView
	}

	data class Person(val name: String)

}
