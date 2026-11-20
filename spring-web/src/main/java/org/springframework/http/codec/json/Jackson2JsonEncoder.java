/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using Jackson 2.x.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jackson2JsonDecoder
 * @deprecated since 7.0 in favor of {@link JacksonJsonEncoder}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2JsonEncoder extends AbstractJackson2Encoder {

	private static final List<MimeType> problemDetailMimeTypes =
			Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);


	private final @Nullable PrettyPrinter ssePrettyPrinter;


	public Jackson2JsonEncoder() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		setStreamingMediaTypes(Arrays.asList(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSONL));
		this.ssePrettyPrinter = initSsePrettyPrinter();
	}

	private static PrettyPrinter initSsePrettyPrinter() {
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(new DefaultIndenter("  ", "\ndata: "));
		return printer;
	}


	@Override
	protected List<MimeType> getMediaTypesForProblemDetail() {
		return problemDetailMimeTypes;
	}

	@Override
	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
			ResolvableType elementType, @Nullable Map<String, Object> hints) {

		return (this.ssePrettyPrinter != null &&
				MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType) &&
				writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT) ?
				writer.with(this.ssePrettyPrinter) : writer);
	}

}
