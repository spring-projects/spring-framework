/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.mail.internet.MimeUtility;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code HttpMessageWriter} for {@code "multipart/form-data"} requests.
 *
 * <p>This writer delegates to other message writers to write the respective
 * parts. By default basic writers are registered for {@code String}, and
 * {@code Resources}. These can be overridden through the provided constructors.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MultipartHttpMessageWriter implements HttpMessageWriter<MultiValueMap<String, ?>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	private final List<HttpMessageWriter<?>> partWriters;

	private Charset filenameCharset = DEFAULT_CHARSET;

	private final DataBufferFactory bufferFactory;


	public MultipartHttpMessageWriter() {
		this(new DefaultDataBufferFactory());
	}

	public MultipartHttpMessageWriter(DataBufferFactory bufferFactory) {
		this.partWriters = Arrays.asList(
				new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()),
				new ResourceHttpMessageWriter()
		);
		this.bufferFactory = bufferFactory;
	}

	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters) {
		this(partWriters, new DefaultDataBufferFactory());
	}

	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters, DataBufferFactory factory) {
		this.partWriters = partWriters;
		this.bufferFactory = factory;
	}

	/**
	 * Set the character set to use for writing file names in the multipart request.
	 * <p>By default this is set to "UTF-8".
	 */
	public void setFilenameCharset(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		this.filenameCharset = charset;
	}

	/**
	 * Return the configured filename charset.
	 */
	public Charset getFilenameCharset() {
		return this.filenameCharset;
	}


	@Override
	public List<MediaType> getWritableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return MultiValueMap.class.isAssignableFrom(elementType.getRawClass()) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, ?>> inputStream,
			ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		byte[] boundary = generateMultipartBoundary();

		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(new MediaType(MediaType.MULTIPART_FORM_DATA,
				Collections.singletonMap("boundary", new String(boundary, StandardCharsets.US_ASCII))));

		return Mono.from(inputStream).flatMap(multiValueMap ->
				outputMessage.writeWith(generateParts(multiValueMap, boundary)));
	}

	/**
	 * Generate a multipart boundary.
	 * <p>By default delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	private Flux<DataBuffer> generateParts(MultiValueMap<String, ?> map, byte[] boundary) {
		return Flux.fromIterable(map.entrySet())
				.concatMap(entry -> Flux
						.fromIterable(entry.getValue())
						.concatMap(value -> generatePart(entry.getKey(), value, boundary)))
				.concatWith(Mono.just(generateLastLine(boundary)));
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> generatePart(String name, T value, byte[] boundary) {

		MultipartHttpOutputMessage outputMessage = new MultipartHttpOutputMessage(this.bufferFactory);

		T body;
		if (value instanceof HttpEntity) {
			outputMessage.getHeaders().putAll(((HttpEntity<T>) value).getHeaders());
			body = ((HttpEntity<T>) value).getBody();
		}
		else {
			body = value;
		}

		ResolvableType bodyType = ResolvableType.forClass(body.getClass());
		outputMessage.getHeaders().setContentDispositionFormData(name, getFilename(body));

		MediaType contentType = outputMessage.getHeaders().getContentType();

		Optional<HttpMessageWriter<?>> writer = this.partWriters.stream()
				.filter(partWriter -> partWriter.canWrite(bodyType, contentType))
				.findFirst();

		if(!writer.isPresent()) {
			return Flux.error(new CodecException("No suitable writer found for part: " + name));
		}

		Mono<Void> partWritten = ((HttpMessageWriter<T>) writer.get())
				.write(Mono.just(body), bodyType, contentType, outputMessage, Collections.emptyMap());

		// partWritten.subscribe() is required in order to make sure MultipartHttpOutputMessage#getBody()
		// returns a non-null value (occurs with ResourceHttpMessageWriter that invokes
		// ReactiveHttpOutputMessage.writeWith() only when at least one element has been
		// requested).
		partWritten.subscribe();

		return Flux.concat(
				Mono.just(generateBoundaryLine(boundary)),
				outputMessage.getBody(),
				Mono.just(generateNewLine())
		);
	}

	/**
	 * Return the filename of the given multipart part. This value will be used
	 * for the {@code Content-Disposition} header.
	 * <p>The default implementation returns {@link Resource#getFilename()} if
	 * the part is a {@code Resource}, and {@code null} in other cases.
	 * @param part the part for which return a file name
	 * @return the filename or {@code null}
	 */
	protected String getFilename(Object part) {
		if (part instanceof Resource) {
			Resource resource = (Resource) part;
			String filename = resource.getFilename();
			filename = MimeDelegate.encode(filename, this.filenameCharset.name());
			return filename;
		}
		else {
			return null;
		}
	}

	private DataBuffer generateBoundaryLine(byte[] boundary) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(boundary.length + 4);
		buffer.write((byte)'-');
		buffer.write((byte)'-');
		buffer.write(boundary);
		buffer.write((byte)'\r');
		buffer.write((byte)'\n');
		return buffer;
	}

	private DataBuffer generateNewLine() {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(2);
		buffer.write((byte)'\r');
		buffer.write((byte)'\n');
		return buffer;
	}

	private DataBuffer generateLastLine(byte[] boundary) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(boundary.length + 6);
		buffer.write((byte)'-');
		buffer.write((byte)'-');
		buffer.write(boundary);
		buffer.write((byte)'-');
		buffer.write((byte)'-');
		buffer.write((byte)'\r');
		buffer.write((byte)'\n');
		return buffer;
	}


	private static class MultipartHttpOutputMessage implements ReactiveHttpOutputMessage {

		private final DataBufferFactory bufferFactory;

		private final HttpHeaders headers = new HttpHeaders();

		private final AtomicBoolean commited = new AtomicBoolean();

		private Flux<DataBuffer> body;


		public MultipartHttpOutputMessage(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
		}


		@Override
		public HttpHeaders getHeaders() {
			return (this.body != null ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public DataBufferFactory bufferFactory() {
			return this.bufferFactory;
		}

		@Override
		public void beforeCommit(Supplier<? extends Mono<Void>> action) {
			this.commited.set(true);
		}

		@Override
		public boolean isCommitted() {
			return this.commited.get();
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			if (this.body != null) {
				return Mono.error(new IllegalStateException("Multiple calls to writeWith() not supported"));
			}
			this.body = Flux.just(generateHeaders()).concatWith(body);
			return this.body.then();
		}

		private DataBuffer generateHeaders() {
			DataBuffer buffer = this.bufferFactory.allocateBuffer();
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				byte[] headerName = entry.getKey().getBytes(StandardCharsets.US_ASCII);
				for (String headerValueString : entry.getValue()) {
					byte[] headerValue = headerValueString.getBytes(StandardCharsets.US_ASCII);
					buffer.write(headerName);
					buffer.write((byte)':');
					buffer.write((byte)' ');
					buffer.write(headerValue);
					buffer.write((byte)'\r');
					buffer.write((byte)'\n');
				}
			}
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			return Mono.error(new UnsupportedOperationException());
		}

		public Flux<DataBuffer> getBody() {
			return (this.body != null ? this.body :
					Flux.error(new IllegalStateException("Body has not been written yet")));
		}

		@Override
		public Mono<Void> setComplete() {
			return (this.body != null ? this.body.then() :
					Mono.error(new IllegalStateException("Body has not been written yet")));
		}

	}

	/**
	 * Inner class to avoid a hard dependency on the JavaMail API.
	 */
	private static class MimeDelegate {

		public static String encode(String value, String charset) {
			try {
				return MimeUtility.encodeText(value, charset, null);
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
