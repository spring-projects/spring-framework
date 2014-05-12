package org.springframework.http.converter;

import org.springframework.core.MethodParameter;
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
public interface ReturnValueHttpMessageConverter<T> extends HttpMessageConverter<T> {


	boolean canWrite(Class<?> clazz, MediaType mediaType, MethodParameter parameter);

	void write(T t, MediaType contentType, HttpOutputMessage outputMessage, MethodParameter parameter)
			throws IOException, HttpMessageNotWritableException;

}
