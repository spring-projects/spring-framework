/*
 * Copyright 2002-present the original author or authors.
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * Utility class to assist with processing "Forwarded" and "X-Forwarded-*" headers.
 *
 * <p><strong>Note:</strong> There are security considerations surrounding the use
 * of forwarded headers. Those should not be used unless the application is
 * behind a trusted proxy that inserts them and also explicitly removes any such
 * headers coming from an external source.
 *
 * <p>In most cases, you should not use this class directly but rather rely on
 * {@link org.springframework.web.filter.ForwardedHeaderFilter} for Spring MVC or
 * {@link org.springframework.web.server.adapter.ForwardedHeaderTransformer} in
 * order to extract the information from the headers as early as possible and discard
 * such headers. Underlying servers such as Tomcat, Jetty, and Reactor Netty also
 * provide options to handle forwarded headers even earlier.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public abstract class ForwardedHeaderUtils {

	private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);

	private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);

	private static final Pattern FORWARDED_BY_PATTERN = Pattern.compile("(?i:by)=" + FORWARDED_VALUE);


	/**
	 * Parse the "Forwarded" header.
	 * @param uri the request {@code URI}
	 * @param headers the HTTP headers to get the "Forwarded" header from
	 * @param remoteAddress for a default port for the parsed "for" value
	 * @param localAddress for a default port for the parsed "by" value
	 * @return a {@link ForwardedInfo} with the parse results
	 * @since 7.1
	 * @see <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>
	 */
	public static ForwardedInfo parseStandardHeader(URI uri, HttpHeaders headers,
			@Nullable InetSocketAddress remoteAddress, @Nullable InetSocketAddress localAddress) {

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri);
		InetSocketAddress forAddress = null;
		InetSocketAddress byAddress = null;

		String forwardedHeader = headers.getFirst("Forwarded");
		if (StringUtils.hasText(forwardedHeader)) {
			Map<String, String> pairs = parseFirstElement(forwardedHeader);

			String proto = pairs.get("proto");
			if (proto != null) {
				uriComponentsBuilder.scheme(proto);
				uriComponentsBuilder.port(null);
			}
			String host = pairs.get("host");
			if (host != null) {
				adaptForwardedHost(uriComponentsBuilder, host);
			}
			uriComponentsBuilder.resetPortIfDefaultForScheme();

			String forValue = pairs.get("for");
			if (forValue != null) {
				forAddress = parseInetSocketAddress(forValue, getPortToUse(remoteAddress, uri));
			}
			String byValue = pairs.get("by");
			if (byValue != null) {
				byAddress = parseInetSocketAddress(byValue, getPortToUse(localAddress, uri));
			}
		}

		return new ForwardedInfo(uriComponentsBuilder, forAddress, byAddress);
	}

	/**
	 * Parse the first "forwarded-element" of the "Forwarded" header into its
	 * "forwarded-pair" parameters, keyed by lower-case name, using the
	 * ABNF grammar from RFC 7239, Section 4.
	 * <p>Parameter values are parsed more leniently than the strict "token"
	 * production, allowing any character up to the next delimiter to
	 * accept an unquoted host:port or IPv6 "node" value.
	 */
	private static Map<String, String> parseFirstElement(String header) {
		Map<String, String> pairs = new LinkedHashMap<>(4);
		int index = 0;
		int length = header.length();
		while (index < length) {
			index = skipWhiteSpaces(header, index);
			if (index == length || header.charAt(index) == ',') {
				// End of the first "forwarded-element"
				break;
			}
			if (header.charAt(index) == ';') {
				// Empty "forwarded-pair"
				index++;
				continue;
			}
			// forwarded-pair = token "=" value
			int nameStart = index;
			while (index < length && isTokenChar(header.charAt(index))) {
				index++;
			}
			String name = header.substring(nameStart, index);
			index = skipWhiteSpaces(header, index);
			if (!name.isEmpty() && index < length && header.charAt(index) == '=') {
				index = skipWhiteSpaces(header, index + 1);
				String value;
				if (index < length && header.charAt(index) == '"') {
					// quoted-string
					StringBuilder sb = new StringBuilder();
					index++;
					while (index < length) {
						char c = header.charAt(index++);
						if (c == '"') {
							break;
						}
						if (c == '\\' && index < length) {
							// quoted-pair
							c = header.charAt(index++);
						}
						sb.append(c);
					}
					value = sb.toString();
				}
				else {
					int valueStart = index;
					index = skipToDelimiter(header, index);
					value = header.substring(valueStart, index);
				}
				pairs.putIfAbsent(name.toLowerCase(Locale.ROOT), value.trim());
			}
			index = skipToDelimiter(header, index);
			if (index < length && header.charAt(index) == ',') {
				break;
			}
			index++;
		}
		return pairs;
	}

	private static int skipWhiteSpaces(String header, int index) {
		while (index < header.length() && (header.charAt(index) == ' ' || header.charAt(index) == '\t')) {
			index++;
		}
		return index;
	}

	private static int skipToDelimiter(String header, int index) {
		while (index < header.length() && header.charAt(index) != ';' && header.charAt(index) != ',') {
			index++;
		}
		return index;
	}

	private static boolean isTokenChar(char c) {
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
				c == '!' || c == '#' || c == '$' || c == '%' || c == '&' || c == '\'' ||
				c == '*' || c == '+' || c == '-' || c == '.' || c == '^' || c == '_' ||
				c == '`' || c == '|' || c == '~');
	}

	private static void adaptForwardedHost(UriComponentsBuilder uriComponentsBuilder, String rawValue) {
		int portSeparatorIdx = rawValue.lastIndexOf(':');
		int squareBracketIdx = rawValue.lastIndexOf(']');
		if (portSeparatorIdx > squareBracketIdx) {
			if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
				throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
			}
			uriComponentsBuilder.host(rawValue.substring(0, portSeparatorIdx));
			try {
				uriComponentsBuilder.port(
						Integer.parseInt(rawValue, portSeparatorIdx + 1, rawValue.length(), 10));
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(
						"Failed to parse port in forwarded host value: " + rawValue + "\"");
			}
		}
		else {
			uriComponentsBuilder.host(rawValue);
			uriComponentsBuilder.port(null);
		}
	}

	private static int getPortToUse(@Nullable InetSocketAddress address, URI uri) {
		return (address != null ? address.getPort() : "https".equals(uri.getScheme()) ? 443 : 80);
	}

	private static InetSocketAddress parseInetSocketAddress(String value, int port) {
		String host = value;
		int portSeparatorIdx = value.lastIndexOf(':');
		int squareBracketIdx = value.lastIndexOf(']');
		if (portSeparatorIdx > squareBracketIdx) {
			if (squareBracketIdx == -1 && value.indexOf(':') != portSeparatorIdx) {
				throw new IllegalArgumentException("Invalid IPv4 address: " + value);
			}
			host = value.substring(0, portSeparatorIdx);
			try {
				port = Integer.parseInt(value, portSeparatorIdx + 1, value.length(), 10);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(
						"Failed to parse port in forwarded address value: " + value);
			}
		}
		return InetSocketAddress.createUnresolved(host, port);
	}

	/**
	 * Parse the "X-Forwarded-Proto", "X-Forwarded-Host", "X-Forwarded-Port", and
	 * "X-Forwarded-For" headers, the alternative to the "Forwarded" header.
	 * <p>There is no parsing of "X-Forwarded-By" currently, so the
	 * {@code byAddress} in {@link ForwardedInfo} is always {@code null}.
	 * @param uri the request {@code URI}
	 * @param headers the HTTP headers to get the "Forwarded" header from
	 * @param remoteAddress for a default port for the parsed "for" value
	 * @param localAddress for a default port for the parsed "by" value;
	 * this argument is ignored currently and the byAddress is always {@code null}
	 * @return a {@link ForwardedInfo} with the scheme, host, and port adapted
	 * from the "X-Forwarded-*" headers, and the parsed "for" address
	 * @since 7.1
	 */
	public static ForwardedInfo parseXForwardedHeaders(URI uri, HttpHeaders headers,
			@Nullable InetSocketAddress remoteAddress, @Nullable InetSocketAddress localAddress) {

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri);

		String protocolHeader = headers.getFirst("X-Forwarded-Proto");
		if (StringUtils.hasText(protocolHeader)) {
			uriComponentsBuilder.scheme(getLeftMostValue(protocolHeader));
			uriComponentsBuilder.port(null);
		}
		else if (isForwardedSslOn(headers)) {
			uriComponentsBuilder.scheme("https");
			uriComponentsBuilder.port(null);
		}
		String hostHeader = headers.getFirst("X-Forwarded-Host");
		if (StringUtils.hasText(hostHeader)) {
			adaptForwardedHost(uriComponentsBuilder, getLeftMostValue(hostHeader));
		}
		String portHeader = headers.getFirst("X-Forwarded-Port");
		if (StringUtils.hasText(portHeader)) {
			try {
				uriComponentsBuilder.port(Integer.parseInt(getLeftMostValue(portHeader)));
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Failed to parse \"X-Forwarded-Port: " + portHeader + "\"");
			}
		}
		uriComponentsBuilder.resetPortIfDefaultForScheme();

		InetSocketAddress forAddress = null;
		String forHeader = headers.getFirst("X-Forwarded-For");
		if (StringUtils.hasText(forHeader)) {
			String host = getLeftMostValue(forHeader);
			boolean ipv6 = (host.indexOf(':') != -1);
			host = (ipv6 && !host.startsWith("[") && !host.endsWith("]") ? "[" + host + "]" : host);
			int port = getPortToUse(remoteAddress, uri);
			forAddress = InetSocketAddress.createUnresolved(host, port);
		}

		return new ForwardedInfo(uriComponentsBuilder, forAddress, null);
	}

	private static String getLeftMostValue(String headerValue) {
		return StringUtils.tokenizeToStringArray(headerValue, ",")[0];
	}

	private static boolean isForwardedSslOn(HttpHeaders headers) {
		String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
		return (StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on"));
	}

	/**
	 * Adapt the scheme+host+port of the given {@link URI} from the "Forwarded" header
	 * (see <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>) or from the
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers if
	 * "Forwarded" is not present.
	 * @param uri the request {@code URI}
	 * @param headers the HTTP headers to consider
	 * @return a {@link UriComponentsBuilder} that reflects the request URI and
	 * additional updates from forwarded headers
	 */
	public static UriComponentsBuilder adaptFromForwardedHeaders(URI uri, HttpHeaders headers) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri);
		try {
			String forwardedHeader = headers.getFirst("Forwarded");
			if (StringUtils.hasText(forwardedHeader)) {
				Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					uriComponentsBuilder.scheme(matcher.group(1).trim());
					uriComponentsBuilder.port(null);
				}
				else if (isForwardedSslOn(headers)) {
					uriComponentsBuilder.scheme("https");
					uriComponentsBuilder.port(null);
				}
				matcher = FORWARDED_HOST_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					adaptForwardedHost(uriComponentsBuilder, matcher.group(1).trim());
				}
			}
			else {
				String protocolHeader = headers.getFirst("X-Forwarded-Proto");
				if (StringUtils.hasText(protocolHeader)) {
					uriComponentsBuilder.scheme(getLeftMostValue(protocolHeader));
					uriComponentsBuilder.port(null);
				}
				else if (isForwardedSslOn(headers)) {
					uriComponentsBuilder.scheme("https");
					uriComponentsBuilder.port(null);
				}
				String hostHeader = headers.getFirst("X-Forwarded-Host");
				if (StringUtils.hasText(hostHeader)) {
					adaptForwardedHost(uriComponentsBuilder, getLeftMostValue(hostHeader));
				}
				String portHeader = headers.getFirst("X-Forwarded-Port");
				if (StringUtils.hasText(portHeader)) {
					uriComponentsBuilder.port(Integer.parseInt(getLeftMostValue(portHeader)));
				}
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " +
					"If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
					"with removeOnly=true. Request headers: " + headers);
		}

		uriComponentsBuilder.resetPortIfDefaultForScheme();

		return uriComponentsBuilder;
	}

	/**
	 * Parse the first "Forwarded: for=..." or "X-Forwarded-For" header value to
	 * an {@code InetSocketAddress} representing the address of the client.
	 * @param uri the request {@code URI}
	 * @param headers the request headers that may contain forwarded headers
	 * @param remoteAddress the current remote address
	 * @return an {@code InetSocketAddress} with the extracted host and port, or
	 * {@code null} if the headers are not present
	 * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.2">RFC 7239, Section 5.2</a>
	 */
	public static @Nullable InetSocketAddress parseForwardedFor(
			URI uri, HttpHeaders headers, @Nullable InetSocketAddress remoteAddress) {

		String forwardedHeader = headers.getFirst("Forwarded");
		if (StringUtils.hasText(forwardedHeader)) {
			String forwardedToUse = getLeftMostValue(forwardedHeader);
			Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwardedToUse);
			if (matcher.find()) {
				String value = matcher.group(1).trim();
				return parseInetSocketAddress(value, getPortToUse(remoteAddress, uri));
			}
		}

		String forHeader = headers.getFirst("X-Forwarded-For");
		if (StringUtils.hasText(forHeader)) {
			String host = getLeftMostValue(forHeader);
			boolean ipv6 = (host.indexOf(':') != -1);
			host = (ipv6 && !host.startsWith("[") && !host.endsWith("]") ? "[" + host + "]" : host);
			return InetSocketAddress.createUnresolved(host, getPortToUse(remoteAddress, uri));
		}

		return null;
	}

	/**
	 * Parse the first "Forwarded: by=..." header value to
	 * an {@code InetSocketAddress} representing the address of the server.
	 * @param uri the request {@code URI}
	 * @param headers the request headers that may contain forwarded headers
	 * @param localAddress the current local address
	 * @return an {@code InetSocketAddress} with the extracted host and port, or
	 * {@code null} if the headers are not present
	 * @since 7.0
	 * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.1">RFC 7239, Section 5.1</a>
	 */
	public static @Nullable InetSocketAddress parseForwardedBy(
			URI uri, HttpHeaders headers, @Nullable InetSocketAddress localAddress) {

		String forwardedHeader = headers.getFirst("Forwarded");
		if (StringUtils.hasText(forwardedHeader)) {
			String forwardedToUse = getLeftMostValue(forwardedHeader);
			Matcher matcher = FORWARDED_BY_PATTERN.matcher(forwardedToUse);
			if (matcher.find()) {
				String value = matcher.group(1).trim();
				return parseInetSocketAddress(value, getPortToUse(localAddress, uri));
			}
		}

		return null;
	}


	/**
	 * Container for the combined results of parsing either the "Forwarded"
	 * header or the "X-Forwarded-*" alternative headers.
	 * @since 7.1
	 * @param uriComponentsBuilder the request URI adapted with the scheme, host,
	 * and port from forwarded header values
	 * @param forAddress the address parsed from the "Forwarded" header "for" or
	 * "X-Forwarded-For" value, representing the client, or {@code null} if not present
	 * @param byAddress the address parsed from the "Forwarded" header "by",
	 * representing the server, or {@code null} if not present;
	 * always {@code null} when returned from {@link #parseXForwardedHeaders}
	 */
	public record ForwardedInfo(UriComponentsBuilder uriComponentsBuilder,
			@Nullable InetSocketAddress forAddress, @Nullable InetSocketAddress byAddress) {

		/**
		 * Return a {@link URI} initialized from {@link #uriComponentsBuilder()}.
		 */
		public URI uri() {
			// URI should be encoded, but avoid validation with build(true) for lenient handling (gh-30137)
			UriComponents components = uriComponentsBuilder().build();
			try {
				return new URI(components.toUriString());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
			}
		}
	}

}
