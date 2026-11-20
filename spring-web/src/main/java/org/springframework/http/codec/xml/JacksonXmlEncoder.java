/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.xml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of XML objects using Jackson 3.x.
 *
 * <p>Stream encoding is currently not supported, but a single value
 * can be encoded, for example as a {@code Mono}.
 *
 * @author Sebastien Deleuze
 * @since 7.0.3
 * @see JacksonXmlDecoder
 */
public class JacksonXmlEncoder extends AbstractJacksonEncoder<XmlMapper> {

	private static final MediaType[] DEFAULT_XML_MIME_TYPES = new MediaType[] {
			new MediaType("application", "xml", StandardCharsets.UTF_8),
			new MediaType("text", "xml", StandardCharsets.UTF_8),
			new MediaType("application", "*+xml", StandardCharsets.UTF_8)
	};


	/**
	 * Construct a new instance with a {@link XmlMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonXmlEncoder() {
		super(XmlMapper.builder(), DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper.Builder builder) {
		super(builder, DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper mapper) {
		super(mapper, DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper} and {@link MimeType}s.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (super.canEncode(elementType, mimeType) && String.class != elementType.toClass());
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (inputStream instanceof Mono) {
			return super.encode(inputStream, bufferFactory, elementType, mimeType, hints);
		}
		throw new UnsupportedOperationException("Stream encoding is currently not supported");
	}

}
