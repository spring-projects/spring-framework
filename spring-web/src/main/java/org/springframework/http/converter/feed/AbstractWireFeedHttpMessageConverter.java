/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.feed;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import com.rometools.rome.io.WireFeedOutput;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for Atom and RSS Feed message converters, using the
 * <a href="https://github.com/rometools/rome">ROME tools</a> project.
 *
 * <p><b>NOTE: As of Spring 4.1, this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @param <T> the converted object type
 * @see AtomFeedHttpMessageConverter
 * @see RssChannelHttpMessageConverter
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed>
		extends AbstractHttpMessageConverter<T> {

	/**
	 * The default charset used by the converter.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		WireFeedInput feedInput = new WireFeedInput();
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : DEFAULT_CHARSET);
		try {
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			Reader reader = new InputStreamReader(inputStream, charset);
			return (T) feedInput.build(reader);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Charset charset = (StringUtils.hasLength(wireFeed.getEncoding()) ?
				Charset.forName(wireFeed.getEncoding()) : DEFAULT_CHARSET);
		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType != null) {
			contentType = new MediaType(contentType, charset);
			outputMessage.getHeaders().setContentType(contentType);
		}

		WireFeedOutput feedOutput = new WireFeedOutput();
		try {
			Writer writer = new OutputStreamWriter(outputMessage.getBody(), charset);
			feedOutput.output(wireFeed, writer);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotWritableException("Could not write WireFeed: " + ex.getMessage(), ex);
		}
	}

}
