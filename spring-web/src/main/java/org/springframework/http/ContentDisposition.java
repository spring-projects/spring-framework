/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.nio.charset.StandardCharsets.*;

/**
 * Represent the content disposition type and parameters as defined in RFC 2183.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183</a>
 */
public class ContentDisposition {

	private final String type;

	private final String name;

	private final String filename;

	private final Charset charset;

	private final Long size;


	/**
	 * Private constructor. See static factory methods in this class.
	 */
	private ContentDisposition(String type, String name, String filename, Charset charset, Long size) {
		this.type = type;
		this.name = name;
		this.filename = filename;
		this.charset = charset;
		this.size = size;
	}


	/**
	 * Return the disposition type, like for example {@literal inline}, {@literal attachment},
	 * {@literal form-data}, or {@code null} if not defined.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Return the value of the {@literal name} parameter, or {@code null} if not defined.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the value of the {@literal filename} parameter (or the value of the
	 * {@literal filename*} one decoded as defined in the RFC 5987), or {@code null} if not defined.
	 */
	public String getFilename() {
		return this.filename;
	}

	/**
	 * Return the charset defined in {@literal filename*} parameter, or {@code null} if not defined.
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Return the value of the {@literal size} parameter, or {@code null} if not defined.
	 */
	public Long getSize() {
		return this.size;
	}


	/**
	 * Return a builder for a {@code ContentDisposition}.
	 * @param type the disposition type like for example {@literal inline},
	 * {@literal attachment}, or {@literal form-data}
	 * @return the builder
	 */
	public static Builder builder(String type) {
		return new BuilderImpl(type);
	}

	/**
	 * Return an empty content disposition.
	 */
	public static ContentDisposition empty() {
		return new ContentDisposition(null, null, null, null, null);
	}

	/**
	 * Parse a {@literal Content-Disposition} header value as defined in RFC 2183.
	 * @param contentDisposition the {@literal Content-Disposition} header value
	 * @return the parsed content disposition
	 * @see #toString()
	 */
	public static ContentDisposition parse(String contentDisposition) {
		String[] parts = StringUtils.tokenizeToStringArray(contentDisposition, ";");
		Assert.isTrue(parts.length >= 1, "Content-Disposition header must not be empty");
		String type = parts[0];
		String name = null;
		String filename = null;
		Charset charset = null;
		Long size = null;
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			int eqIndex = part.indexOf('=');
			if (eqIndex != -1) {
				String attribute = part.substring(0, eqIndex);
				String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ?
						part.substring(eqIndex + 2, part.length() - 1) :
						part.substring(eqIndex + 1, part.length()));
				if (attribute.equals("name") ) {
					name = value;
				}
				else if (attribute.equals("filename*") ) {
					filename = decodeHeaderFieldParam(value);
					charset = Charset.forName(value.substring(0, value.indexOf("'")));
					Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
							"Charset should be UTF-8 or ISO-8859-1");
				}
				else if (attribute.equals("filename") && (filename == null)) {
					filename = value;
				}
				else if (attribute.equals("size") ) {
					size = Long.parseLong(value);
				}
			}
			else {
				throw new IllegalArgumentException("Invalid content disposition format");
			}
		}
		return new ContentDisposition(type, name, filename, charset, size);
	}

	/**
	 * Decode the given header field param as describe in RFC 5987.
	 * <p>Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
	 * @param input the header field param
	 * @return the encoded header field param
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String decodeHeaderFieldParam(String input) {
		Assert.notNull(input, "Input String should not be null");
		int firstQuoteIndex = input.indexOf("'");
		int secondQuoteIndex = input.indexOf("'", firstQuoteIndex + 1);
		// US_ASCII
		if (firstQuoteIndex == -1 || secondQuoteIndex == -1) {
			return input;
		}
		Charset charset = Charset.forName(input.substring(0, firstQuoteIndex));
		Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
				"Charset should be UTF-8 or ISO-8859-1");
		byte[] value = input.substring(secondQuoteIndex + 1, input.length()).getBytes(charset);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int index = 0;
		while (index < value.length) {
			byte b = value[index];
			if (isRFC5987AttrChar(b)) {
				bos.write((char) b);
				index++;
			}
			else if (b == '%') {
				char[] array = { (char)value[index + 1], (char)value[index + 2]};
				bos.write(Integer.parseInt(String.valueOf(array), 16));
				index+=3;
			}
			else {
				throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987)");
			}
		}
		return new String(bos.toByteArray(), charset);
	}

	private static boolean isRFC5987AttrChar(byte c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
				c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ContentDisposition that = (ContentDisposition) o;
		if (type != null ? !type.equals(that.type) : that.type != null) {
			return false;
		}
		if (name != null ? !name.equals(that.name) : that.name != null) {
			return false;
		}
		if (filename != null ? !filename.equals(that.filename) : that.filename != null) {
			return false;
		}
		if (charset != null ? !charset.equals(that.charset) : that.charset != null) {
			return false;
		}
		return size != null ? size.equals(that.size) : that.size == null;
	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (filename != null ? filename.hashCode() : 0);
		result = 31 * result + (charset != null ? charset.hashCode() : 0);
		result = 31 * result + (size != null ? size.hashCode() : 0);
		return result;
	}

	/**
	 * Return the header value for this content disposition as defined in RFC 2183.
	 * @see #parse(String)
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(this.type);
		if (this.name != null) {
			builder.append("; name=\"");
			builder.append(this.name).append('\"');
		}
		if (this.filename != null) {
			if(this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
				builder.append("; filename=\"");
				builder.append(this.filename).append('\"');
			}
			else {
				builder.append("; filename*=");
				builder.append(encodeHeaderFieldParam(this.filename, this.charset));
			}
		}
		if (this.size != null) {
			builder.append("; size=");
			builder.append(this.size);
		}
		return builder.toString();
	}

	/**
	 * Encode the given header field param as describe in RFC 5987.
	 * @param input the header field param
	 * @param charset the charset of the header field param string,
	 * only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported
	 * @return the encoded header field param
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String encodeHeaderFieldParam(String input, Charset charset) {
		Assert.notNull(input, "Input String should not be null");
		Assert.notNull(charset, "Charset should not be null");
		if (StandardCharsets.US_ASCII.equals(charset)) {
			return input;
		}
		Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
				"Charset should be UTF-8 or ISO-8859-1");
		byte[] source = input.getBytes(charset);
		int len = source.length;
		StringBuilder sb = new StringBuilder(len << 1);
		sb.append(charset.name());
		sb.append("''");
		for (byte b : source) {
			if (isRFC5987AttrChar(b)) {
				sb.append((char) b);
			}
			else {
				sb.append('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				sb.append(hex1);
				sb.append(hex2);
			}
		}
		return sb.toString();
	}


	/**
	 * A mutable builder for {@code ContentDisposition}.
	 */
	public interface Builder {

		/**
		 * Set the value of the {@literal name} parameter
		 */
		Builder name(String name);

		/**
		 * Set the value of the {@literal filename} parameter
		 */
		Builder filename(String filename);

		/**
		 * Set the value of the {@literal filename*} that will be encoded as defined in
		 * the RFC 5987. Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
		 */
		Builder filename(String filename, Charset charset);

		/**
		 * Set the value of the {@literal size} parameter
		 */
		Builder size(Long size);

		/**
		 * Build the content disposition
		 */
		ContentDisposition build();

	}

	private static class BuilderImpl implements Builder {

		private String type;

		private String name;

		private String filename;

		private Charset charset;

		private Long size;

		public BuilderImpl(String type) {
			Assert.hasText(type, "'type' must not be not empty");
			this.type = type;
		}

		@Override
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder filename(String filename) {
			this.filename = filename;
			return this;
		}

		@Override
		public Builder filename(String filename, Charset charset) {
			this.filename = filename;
			this.charset = charset;
			return this;
		}

		@Override
		public Builder size(Long size) {
			this.size = size;
			return this;
		}

		@Override
		public ContentDisposition build() {
			return new ContentDisposition(this.type, this.name, this.filename, this.charset, this.size);
		}
	}

}
