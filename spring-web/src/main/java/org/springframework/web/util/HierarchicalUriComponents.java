/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.util;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link UriComponents} for hierarchical URIs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1.3
 * @see <a href="https://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical URIs</a>
 */
@SuppressWarnings("serial")
final class HierarchicalUriComponents extends UriComponents {

	private static final char PATH_DELIMITER = '/';

	private static final String PATH_DELIMITER_STRING = String.valueOf(PATH_DELIMITER);

	private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS =
			CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());


	/**
	 * Represents an empty path.
	 */
	static final PathComponent NULL_PATH_COMPONENT = new PathComponent() {
		@Override
		public String getPath() {
			return "";
		}
		@Override
		public List<String> getPathSegments() {
			return Collections.emptyList();
		}
		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			return this;
		}
		@Override
		public void verify() {
		}
		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			return this;
		}
		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		}
		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other);
		}
		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};


	private final @Nullable String userInfo;

	private final @Nullable String host;

	private final @Nullable String port;

	private final PathComponent path;

	private final MultiValueMap<String, String> queryParams;

	private final EncodeState encodeState;

	private @Nullable UnaryOperator<String> variableEncoder;


	/**
	 * Package-private constructor. All arguments are optional, and can be {@code null}.
	 * @param scheme the scheme
	 * @param userInfo the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query the query parameters
	 * @param fragment the fragment
	 * @param encoded whether the components are already encoded
	 */
	HierarchicalUriComponents(@Nullable String scheme, @Nullable String fragment, @Nullable String userInfo,
			@Nullable String host, @Nullable String port, @Nullable PathComponent path,
			@Nullable MultiValueMap<String, String> query, boolean encoded) {

		super(scheme, fragment);

		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = path != null ? path : NULL_PATH_COMPONENT;
		this.queryParams = query != null ? CollectionUtils.unmodifiableMultiValueMap(query) : EMPTY_QUERY_PARAMS;
		this.encodeState = encoded ? EncodeState.FULLY_ENCODED : EncodeState.RAW;

		// Check for illegal characters..
		if (encoded) {
			verify();
		}
	}

	private HierarchicalUriComponents(@Nullable String scheme, @Nullable String fragment,
			@Nullable String userInfo, @Nullable String host, @Nullable String port,
			PathComponent path, MultiValueMap<String, String> queryParams,
			EncodeState encodeState, @Nullable UnaryOperator<String> variableEncoder) {

		super(scheme, fragment);

		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = path;
		this.queryParams = queryParams;
		this.encodeState = encodeState;
		this.variableEncoder = variableEncoder;
	}


	// Component getters

	@Override
	public @Nullable String getSchemeSpecificPart() {
		return null;
	}

	@Override
	public @Nullable String getUserInfo() {
		return this.userInfo;
	}

	@Override
	public @Nullable String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		if (this.port == null) {
			return -1;
		}
		else if (this.port.contains("{")) {
			throw new IllegalStateException(
					"The port contains a URI variable but has not been expanded yet: " + this.port);
		}
		try {
			return Integer.parseInt(this.port);
		}
		catch (NumberFormatException ex) {
			throw new IllegalStateException("The port must be an integer: " + this.port);
		}
	}

	@Override
	public String getPath() {
		return this.path.getPath();
	}

	@Override
	public List<String> getPathSegments() {
		return this.path.getPathSegments();
	}

	@Override
	public @Nullable String getQuery() {
		if (!this.queryParams.isEmpty()) {
			StringBuilder queryBuilder = new StringBuilder();
			this.queryParams.forEach((name, values) -> {
				if (CollectionUtils.isEmpty(values)) {
					if (queryBuilder.length() != 0) {
						queryBuilder.append('&');
					}
					queryBuilder.append(name);
				}
				else {
					for (Object value : values) {
						if (queryBuilder.length() != 0) {
							queryBuilder.append('&');
						}
						queryBuilder.append(name);
						if (value != null) {
							queryBuilder.append('=').append(value.toString());
						}
					}
				}
			});
			return queryBuilder.toString();
		}
		else {
			return null;
		}
	}

	/**
	 * Return the map of query parameters. Empty if no query has been set.
	 */
	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}


	// Encoding

	/**
	 * Identical to {@link #encode()} but skipping over URI variable placeholders.
	 * Also {@link #variableEncoder} is initialized with the given charset for
	 * use later when URI variables are expanded.
	 */
	HierarchicalUriComponents encodeTemplate(Charset charset) {
		if (this.encodeState.isEncoded()) {
			return this;
		}

		// Remember the charset to encode URI variables later..
		this.variableEncoder = value -> encodeUriComponent(value, charset, Type.URI);

		UriTemplateEncoder encoder = new UriTemplateEncoder(charset);
		String schemeTo = (getScheme() != null ? encoder.apply(getScheme(), Type.SCHEME) : null);
		String fragmentTo = (getFragment() != null ? encoder.apply(getFragment(), Type.FRAGMENT) : null);
		String userInfoTo = (getUserInfo() != null ? encoder.apply(getUserInfo(), Type.USER_INFO) : null);
		String hostTo = (getHost() != null ? encoder.apply(getHost(), getHostType()) : null);
		PathComponent pathTo = this.path.encode(encoder);
		MultiValueMap<String, String> queryParamsTo = encodeQueryParams(encoder);

		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, this.port, pathTo, queryParamsTo, EncodeState.TEMPLATE_ENCODED, this.variableEncoder);
	}

	@Override
	public HierarchicalUriComponents encode(Charset charset) {
		if (this.encodeState.isEncoded()) {
			return this;
		}
		String scheme = getScheme();
		String fragment = getFragment();
		String schemeTo = (scheme != null ? encodeUriComponent(scheme, charset, Type.SCHEME) : null);
		String fragmentTo = (fragment != null ? encodeUriComponent(fragment, charset, Type.FRAGMENT) : null);
		String userInfoTo = (this.userInfo != null ? encodeUriComponent(this.userInfo, charset, Type.USER_INFO) : null);
		String hostTo = (this.host != null ? encodeUriComponent(this.host, charset, getHostType()) : null);
		BiFunction<String, Type, String> encoder = (s, type) -> encodeUriComponent(s, charset, type);
		PathComponent pathTo = this.path.encode(encoder);
		MultiValueMap<String, String> queryParamsTo = encodeQueryParams(encoder);

		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, this.port, pathTo, queryParamsTo, EncodeState.FULLY_ENCODED, null);
	}

	private MultiValueMap<String, String> encodeQueryParams(BiFunction<String, Type, String> encoder) {
		int size = this.queryParams.size();
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(size);
		this.queryParams.forEach((key, values) -> {
			String name = encoder.apply(key, Type.QUERY_PARAM);
			List<String> encodedValues = new ArrayList<>(values.size());
			for (String value : values) {
				encodedValues.add(value != null ? encoder.apply(value, Type.QUERY_PARAM) : null);
			}
			result.put(name, encodedValues);
		});
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	/**
	 * Encode the given source into an encoded String using the rules specified
	 * by the given component and with the given options.
	 * @param source the source String
	 * @param encoding the encoding of the source String
	 * @param type the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given value is not a valid URI component
	 */
	static String encodeUriComponent(String source, String encoding, Type type) {
		return encodeUriComponent(source, Charset.forName(encoding), type);
	}

	/**
	 * Encode the given source into an encoded String using the rules specified
	 * by the given component and with the given options.
	 * @param source the source String
	 * @param charset the encoding of the source String
	 * @param type the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given value is not a valid URI component
	 */
	static String encodeUriComponent(String source, Charset charset, Type type) {
		if (!StringUtils.hasLength(source)) {
			return source;
		}
		Assert.notNull(charset, "Charset must not be null");
		Assert.notNull(type, "Type must not be null");

		byte[] bytes = source.getBytes(charset);
		boolean original = true;
		for (byte b : bytes) {
			if (!type.isAllowed(b)) {
				original = false;
				break;
			}
		}
		if (original) {
			return source;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
		for (byte b : bytes) {
			if (type.isAllowed(b)) {
				baos.write(b);
			}
			else {
				baos.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				baos.write(hex1);
				baos.write(hex2);
			}
		}
		return StreamUtils.copyToString(baos, charset);
	}

	private Type getHostType() {
		return (this.host != null && this.host.startsWith("[") ? Type.HOST_IPV6 : Type.HOST_IPV4);
	}

	// Verifying

	/**
	 * Check if any of the URI components contain any illegal characters.
	 * @throws IllegalArgumentException if any component has illegal characters
	 */
	private void verify() {
		verifyUriComponent(getScheme(), Type.SCHEME);
		verifyUriComponent(this.userInfo, Type.USER_INFO);
		verifyUriComponent(this.host, getHostType());
		this.path.verify();
		this.queryParams.forEach((key, values) -> {
			verifyUriComponent(key, Type.QUERY_PARAM);
			for (String value : values) {
				verifyUriComponent(value, Type.QUERY_PARAM);
			}
		});
		verifyUriComponent(getFragment(), Type.FRAGMENT);
	}

	private static void verifyUriComponent(@Nullable String source, Type type) {
		if (source == null) {
			return;
		}
		int length = source.length();
		for (int i = 0; i < length; i++) {
			char ch = source.charAt(i);
			if (ch == '%') {
				if ((i + 2) < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" +
								source.substring(i) + "\"");
					}
					i += 2;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" +
							source.substring(i) + "\"");
				}
			}
			else if (!type.isAllowed(ch)) {
				throw new IllegalArgumentException("Invalid character '" + ch + "' for " +
						type.name() + " in \"" + source + "\"");
			}
		}
	}


	// Expanding

	@Override
	protected HierarchicalUriComponents expandInternal(UriTemplateVariables uriVariables) {
		Assert.state(!this.encodeState.equals(EncodeState.FULLY_ENCODED),
				"URI components already encoded, and could not possibly contain '{' or '}'.");

		// Array-based vars rely on the order below...
		String schemeTo = expandUriComponent(getScheme(), uriVariables, this.variableEncoder);
		String userInfoTo = expandUriComponent(this.userInfo, uriVariables, this.variableEncoder);
		String hostTo = expandUriComponent(this.host, uriVariables, this.variableEncoder);
		String portTo = expandUriComponent(this.port, uriVariables, this.variableEncoder);
		PathComponent pathTo = this.path.expand(uriVariables, this.variableEncoder);
		MultiValueMap<String, String> queryParamsTo = expandQueryParams(uriVariables);
		String fragmentTo = expandUriComponent(getFragment(), uriVariables, this.variableEncoder);

		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, portTo, pathTo, queryParamsTo, this.encodeState, this.variableEncoder);
	}

	private MultiValueMap<String, String> expandQueryParams(UriTemplateVariables variables) {
		int size = this.queryParams.size();
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(size);
		UriTemplateVariables queryVariables = new QueryUriTemplateVariables(variables);
		this.queryParams.forEach((key, values) -> {
			String name = expandUriComponent(key, queryVariables, this.variableEncoder);
			List<String> expandedValues = result.computeIfAbsent(name, k -> new ArrayList<>(values.size()));
			for (String value : values) {
				expandedValues.add(expandUriComponent(value, queryVariables, this.variableEncoder));
			}
		});
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Override
	public UriComponents normalize() {
		String normalizedPath = StringUtils.cleanPath(getPath());
		FullPathComponent path = new FullPathComponent(normalizedPath);
		return new HierarchicalUriComponents(getScheme(), getFragment(), this.userInfo, this.host, this.port,
				path, this.queryParams, this.encodeState, this.variableEncoder);
	}


	// Other functionality

	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();
		if (getScheme() != null) {
			uriBuilder.append(getScheme()).append(':');
		}
		if (this.userInfo != null || this.host != null) {
			uriBuilder.append("//");
			if (this.userInfo != null) {
				uriBuilder.append(this.userInfo).append('@');
			}
			if (this.host != null) {
				uriBuilder.append(this.host);
			}
			if (StringUtils.hasText(this.port) && !this.port.equals("-1")) {
				uriBuilder.append(':').append(this.port);
			}
		}
		String path = getPath();
		if (StringUtils.hasLength(path)) {
			if (uriBuilder.length() != 0 && path.charAt(0) != PATH_DELIMITER) {
				uriBuilder.append(PATH_DELIMITER);
			}
			uriBuilder.append(path);
		}
		String query = getQuery();
		if (query != null) {
			uriBuilder.append('?').append(query);
		}
		if (getFragment() != null) {
			uriBuilder.append('#').append(getFragment());
		}
		return uriBuilder.toString();
	}

	@Override
	public URI toUri() {
		try {
			if (this.encodeState.isEncoded()) {
				return new URI(toUriString());
			}
			else {
				String path = getPath();
				if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
					// Only prefix the path delimiter if something exists before it
					if (getScheme() != null || getUserInfo() != null || getHost() != null || getPort() != -1) {
						path = PATH_DELIMITER + path;
					}
				}
				return new URI(getScheme(), getUserInfo(), getHost(), getPort(), path, getQuery(), getFragment());
			}
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		if (getScheme() != null) {
			builder.scheme(getScheme());
		}
		if (getUserInfo() != null) {
			builder.userInfo(getUserInfo());
		}
		if (getHost() != null) {
			builder.host(getHost());
		}
		// Avoid parsing the port, may have URI variable.
		if (this.port != null) {
			builder.port(this.port);
		}
		this.path.copyToUriComponentsBuilder(builder);
		if (!getQueryParams().isEmpty()) {
			builder.queryParams(getQueryParams());
		}
		if (getFragment() != null) {
			builder.fragment(getFragment());
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof HierarchicalUriComponents that &&
				ObjectUtils.nullSafeEquals(getScheme(), that.getScheme()) &&
				ObjectUtils.nullSafeEquals(getUserInfo(), that.getUserInfo()) &&
				ObjectUtils.nullSafeEquals(getHost(), that.getHost()) &&
				getPort() == that.getPort() &&
				this.path.equals(that.path) &&
				this.queryParams.equals(that.queryParams) &&
				ObjectUtils.nullSafeEquals(getFragment(), that.getFragment())));
	}

	@Override
	public int hashCode() {
		return Objects.hash(getScheme(), this.userInfo, this.host, this.port,
				this.path, this.queryParams, getFragment());
	}


	// Nested types

	/**
	 * Enumeration used to identify the allowed characters per URI component.
	 * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
	 * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
	 */
	enum Type {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		AUTHORITY {
			@Override
			public boolean isAllowed(int c) {
				return (isUnreservedOrSubDelimiter(c) || ':' == c || '@' == c);
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				return (isUnreservedOrSubDelimiter(c) || ':' == c);
			}
		},
		HOST_IPV4 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreservedOrSubDelimiter(c);
			}
		},
		HOST_IPV6 {
			@Override
			public boolean isAllowed(int c) {
				return (isUnreservedOrSubDelimiter(c) || '[' == c || ']' == c || ':' == c);
			}
		},
		PORT {
			@Override
			public boolean isAllowed(int c) {
				return isDigit(c);
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				return (isPchar(c) || '/' == c);
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY {
			@Override
			public boolean isAllowed(int c) {
				return (isPchar(c) || '/' == c || '?' == c);
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '&' == c) {
					return false;
				}
				else {
					return (isPchar(c) || '/' == c || '?' == c);
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				return (isPchar(c) || '/' == c || '?' == c);
			}
		},
		URI {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c);
			}
		};

		private static final boolean[] unreservedOrSubDelimiterArray = new boolean[128];

		static {
			for (int i = 0; i < 128; i++) {
				char c = (char) i;
				unreservedOrSubDelimiterArray[i] = (URI.isUnreserved(c) || URI.isSubDelimiter(c));
			}
		}

		/**
		 * Indicates whether the given character is allowed in this URI component.
		 * @return {@code true} if the character is allowed; {@code false} otherwise
		 */
		public abstract boolean isAllowed(int c);

		/**
		 * Indicates whether the given character is in the {@code ALPHA} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isAlpha(int c) {
			return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
		}

		/**
		 * Indicates whether the given character is in the {@code DIGIT} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isDigit(int c) {
			return (c >= '0' && c <= '9');
		}

		/**
		 * Indicates whether the given character is in the {@code gen-delims} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isGenericDelimiter(int c) {
			return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
		}

		/**
		 * Indicates whether the given character is in the {@code sub-delims} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isSubDelimiter(int c) {
			return ('!' == c || '$' == c || '&' == c || '\'' == c ||
					'(' == c || ')' == c || '*' == c || '+' == c || ',' == c || ';' == c || '=' == c);
		}

		/**
		 * Indicates whether the given character is in the {@code reserved} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isReserved(int c) {
			return (isGenericDelimiter(c) || isSubDelimiter(c));
		}

		/**
		 * Indicates whether the given character is in the {@code unreserved} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isUnreserved(int c) {
			return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
		}

		/**
		 * Indicates whether the given character is in the {@code pchar} set.
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isPchar(int c) {
			return (isUnreservedOrSubDelimiter(c) || ':' == c || '@' == c);
		}

		/**
		 * Combined check whether a character is unreserved or a sub-delimiter.
		 */
		protected boolean isUnreservedOrSubDelimiter(int c) {
			return (c < unreservedOrSubDelimiterArray.length && c >= 0 && unreservedOrSubDelimiterArray[c]);
		}

	}


	private enum EncodeState {

		/**
		 * Not encoded.
		 */
		RAW,

		/**
		 * URI vars expanded first and then each URI component encoded by
		 * quoting only illegal characters within that URI component.
		 */
		FULLY_ENCODED,

		/**
		 * URI template encoded first by quoting illegal characters only, and
		 * then URI vars encoded more strictly when expanded, by quoting both
		 * illegal chars and chars with reserved meaning.
 		 */
		TEMPLATE_ENCODED;


		public boolean isEncoded() {
			return this.equals(FULLY_ENCODED) || this.equals(TEMPLATE_ENCODED);
		}
	}


	private static class UriTemplateEncoder implements BiFunction<String, Type, String> {

		private final Charset charset;

		private final StringBuilder currentLiteral = new StringBuilder();

		private final StringBuilder currentVariable = new StringBuilder();

		private final StringBuilder output = new StringBuilder();

		private boolean variableWithNameAndRegex;

		public UriTemplateEncoder(Charset charset) {
			this.charset = charset;
		}

		@Override
		public String apply(String source, Type type) {
			// URI variable only?
			if (isUriVariable(source)) {
				return source;
			}
			// Literal template only?
			if (source.indexOf('{') == -1) {
				return encodeUriComponent(source, this.charset, type);
			}
			int level = 0;
			clear(this.currentLiteral);
			clear(this.currentVariable);
			clear(this.output);
			for (int i = 0; i < source.length(); i++) {
				char c = source.charAt(i);
				if (c == ':' && level == 1) {
					this.variableWithNameAndRegex = true;
				}
				if (c == '{') {
					level++;
					if (level == 1) {
						append(this.currentLiteral, true, type);
					}
				}
				if (c == '}' && level > 0) {
					level--;
					this.currentVariable.append('}');
					if (level == 0) {
						boolean encode = !isUriVariable(this.currentVariable);
						append(this.currentVariable, encode, type);
					}
					else if (!this.variableWithNameAndRegex) {
						append(this.currentVariable, true, type);
						level = 0;
					}
				}
				else if (level > 0) {
					this.currentVariable.append(c);
				}
				else {
					this.currentLiteral.append(c);
				}
			}
			if (level > 0) {
				this.currentLiteral.append(this.currentVariable);
			}
			append(this.currentLiteral, true, type);
			return this.output.toString();
		}

		/**
		 * Whether the given String is a single URI variable that can be
		 * expanded. It must have '{' and '}' surrounding non-empty text and no
		 * nested placeholders unless it is a variable with regex syntax,
		 * for example, {@code "/{year:\d{1,4}}"}.
		 */
		private boolean isUriVariable(CharSequence source) {
			if (source.length() < 2 || source.charAt(0) != '{' || source.charAt(source.length() -1) != '}') {
				return false;
			}
			boolean hasText = false;
			for (int i = 1; i < source.length() - 1; i++) {
				char c = source.charAt(i);
				if (c == ':' && i > 1) {
					return true;
				}
				if (c == '{' || c == '}') {
					return false;
				}
				hasText = (hasText || !Character.isWhitespace(c));
			}
			return hasText;
		}

		private void append(StringBuilder sb, boolean encode, Type type) {
			this.output.append(encode ? encodeUriComponent(sb.toString(), this.charset, type) : sb);
			clear(sb);
			this.variableWithNameAndRegex = false;
		}

		private void clear(StringBuilder sb) {
			sb.delete(0, sb.length());
		}
	}


	/**
	 * Defines the contract for path (segments).
	 */
	interface PathComponent extends Serializable {

		String getPath();

		List<String> getPathSegments();

		PathComponent encode(BiFunction<String, Type, String> encoder);

		void verify();

		PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder);

		void copyToUriComponentsBuilder(UriComponentsBuilder builder);
	}


	/**
	 * Represents a path backed by a String.
	 */
	static final class FullPathComponent implements PathComponent {

		private final String path;

		public FullPathComponent(@Nullable String path) {
			this.path = (path != null ? path : "");
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public List<String> getPathSegments() {
			String[] segments = StringUtils.tokenizeToStringArray(getPath(), PATH_DELIMITER_STRING);
			return List.of(segments);
		}

		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			String encodedPath = encoder.apply(getPath(), Type.PATH);
			return new FullPathComponent(encodedPath);
		}

		@Override
		public void verify() {
			verifyUriComponent(getPath(), Type.PATH);
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			String expandedPath = expandUriComponent(getPath(), uriVariables, encoder);
			return new FullPathComponent(expandedPath);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.path(getPath());
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof FullPathComponent fullPathComponent &&
					getPath().equals(fullPathComponent.getPath())));
		}

		@Override
		public int hashCode() {
			return getPath().hashCode();
		}
	}


	/**
	 * Represents a path backed by a String list (i.e. path segments).
	 */
	static final class PathSegmentComponent implements PathComponent {

		private final List<String> pathSegments;

		public PathSegmentComponent(List<String> pathSegments) {
			Assert.notNull(pathSegments, "List must not be null");
			this.pathSegments = List.copyOf(pathSegments);
		}

		@Override
		public String getPath() {
			String delimiter = PATH_DELIMITER_STRING;
			StringJoiner pathBuilder = new StringJoiner(delimiter, delimiter, "");
			for (String pathSegment : this.pathSegments) {
				pathBuilder.add(pathSegment);
			}
			return pathBuilder.toString();
		}

		@Override
		public List<String> getPathSegments() {
			return this.pathSegments;
		}

		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			List<String> pathSegments = getPathSegments();
			List<String> encodedPathSegments = new ArrayList<>(pathSegments.size());
			for (String pathSegment : pathSegments) {
				String encodedPathSegment = encoder.apply(pathSegment, Type.PATH_SEGMENT);
				encodedPathSegments.add(encodedPathSegment);
			}
			return new PathSegmentComponent(encodedPathSegments);
		}

		@Override
		public void verify() {
			for (String pathSegment : getPathSegments()) {
				verifyUriComponent(pathSegment, Type.PATH_SEGMENT);
			}
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			List<String> pathSegments = getPathSegments();
			List<String> expandedPathSegments = new ArrayList<>(pathSegments.size());
			for (String pathSegment : pathSegments) {
				String expandedPathSegment = expandUriComponent(pathSegment, uriVariables, encoder);
				expandedPathSegments.add(expandedPathSegment);
			}
			return new PathSegmentComponent(expandedPathSegments);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.pathSegment(StringUtils.toStringArray(getPathSegments()));
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof PathSegmentComponent pathSegmentComponent &&
					getPathSegments().equals(pathSegmentComponent.getPathSegments())));
		}

		@Override
		public int hashCode() {
			return getPathSegments().hashCode();
		}
	}


	/**
	 * Represents a collection of PathComponents.
	 */
	static final class PathComponentComposite implements PathComponent {

		private final List<PathComponent> pathComponents;

		public PathComponentComposite(List<PathComponent> pathComponents) {
			Assert.notNull(pathComponents, "PathComponent List must not be null");
			this.pathComponents = pathComponents;
		}

		@Override
		public String getPath() {
			StringBuilder pathBuilder = new StringBuilder();
			for (PathComponent pathComponent : this.pathComponents) {
				pathBuilder.append(pathComponent.getPath());
			}
			return pathBuilder.toString();
		}

		@Override
		public List<String> getPathSegments() {
			List<String> result = new ArrayList<>();
			for (PathComponent pathComponent : this.pathComponents) {
				result.addAll(pathComponent.getPathSegments());
			}
			return result;
		}

		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			List<PathComponent> encodedComponents = new ArrayList<>(this.pathComponents.size());
			for (PathComponent pathComponent : this.pathComponents) {
				encodedComponents.add(pathComponent.encode(encoder));
			}
			return new PathComponentComposite(encodedComponents);
		}

		@Override
		public void verify() {
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.verify();
			}
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			List<PathComponent> expandedComponents = new ArrayList<>(this.pathComponents.size());
			for (PathComponent pathComponent : this.pathComponents) {
				expandedComponents.add(pathComponent.expand(uriVariables, encoder));
			}
			return new PathComponentComposite(expandedComponents);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.copyToUriComponentsBuilder(builder);
			}
		}
	}


	private static class QueryUriTemplateVariables implements UriTemplateVariables {

		private final UriTemplateVariables delegate;

		public QueryUriTemplateVariables(UriTemplateVariables delegate) {
			this.delegate = delegate;
		}

		@Override
		public @Nullable Object getValue(@Nullable String name) {
			Object value = this.delegate.getValue(name);
			if (ObjectUtils.isArray(value)) {
				value = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(value));
			}
			else if (value instanceof Collection<?> collection) {
				value = StringUtils.collectionToCommaDelimitedString(collection);
			}
			return value;
		}
	}

}
