/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.MimeType.SpecificityComparator;



/**
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MimeTypeUtils {

	/**
	 * Public constant mime type that includes all media ranges (i.e. "&#42;/&#42;").
	 */
	public static final MimeType ALL;

	/**
	 * A String equivalent of {@link MediaType#ALL}.
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 *  Public constant mime type for {@code application/atom+xml}.
	 */
	public final static MimeType APPLICATION_ATOM_XML;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_ATOM_XML}.
	 */
	public final static String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * Public constant mime type for {@code application/x-www-form-urlencoded}.
	 *  */
	public final static MimeType APPLICATION_FORM_URLENCODED;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_FORM_URLENCODED}.
	 */
	public final static String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * Public constant mime type for {@code application/json}.
	 * */
	public final static MimeType APPLICATION_JSON;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_JSON}.
	 */
	public final static String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * Public constant mime type for {@code application/octet-stream}.
	 *  */
	public final static MimeType APPLICATION_OCTET_STREAM;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_OCTET_STREAM}.
	 */
	public final static String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * Public constant mime type for {@code application/xhtml+xml}.
	 *  */
	public final static MimeType APPLICATION_XHTML_XML;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_XHTML_XML}.
	 */
	public final static String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * Public constant mime type for {@code application/xml}.
	 */
	public final static MimeType APPLICATION_XML;

	/**
	 * A String equivalent of {@link MimeType#APPLICATION_XML}.
	 */
	public final static String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * Public constant mime type for {@code image/gif}.
	 */
	public final static MimeType IMAGE_GIF;

	/**
	 * A String equivalent of {@link MimeType#IMAGE_GIF}.
	 */
	public final static String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * Public constant mime type for {@code image/jpeg}.
	 */
	public final static MimeType IMAGE_JPEG;

	/**
	 * A String equivalent of {@link MimeType#IMAGE_JPEG}.
	 */
	public final static String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * Public constant mime type for {@code image/png}.
	 */
	public final static MimeType IMAGE_PNG;

	/**
	 * A String equivalent of {@link MimeType#IMAGE_PNG}.
	 */
	public final static String IMAGE_PNG_VALUE = "image/png";

	/**
	 * Public constant mime type for {@code multipart/form-data}.
	 *  */
	public final static MimeType MULTIPART_FORM_DATA;

	/**
	 * A String equivalent of {@link MimeType#MULTIPART_FORM_DATA}.
	 */
	public final static String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * Public constant mime type for {@code text/html}.
	 *  */
	public final static MimeType TEXT_HTML;

	/**
	 * A String equivalent of {@link MimeType#TEXT_HTML}.
	 */
	public final static String TEXT_HTML_VALUE = "text/html";

	/**
	 * Public constant mime type for {@code text/plain}.
	 *  */
	public final static MimeType TEXT_PLAIN;

	/**
	 * A String equivalent of {@link MimeType#TEXT_PLAIN}.
	 */
	public final static String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * Public constant mime type for {@code text/xml}.
	 *  */
	public final static MimeType TEXT_XML;

	/**
	 * A String equivalent of {@link MimeType#TEXT_XML}.
	 */
	public final static String TEXT_XML_VALUE = "text/xml";


	static {
		ALL = MimeType.valueOf(ALL_VALUE);
		APPLICATION_ATOM_XML = MimeType.valueOf(APPLICATION_ATOM_XML_VALUE);
		APPLICATION_FORM_URLENCODED = MimeType.valueOf(APPLICATION_FORM_URLENCODED_VALUE);
		APPLICATION_JSON = MimeType.valueOf(APPLICATION_JSON_VALUE);
		APPLICATION_OCTET_STREAM = MimeType.valueOf(APPLICATION_OCTET_STREAM_VALUE);
		APPLICATION_XHTML_XML = MimeType.valueOf(APPLICATION_XHTML_XML_VALUE);
		APPLICATION_XML = MimeType.valueOf(APPLICATION_XML_VALUE);
		IMAGE_GIF = MimeType.valueOf(IMAGE_GIF_VALUE);
		IMAGE_JPEG = MimeType.valueOf(IMAGE_JPEG_VALUE);
		IMAGE_PNG = MimeType.valueOf(IMAGE_PNG_VALUE);
		MULTIPART_FORM_DATA = MimeType.valueOf(MULTIPART_FORM_DATA_VALUE);
		TEXT_HTML = MimeType.valueOf(TEXT_HTML_VALUE);
		TEXT_PLAIN = MimeType.valueOf(TEXT_PLAIN_VALUE);
		TEXT_XML = MimeType.valueOf(TEXT_XML_VALUE);
	}


	/**
	 * Parse the given String into a single {@code MimeType}.
	 * @param mimeType the string to parse
	 * @return the mime type
	 * @throws InvalidMimeTypeException if the string cannot be parsed
	 */
	public static MimeType parseMimeType(String mimeType) {
		if (!StringUtils.hasLength(mimeType)) {
			throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
		}
		String[] parts = StringUtils.tokenizeToStringArray(mimeType, ";");

		String fullType = parts[0].trim();
		// java.net.HttpURLConnection returns a *; q=.2 Accept header
		if (MimeType.WILDCARD_TYPE.equals(fullType)) {
			fullType = "*/*";
		}
		int subIndex = fullType.indexOf('/');
		if (subIndex == -1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain '/'");
		}
		if (subIndex == fullType.length() - 1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain subtype after '/'");
		}
		String type = fullType.substring(0, subIndex);
		String subtype = fullType.substring(subIndex + 1, fullType.length());
		if (MimeType.WILDCARD_TYPE.equals(type) && !MimeType.WILDCARD_TYPE.equals(subtype)) {
			throw new InvalidMimeTypeException(mimeType, "wildcard type is legal only in '*/*' (all mime types)");
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

		try {
			return new MimeType(type, subtype, parameters);
		}
		catch (UnsupportedCharsetException ex) {
			throw new InvalidMimeTypeException(mimeType, "unsupported charset '" + ex.getCharsetName() + "'");
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMimeTypeException(mimeType, ex.getMessage());
		}
	}

	/**
	 * Parse the given, comma-separated string into a list of {@code MimeType} objects.
	 * @param mimeTypes the string to parse
	 * @return the list of mime types
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<MimeType> parseMimeTypes(String mimeTypes) {
		if (!StringUtils.hasLength(mimeTypes)) {
			return Collections.emptyList();
		}
		String[] tokens = mimeTypes.split(",\\s*");
		List<MimeType> result = new ArrayList<MimeType>(tokens.length);
		for (String token : tokens) {
			result.add(parseMimeType(token));
		}
		return result;
	}

	/**
	 * Return a string representation of the given list of {@code MimeType} objects.
	 * @param mimeTypes the string to parse
	 * @return the list of mime types
	 * @throws IllegalArgumentException if the String cannot be parsed
	 */
	public static String toString(Collection<? extends MimeType> mimeTypes) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<? extends MimeType> iterator = mimeTypes.iterator(); iterator.hasNext();) {
			MimeType mimeType = iterator.next();
			mimeType.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}


	/**
	 * Sorts the given list of {@code MimeType} objects by specificity.
	 * <p>
	 * Given two mime types:
	 * <ol>
	 * <li>if either mime type has a {@linkplain #isWildcardType() wildcard type}, then
	 * the mime type without the wildcard is ordered before the other.</li>
	 * <li>if the two mime types have different {@linkplain #getType() types}, then
	 * they are considered equal and remain their current order.</li>
	 * <li>if either mime type has a {@linkplain #isWildcardSubtype() wildcard subtype}
	 * , then the mime type without the wildcard is sorted before the other.</li>
	 * <li>if the two mime types have different {@linkplain #getSubtype() subtypes},
	 * then they are considered equal and remain their current order.</li>
	 * <li>if the two mime types have a different amount of
	 * {@linkplain #getParameter(String) parameters}, then the mime type with the most
	 * parameters is ordered before the other.</li>
	 * </ol>
	 * <p>
	 * For example: <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote> <blockquote>audio/basic ==
	 * audio/wave</blockquote>
	 *
	 * @param mimeTypes the list of mime types to be sorted
	 * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.1">HTTP 1.1, section
	 *      14.1</a>
	 */
	public static void sortBySpecificity(List<MimeType> mimeTypes) {
		Assert.notNull(mimeTypes, "'mimeTypes' must not be null");
		if (mimeTypes.size() > 1) {
			Collections.sort(mimeTypes, SPECIFICITY_COMPARATOR);
		}
	}


	/**
	 * Comparator used by {@link #sortBySpecificity(List)}.
	 */
	public static final Comparator<MimeType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MimeType>();

}
