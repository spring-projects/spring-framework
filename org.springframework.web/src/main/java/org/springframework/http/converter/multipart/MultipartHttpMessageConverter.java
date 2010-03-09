/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * <p>Inspired by {@link org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity}.
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 */
public class MultipartHttpMessageConverter extends AbstractHttpMessageConverter<MultipartMap> {

	private static final byte[] BOUNDARY_CHARS =
			new byte[]{'-', '_',
					'1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
					'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
					'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	private final Random rnd = new Random();

	public MultipartHttpMessageConverter() {
		super(new MediaType("multipart", "form-data"));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return MultipartMap.class.isAssignableFrom(clazz);
	}

	@Override
	protected void writeInternal(MultipartMap map, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		byte[] boundary = generateBoundary();
		HttpHeaders headers = outputMessage.getHeaders();
		MediaType contentType = headers.getContentType();
		if (contentType != null) {
			String boundaryString = new String(boundary, "US-ASCII");
			Map<String, String> params = Collections.singletonMap("boundary", boundaryString);
			contentType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
			headers.setContentType(contentType);
		}
		OutputStream os = outputMessage.getBody();
		for (Map.Entry<String, List<Part>> entry : map.entrySet()) {
			String name = entry.getKey();
			for (Part part : entry.getValue()) {
				part.write(boundary, name, os);
			}
		}
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		os.write('\r');
		os.write('\n');
	}



	/**
	 * Generate a multipart boundary.
	 *
	 * <p>Default implementation returns a random boundary.
	 */
	protected byte[] generateBoundary() {
		byte[] boundary = new byte[rnd.nextInt(11) + 30];
		for (int i = 0; i < boundary.length; i++) {
			boundary[i] = BOUNDARY_CHARS[rnd.nextInt(BOUNDARY_CHARS.length)];
		}
		return boundary;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		// reading not supported yet
		return false;
	}

	@Override
	protected MultipartMap readInternal(Class<? extends MultipartMap> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException();
	}
}
