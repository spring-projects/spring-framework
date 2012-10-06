/*
 * Copyright 2002-2012 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.CompoundComparator;

/**
 * Represents an Internet Media Type, as defined in the HTTP specification.
 *
 * <p>Consists of a {@linkplain #getType() type} and a {@linkplain #getSubtype() subtype}.
 * Also has functionality to parse media types from a string using {@link #parseMediaType(String)},
 * or multiple comma-separated media types using {@link #parseMediaTypes(String)}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-3.7">HTTP 1.1, section 3.7</a>
 */
public class MediaType implements Comparable<MediaType> {

	/**
	 * Public constant media type that includes all media ranges (i.e. <code>&#42;/&#42;</code>).
	 */
	public static final MediaType ALL;

	/**
	 * A String equivalent of {@link MediaType#ALL}.
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 *  Public constant media type for {@code application/atom+xml}.
	 */
	public final static MediaType APPLICATION_ATOM_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_ATOM_XML}.
	 */
	public final static String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * Public constant media type for {@code application/x-www-form-urlencoded}.
	 *  */
	public final static MediaType APPLICATION_FORM_URLENCODED;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_FORM_URLENCODED}.
	 */
	public final static String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * Public constant media type for {@code application/json}.
	 * */
	public final static MediaType APPLICATION_JSON;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_JSON}.
	 */
	public final static String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * Public constant media type for {@code application/octet-stream}.
	 *  */
	public final static MediaType APPLICATION_OCTET_STREAM;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_OCTET_STREAM}.
	 */
	public final static String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * Public constant media type for {@code application/xhtml+xml}.
	 *  */
	public final static MediaType APPLICATION_XHTML_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_XHTML_XML}.
	 */
	public final static String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * Public constant media type for {@code application/xml}.
	 */
	public final static MediaType APPLICATION_XML;

	/**
	 * A String equivalent of {@link MediaType#APPLICATION_XML}.
	 */
	public final static String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * Public constant media type for {@code image/gif}.
	 */
	public final static MediaType IMAGE_GIF;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_GIF}.
	 */
	public final static String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * Public constant media type for {@code image/jpeg}.
	 */
	public final static MediaType IMAGE_JPEG;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_JPEG}.
	 */
	public final static String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * Public constant media type for {@code image/png}.
	 */
	public final static MediaType IMAGE_PNG;

	/**
	 * A String equivalent of {@link MediaType#IMAGE_PNG}.
	 */
	public final static String IMAGE_PNG_VALUE = "image/png";

	/**
	 * Public constant media type for {@code multipart/form-data}.
	 *  */
	public final static MediaType MULTIPART_FORM_DATA;

	/**
	 * A String equivalent of {@link MediaType#MULTIPART_FORM_DATA}.
	 */
	public final static String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * Public constant media type for {@code text/html}.
	 *  */
	public final static MediaType TEXT_HTML;

	/**
	 * A String equivalent of {@link MediaType#TEXT_HTML}.
	 */
	public final static String TEXT_HTML_VALUE = "text/html";

	/**
	 * Public constant media type for {@code text/plain}.
	 *  */
	public final static MediaType TEXT_PLAIN;

	/**
	 * A String equivalent of {@link MediaType#TEXT_PLAIN}.
	 */
	public final static String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * Public constant media type for {@code text/xml}.
	 *  */
	public final static MediaType TEXT_XML;

	/**
	 * A String equivalent of {@link MediaType#TEXT_XML}.
	 */
	public final static String TEXT_XML_VALUE = "text/xml";


	private static final BitSet TOKEN;

	private static final String WILDCARD_TYPE = "*";

	private static final String PARAM_QUALITY_FACTOR = "q";

	private static final String PARAM_CHARSET = "charset";


	private final String type;

	private final String subtype;

	private final Map<String, String> parameters;


	static {
		// variable names refer to RFC 2616, section 2.2
		BitSet ctl = new BitSet(128);
		for (int i=0; i <= 31; i++) {
			ctl.set(i);
		}
		ctl.set(127);

		BitSet separators = new BitSet(128);
		separators.set('(');
		separators.set(')');
		separators.set('<');
		separators.set('>');
		separators.set('@');
		separators.set(',');
		separators.set(';');
		separators.set(':');
		separators.set('\\');
		separators.set('\"');
		separators.set('/');
		separators.set('[');
		separators.set(']');
		separators.set('?');
		separators.set('=');
		separators.set('{');
		separators.set('}');
		separators.set(' ');
		separators.set('\t');

		TOKEN = new BitSet(128);
		TOKEN.set(0, 128);
		TOKEN.andNot(ctl);
		TOKEN.andNot(separators);

		ALL = MediaType.valueOf(ALL_VALUE);
		APPLICATION_ATOM_XML = MediaType.valueOf(APPLICATION_ATOM_XML_VALUE);
		APPLICATION_FORM_URLENCODED = MediaType.valueOf(APPLICATION_FORM_URLENCODED_VALUE);
		APPLICATION_JSON = MediaType.valueOf(APPLICATION_JSON_VALUE);
		APPLICATION_OCTET_STREAM = MediaType.valueOf(APPLICATION_OCTET_STREAM_VALUE);
		APPLICATION_XHTML_XML = MediaType.valueOf(APPLICATION_XHTML_XML_VALUE);
		APPLICATION_XML = MediaType.valueOf(APPLICATION_XML_VALUE);
		IMAGE_GIF = MediaType.valueOf(IMAGE_GIF_VALUE);
		IMAGE_JPEG = MediaType.valueOf(IMAGE_JPEG_VALUE);
		IMAGE_PNG = MediaType.valueOf(IMAGE_PNG_VALUE);
		MULTIPART_FORM_DATA = MediaType.valueOf(MULTIPART_FORM_DATA_VALUE);
		TEXT_HTML = MediaType.valueOf(TEXT_HTML_VALUE);
		TEXT_PLAIN = MediaType.valueOf(TEXT_PLAIN_VALUE);
		TEXT_XML = MediaType.valueOf(TEXT_XML_VALUE);
	}


	/**
	 * Create a new {@code MediaType} for the given primary type.
	 * <p>The {@linkplain #getSubtype() subtype} is set to <code>&#42;</code>, parameters empty.
	 * @param type the primary type
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type) {
		this(type, WILDCARD_TYPE);
	}

	/**
	 * Create a new {@code MediaType} for the given primary type and subtype.
	 * <p>The parameters are empty.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype) {
		this(type, subtype, Collections.<String, String>emptyMap());
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and character set.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param charSet the character set
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, Charset charSet) {
		this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charSet.name()));
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and quality value.
	 *
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param qualityValue the quality value
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, double qualityValue) {
		this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
	}

	/**
	 * Copy-constructor that copies the type and subtype of the given {@code MediaType},
	 * and allows for different parameter.
	 * @param other the other media type
	 * @param parameters the parameters, may be <code>null</code>
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(MediaType other, Map<String, String> parameters) {
		this(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * Create a new {@code MediaType} for the given type, subtype, and parameters.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param parameters the parameters, may be <code>null</code>
	 * @throws IllegalArgumentException if any of the parameters contain illegal characters
	 */
	public MediaType(String type, String subtype, Map<String, String> parameters) {
		Assert.hasLength(type, "'type' must not be empty");
		Assert.hasLength(subtype, "'subtype' must not be empty");
		checkToken(type);
		checkToken(subtype);
		this.type = type.toLowerCase(Locale.ENGLISH);
		this.subtype = subtype.toLowerCase(Locale.ENGLISH);
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> m = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				String attribute = entry.getKey();
				String value = entry.getValue();
				checkParameters(attribute, value);
				m.put(attribute, value);
			}
			this.parameters = Collections.unmodifiableMap(m);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * Checks the given token string for illegal characters, as defined in RFC 2616, section 2.2.
	 * @throws IllegalArgumentException in case of illegal characters
	 * @see <a href="http://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
	 */
	private void checkToken(String s) {
		for (int i=0; i < s.length(); i++ ) {
			char ch = s.charAt(i);
			if (!TOKEN.get(ch)) {
				throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + s + "\"");
			}
		}
	}

	private void checkParameters(String attribute, String value) {
		Assert.hasLength(attribute, "parameter attribute must not be empty");
		Assert.hasLength(value, "parameter value must not be empty");
		checkToken(attribute);
		if (PARAM_QUALITY_FACTOR.equals(attribute)) {
			value = unquote(value);
			double d = Double.parseDouble(value);
			Assert.isTrue(d >= 0D && d <= 1D,
					"Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
		}
		else if (PARAM_CHARSET.equals(attribute)) {
			value = unquote(value);
			Charset.forName(value);
		}
		else if (!isQuotedString(value)) {
			checkToken(value);
		}
	}

	private boolean isQuotedString(String s) {
		if (s.length() < 2) {
			return false;
		}
		else {
			return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
		}
	}

	private String unquote(String s) {
		if (s == null) {
			return null;
		}
		return isQuotedString(s) ? s.substring(1, s.length() - 1) : s;
	}

	/**
	 * Return the primary type.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Indicates whether the {@linkplain #getType() type} is the wildcard character <code>&#42;</code> or not.
	 */
	public boolean isWildcardType() {
		return WILDCARD_TYPE.equals(type);
	}

	/**
	 * Return the subtype.
	 */
	public String getSubtype() {
		return this.subtype;
	}

	/**
	 * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard character <code>&#42;</code>
	 * or the wildcard character followed by a sufiix (e.g. <code>&#42;+xml</code>), or not.
	 * @return whether the subtype is <code>&#42;</code>
	 */
	public boolean isWildcardSubtype() {
		return WILDCARD_TYPE.equals(subtype) || subtype.startsWith("*+");
	}

	/**
	 * Indicates whether this media type is concrete, i.e. whether neither the type or subtype is a wildcard
	 * character <code>&#42;</code>.
	 * @return whether this media type is concrete
	 */
	public boolean isConcrete() {
		return !isWildcardType() && !isWildcardSubtype();
	}

	/**
	 * Return the character set, as indicated by a <code>charset</code> parameter, if any.
	 * @return the character set; or <code>null</code> if not available
	 */
	public Charset getCharSet() {
		String charSet = getParameter(PARAM_CHARSET);
		return (charSet != null ? Charset.forName(unquote(charSet)) : null);
	}

	/**
	 * Return the quality value, as indicated by a <code>q</code> parameter, if any.
	 * Defaults to <code>1.0</code>.
	 * @return the quality factory
	 */
	public double getQualityValue() {
		String qualityFactory = getParameter(PARAM_QUALITY_FACTOR);
		return (qualityFactory != null ? Double.parseDouble(unquote(qualityFactory)) : 1D);
	}

	/**
	 * Return a generic parameter value, given a parameter name.
	 * @param name the parameter name
	 * @return the parameter value; or <code>null</code> if not present
	 */
	public String getParameter(String name) {
		return this.parameters.get(name);
	}

	/**
	 * Return all generic parameter values.
	 * @return a read-only map, possibly empty, never <code>null</code>
	 */
	public Map<String, String> getParameters() {
	        return parameters;
	}

	/**
	 * Indicate whether this {@code MediaType} includes the given media type.
	 * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html}, and {@code application/*+xml}
	 * includes {@code application/soap+xml}, etc. This method is <b>not</b> symmetric.
	 * @param other the reference media type with which to compare
	 * @return <code>true</code> if this media type includes the given media type; <code>false</code> otherwise
	 */
	public boolean includes(MediaType other) {
		if (other == null) {
			return false;
		}
		if (this.isWildcardType()) {
			// */* includes anything
			return true;
		}
		else if (this.type.equals(other.type)) {
			if (this.subtype.equals(other.subtype) || this.isWildcardSubtype()) {
				return true;
			}
			// application/*+xml includes application/soap+xml
			int thisPlusIdx = this.subtype.indexOf('+');
			int otherPlusIdx = other.subtype.indexOf('+');
			if (thisPlusIdx != -1 && otherPlusIdx != -1) {
				String thisSubtypeNoSuffix = this.subtype.substring(0, thisPlusIdx);
				String thisSubtypeSuffix = this.subtype.substring(thisPlusIdx + 1);
				String otherSubtypeSuffix = other.subtype.substring(otherPlusIdx + 1);
				if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Indicate whether this {@code MediaType} is compatible with the given media type.
	 * <p>For instance, {@code text/*} is compatible with {@code text/plain}, {@code text/html}, and vice versa.
	 * In effect, this method is similar to {@link #includes(MediaType)}, except that it <b>is</b> symmetric.
	 * @param other the reference media type with which to compare
	 * @return <code>true</code> if this media type is compatible with the given media type; <code>false</code> otherwise
	 */
	public boolean isCompatibleWith(MediaType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType() || other.isWildcardType()) {
			return true;
		}
		else if (this.type.equals(other.type)) {
			if (this.subtype.equals(other.subtype) || this.isWildcardSubtype() || other.isWildcardSubtype()) {
				return true;
			}
			// application/*+xml is compatible with application/soap+xml, and vice-versa
			int thisPlusIdx = this.subtype.indexOf('+');
			int otherPlusIdx = other.subtype.indexOf('+');
			if (thisPlusIdx != -1 && otherPlusIdx != -1) {
				String thisSubtypeNoSuffix = this.subtype.substring(0, thisPlusIdx);
				String otherSubtypeNoSuffix = other.subtype.substring(0, otherPlusIdx);

				String thisSubtypeSuffix = this.subtype.substring(thisPlusIdx + 1);
				String otherSubtypeSuffix = other.subtype.substring(otherPlusIdx + 1);

				if (thisSubtypeSuffix.equals(otherSubtypeSuffix) &&
						(WILDCARD_TYPE.equals(thisSubtypeNoSuffix) || WILDCARD_TYPE.equals(otherSubtypeNoSuffix))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a replica of this instance with the quality value of the given MediaType.
	 * @return the same instance if the given MediaType doesn't have a quality value, or a new one otherwise
	 */
	public MediaType copyQualityValue(MediaType mediaType) {
		if (!mediaType.parameters.containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<String, String>(this.parameters);
		params.put(PARAM_QUALITY_FACTOR, mediaType.parameters.get(PARAM_QUALITY_FACTOR));
		return new MediaType(this, params);
	}

	/**
	 * Return a replica of this instance with its quality value removed.
	 * @return the same instance if the media type doesn't contain a quality value, or a new one otherwise
	 */
	public MediaType removeQualityValue() {
		if (!this.parameters.containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<String, String>(this.parameters);
		params.remove(PARAM_QUALITY_FACTOR);
		return new MediaType(this, params);
	}

	/**
	 * Compares this {@code MediaType} to another alphabetically.
	 * @param other media type to compare to
	 * @see #sortBySpecificity(List)
	 */
	public int compareTo(MediaType other) {
		int comp = this.type.compareToIgnoreCase(other.type);
		if (comp != 0) {
			return comp;
		}
		comp = this.subtype.compareToIgnoreCase(other.subtype);
		if (comp != 0) {
			return comp;
		}
		comp = this.parameters.size() - other.parameters.size();
		if (comp != 0) {
			return comp;
		}
		TreeSet<String> thisAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		thisAttributes.addAll(this.parameters.keySet());
		TreeSet<String> otherAttributes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		otherAttributes.addAll(other.parameters.keySet());
		Iterator<String> thisAttributesIterator = thisAttributes.iterator();
		Iterator<String> otherAttributesIterator = otherAttributes.iterator();
		while (thisAttributesIterator.hasNext()) {
			String thisAttribute = thisAttributesIterator.next();
			String otherAttribute = otherAttributesIterator.next();
			comp = thisAttribute.compareToIgnoreCase(otherAttribute);
			if (comp != 0) {
				return comp;
			}
			String thisValue = this.parameters.get(thisAttribute);
			String otherValue = other.parameters.get(otherAttribute);
			if (otherValue == null) {
				otherValue = "";
			}
			comp = thisValue.compareTo(otherValue);
			if (comp != 0) {
				return comp;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MediaType)) {
			return false;
		}
		MediaType otherType = (MediaType) other;
		return (this.type.equalsIgnoreCase(otherType.type) && this.subtype.equalsIgnoreCase(otherType.subtype) &&
				this.parameters.equals(otherType.parameters));
	}

	@Override
	public int hashCode() {
		int result = this.type.hashCode();
		result = 31 * result + this.subtype.hashCode();
		result = 31 * result + this.parameters.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendTo(builder);
		return builder.toString();
	}

	private void appendTo(StringBuilder builder) {
		builder.append(this.type);
		builder.append('/');
		builder.append(this.subtype);
		appendTo(this.parameters, builder);
	}

	private void appendTo(Map<String, String> map, StringBuilder builder) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			builder.append(';');
			builder.append(entry.getKey());
			builder.append('=');
			builder.append(entry.getValue());
		}
	}


	/**
	 * Parse the given String value into a {@code MediaType} object,
	 * with this method name following the 'valueOf' naming convention
	 * (as supported by {@link org.springframework.core.convert.ConversionService}.
	 * @see #parseMediaType(String)
	 */
	public static MediaType valueOf(String value) {
		return parseMediaType(value);
	}

	/**
	 * Parse the given String into a single {@code MediaType}.
	 * @param mediaType the string to parse
	 * @return the media type
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static MediaType parseMediaType(String mediaType) {
		Assert.hasLength(mediaType, "'mediaType' must not be empty");
		String[] parts = StringUtils.tokenizeToStringArray(mediaType, ";");

		String fullType = parts[0].trim();
		// java.net.HttpURLConnection returns a *; q=.2 Accept header
		if (WILDCARD_TYPE.equals(fullType)) {
			fullType = "*/*";
		}
		int subIndex = fullType.indexOf('/');
		if (subIndex == -1) {
			throw new IllegalArgumentException("\"" + mediaType + "\" does not contain '/'");
		}
		if (subIndex == fullType.length() - 1) {
			throw new IllegalArgumentException("\"" + mediaType + "\" does not contain subtype after '/'");
		}
		String type = fullType.substring(0, subIndex);
		String subtype = fullType.substring(subIndex + 1, fullType.length());
		if (WILDCARD_TYPE.equals(type) && !WILDCARD_TYPE.equals(subtype)) {
			throw new IllegalArgumentException("A wildcard type is legal only in '*/*' (all media types).");
		}

		Map<String, String> parameters = null;
		if (parts.length > 1) {
			parameters = new LinkedHashMap<String, String>(parts.length - 1);
			for (int i = 1; i < parts.length; i++) {
				String parameter = parts[i];
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex);
					String value = parameter.substring(eqIndex + 1, parameter.length());
					parameters.put(attribute, value);
				}
			}
		}

		return new MediaType(type, subtype, parameters);
	}


	/**
	 * Parse the given, comma-separated string into a list of {@code MediaType} objects.
	 * <p>This method can be used to parse an Accept or Content-Type header.
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<MediaType> parseMediaTypes(String mediaTypes) {
		if (!StringUtils.hasLength(mediaTypes)) {
			return Collections.emptyList();
		}
		String[] tokens = mediaTypes.split(",\\s*");
		List<MediaType> result = new ArrayList<MediaType>(tokens.length);
		for (String token : tokens) {
			result.add(parseMediaType(token));
		}
		return result;
	}

	/**
	 * Return a string representation of the given list of {@code MediaType} objects.
	 * <p>This method can be used to for an {@code Accept} or {@code Content-Type} header.
	 * @param mediaTypes the string to parse
	 * @return the list of media types
	 * @throws IllegalArgumentException if the String cannot be parsed
	 */
	public static String toString(Collection<MediaType> mediaTypes) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<MediaType> iterator = mediaTypes.iterator(); iterator.hasNext();) {
			MediaType mediaType = iterator.next();
			mediaType.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	/**
	 * Sorts the given list of {@code MediaType} objects by specificity.
	 * <p>Given two media types:
	 * <ol>
	 *   <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
	 *   wildcard is ordered before the other.</li>
	 *   <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
	 *   remain their current order.</li>
	 *   <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
	 *   the wildcard is sorted before the other.</li>
	 *   <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
	 *   and remain their current order.</li>
	 *   <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
	 *   with the highest quality value is ordered before the other.</li>
	 *   <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
	 *   media type with the most parameters is ordered before the other.</li>
	 * </ol>
	 * <p>For example:
	 * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote>
	 * <blockquote>audio/basic == audio/wave</blockquote>
	 * @param mediaTypes the list of media types to be sorted
	 * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.1">HTTP 1.1, section 14.1</a>
	 */
	public static void sortBySpecificity(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, SPECIFICITY_COMPARATOR);
		}
	}

	/**
	 * Sorts the given list of {@code MediaType} objects by quality value.
	 * <p>Given two media types:
	 * <ol>
	 *   <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
	 *   with the highest quality value is ordered before the other.</li>
	 *   <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
	 *   wildcard is ordered before the other.</li>
	 *   <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
	 *   remain their current order.</li>
	 *   <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
	 *   the wildcard is sorted before the other.</li>
	 *   <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
	 *   and remain their current order.</li>
	 *   <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
	 *   media type with the most parameters is ordered before the other.</li>
	 * </ol>
	 * @param mediaTypes the list of media types to be sorted
	 * @see #getQualityValue()
	 */
	public static void sortByQualityValue(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, QUALITY_VALUE_COMPARATOR);
		}
	}

	/**
	 * Sorts the given list of {@code MediaType} objects by specificity as the
	 * primary criteria and quality value the secondary.
	 * @see MediaType#sortBySpecificity(List)
	 * @see MediaType#sortByQualityValue(List)
	 */
	public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, new CompoundComparator<MediaType>(
					MediaType.SPECIFICITY_COMPARATOR, MediaType.QUALITY_VALUE_COMPARATOR));
		}
	}


	/**
	 * Comparator used by {@link #sortBySpecificity(List)}.
	 */
	public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new Comparator<MediaType>() {

		public int compare(MediaType mediaType1, MediaType mediaType2) {
			if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
				return 1;
			}
			else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
				return -1;
			}
			else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
				return 0;
			}
			else { // mediaType1.getType().equals(mediaType2.getType())
				if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
					return 1;
				}
				else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
					return -1;
				}
				else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
					return 0;
				}
				else { // mediaType2.getSubtype().equals(mediaType2.getSubtype())
					double quality1 = mediaType1.getQualityValue();
					double quality2 = mediaType2.getQualityValue();
					int qualityComparison = Double.compare(quality2, quality1);
					if (qualityComparison != 0) {
						return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
					}
					else {
						int paramsSize1 = mediaType1.parameters.size();
						int paramsSize2 = mediaType2.parameters.size();
						return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1)); // audio/basic;level=1 < audio/basic
					}
				}
			}
		}
	};


	/**
	 * Comparator used by {@link #sortByQualityValue(List)}.
	 */
	public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = new Comparator<MediaType>() {

		public int compare(MediaType mediaType1, MediaType mediaType2) {
			double quality1 = mediaType1.getQualityValue();
			double quality2 = mediaType2.getQualityValue();
			int qualityComparison = Double.compare(quality2, quality1);
			if (qualityComparison != 0) {
				return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
			}
			else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
				return 1;
			}
			else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
				return -1;
			}
			else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
				return 0;
			}
			else { // mediaType1.getType().equals(mediaType2.getType())
				if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
					return 1;
				}
				else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
					return -1;
				}
				else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
					return 0;
				}
				else {
					int paramsSize1 = mediaType1.parameters.size();
					int paramsSize2 = mediaType2.parameters.size();
					return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1)); // audio/basic;level=1 < audio/basic
				}
			}
		}
	};

}
