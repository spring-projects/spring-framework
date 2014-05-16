package org.springframework.http.converter;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * An HttpMessageConverter that supports converting the value returned from a
 * method by incorporating {@link org.springframework.core.MethodParameter}
 * information into the conversion. Such a converter can for example take into
 * account information from method-level annotations.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface MethodParameterHttpMessageConverter<T> extends HttpMessageConverter<T> {

	boolean canRead(Class<?> clazz, MediaType mediaType, MethodParameter parameter);

	boolean canWrite(Class<?> clazz, MediaType mediaType, MethodParameter parameter);

	T read(Class<? extends T> clazz, HttpInputMessage inputMessage, MethodParameter parameter)
			throws IOException, HttpMessageNotReadableException;

	void write(T t, MediaType contentType, HttpOutputMessage outputMessage, MethodParameter parameter)
			throws IOException, HttpMessageNotWritableException;

}
