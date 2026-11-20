/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.StringFormat;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 * Abstract base class for {@link HttpMessageConverter} implementations that
 * defer to Kotlin {@linkplain StringFormat string serializers}.
 *
 * <p>As of Spring Framework 7.0, by default it only encodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics level
 * since it allows combined usage with other general purpose converters without conflicts.
 * Alternative constructors with a {@code Predicate<ResolvableType>} parameter can be used
 * to customize this behavior.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link StringFormat}
 */
public abstract class KotlinSerializationStringHttpMessageConverter<T extends StringFormat>
		extends AbstractKotlinSerializationHttpMessageConverter<T> {


	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only converters types annotated with
	 * {@link kotlinx.serialization.Serializable @Serializable} at type or
	 * generics level.
	 */
	protected KotlinSerializationStringHttpMessageConverter(T format, MediaType... supportedMediaTypes) {
		super(format, supportedMediaTypes);
	}

	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only converts types for which the specified predicate returns
	 * {@code true}.
	 * @since 7.0
	 */
	protected KotlinSerializationStringHttpMessageConverter(T format, Predicate<ResolvableType> typePredicate, MediaType... supportedMediaTypes) {
		super(format, typePredicate, supportedMediaTypes);
	}


	@Override
	protected Object readInternal(KSerializer<Object> serializer, T format, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		Charset charset = charset(inputMessage.getHeaders().getContentType());
		String s = StreamUtils.copyToString(inputMessage.getBody(), charset);
		try {
			return format.decodeFromString(serializer, s);
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotReadableException("Could not read " + format + ": " + ex.getMessage(), ex,
					inputMessage);
		}
	}

	@Override
	protected void writeInternal(Object object, KSerializer<Object> serializer, T format,
			HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

		try {
			String s = format.encodeToString(serializer, object);
			Charset charset = charset(outputMessage.getHeaders().getContentType());
			outputMessage.getBody().write(s.getBytes(charset));
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotWritableException("Could not write " + format + ": " + ex.getMessage(), ex);
		}
	}

	private static Charset charset(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		return StandardCharsets.UTF_8;
	}
}
