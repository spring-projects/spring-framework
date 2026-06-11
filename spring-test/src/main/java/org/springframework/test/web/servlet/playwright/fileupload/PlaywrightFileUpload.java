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

package org.springframework.test.web.servlet.playwright.fileupload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.microsoft.playwright.Request;

import org.springframework.core.ResolvableType;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

public class PlaywrightFileUpload {

	private final MultipartHttpMessageConverter converter;

	public PlaywrightFileUpload(MultipartHttpMessageConverter converter) {
		this.converter = converter;
	}

	private Collection<List<org.springframework.http.converter.multipart.Part>> parseRequest(Request request) throws IOException {
		var type = ResolvableType.forClass(request.getClass());
		var message = new MockHttpInputMessage(request.postDataBuffer());
		request.headersArray().forEach(header -> message.getHeaders().add(header.name, header.value));
		var converted = this.converter.read(type, message, new HashMap<>());
		return converted.values();
	}

	public FileUploadContent getContent(Request request) {
		try {
			var items = parseRequest(request);
			var parts = new ArrayList<FormFieldPart>();
			var files = new ArrayList<FilePart>();
			for (var itemList : items) {
				for (var item : itemList) {
					if (item instanceof FilePart filePart) {
						files.add(filePart);
					}
					else if (item instanceof FormFieldPart formFieldPart) {
						parts.add(formFieldPart);
					}
					else {
						throw new IllegalArgumentException("Unsupported part type: " + item.getClass().getName());
					}
				}
			}
			return new FileUploadContent(parts, files);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
