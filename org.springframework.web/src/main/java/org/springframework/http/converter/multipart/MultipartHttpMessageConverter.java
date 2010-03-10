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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can write multipart form data
 * (i.e. file uploads).
 *
 * <p>This converter writes the media type ({@code multipart/form-data}). Multipart form data is provided as
 * a {@link MultipartMap}.
 *
 * <p>Inspired by {@link org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity}.
 *
 * @author Arjen Poutsma
 * @see MultipartMap
 * @since 3.0.2
 */
public class MultipartHttpMessageConverter implements HttpMessageConverter<MultipartMap> {

	private static final byte[] BOUNDARY_CHARS =
			new byte[]{'-', '_',
					'1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
					'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
					'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	private final Random rnd = new Random();

	private List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();

	public MultipartHttpMessageConverter() {
		this.partConverters.add(new ByteArrayHttpMessageConverter());
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		this.partConverters.add(stringHttpMessageConverter);
		this.partConverters.add(new ResourceHttpMessageConverter());
		this.partConverters.add(new SourceHttpMessageConverter());
	}

	/**
	 * Set the message body converters to use. These converters are used to convert to MIME parts.
	 */
	public void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
		Assert.notEmpty(partConverters, "'messageConverters' must not be empty");
		this.partConverters = partConverters;
	}

	/**
	 * Returns {@code false}, as reading multipart data is not supported.
	 */
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return false;
	}

	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		if (!MultipartMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType != null) {
				return mediaType.includes(MediaType.MULTIPART_FORM_DATA);
		} else {
		return true;
		}
	}

	public List<MediaType> getSupportedMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	public MultipartMap read(Class<? extends MultipartMap> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException();
	}

	public void write(MultipartMap map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		byte[] boundary = generateBoundary();

		HttpHeaders headers = outputMessage.getHeaders();
		OutputStream os = outputMessage.getBody();

		setContentType(headers, boundary);
		writeParts(os, map, boundary);
		writeEnd(boundary, os);
	}

	private void setContentType(HttpHeaders headers, byte[] boundary) throws UnsupportedEncodingException {
		Map<String, String> parameters = Collections.singletonMap("boundary", new String(boundary, "US-ASCII"));
		MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
		headers.setContentType(contentType);
	}

	private void writeParts(OutputStream os, MultipartMap map, byte[] boundary)
			throws IOException {
		for (Map.Entry<String,List<Object>> entry : map.entrySet()) {
			String name = entry.getKey();
			for (Object part : entry.getValue()) {
				writeBoundary(boundary, os);
				writePart(name, part, os);
				writeNewLine(os);
			}
		}
	}

	private void writeBoundary(byte[] boundary, OutputStream os) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, Object part, OutputStream os) throws IOException {
		Class<?> partType = part.getClass();
		for (HttpMessageConverter messageConverter : partConverters) {
			if (messageConverter.canWrite(partType, null)) {
				HttpOutputMessage multipartOutputMessage = new MultipartHttpOutputMessage(os);
				multipartOutputMessage.getHeaders().setContentDispositionFormData(name, getFileName(part));
				messageConverter.write(part, null, multipartOutputMessage);
				return;
			}
		}
		throw new HttpMessageNotWritableException(
				"Could not write request: no suitable HttpMessageConverter found for request type [" +
						partType.getName() + "]");
	}

	protected String getFileName(Object part) {
		if (part instanceof Resource) {
			Resource resource = (Resource) part;
			return resource.getFilename();
		}
		else {
			return null;
		}
	}

	private void writeEnd(byte[] boundary, OutputStream os) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		writeNewLine(os);
	}

	private void writeNewLine(OutputStream os) throws IOException {
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

}
