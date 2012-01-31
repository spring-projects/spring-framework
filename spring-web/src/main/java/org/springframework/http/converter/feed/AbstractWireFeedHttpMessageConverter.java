/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.converter.feed;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import com.sun.syndication.io.WireFeedOutput;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for Atom and RSS Feed message converters, using java.net's
 * <a href="https://rome.dev.java.net/">ROME</a> package.
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @see AtomFeedHttpMessageConverter
 * @see RssChannelHttpMessageConverter
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed> extends AbstractHttpMessageConverter<T> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		WireFeedInput feedInput = new WireFeedInput();
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset;
		if (contentType != null && contentType.getCharSet() != null) {
			charset = contentType.getCharSet();
		} else {
			charset = DEFAULT_CHARSET;
		}
		try {
			Reader reader = new InputStreamReader(inputMessage.getBody(), charset);
			return (T) feedInput.build(reader);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		String wireFeedEncoding = wireFeed.getEncoding();
		if (!StringUtils.hasLength(wireFeedEncoding)) {
			wireFeedEncoding = DEFAULT_CHARSET.name();
		}
		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType != null) {
			Charset wireFeedCharset = Charset.forName(wireFeedEncoding);
			contentType = new MediaType(contentType.getType(), contentType.getSubtype(), wireFeedCharset);
			outputMessage.getHeaders().setContentType(contentType);
		}

		WireFeedOutput feedOutput = new WireFeedOutput();

		try {
			Writer writer = new OutputStreamWriter(outputMessage.getBody(), wireFeedEncoding);
			feedOutput.output(wireFeed, writer);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotWritableException("Could not write WiredFeed: " + ex.getMessage(), ex);
		}
	}
}
