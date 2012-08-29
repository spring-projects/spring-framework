package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write
 * numbers.<br>
 * Only basic primitives ({@link Byte}, {@link Short}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}) are supported.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class NumberHttpMessageConverter extends AbstractHttpMessageConverter<Number> {

	static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("text", "plain",
			StringHttpMessageConverter.DEFAULT_CHARSET);

	public NumberHttpMessageConverter() {
		super(DEFAULT_MEDIA_TYPE, MediaType.ALL);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == Byte.class || clazz == Short.class || clazz == Integer.class
				|| clazz == Long.class || clazz == Float.class || clazz == Double.class;
	}

	@Override
	protected Number readInternal(Class<? extends Number> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());

		String s = FileCopyUtils
				.copyToString(new InputStreamReader(inputMessage.getBody(), charset));

		try {
			return (Number) clazz.getMethod("valueOf", String.class).invoke(null, s);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof NumberFormatException) {
				throw new HttpMessageNotReadableException("String '" + s + "' is not parseable",
						e.getCause());
			}

			// should not occur
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			// should not occur
			throw new RuntimeException(e);
		}
		catch (NoSuchMethodException e) {
			// should not occur
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Long getContentLength(Number t, MediaType contentType) {
		Charset charset = getContentTypeCharset(contentType);

		return Long.valueOf(t.toString().getBytes(charset).length);
	}

	@Override
	protected void writeInternal(Number t, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		Charset charset = getContentTypeCharset(outputMessage.getHeaders().getContentType());

		FileCopyUtils.copy(t.toString(), new OutputStreamWriter(outputMessage.getBody(), charset));
	}

	private Charset getContentTypeCharset(MediaType contentType) {
		if (contentType != null && contentType.getCharSet() != null) {
			return contentType.getCharSet();
		}

		return StringHttpMessageConverter.DEFAULT_CHARSET;
	}
}
