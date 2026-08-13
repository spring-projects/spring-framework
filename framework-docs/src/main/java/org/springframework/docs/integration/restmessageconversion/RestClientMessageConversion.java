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

package org.springframework.docs.integration.restmessageconversion;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.Part;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class RestClientMessageConversion {

	private final RestClient restClient = RestClient.create();

	private final Object myBean = new Object();

	void useJsonView() {
		// tag::jsonview[]
		User user = new User("eric", "7!jd#h23");

		ResponseEntity<Void> response = this.restClient.post()
				.contentType(APPLICATION_JSON)
				.body(user)
				.hint(JsonView.class.getName(), User.WithoutPasswordView.class)
				.retrieve()
				.toBodilessEntity();
		// end::jsonview[]
	}

	void sendUrlEncodedForm() {
		// tag::urlencodedform[]
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("project", "Spring Framework");
		form.add("module", "spring-web");
		ResponseEntity<Void> response = this.restClient.post()
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.toBodilessEntity();
		// end::urlencodedform[]
	}

	void sendMultipartData() {
		// tag::multipartrequest[]
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

		parts.add("fieldPart", "fieldValue");
		parts.add("filePart", new FileSystemResource("...logo.png"));
		parts.add("jsonPart", new Person("Jason"));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		parts.add("xmlPart", new HttpEntity<>(this.myBean, headers));

		ResponseEntity<Void> response = this.restClient.post()
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(parts)
				.retrieve()
				.toBodilessEntity();
		// end::multipartrequest[]
	}

	void receiveMultipartData() throws IOException {
		// tag::multipartresponse[]
		MultiValueMap<String, Part> result = this.restClient.get()
				.uri("https://example.com/upload")
				.accept(MediaType.MULTIPART_FORM_DATA)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});

		Part field = result.getFirst("fieldPart");
		if (field instanceof FormFieldPart formField) {
			String fieldValue = formField.value();
		}
		Part file = result.getFirst("filePart");
		if (file instanceof FilePart filePart) {
			filePart.transferTo(Path.of("/tmp/" + filePart.filename()));
		}
		// end::multipartresponse[]
	}

	public static class User {

		private String username;
		private String password;

		public User() {
		}

		public User(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@JsonView(WithoutPasswordView.class)
		public String getUsername() {
			return this.username;
		}

		@JsonView(WithPasswordView.class)
		public String getPassword() {
			return this.password;
		}

		public interface WithoutPasswordView {
		}

		public interface WithPasswordView extends WithoutPasswordView {
		}
	}

	private record Person(String name) {

	}

}
