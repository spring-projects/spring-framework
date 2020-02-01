package org.springframework.http.converter.cbor;

import kotlinx.serialization.cbor.Cbor;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.KotlinSerializationResolver;
import org.springframework.util.StreamUtils;

import java.io.IOException;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can read and write CBOR using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes.
 *
 * @author Andreas Ahlenstorf
 */
public class KotlinSerializationCborHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final KotlinSerializationResolver resolver = new KotlinSerializationResolver();

	public KotlinSerializationCborHttpMessageConverter() {
		super(MediaType.APPLICATION_CBOR);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		try {
			this.resolver.resolve(clazz);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected Object readInternal(
			Class<?> clazz,
			HttpInputMessage inputMessage
	) throws IOException, HttpMessageNotReadableException {
		try {
			byte[] payload = StreamUtils.copyToByteArray(inputMessage.getBody());
			return Cbor.Companion.load(this.resolver.resolve(clazz), payload);
		} catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not read CBOR: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void writeInternal(
			Object o,
			HttpOutputMessage outputMessage
	) throws IOException, HttpMessageNotWritableException {
		try {
			outputMessage.getBody().write(Cbor.Companion.dump(this.resolver.resolve(o.getClass()), o));
			outputMessage.getBody().flush();
		} catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write CBOR: " + ex.getMessage(), ex);
		}
	}
}
