/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can handle form data, including multipart form data (i.e. file
 * uploads).
 *
 * <p>This converter can write the {@code application/x-www-form-urlencoded} and {@code multipart/form-data} media
 * types, and read the {@code application/x-www-form-urlencoded}) media type (but not {@code multipart/form-data}).
 *
 * <p>In other words, this converter can read and write 'normal' HTML forms (as {@link MultiValueMap
 * MultiValueMap&lt;String, String&gt;}), and it can write multipart form (as {@link MultiValueMap
 * MultiValueMap&lt;String, Object&gt;}. When writing multipart, this converter uses other {@link HttpMessageConverter
 * HttpMessageConverters} to write the respective MIME parts. By default, basic converters are registered (supporting
 * {@code Strings} and {@code Resources}, for instance); these can be overridden by setting the {@link
 * #setPartConverters(java.util.List) partConverters} property.
 *
 * <p>For example, the following snippet shows how to submit an HTML form: <pre class="code"> RestTemplate template =
 * new RestTemplate(); // FormHttpMessageConverter is configured by default MultiValueMap&lt;String, String&gt; form =
 * new LinkedMultiValueMap&lt;String, String&gt;(); form.add("field 1", "value 1"); form.add("field 2", "value 2");
 * form.add("field 2", "value 3"); template.postForLocation("http://example.com/myForm", form); </pre> <p>The following
 * snippet shows how to do a file upload: <pre class="code"> MultiValueMap&lt;String, Object&gt; parts = new
 * LinkedMultiValueMap&lt;String, Object&gt;(); parts.add("field 1", "value 1"); parts.add("file", new
 * ClassPathResource("myFile.jpg")); template.postForLocation("http://example.com/myFileUpload", parts); </pre>
 *
 * <p>Some methods in this class were inspired by {@code org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity}.
 *
 * @author Arjen Poutsma
 * @see MultiValueMap
 * @since 3.0
 */
public class FormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	private static final byte[] BOUNDARY_CHARS =
			new byte[]{'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
					'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
					'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
					'V', 'W', 'X', 'Y', 'Z'};

	private final Random rnd = new Random();

	private Charset charset = Charset.forName("UTF-8");

	private List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();

	private List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();


	public FormHttpMessageConverter() {
		this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
		this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);

		this.partConverters.add(new ByteArrayHttpMessageConverter());
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		this.partConverters.add(stringHttpMessageConverter);
		this.partConverters.add(new ResourceHttpMessageConverter());
	}


	/**
	 * Set the message body converters to use. These converters are used to convert objects to MIME parts.
	 */
	public final void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
		Assert.notEmpty(partConverters, "'partConverters' must not be empty");
		this.partConverters = partConverters;
	}

	/**
	 * Add a message body converter. Such a converters is used to convert objects to MIME parts.
	 */
	public final void addPartConverter(HttpMessageConverter<?> partConverter) {
		Assert.notNull(partConverter, "'partConverter' must not be NULL");
		this.partConverters.add(partConverter);
	}

	/**
	 * Sets the character set used for writing form data.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// we can't read multipart
			if (!supportedMediaType.equals(MediaType.MULTIPART_FORM_DATA) &&
				supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set the list of {@link MediaType} objects supported by this converter.
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	@Override
	public MultiValueMap<String, String> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : this.charset;
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(pairs.length);

		for (String pair : pairs) {
			int idx = pair.indexOf('=');
			if (idx == -1) {
				result.add(URLDecoder.decode(pair, charset.name()), null);
			}
			else {
				String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
				String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
				result.add(name, value);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		if (!isMultipart(map, contentType)) {
			writeForm((MultiValueMap<String, String>) map, contentType, outputMessage);
		}
		else {
			writeMultipart((MultiValueMap<String, Object>) map, outputMessage);
		}
	}

	private boolean isMultipart(MultiValueMap<String, ?> map, MediaType contentType) {
		if (contentType != null) {
			return MediaType.MULTIPART_FORM_DATA.equals(contentType);
		}
		for (String name : map.keySet()) {
			for (Object value : map.get(name)) {
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	private void writeForm(MultiValueMap<String, String> form, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException {
		Charset charset;
		if (contentType != null) {
			outputMessage.getHeaders().setContentType(contentType);
			charset = contentType.getCharSet() != null ? contentType.getCharSet() : this.charset;
		}
		else {
			outputMessage.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			charset = this.charset;
		}
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
			String name = nameIterator.next();
			for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext();) {
				String value = valueIterator.next();
				builder.append(URLEncoder.encode(name, charset.name()));
				if (value != null) {
					builder.append('=');
					builder.append(URLEncoder.encode(value, charset.name()));
					if (valueIterator.hasNext()) {
						builder.append('&');
					}
				}
			}
			if (nameIterator.hasNext()) {
				builder.append('&');
			}
		}
		byte[] bytes = builder.toString().getBytes(charset.name());
		outputMessage.getHeaders().setContentLength(bytes.length);
		StreamUtils.copy(bytes, outputMessage.getBody());
	}

	private void writeMultipart(MultiValueMap<String, Object> parts, HttpOutputMessage outputMessage)
			throws IOException {
		byte[] boundary = generateMultipartBoundary();

		Map<String, String> parameters = Collections.singletonMap("boundary", new String(boundary, "US-ASCII"));
		MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
		outputMessage.getHeaders().setContentType(contentType);

		writeParts(outputMessage.getBody(), parts, boundary);
		writeEnd(boundary, outputMessage.getBody());
	}

	private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
		for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
			String name = entry.getKey();
			for (Object part : entry.getValue()) {
				if (part != null) {
					writeBoundary(boundary, os);
					HttpEntity<?> entity = getEntity(part);
					writePart(name, entity, os);
					writeNewLine(os);
				}
			}
		}
	}

	private void writeBoundary(byte[] boundary, OutputStream os) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	private HttpEntity<?> getEntity(Object part) {
		if (part instanceof HttpEntity) {
			return (HttpEntity<?>) part;
		}
		else {
			return new HttpEntity<Object>(part);
		}
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
		Object partBody = partEntity.getBody();
		Class<?> partType = partBody.getClass();
		HttpHeaders partHeaders = partEntity.getHeaders();
		MediaType partContentType = partHeaders.getContentType();
		for (HttpMessageConverter<?> messageConverter : partConverters) {
			if (messageConverter.canWrite(partType, partContentType)) {
				HttpOutputMessage multipartOutputMessage = new MultipartHttpOutputMessage(os);
				multipartOutputMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
				if (!partHeaders.isEmpty()) {
					multipartOutputMessage.getHeaders().putAll(partHeaders);
				}
				((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartOutputMessage);
				return;
			}
		}
		throw new HttpMessageNotWritableException(
				"Could not write request: no suitable HttpMessageConverter found for request type [" +
						partType.getName() + "]");
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
	 * <p>The default implementation returns a random boundary.
	 * Can be overridden in subclasses.
	 */
	protected byte[] generateMultipartBoundary() {
		byte[] boundary = new byte[rnd.nextInt(11) + 30];
		for (int i = 0; i < boundary.length; i++) {
			boundary[i] = BOUNDARY_CHARS[rnd.nextInt(BOUNDARY_CHARS.length)];
		}
		return boundary;
	}

	/**
	 * Return the filename of the given multipart part. This value will be used for the
	 * {@code Content-Disposition} header.
	 * <p>The default implementation returns {@link Resource#getFilename()} if the part is a
	 * {@code Resource}, and {@code null} in other cases. Can be overridden in subclasses.
	 * @param part the part to determine the file name for
	 * @return the filename, or {@code null} if not known
	 */
	protected String getFilename(Object part) {
		if (part instanceof Resource) {
			Resource resource = (Resource) part;
			return resource.getFilename();
		}
		else {
			return null;
		}
	}


	/**
	 * Implementation of {@link org.springframework.http.HttpOutputMessage} used for writing multipart data.
	 */
	private class MultipartHttpOutputMessage implements HttpOutputMessage {

		private final HttpHeaders headers = new HttpHeaders();

		private final OutputStream os;

		private boolean headersWritten = false;

		public MultipartHttpOutputMessage(OutputStream os) {
			this.os = os;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headersWritten ? HttpHeaders.readOnlyHttpHeaders(headers) : this.headers;
		}

		@Override
		public OutputStream getBody() throws IOException {
			writeHeaders();
			return this.os;
		}

		private void writeHeaders() throws IOException {
			if (!this.headersWritten) {
				for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
					byte[] headerName = getAsciiBytes(entry.getKey());
					for (String headerValueString : entry.getValue()) {
						byte[] headerValue = getAsciiBytes(headerValueString);
						os.write(headerName);
						os.write(':');
						os.write(' ');
						os.write(headerValue);
						writeNewLine(os);
					}
				}
				writeNewLine(os);
				this.headersWritten = true;
			}
		}

		protected byte[] getAsciiBytes(String name) {
			try {
				return name.getBytes("US-ASCII");
			}
			catch (UnsupportedEncodingException ex) {
				// should not happen, US-ASCII is always supported
				throw new IllegalStateException(ex);
			}
		}
	}

}
