/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.CollectionFactory;

/**
 * Represents an Internet Media Type, as defined in the HTTP specification.
 *
 * <p>Consists of a {@linkplain #getType() type}
 * and a {@linkplain #getSubtype() subtype}. Also has functionality to parse media types from a string using
 * {@link #parseMediaType(String)}, or multiple comma-separated media types using {@link #parseMediaTypes(String)}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP 1.1</a>
 */
public final class MediaType implements Comparable<MediaType> {

	public static final MediaType ALL = new MediaType();

	private static final String PARAM_QUALITY_FACTORY = "q";

	private static final String PARAM_CHARSET = "charset";

	private static final String WILDCARD_TYPE = "*";

	private final String type;

	private final String subtype;

	private final Map<String, String> parameters;

	/**
	 * Private constructor that creates a new {@link MediaType} representing <code>&#42;&#47;&#42;</code>.
	 *
	 * @see #ALL
	 */
	private MediaType() {
		this(WILDCARD_TYPE, WILDCARD_TYPE);
	}

	/**
	 * Create a new {@link MediaType} for the given primary type. The {@linkplain #getSubtype() subtype} is set to
	 * <code>&#42;</code>, parameters empty.
	 *
	 * @param type the primary type
	 */
	public MediaType(String type) {
		this(type, WILDCARD_TYPE);
	}

	/**
	 * Create a new {@link MediaType} for the given primary type and subtype. The parameters are empty.
	 *
	 * @param type	the primary type
	 * @param subtype the subtype
	 */
	public MediaType(String type, String subtype) {
		this(type, subtype, Collections.<String, String>emptyMap());
	}

	/**
	 * Creates a new {@link MediaType} for the given type, subtype, and character set.
	 *
	 * @param type	the primary type
	 * @param subtype the subtype
	 * @param charSet the character set
	 */
	public MediaType(String type, String subtype, Charset charSet) {
		this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charSet.toString()));
	}

	/**
	 * Creates a new {@link MediaType} for the given type, subtype, and parameters.
	 *
	 * @param type	   the primary type
	 * @param subtype	the subtype
	 * @param parameters the parameters, mat be <code>null</code>
	 */
	public MediaType(String type, String subtype, Map<String, String> parameters) {
		Assert.hasText(type, "'type' must not be empty");
		Assert.hasText(subtype, "'subtype' must not be empty");
		this.type = type.toLowerCase(Locale.ENGLISH);
		this.subtype = subtype.toLowerCase(Locale.ENGLISH);
		if (parameters != null) {
			this.parameters = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(parameters.size());
			this.parameters.putAll(parameters);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * Parses the given string into a single {@link MediaType}.
	 *
	 * @param mediaType the string to parse
	 * @return the media type
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static MediaType parseMediaType(String mediaType) {
		Assert.hasLength(mediaType, "'mediaType' must not be empty");
		mediaType = mediaType.trim();
		int subTypeIdx = mediaType.indexOf('/');
		if (subTypeIdx == -1) {
			throw new IllegalArgumentException("mediaType " + mediaType + " contains no /");
		}
		String type = mediaType.substring(0, subTypeIdx);
		String subtype;
		Map<String, String> parameters;
		int paramIdx = mediaType.indexOf(';', subTypeIdx + 1);
		if (paramIdx == -1) {
			subtype = mediaType.substring(subTypeIdx + 1).trim();
			parameters = null;
		}
		else {
			subtype = mediaType.substring(subTypeIdx + 1, paramIdx).trim();
			String[] tokens = StringUtils.tokenizeToStringArray(mediaType.substring(paramIdx), "; ");
			parameters = new LinkedHashMap<String, String>(tokens.length);
			for (String token : tokens) {
				int eqPos = token.indexOf('=');
				parameters.put(token.substring(0, eqPos), token.substring(eqPos + 1));
			}
		}
		return new MediaType(type, subtype, parameters);
	}

	/**
	 * Parses the given, comma-seperated string into a list of {@link MediaType} objects. This method can be used to
	 * parse an Accept or Content-Type header.
	 *
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<MediaType> parseMediaTypes(String mediaTypes) {
		Assert.hasLength(mediaTypes, "'mediaTypes' must not be empty");
		String[] tokens = mediaTypes.split(",\\s*");
		List<MediaType> result = new ArrayList<MediaType>(tokens.length);
		for (String token : tokens) {
			result.add(parseMediaType(token));
		}
		return result;
	}

	/**
	 * Returns the primary type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Indicates whether the {@linkplain #getType() type} is the wildcard character <code>&#42;</code> or not.
	 *
	 * @return whether the type is <code>&#42;</code>
	 */
	public boolean isWildcardType() {
		return WILDCARD_TYPE.equals(type);
	}

	/**
	 * Returns the subtype.
	 *
	 * @return the subtype
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard character <code>&#42;</code> or not.
	 *
	 * @return whether the subtype is <code>&#42;</code>
	 */
	public boolean isWildcardSubtype() {
		return WILDCARD_TYPE.equals(subtype);
	}

	/**
	 * Returns the character set, as indicated by a <code>charset</code> parameter, if any.
	 *
	 * @return the character set; or <code>null</code> if not available
	 */
	public Charset getCharSet() {
		String charSet = parameters.get(PARAM_CHARSET);
		return charSet != null ? Charset.forName(charSet) : null;
	}

	/**
	 * Returns the quality value, as indicated by a <code>q</code> parameter, if any. Defaults to <code>1.0</code>.
	 *
	 * @return the quality factory
	 */
	public double getQualityValue() {
		String qualityFactory = parameters.get(PARAM_QUALITY_FACTORY);
		return qualityFactory != null ? Double.parseDouble(qualityFactory) : 1D;
	}

	/**
	 * Returns a generic parameter value, given a parameter name.
	 *
	 * @param name the parameter name
	 * @return the parameter value; or <code>null</code> if not present
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}

	/**
	 * Indicates whether this {@link MediaType} includes the given media type. For instance, <code>text/*</code>
	 * includes <code>text/plain</code>, <code>text/html</code>, etc.
	 *
	 * @param other the reference media type with which to compare
	 * @return <code>true</code> if this media type includes the given media type; <code>false</code> otherwise
	 */
	public boolean includes(MediaType other) {
		if (this == other) {
			return true;
		}
		if (this.type.equals(other.type)) {
			if (this.subtype.equals(other.subtype) || isWildcardSubtype()) {
				return true;
			}
		}
		return isWildcardType();
	}

	/**
	 * Compares this {@link MediaType} to another. Sorting with this comparator follows the general rule: <blockquote>
	 * audio/basic &lt; audio/* &lt; *&#047;* </blockquote>. That is, an explicit media type is sorted before an
	 * unspecific media type. Quality parameters are also considered, so that <blockquote> audio/* &lt; audio/*;q=0.7;
	 * audio/*;q=0.3</blockquote>.
	 *
	 * @param other the media type to compare to
	 * @return a negative integer, zero, or a positive integer as this media type is less than, equal to, or greater
	 *         than the specified media type
	 */
	public int compareTo(MediaType other) {
		double qVal1 = this.getQualityValue();
		double qVal2 = other.getQualityValue();
		int qComp = Double.compare(qVal2, qVal1);
		if (qComp != 0) {
			return qComp;
		}
		else if (this.isWildcardType() && !other.isWildcardType()) {
			return 1;
		}
		else if (other.isWildcardType() && !this.isWildcardType()) {
			return -1;
		}
		else if (!this.getType().equals(other.getType())) {
			return this.getType().compareTo(other.getType());
		}
		else { // mediaType1.getType().equals(mediaType2.getType())
			if (this.isWildcardSubtype() && !other.isWildcardSubtype()) {
				return 1;
			}
			else if (other.isWildcardSubtype() && !this.isWildcardSubtype()) {
				return -1;
			}
			else if (!this.getSubtype().equals(other.getSubtype())) {
				return this.getSubtype().compareTo(other.getSubtype());
			}
			else { // mediaType2.getSubtype().equals(mediaType2.getSubtype())
				double quality1 = this.getQualityValue();
				double quality2 = other.getQualityValue();
				return Double.compare(quality2, quality1);
			}
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && o instanceof MediaType) {
			MediaType other = (MediaType) o;
			return this.type.equals(other.type) && this.subtype.equals(other.subtype) &&
					this.parameters.equals(other.parameters);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + subtype.hashCode();
		result = 31 * result + parameters.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendTo(builder);
		return builder.toString();
	}

	/**
	 * Returns a string representation of the given list of {@link MediaType} objects. This method can be used to for an
	 * Accept or Content-Type header.
	 *
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static String toString(List<MediaType> mediaTypes) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<MediaType> iterator = mediaTypes.iterator(); iterator.hasNext();) {
			MediaType mediaType = iterator.next();
			mediaType.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(',');
			}
		}
		return builder.toString();
	}

	private void appendTo(StringBuilder builder) {
		builder.append(type);
		builder.append('/');
		builder.append(subtype);
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			builder.append(';');
			builder.append(entry.getKey());
			builder.append('=');
			builder.append(entry.getValue());
		}
	}
}
