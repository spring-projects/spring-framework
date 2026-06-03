/* (C)2024 */
package org.springframework.test.web.servlet.playwright.fileupload;

import com.microsoft.playwright.Request;
import org.springframework.core.ResolvableType;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PlaywrightFileUpload {

	private final MultipartHttpMessageConverter converter;

	public PlaywrightFileUpload(MultipartHttpMessageConverter converter) {
		this.converter = converter;
	}

	private Collection<List<org.springframework.http.converter.multipart.Part>> parseRequest(Request request) throws IOException {
		var type = ResolvableType.forClass(request.getClass());
		var message = new MockHttpInputMessage(request.postDataBuffer());
		request.headersArray().forEach(header -> message.getHeaders().add(header.name, header.value));
		var converted = converter.read(type, message, new HashMap<>());
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
					} else if(item instanceof FormFieldPart formFieldPart) {
						parts.add(formFieldPart);
					}else {
						throw new IllegalArgumentException("Unsupported part type: " + item.getClass().getName());
					}
				}
			}
			return new FileUploadContent(parts, files);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
