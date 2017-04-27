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
import reactor.util.function.Tuples;

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
 * Implementation of {@link HttpMessageWriter} to write multipart HTML
 * forms with {@code "multipart/form-data"} media type.
 *
 * <p>When writing multipart data, this writer uses other
 * {@link HttpMessageWriter HttpMessageWriters} to write the respective
 * MIME parts. By default, basic writers are registered (for {@code Strings}
 * and {@code Resources}). These can be overridden through the provided
 * constructors.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class MultipartHttpMessageWriter implements HttpMessageWriter<MultiValueMap<String, ?>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	private List<HttpMessageWriter<?>> partWriters;

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

	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters, DataBufferFactory bufferFactory) {
		this.partWriters = partWriters;
		this.bufferFactory = bufferFactory;
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
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return (mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) &&
				(MultiValueMap.class.isAssignableFrom(elementType.getRawClass()) && String.class.isAssignableFrom(elementType.resolveGeneric(0)));
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, ?>> inputStream,
			ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		final byte[] boundary = generateMultipartBoundary();
		Map<String, String> parameters = Collections.singletonMap("boundary", new String(boundary, StandardCharsets.US_ASCII));

		MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(contentType);

		return Flux
				.from(inputStream)
				.single()
				.flatMap(form -> {
					Flux<DataBuffer> body = Flux.fromIterable(form.entrySet())
						.concatMap(entry -> Flux.fromIterable(entry.getValue()).map(value -> Tuples.of(entry.getKey(), value)))
						.concatMap(part -> generatePart(part.getT1(), getHttpEntity(part.getT2()), boundary))
						.concatWith(Mono.just(generateLastLine(boundary)));
					return outputMessage.writeWith(body);
				});
	}

	@SuppressWarnings("unchecked")
	private Flux<DataBuffer> generatePart(String name, HttpEntity<?> partEntity, byte[] boundary) {
		Object partBody = partEntity.getBody();
		ResolvableType partType = ResolvableType.forClass(partBody.getClass());
		MultipartHttpOutputMessage outputMessage = new MultipartHttpOutputMessage(this.bufferFactory);
		HttpHeaders partHeaders = outputMessage.getHeaders();
		outputMessage.getHeaders().putAll(partHeaders);
		MediaType partContentType = partHeaders.getContentType();
		partHeaders.setContentDispositionFormData(name, getFilename(partBody));

		Optional<HttpMessageWriter<?>> writer = this.partWriters
				.stream()
				.filter(e -> e.canWrite(partType, partContentType))
				.findFirst();

		if(!writer.isPresent()) {
			return Flux.error(new CodecException("No suitable writer found!"));
		}
		Mono<Void> partWritten = ((HttpMessageWriter<Object>)writer.get())
				.write(Mono.just(partBody), partType, partContentType, outputMessage, Collections.emptyMap());

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
	 * Generate a multipart boundary.
	 * <p>This implementation delegates to
	 * {@link MimeTypeUtils#generateMultipartBoundary()}.
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * Return an {@link HttpEntity} for the given part Object.
	 * @param part the part to return an {@link HttpEntity} for
	 * @return the part Object itself it is an {@link HttpEntity},
	 * or a newly built {@link HttpEntity} wrapper for that part
	 */
	protected HttpEntity<?> getHttpEntity(Object part) {
		if (part instanceof HttpEntity) {
			return (HttpEntity<?>) part;
		}
		else {
			return new HttpEntity<>(part);
		}
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

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
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

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			return Mono.error(new UnsupportedOperationException());
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
		public Mono<Void> setComplete() {
			return (this.body != null ? this.body.then() : Mono.error(new IllegalStateException("Body has not been written yet")));
		}

		public Flux<DataBuffer> getBody() {
			return (this.body != null ? this.body : Flux.error(new IllegalStateException("Body has not been written yet")));
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
