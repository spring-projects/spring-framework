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

package org.springframework.web.accept;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * {@code ApiVersionDeprecationHandler} based on
 * <a href="https://datatracker.ietf.org/doc/html/rfc9745">RFC 9745</a> and
 * <a href="https://datatracker.ietf.org/doc/html/rfc8594">RFC 8594</a> that
 * provides the option to set the "Deprecation" and "Sunset" response headers,
 * as well as to add "Link" headers with further details about both.
 * <p>To use this handler, create an instance, call {@link #configureVersion}
 * for each deprecated version, and use the returned {@link VersionSpec} to
 * provide the deprecation details to send to clients.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class StandardApiVersionDeprecationHandler implements ApiVersionDeprecationHandler {

	private final ApiVersionParser<?> versionParser;

	private final Map<Comparable<?>, VersionInfo> infos = new HashMap<>();


	/**
	 * Create an instance.
	 * <p>By default, {@link SemanticApiVersionParser} is used to parse configured
	 * API versions, so those can be compared to request versions parsed at runtime.
	 * If you have a custom parser, then please use the
	 * {@link #StandardApiVersionDeprecationHandler(ApiVersionParser)} constructor.
	 */
	public StandardApiVersionDeprecationHandler() {
		this(new SemanticApiVersionParser());
	}

	/**
	 * Variant of the default constructor with a custom {@link ApiVersionParser}.
	 * This needs to be the same as the parser type used at runtime to parse
	 * request versions.
	 */
	public StandardApiVersionDeprecationHandler(ApiVersionParser<?> parser) {
		this.versionParser = parser;
	}


	/**
	 * Mark the given API version as deprecated, and use the returned
	 * {@link VersionSpec} to configure the deprecation details to send to clients.
	 * @param version the version to mark as deprecated
	 * @return a spec to configure deprecation details
	 */
	public VersionSpec configureVersion(String version) {
		Comparable<?> parsedVersion = this.versionParser.parseVersion(version);
		return new VersionSpec(parsedVersion);
	}


	@Override
	public void handleVersion(
			Comparable<?> requestVersion, HttpServletRequest request, HttpServletResponse response) {

		for (VersionInfo info : this.infos.values()) {
			if (info.match(requestVersion, request)) {
				if (info.deprecationDate() != null) {
					response.setHeader("Deprecation", info.deprecationDate());
				}
				if (info.deprecationLink() != null) {
					response.addHeader(HttpHeaders.LINK, info.deprecationLink());
				}
				if (info.sunsetDate() != null) {
					response.setHeader("Sunset", info.sunsetDate());
				}
				if (info.sunsetLink() != null) {
					response.addHeader(HttpHeaders.LINK, info.sunsetLink());
				}
			}
		}
	}

	@Override
	public String toString() {
		return "StandardApiVersionDeprecationHandler " + this.infos.values();
	}


	/**
	 * A spec to configure deprecation details for an API version.
	 */
	public final class VersionSpec {

		private final Comparable<?> version;

		private VersionSpec(Comparable<?> version) {
			this.version = version;
			StandardApiVersionDeprecationHandler.this.infos.put(version, new VersionInfo(version));
		}

		/**
		 * Set a predicate to filter which requests to send deprecation info to.
		 * <p>By default, all requests with the deprecated version are handled.
		 * This predicate to narrow the requests that should expose deprecation
		 * information.
		 * @param predicate a predicate to check the request with
		 * @return the same spec instance
		 */
		public VersionSpec setRequestPredicate(Predicate<HttpServletRequest> predicate) {
			return map(info -> info.withRequestPredicate(predicate));
		}

		/**
		 * Specify a deprecation date for the "Deprecation" response header.
		 * @param date the deprecation date
		 * @return the same spec instance
		 */
		public VersionSpec setDeprecationDate(ZonedDateTime date) {
			return map(info -> info.withDeprecationDate(date));
		}

		/**
		 * Specify a URL for the "Link" response header with
		 * {@code rel="deprecation"} and {@code type="text/html"}.
		 * @param uri the link value
		 * @return the same spec instance
		 */
		public VersionSpec setDeprecationLink(URI uri) {
			return setDeprecationLink(uri, MediaType.TEXT_HTML);
		}

		/**
		 * Variation of {@link #setDeprecationLink(URI)} for use with a media type
		 * other than "text/html".
		 * @param uri the link value
		 * @param mediaType the media type to use
		 * @return the same spec instance
		 */
		public VersionSpec setDeprecationLink(URI uri, MediaType mediaType) {
			return map(info -> info.withDeprecationLink(uri, mediaType));
		}

		/**
		 * Specify a deprecation date for the "Sunset" response header.
		 * @param date the sunset date
		 * @return the same spec instance
		 */
		public VersionSpec setSunsetDate(ZonedDateTime date) {
			return map(info -> info.withSunsetDate(date));
		}

		/**
		 * Specify a URL for the "Link" response header with
		 * {@code rel="sunset"} and {@code type="text/html"}.
		 * @param uri the link value
		 * @return the same spec instance
		 */
		public VersionSpec setSunsetLink(URI uri) {
			return setSunsetLink(uri, MediaType.TEXT_HTML);
		}

		/**
		 * Variation of {@link #setSunsetLink(URI)} for use with a media type
		 * other than "text/html".
		 * @param uri the link value
		 * @param mediaType the media type to use
		 * @return the same spec instance
		 */
		public VersionSpec setSunsetLink(URI uri, MediaType mediaType) {
			return map(info -> info.withSunsetLink(uri, mediaType));
		}

		private VersionSpec map(Function<VersionInfo, VersionInfo> function) {
			StandardApiVersionDeprecationHandler.this.infos.compute(this.version, (version, versionInfo) -> {
				Assert.state(versionInfo != null, "No VersionInfo");
				return function.apply(versionInfo);
			});
			return this;
		}

	}


	private record VersionInfo(
			Comparable<?> version,
			Predicate<HttpServletRequest> requestPredicate,
			@Nullable String deprecationDate, @Nullable String deprecationLink,
			@Nullable String sunsetDate, @Nullable String sunsetLink) {

		VersionInfo(Comparable<?> version) {
			this(version, request -> true, null, null, null, null);
		}

		public VersionInfo withRequestPredicate(Predicate<HttpServletRequest> predicate) {
			return new VersionInfo(version(), predicate,
					deprecationDate(), deprecationLink(), sunsetDate(), sunsetLink());
		}

		public VersionInfo withDeprecationDate(ZonedDateTime deprecationDate) {
			return new VersionInfo(version(), requestPredicate(),
					"@" + deprecationDate.toInstant().getEpochSecond(), deprecationLink(),
					sunsetDate(), sunsetLink());
		}

		public VersionInfo withDeprecationLink(URI uri, MediaType mediaType) {
			return new VersionInfo(version(), requestPredicate(),
					deprecationDate(), String.format("<%s>; rel=\"deprecation\"; type=\"%s\"", uri, mediaType),
					sunsetDate(), sunsetLink());
		}

		public VersionInfo withSunsetDate(ZonedDateTime sunsetDate) {
			return new VersionInfo(version(), requestPredicate(),
					deprecationDate(), deprecationLink(),
					sunsetDate.format(DateTimeFormatter.RFC_1123_DATE_TIME), sunsetLink());
		}

		public VersionInfo withSunsetLink(URI uri, MediaType mediaType) {
			return new VersionInfo(version(), requestPredicate(),
					deprecationDate(), deprecationLink(),
					sunsetDate(), String.format("<%s>; rel=\"sunset\"; type=\"%s\"", uri, mediaType));
		}

		boolean match(Comparable<?> requestVersion, HttpServletRequest request) {
			return (version().equals(requestVersion) && requestPredicate().test(request));
		}
	}

}
