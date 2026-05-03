/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

/**
 * Implementation of {@link HttpMessageConverter} to read and write
 * multipart data (for example, file uploads).
 *
 * <p>This converter can read {@code "multipart/form-data"}
 * and {@code "multipart/mixed"} messages as
 * {@link MultiValueMap MultiValueMap&lt;String, Part&gt;}, and
 * write {@link MultiValueMap MultiValueMap&lt;String, Object&gt;} as
 * multipart messages.
 *
 * <p>On Servlet containers, the reading of multipart messages should be
 * delegated to the {@link org.springframework.web.multipart.MultipartResolver}.
 *
 * <h3>Multipart Data</h3>
 *
 * <p>By default, {@code "multipart/form-data"} is used as the content type when
 * {@linkplain #write writing} multipart data. It is also possible to write
 * multipart data using other multipart subtypes such as {@code "multipart/mixed"}
 * and {@code "multipart/related"}, as long as the multipart subtype is registered
 * as a {@linkplain #getSupportedMediaTypes supported media type} <em>and</em> the
 * desired multipart subtype is specified as the content type when
 * {@linkplain #write writing} the multipart data. Note that {@code "multipart/mixed"}
 * is registered as a supported media type by default.
 *
 * <p>When writing multipart data, this converter uses other
 * {@link HttpMessageConverter HttpMessageConverters} to write the respective
 * MIME parts. By default, basic converters are registered for byte array,
 * {@code String}, and {@code Resource}. This can be set with the main
 * {@link #MultipartHttpMessageConverter(Iterable) constructor}.
 *
 * <h3>Examples</h3>
 *
 * <p>The following snippet shows how to submit an HTML form using the
 * {@code "multipart/form-data"} content type.
 *
 * <pre class="code">
 * RestClient restClient = RestClient.create();
 * // MultipartHttpMessageConverter is configured by default
 *
 * MultiValueMap&lt;String, Object&gt; form = new LinkedMultiValueMap&lt;&gt;();
 * form.add("field 1", "value 1");
 * form.add("field 2", "value 2");
 * form.add("field 2", "value 3");
 * form.add("field 3", 4);
 *
 * ResponseEntity&lt;Void&gt; response = restClient.post()
 *   .uri("https://example.com/myForm")
 *   .contentType(MULTIPART_FORM_DATA)
 *   .body(form)
 *   .retrieve()
 *   .toBodilessEntity();</pre>
 *
 * <p>The following snippet shows how to do a file upload using the
 * {@code "multipart/form-data"} content type.
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * ResponseEntity&lt;Void&gt; response = restClient.post()
 *   .uri("https://example.com/myForm")
 *   .contentType(MULTIPART_FORM_DATA)
 *   .body(parts)
 *   .retrieve()
 *   .toBodilessEntity();</pre>
 *
 * <p>The following snippet shows how to decode a multipart response.
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Part&gt; body = this.restClient.get()
 * 				.uri("https://example.com/parts/42")
 * 				.accept(MediaType.MULTIPART_FORM_DATA)
 * 				.retrieve()
 * 				.body(new ParameterizedTypeReference&lt;&gt;() {});</pre>
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.1
 * @see org.springframework.util.MultiValueMap
 */
public class MultipartHttpMessageConverter implements SmartHttpMessageConverter<MultiValueMap<String, ?>> {

	private final List<HttpMessageConverter<?>> partConverters;

	private @Nullable Path tempDirectory;

	private List<MediaType> supportedMediaTypes = new ArrayList<>();

	private Charset charset = StandardCharsets.UTF_8;

	private @Nullable Charset multipartCharset;

	private int maxInMemorySize = 256 * 1024;

	private int maxHeadersSize = 10 * 1024;

	private long maxDiskUsagePerPart = -1;

	private int maxParts = -1;

	/**
	 * Create a new converter instance with the given converter instances for reading and
	 * writing parts.
	 * @param converters the converters to use for reading and writing parts
	 */
	public MultipartHttpMessageConverter(Iterable<HttpMessageConverter<?>> converters) {
		this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
		this.supportedMediaTypes.add(MediaType.MULTIPART_MIXED);
		this.supportedMediaTypes.add(MediaType.MULTIPART_RELATED);

		this.partConverters = new ArrayList<>();
		converters.forEach(this.partConverters::add);
		applyDefaultCharset();
	}

	/**
	 * Create a new converter instance with default converter instances for reading and
	 * writing parts.
	 * @see ByteArrayHttpMessageConverter
	 * @see StringHttpMessageConverter
	 * @see ResourceHttpMessageConverter
	 */
	public MultipartHttpMessageConverter() {
		this(List.of( new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
				new ResourceHttpMessageConverter()));
	}

	/**
	 * Set the list of {@link MediaType} objects supported by this converter.
	 * @see #addSupportedMediaTypes(MediaType...)
	 * @see #getSupportedMediaTypes()
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		// Ensure internal list is mutable.
		this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
	}

	/**
	 * Add {@link MediaType} objects to be supported by this converter.
	 * <p>The supplied {@code MediaType} objects will be appended to the list
	 * of {@linkplain #getSupportedMediaTypes() supported MediaType objects}.
	 * @param supportedMediaTypes a var-args list of {@code MediaType} objects to add
	 * @see #setSupportedMediaTypes(List)
	 */
	public void addSupportedMediaTypes(MediaType... supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		Assert.noNullElements(supportedMediaTypes, "'supportedMediaTypes' must not contain null elements");
		Collections.addAll(this.supportedMediaTypes, supportedMediaTypes);
	}

	/**
	 * {@inheritDoc}
	 * @see #setSupportedMediaTypes(List)
	 * @see #addSupportedMediaTypes(MediaType...)
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}


	/**
	 * Return the configured converters for MIME parts.
	 */
	public List<HttpMessageConverter<?>> getPartConverters() {
		return Collections.unmodifiableList(this.partConverters);
	}

	/**
	 * Set the default character set to use for reading and writing form data when
	 * the request or response {@code Content-Type} header does not explicitly
	 * specify it.
	 * <p>As of 4.3, this is also used as the default charset for the conversion
	 * of text bodies in a multipart request.
	 * <p>As of 5.0, this is also used for part headers including
	 * {@code Content-Disposition} (and its filename parameter) unless (the mutually
	 * exclusive) {@link #setMultipartCharset multipartCharset} is also set, in
	 * which case part headers are encoded as ASCII and <i>filename</i> is encoded
	 * with the {@code encoded-word} syntax from RFC 2047.
	 * <p>By default, this is set to "UTF-8".
	 */
	public void setCharset(@Nullable Charset charset) {
		if (charset != this.charset) {
			this.charset = (charset != null ? charset : StandardCharsets.UTF_8);
			applyDefaultCharset();
		}
	}

	/**
	 * Apply the configured charset as a default to registered part converters.
	 */
	private void applyDefaultCharset() {
		for (HttpMessageConverter<?> candidate : this.partConverters) {
			if (candidate instanceof AbstractHttpMessageConverter<?> converter) {
				// Only override default charset if the converter operates with a charset to begin with...
				if (converter.getDefaultCharset() != null) {
					converter.setDefaultCharset(this.charset);
				}
			}
		}
	}

	/**
	 * Set the character set to use when writing multipart data to encode file
	 * names. Encoding is based on the {@code encoded-word} syntax defined in
	 * RFC 2047 and relies on {@code MimeUtility} from {@code jakarta.mail}.
	 * <p>As of 5.0 by default part headers, including {@code Content-Disposition}
	 * (and its filename parameter) will be encoded based on the setting of
	 * {@link #setCharset(Charset)} or {@code UTF-8} by default.
	 * @see <a href="https://en.wikipedia.org/wiki/MIME#Encoded-Word">Encoded-Word</a>
	 */
	public void setMultipartCharset(Charset charset) {
		this.multipartCharset = charset;
	}


	/**
	 * Configure the maximum amount of memory that is allowed per headers section of each part.
	 * <p>By default, this is set to 10K.
	 * @param byteCount the maximum amount of memory for headers
	 */
	public void setMaxHeadersSize(int byteCount) {
		this.maxHeadersSize = byteCount;
	}

	/**
	 * Configure the maximum amount of memory allowed per part.
	 * When the limit is exceeded:
	 * <ul>
	 * <li>File parts are written to a temporary file.
	 * <li>Non-file parts are rejected with {@link DataBufferLimitException}.
	 * </ul>
	 * <p>By default, this is set to 256K.
	 * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
	 * contents will be stored in memory
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of disk space allowed for file parts.
	 * <p>By default, this is set to -1, meaning that there is no maximum.
	 * <p>Note that this property is ignored when
	 * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * Specify the maximum number of parts allowed in a given multipart request.
	 * <p>By default, this is set to -1, meaning that there is no maximum.
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		if (!supportsMediaType(mediaType)) {
			return false;
		}
		if (!MultiValueMap.class.isAssignableFrom(elementType.toClass()) ||
				(!elementType.hasUnresolvableGenerics() &&
				!Part.class.isAssignableFrom(elementType.getGeneric(1).toClass()))) {
			return false;
		}
		return true;
	}

	private boolean supportsMediaType(@Nullable MediaType mediaType) {
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MultiValueMap<String, Part> read(ResolvableType type, HttpInputMessage message, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotReadableException {

		Charset headersCharset = MultipartUtils.charset(message.getHeaders());
		byte[] boundary = boundary(message, headersCharset);
		if (boundary == null) {
			throw new HttpMessageNotReadableException("No multipart boundary found in Content-Type: \"" +
					message.getHeaders().getContentType() + "\"", message);
		}
		PartGenerator partListener = new PartGenerator(this.maxInMemorySize, this.maxDiskUsagePerPart, this.maxParts, getTempDirectory());
		new MultipartParser(this.maxHeadersSize, 2 * 1024).parse(message.getBody(), boundary,
				headersCharset, partListener);
		return partListener.getParts();
	}


	private static byte @Nullable [] boundary(HttpInputMessage message, Charset headersCharset) {
		MediaType contentType = message.getHeaders().getContentType();
		if (contentType != null) {
			String boundary = contentType.getParameter("boundary");
			if (boundary != null) {
				int len = boundary.length();
				if (len > 2 && boundary.charAt(0) == '"' && boundary.charAt(len - 1) == '"') {
					boundary = boundary.substring(1, len - 1);
				}
				return boundary.getBytes(headersCharset);
			}
		}
		return null;
	}

	private Path getTempDirectory() throws IOException {
		if (this.tempDirectory == null || !this.tempDirectory.toFile().exists()) {
			this.tempDirectory = Files.createTempDirectory("spring-multipart-");
		}
		return this.tempDirectory;
	}

	@Override
	public boolean canWrite(ResolvableType targetType, Class<?> valueClass, @Nullable MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(targetType.toClass())) {
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

	@Override
	@SuppressWarnings("unchecked")
	public void write(MultiValueMap<String, ?> map, ResolvableType type, @Nullable MediaType contentType, HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {
		MultiValueMap<String, Object> parts = (MultiValueMap<String, Object>) map;

		// If the supplied content type is null, fall back to multipart/form-data.
		// Otherwise, rely on the fact that isMultipart() already verified the
		// supplied content type is multipart.
		if (contentType == null) {
			contentType = MediaType.MULTIPART_FORM_DATA;
		}

		Map<String, String> parameters = new LinkedHashMap<>(contentType.getParameters().size() + 2);
		parameters.putAll(contentType.getParameters());

		byte[] boundary = MimeTypeUtils.generateMultipartBoundary();
		if (!isFilenameCharsetSet()) {
			if (!this.charset.equals(StandardCharsets.UTF_8) &&
					!this.charset.equals(StandardCharsets.US_ASCII)) {
				parameters.put("charset", this.charset.name());
			}
		}
		parameters.put("boundary", new String(boundary, StandardCharsets.US_ASCII));

		// Add parameters to output content type
		contentType = new MediaType(contentType, parameters);
		outputMessage.getHeaders().setContentType(contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
			boolean repeatable = checkPartsRepeatable(parts, contentType);
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					MultipartHttpMessageConverter.this.writeParts(outputStream, parts, boundary);
					writeEnd(outputStream, boundary);
				}

				@Override
				public boolean repeatable() {
					return repeatable;
				}
			});
		}
		else {
			writeParts(outputMessage.getBody(), parts, boundary);
			writeEnd(outputMessage.getBody(), boundary);
		}
	}


	@SuppressWarnings({"unchecked", "ConstantValue"})
	private <T> boolean checkPartsRepeatable(MultiValueMap<String, Object> map, MediaType contentType) {
		return map.entrySet().stream().allMatch(e -> e.getValue().stream().filter(Objects::nonNull).allMatch(part -> {
			HttpHeaders headers = null;
			Object body = part;
			if (part instanceof HttpEntity<?> entity) {
				headers = entity.getHeaders();
				body = entity.getBody();
				Assert.state(body != null, "Empty body for part '" + e.getKey() + "': " + part);
			}
			HttpMessageConverter<T> converter = (HttpMessageConverter<T>) findConverterFor(e.getKey(), headers, body);
			return converter != null && converter.canWriteRepeatedly((T) body, contentType);
		}));
	}

	private @Nullable HttpMessageConverter<?> findConverterFor(
			String name, @Nullable HttpHeaders headers, Object body) {

		Class<?> partType = body.getClass();
		MediaType contentType = (headers != null ? headers.getContentType() : null);
		for (HttpMessageConverter<?> converter : this.partConverters) {
			if (converter.canWrite(partType, contentType)) {
				return converter;
			}
		}
		return null;
	}

	/**
	 * When {@link #setMultipartCharset(Charset)} is configured (i.e. RFC 2047,
	 * {@code encoded-word} syntax) we need to use ASCII for part headers, or
	 * otherwise we encode directly using the configured {@link #setCharset(Charset)}.
	 */
	private boolean isFilenameCharsetSet() {
		return (this.multipartCharset != null);
	}

	private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
		for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
			String name = entry.getKey();
			for (Object part : entry.getValue()) {
				if (part != null) {
					writeBoundary(os, boundary);
					writePart(name, getHttpEntity(part), os);
					writeNewLine(os);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
		Object partBody = partEntity.getBody();
		Assert.state(partBody != null, "Empty body for part '" + name + "': " + partEntity);
		HttpHeaders partHeaders = partEntity.getHeaders();
		MediaType partContentType = partHeaders.getContentType();
		HttpMessageConverter<?> converter = findConverterFor(name, partHeaders, partBody);
		if (converter != null) {
			Charset charset = isFilenameCharsetSet() ? StandardCharsets.US_ASCII : this.charset;
			HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os, charset);
			String filename = getFilename(partBody);
			ContentDisposition.Builder cd = ContentDisposition.formData().name(name);
			if (filename != null) {
				cd.filename(filename, this.multipartCharset);
			}
			multipartMessage.getHeaders().setContentDisposition(cd.build());
			if (!partHeaders.isEmpty()) {
				multipartMessage.getHeaders().putAll(partHeaders);
			}
			((HttpMessageConverter<Object>) converter).write(partBody, partContentType, multipartMessage);
			return;
		}
		throw new HttpMessageNotWritableException("Could not write request: " +
				"no suitable HttpMessageConverter found for request type [" + partBody.getClass().getName() + "]");
	}

	/**
	 * Return an {@link HttpEntity} for the given part Object.
	 * @param part the part to return an {@link HttpEntity} for
	 * @return the part Object itself it is an {@link HttpEntity},
	 * or a newly built {@link HttpEntity} wrapper for that part
	 */
	protected HttpEntity<?> getHttpEntity(Object part) {
		return (part instanceof HttpEntity<?> httpEntity ? httpEntity : new HttpEntity<>(part));
	}

	/**
	 * Return the filename of the given multipart part. This value will be used for the
	 * {@code Content-Disposition} header.
	 * <p>The default implementation returns {@link Resource#getFilename()} if the part is a
	 * {@code Resource}, and {@code null} in other cases. Can be overridden in subclasses.
	 * @param part the part to determine the file name for
	 * @return the filename, or {@code null} if not known
	 */
	protected @Nullable String getFilename(Object part) {
		if (part instanceof Resource resource) {
			return resource.getFilename();
		}
		else {
			return null;
		}
	}


	private void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		writeNewLine(os);
	}

	private static void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}


	/**
	 * Implementation of {@link org.springframework.http.HttpOutputMessage} used
	 * to write a MIME multipart.
	 */
	private static class MultipartHttpOutputMessage implements HttpOutputMessage {

		private final OutputStream outputStream;

		private final Charset charset;

		private final HttpHeaders headers = new HttpHeaders();

		private boolean headersWritten = false;

		public MultipartHttpOutputMessage(OutputStream outputStream, Charset charset) {
			this.outputStream = new MultipartOutputStream(outputStream);
			this.charset = charset;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public OutputStream getBody() throws IOException {
			writeHeaders();
			return this.outputStream;
		}

		private void writeHeaders() throws IOException {
			if (!this.headersWritten) {
				for (Map.Entry<String, List<String>> entry : this.headers.headerSet()) {
					byte[] headerName = getBytes(entry.getKey());
					for (String headerValueString : entry.getValue()) {
						byte[] headerValue = getBytes(headerValueString);
						this.outputStream.write(headerName);
						this.outputStream.write(':');
						this.outputStream.write(' ');
						this.outputStream.write(headerValue);
						writeNewLine(this.outputStream);
					}
				}
				writeNewLine(this.outputStream);
				this.headersWritten = true;
			}
		}

		private byte[] getBytes(String name) {
			return name.getBytes(this.charset);
		}

	}


	/**
	 * OutputStream that neither flushes nor closes.
	 */
	private static class MultipartOutputStream extends FilterOutputStream {

		public MultipartOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(byte[] b, int off, int let) throws IOException {
			this.out.write(b, off, let);
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}
}
