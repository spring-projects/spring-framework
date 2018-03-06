/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;

/**
 * Common base class for plain JSON converters, e.g. Gson and JSON-B.
 *
 * <p>Note that the Jackson converters have a dedicated class hierarchy
 * due to their multi-format support.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see GsonHttpMessageConverter
 * @see JsonbHttpMessageConverter
 * @see #readInternal(Type, Reader)
 * @see #writeInternal(Object, Type, Writer)
 */
public abstract class AbstractJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	@Nullable
	private String jsonPrefix;


	public AbstractJsonHttpMessageConverter() {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		setDefaultCharset(DEFAULT_CHARSET);
	}


	/**
	 * Specify a custom prefix to use for JSON output. Default is none.
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * Indicate whether the JSON output by this view should be prefixed with ")]}', ".
	 * Default is {@code false}.
	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON
	 * Hijacking. The prefix renders the string syntactically invalid as a script
	 * so that it cannot be hijacked.
	 * This prefix should be stripped before parsing the string as JSON.
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(GenericTypeResolver.resolveType(type, contextClass), inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(clazz, inputMessage);
	}

	private Object readResolved(Type resolvedType, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		Reader reader = getReader(inputMessage);
		try {
			return readInternal(resolvedType, reader);
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected final void writeInternal(Object o, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Writer writer = getWriter(outputMessage);
		if (this.jsonPrefix != null) {
			writer.append(this.jsonPrefix);
		}
		try {
			writeInternal(o, type, writer);
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
		writer.flush();
	}


	/**
	 * Template method that reads the JSON-bound object from the given {@link Reader}.
	 * @param resolvedType the resolved generic type
	 * @param reader the {@code} Reader to use
	 * @return the JSON-bound object
	 * @throws Exception in case of read/parse failures
	 */
	protected abstract Object readInternal(Type resolvedType, Reader reader) throws Exception;

	/**
	 * Template method that writes the JSON-bound object to the given {@link Writer}.
	 * @param o the object to write to the output message
	 * @param type the type of object to write (may be {@code null})
	 * @param writer the {@code} Writer to use
	 * @throws Exception in case of write failures
	 */
	protected abstract void writeInternal(Object o, @Nullable Type type, Writer writer) throws Exception;


	private static Reader getReader(HttpInputMessage inputMessage) throws IOException {
		return new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
	}

	private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
		return new OutputStreamWriter(outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
	}

	private static Charset getCharset(HttpHeaders headers) {
		Charset charset = (headers.getContentType() != null ? headers.getContentType().getCharset() : null);
		return (charset != null ? charset : DEFAULT_CHARSET);
	}

}
