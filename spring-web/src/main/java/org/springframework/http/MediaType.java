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

package org.springframework.http;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * A subclass of {@link MimeType} that adds support for quality parameters
 * as defined in the HTTP specification.
 *
 * <p>This class is meant to reference media types supported by Spring Framework.
 * If your application or library relies on other media types defined in RFCs,
 * please use {@link #parseMediaType(String)} or a custom utility class.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @author Hyoungjune Kim
 * @since 3.0
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">
 *     HTTP 1.1: Semantics and Content, section 3.1.1.1</a>
 */
public class MediaType extends MimeType implements Serializable {

	private static final long serialVersionUID = 2069937152339670231L;

	/**
	 * Media type for "&#42;/&#42;", including all media ranges.
	 */
	public static final MediaType ALL;

	/**
	 * A String equivalent of {@link MediaType#ALL}.
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * Media type for {@code application/atom+xml}.
	 */
	public static final MediaType APPLICATION_ATOM_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_ATOM_XML}.
	 */
	public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * Media type for {@code application/cbor}.
	 * @since 5.2
	 */
	public static final MediaType APPLICATION_CBOR;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_CBOR}.
	 * @since 5.2
	 */
	public static final String APPLICATION_CBOR_VALUE = "application/cbor";

	/**
	 * Media type for {@code application/x-www-form-urlencoded}.
	 */
	public static final MediaType APPLICATION_FORM_URLENCODED;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_FORM_URLENCODED}.
	 */
	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * Media type for {@code application/graphql-response+json}.
	 * @since 6.0.3
	 * @see <a href="https://github.com/graphql/graphql-over-http">GraphQL over HTTP spec</a>
	 */
	public static final MediaType APPLICATION_GRAPHQL_RESPONSE;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_GRAPHQL_RESPONSE}.
	 * @since 6.0.3
	 */
	public static final String APPLICATION_GRAPHQL_RESPONSE_VALUE = "application/graphql-response+json";


	/**
	 * Media type for {@code application/json}.
	 */
	public static final MediaType APPLICATION_JSON;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_JSON}.
	 */
	public static final String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * Media type for {@code application/octet-stream}.
	 */
	public static final MediaType APPLICATION_OCTET_STREAM;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_OCTET_STREAM}.
	 */
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * Media type for {@code application/pdf}.
	 * @since 4.3
	 */
	public static final MediaType APPLICATION_PDF;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_PDF}.
	 * @since 4.3
	 */
	public static final String APPLICATION_PDF_VALUE = "application/pdf";

	/**
	 * Media type for {@code application/problem+json}.
	 * @since 5.0
	 * @see <a href="https://www.iana.org/assignments/media-types/application/problem+json">
	 *     Problem Details for HTTP APIs, 6.1. application/problem+json</a>
	 */
	public static final MediaType APPLICATION_PROBLEM_JSON;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_PROBLEM_JSON}.
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

	/**
	 * Media type for {@code application/problem+xml}.
	 * @since 5.0
	 * @see <a href="https://www.iana.org/assignments/media-types/application/problem+xml">
	 *     Problem Details for HTTP APIs, 6.2. application/problem+xml</a>
	 */
	public static final MediaType APPLICATION_PROBLEM_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_PROBLEM_XML}.
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

	/**
	 * Media type for {@code application/x-protobuf}.
	 * @since 6.0
	 */
	public static final MediaType APPLICATION_PROTOBUF;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_PROTOBUF}.
	 * @since 6.0
	 */
	public static final String APPLICATION_PROTOBUF_VALUE = "application/x-protobuf";

	/**
	 * Media type for {@code application/rss+xml}.
	 * @since 4.3.6
	 */
	public static final MediaType APPLICATION_RSS_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_RSS_XML}.
	 * @since 4.3.6
	 */
	public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

	/**
	 * Media type for {@code application/x-ndjson}.
	 * @since 5.3
	 */
	public static final MediaType APPLICATION_NDJSON;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_NDJSON}.
	 * @since 5.3
	 */
	public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";

	/**
	 * Media type for {@code application/xhtml+xml}.
	 */
	public static final MediaType APPLICATION_XHTML_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_XHTML_XML}.
	 */
	public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * Media type for {@code application/xml}.
	 */
	public static final MediaType APPLICATION_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_XML}.
	 */
	public static final String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * Media type for {@code application/yaml}.
	 * @since 6.2
	 */
	public static final MediaType APPLICATION_YAML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_YAML}.
	 * @since 6.2
	 */
	public static final String APPLICATION_YAML_VALUE = "application/yaml";

	/**
	 * Media type for {@code image/gif}.
	 */
	public static final MediaType IMAGE_GIF;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_GIF}.
	 */
	public static final String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * Media type for {@code image/jpeg}.
	 */
	public static final MediaType IMAGE_JPEG;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_JPEG}.
	 */
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * Media type for {@code image/png}.
	 */
	public static final MediaType IMAGE_PNG;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_PNG}.
	 */
	public static final String IMAGE_PNG_VALUE = "image/png";

	/**
	 * Media type for {@code multipart/form-data}.
	 */
	public static final MediaType MULTIPART_FORM_DATA;

	/**
	 * A String equivalent of {@link MediaType#MULTIPART_FORM_DATA}.
	 */
	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * Media type for {@code multipart/mixed}.
	 * @since 5.2
	 */
	public static final MediaType MULTIPART_MIXED;

	/**
	 * A String equivalent of {@link MediaType#MULTIPART_MIXED}.
	 * @since 5.2
	 */
	public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

	/**
	 * Media type for {@code multipart/related}.
	 * @since 5.2.5
	 */
	public static final MediaType MULTIPART_RELATED;

	/**
	 * A String equivalent of {@link MediaType#MULTIPART_RELATED}.
	 * @since 5.2.5
	 */
	public static final String MULTIPART_RELATED_VALUE = "multipart/related";

	/**
	 * Media type for {@code text/event-stream}.
	 * @since 4.3.6
	 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>
	 */
	public static final MediaType TEXT_EVENT_STREAM;

	/**
	 * A String equivalent of {@link MediaType#TEXT_EVENT_STREAM}.
	 * @since 4.3.6
	 */
	public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

	/**
	 * Media type for {@code text/html}.
	 */
	public static final MediaType TEXT_HTML;

	/**
	 * A String equivalent of {@link MediaType#TEXT_HTML}.
	 */
	public static final String TEXT_HTML_VALUE = "text/html";

	/**
	 * Media type for {@code text/markdown}.
	 * @since 4.3
	 */
	public static final MediaType TEXT_MARKDOWN;

	/**
	 * A String equivalent of {@link MediaType#TEXT_MARKDOWN}.
	 * @since 4.3
	 */
	public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

	/**
	 * Media type for {@code text/plain}.
	 */
	public static final MediaType TEXT_PLAIN;

	/**
	 * A String equivalent of {@link MediaType#TEXT_PLAIN}.
	 */
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * Media type for {@code text/xml}.
	 */
	public static final MediaType TEXT_XML;

	/**
	 * A String equivalent of {@link MediaType#TEXT_XML}.
	 */
	public static final String TEXT_XML_VALUE = "text/xml";

	private static final String PARAM_QUALITY_FACTOR = "q";


	static {
		// Not using "valueOf" to avoid static init cost
		ALL = new MediaType(MimeType.WILDCARD_TYPE, MimeType.WILDCARD_TYPE);
		APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
		APPLICATION_CBOR = new MediaType("application", "cbor");
		APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
		APPLICATION_GRAPHQL_RESPONSE = new MediaType("application", "graphql-response+json");
		APPLICATION_JSON = new MediaType("application", "json");
		APPLICATION_NDJSON = new MediaType("application", "x-ndjson");
		APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
		APPLICATION_PDF = new MediaType("application", "pdf");
		APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
		APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
		APPLICATION_PROTOBUF = new MediaType("application", "x-protobuf");
		APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
		APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
		APPLICATION_XML = new MediaType("application", "xml");
		APPLICATION_YAML = new MediaType("application", "yaml");
		IMAGE_GIF = new MediaType("image", "gif");
		IMAGE_JPEG = new MediaType("image", "jpeg");
		IMAGE_PNG = new MediaType("image", "png");
		MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
		MULTIPART_MIXED = new MediaType("multipart", "mixed");
		MULTIPART_RELATED = new MediaType("multipart", "related");
		TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
		TEXT_HTML = new MediaType("text", "html");
		TEXT_MARKDOWN = new MediaType("text", "markdown");
		TEXT_PLAIN = new MediaType("text", "plain");
		TEXT_XML = new MediaType("text", "xml");
	}


	/**
	 * Create a new {@code MediaType} for the given primary type.
	 * <p>The {@linkplain #getSubtype() subtype} is set to "&#42;", parameters empty.
	 * @param type the primary type
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type) {
		super(type);
	}

	/**
	 * Create a new {@code MediaType} for the given primary type and subtype.
	 * <p>The parameters are empty.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype) {
		super(type, subtype, Collections.emptyMap());
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and character set.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param charset the character set
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, Charset charset) {
		super(type, subtype, charset);
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and quality value.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param qualityValue the quality value
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, double qualityValue) {
		this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
	}

	/**
	 * Copy-constructor that copies the type, subtype and parameters of the given
	 * {@code MediaType}, and allows to set the specified character set.
	 * @param other the other media type
	 * @param charset the character set
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 * @since 4.3
	 */
	public MediaType(MediaType other, Charset charset) {
		super(other, charset);
	}

	/**
	 * Copy-constructor that copies the type and subtype of the given {@code MediaType},
	 * and allows for different parameters.
	 * @param other the other media type
	 * @param parameters the parameters, may be {@code null}
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(MediaType other, @Nullable Map<String, String> parameters) {
		super(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and parameters.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param parameters the parameters, may be {@code null}
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, @Nullable Map<String, String> parameters) {
		super(type, subtype, parameters);
	}

	/**
	 * Create a new {@code MediaType} for the given {@link MimeType}.
	 * The type, subtype and parameters information is copied and {@code MediaType}-specific
	 * checks on parameters are performed.
	 * @param mimeType the MIME type
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 * @since 5.3
	 */
	public MediaType(MimeType mimeType) {
		super(mimeType);
		getParameters().forEach(this::checkParameters);
	}


	@Override
	protected void checkParameters(String parameter, String value) {
		super.checkParameters(parameter, value);
		if (PARAM_QUALITY_FACTOR.equals(parameter)) {
			String unquotedValue = unquote(value);
			double d = Double.parseDouble(unquotedValue);
			Assert.isTrue(d >= 0D && d <= 1D,
					() -> "Invalid quality value \"" + unquotedValue + "\": should be between 0.0 and 1.0");
		}
	}

	/**
	 * Return the quality factor, as indicated by a {@code q} parameter, if any.
	 * Defaults to {@code 1.0}.
	 * @return the quality factor as double value
	 */
	public double getQualityValue() {
		String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
		return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
	}

	/**
	 * Indicates whether this {@code MediaType} more specific than the given type.
	 * <ol>
	 * <li>if this media type has a {@linkplain #getQualityValue() quality factor} higher than the other,
	 * then this method returns {@code true}.</li>
	 * <li>if this media type has a {@linkplain #getQualityValue() quality factor} lower than the other,
	 * then this method returns {@code false}.</li>
	 * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
	 * and the other does not, then this method returns {@code false}.</li>
	 * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
	 * and the other does, then this method returns {@code true}.</li>
	 * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
	 * and the other does not, then this method returns {@code false}.</li>
	 * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
	 * and the other does, then this method returns {@code true}.</li>
	 * <li>if the two mime types have identical {@linkplain #getType() type} and
	 * {@linkplain #getSubtype() subtype}, then the mime type with the most
	 * parameters is more specific than the other.</li>
	 * <li>Otherwise, this method returns {@code false}.</li>
	 * </ol>
	 * @param other the {@code MimeType} to be compared
	 * @return the result of the comparison
	 * @since 6.0
	 * @see #isLessSpecific(MimeType)
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
	 * and Content, section 5.3.2</a>
	 */
	@Override
	public boolean isMoreSpecific(MimeType other) {
		Assert.notNull(other, "Other must not be null");
		if (other instanceof MediaType otherMediaType) {
			double quality1 = getQualityValue();
			double quality2 = otherMediaType.getQualityValue();
			if (quality1 > quality2) {
				return true;
			}
			else if (quality1 < quality2) {
				return false;
			}
		}
		return super.isMoreSpecific(other);
	}

	/**
	 * Indicates whether this {@code MediaType} more specific than the given type.
	 * <ol>
	 * <li>if this media type has a {@linkplain #getQualityValue() quality factor} higher than the other,
	 * then this method returns {@code false}.</li>
	 * <li>if this media type has a {@linkplain #getQualityValue() quality factor} lower than the other,
	 * then this method returns {@code true}.</li>
	 * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
	 * and the other does not, then this method returns {@code true}.</li>
	 * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
	 * and the other does, then this method returns {@code false}.</li>
	 * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
	 * and the other does not, then this method returns {@code true}.</li>
	 * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
	 * and the other does, then this method returns {@code false}.</li>
	 * <li>if the two mime types have identical {@linkplain #getType() type} and
	 * {@linkplain #getSubtype() subtype}, then the mime type with the least
	 * parameters is less specific than the other.</li>
	 * <li>Otherwise, this method returns {@code false}.</li>
	 * </ol>
	 * @param other the {@code MimeType} to be compared
	 * @return the result of the comparison
	 * @since 6.0
	 * @see #isMoreSpecific(MimeType)
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
	 * and Content, section 5.3.2</a>
	 */
	@Override
	public boolean isLessSpecific(MimeType other) {
		Assert.notNull(other, "Other must not be null");
		return other.isMoreSpecific(this);
	}

	/**
	 * Indicate whether this {@code MediaType} includes the given media type.
	 * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html},
	 * and {@code application/*+xml} includes {@code application/soap+xml}, etc.
	 * This method is <b>not</b> symmetric.
	 * <p>Simply calls {@link MimeType#includes(MimeType)} but declared with a
	 * {@code MediaType} parameter for binary backwards compatibility.
	 * @param other the reference media type with which to compare
	 * @return {@code true} if this media type includes the given media type;
	 * {@code false} otherwise
	 */
	public boolean includes(@Nullable MediaType other) {
		return super.includes(other);
	}

	/**
	 * Indicate whether this {@code MediaType} is compatible with the given media type.
	 * <p>For instance, {@code text/*} is compatible with {@code text/plain},
	 * {@code text/html}, and vice versa. In effect, this method is similar to
	 * {@link #includes}, except that it <b>is</b> symmetric.
	 * <p>Simply calls {@link MimeType#isCompatibleWith(MimeType)} but declared with a
	 * {@code MediaType} parameter for binary backwards compatibility.
	 * @param other the reference media type with which to compare
	 * @return {@code true} if this media type is compatible with the given media type;
	 * {@code false} otherwise
	 */
	public boolean isCompatibleWith(@Nullable MediaType other) {
		return super.isCompatibleWith(other);
	}

	/**
	 * Return a replica of this instance with the quality value of the given {@code MediaType}.
	 * @return the same instance if the given MediaType doesn't have a quality value,
	 * or a new one otherwise
	 */
	public MediaType copyQualityValue(MediaType mediaType) {
		if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<>(getParameters());
		params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
		return new MediaType(this, params);
	}

	/**
	 * Return a replica of this instance with its quality value removed.
	 * @return the same instance if the media type doesn't contain a quality value,
	 * or a new one otherwise
	 */
	public MediaType removeQualityValue() {
		if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<>(getParameters());
		params.remove(PARAM_QUALITY_FACTOR);
		return new MediaType(this, params);
	}


	/**
	 * Parse the given String value into a {@code MediaType} object,
	 * with this method name following the 'valueOf' naming convention
	 * (as supported by {@link org.springframework.core.convert.ConversionService}).
	 * @param value the string to parse
	 * @throws InvalidMediaTypeException if the media type value cannot be parsed
	 * @see #parseMediaType(String)
	 */
	public static MediaType valueOf(String value) {
		return parseMediaType(value);
	}

	/**
	 * Parse the given String into a single {@code MediaType}.
	 * @param mediaType the string to parse
	 * @return the media type
	 * @throws InvalidMediaTypeException if the media type value cannot be parsed
	 */
	public static MediaType parseMediaType(String mediaType) {
		MimeType type;
		try {
			type = MimeTypeUtils.parseMimeType(mediaType);
		}
		catch (InvalidMimeTypeException ex) {
			throw new InvalidMediaTypeException(ex);
		}
		try {
			return new MediaType(type);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMediaTypeException(mediaType, ex.getMessage());
		}
	}

	/**
	 * Parse the comma-separated string into a list of {@code MediaType} objects.
	 * <p>This method can be used to parse an Accept or Content-Type header.
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws InvalidMediaTypeException if the media type value cannot be parsed
	 */
	public static List<MediaType> parseMediaTypes(@Nullable String mediaTypes) {
		if (!StringUtils.hasLength(mediaTypes)) {
			return Collections.emptyList();
		}
		// Avoid using java.util.stream.Stream in hot paths
		List<String> tokenizedTypes = MimeTypeUtils.tokenize(mediaTypes);
		List<MediaType> result = new ArrayList<>(tokenizedTypes.size());
		for (String type : tokenizedTypes) {
			if (StringUtils.hasText(type)) {
				result.add(parseMediaType(type));
			}
		}
		return result;
	}

	/**
	 * Parse the given list of (potentially) comma-separated strings into a
	 * list of {@code MediaType} objects.
	 * <p>This method can be used to parse an Accept or Content-Type header.
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws InvalidMediaTypeException if the media type value cannot be parsed
	 * @since 4.3.2
	 */
	public static List<MediaType> parseMediaTypes(@Nullable List<String> mediaTypes) {
		if (CollectionUtils.isEmpty(mediaTypes)) {
			return Collections.emptyList();
		}
		else if (mediaTypes.size() == 1) {
			return parseMediaTypes(mediaTypes.get(0));
		}
		else {
			List<MediaType> result = new ArrayList<>(8);
			for (String mediaType : mediaTypes) {
				result.addAll(parseMediaTypes(mediaType));
			}
			return result;
		}
	}

	/**
	 * Re-create the given mime types as media types.
	 * @since 5.0
	 */
	public static List<MediaType> asMediaTypes(List<MimeType> mimeTypes) {
		List<MediaType> mediaTypes = new ArrayList<>(mimeTypes.size());
		for (MimeType mimeType : mimeTypes) {
			mediaTypes.add(MediaType.asMediaType(mimeType));
		}
		return mediaTypes;
	}

	/**
	 * Re-create the given mime type as a media type.
	 * @since 5.0
	 */
	public static MediaType asMediaType(MimeType mimeType) {
		if (mimeType instanceof MediaType mediaType) {
			return mediaType;
		}
		return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
	}

	/**
	 * Return a string representation of the given list of {@code MediaType} objects.
	 * <p>This method can be used to for an {@code Accept} or {@code Content-Type} header.
	 * @param mediaTypes the media types to create a string representation for
	 * @return the string representation
	 */
	public static String toString(Collection<MediaType> mediaTypes) {
		return MimeTypeUtils.toString(mediaTypes);
	}

}
