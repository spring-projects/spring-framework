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

package org.springframework.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

/**
 * Represents a MIME Type, as originally defined in RFC 2046 and subsequently
 * used in other Internet protocols including HTTP.
 *
 * <p>This class, however, does not contain support for the q-parameters used
 * in HTTP content negotiation. Those can be found in the subclass
 * {@code org.springframework.http.MediaType} in the {@code spring-web} module.
 *
 * <p>Consists of a {@linkplain #getType() type} and a {@linkplain #getSubtype() subtype}.
 * Also has functionality to parse MIME Type values from a {@code String} using
 * {@link #valueOf(String)}. For more parsing options see {@link MimeTypeUtils}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 * @see MimeTypeUtils
 */
public class MimeType implements Comparable<MimeType>, Serializable {

	private static final long serialVersionUID = 4085923477777865903L;


	protected static final String WILDCARD_TYPE = "*";

	private static final String PARAM_CHARSET = "charset";

	private static final BitSet TOKEN;

	static {
		// variable names refer to RFC 2616, section 2.2
		BitSet ctl = new BitSet(128);
		for (int i = 0; i <= 31; i++) {
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
	}


	private final String type;

	private final String subtype;

	@SuppressWarnings("serial")
	private final Map<String, String> parameters;

	private transient @Nullable Charset resolvedCharset;

	private volatile @Nullable String toStringValue;


	/**
	 * Create a new {@code MimeType} for the given primary type.
	 * <p>The {@linkplain #getSubtype() subtype} is set to <code>"&#42;"</code>,
	 * and the parameters are empty.
	 * @param type the primary type
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 */
	public MimeType(String type) {
		this(type, WILDCARD_TYPE);
	}

	/**
	 * Create a new {@code MimeType} for the given primary type and subtype.
	 * <p>The parameters are empty.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 */
	public MimeType(String type, String subtype) {
		this(type, subtype, Collections.emptyMap());
	}

	/**
	 * Create a new {@code MimeType} for the given type, subtype, and character set.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param charset the character set
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 */
	public MimeType(String type, String subtype, Charset charset) {
		this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charset.name()));
		this.resolvedCharset = charset;
	}

	/**
	 * Copy-constructor that copies the type, subtype, parameters of the given {@code MimeType},
	 * and allows to set the specified character set.
	 * @param other the other MimeType
	 * @param charset the character set
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 * @since 4.3
	 */
	public MimeType(MimeType other, Charset charset) {
		this(other.getType(), other.getSubtype(), addCharsetParameter(charset, other.getParameters()));
		this.resolvedCharset = charset;
	}

	/**
	 * Copy-constructor that copies the type and subtype of the given {@code MimeType},
	 * and allows for different parameter.
	 * @param other the other MimeType
	 * @param parameters the parameters (may be {@code null})
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 */
	public MimeType(MimeType other, @Nullable Map<String, String> parameters) {
		this(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * Create a new {@code MimeType} for the given type, subtype, and parameters.
	 * @param type the primary type
	 * @param subtype the subtype
	 * @param parameters the parameters (may be {@code null})
	 * @throws IllegalArgumentException if any of the parameters contains illegal characters
	 */
	public MimeType(String type, String subtype, @Nullable Map<String, String> parameters) {
		Assert.hasLength(type, "'type' must not be empty");
		Assert.hasLength(subtype, "'subtype' must not be empty");
		checkToken(type);
		checkToken(subtype);
		this.type = type.toLowerCase(Locale.ROOT);
		this.subtype = subtype.toLowerCase(Locale.ROOT);
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ROOT);
			parameters.forEach((parameter, value) -> {
				checkParameters(parameter, value);
				map.put(parameter, value);
			});
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * Copy-constructor that copies the type, subtype and parameters of the given {@code MimeType},
	 * skipping checks performed in other constructors.
	 * @param other the other MimeType
	 * @since 5.3
	 */
	protected MimeType(MimeType other) {
		this.type = other.type;
		this.subtype = other.subtype;
		this.parameters = other.parameters;
		this.resolvedCharset = other.resolvedCharset;
		this.toStringValue = other.toStringValue;
	}

	/**
	 * Checks the given token string for illegal characters, as defined in RFC 2616,
	 * section 2.2.
	 * @throws IllegalArgumentException in case of illegal characters
	 * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
	 */
	private void checkToken(String token) {
		for (int i = 0; i < token.length(); i++) {
			char ch = token.charAt(i);
			if (!TOKEN.get(ch)) {
				throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
			}
		}
	}

	protected void checkParameters(String parameter, String value) {
		Assert.hasLength(parameter, "'parameter' must not be empty");
		Assert.hasLength(value, "'value' must not be empty");
		checkToken(parameter);
		if (PARAM_CHARSET.equals(parameter)) {
			if (this.resolvedCharset == null) {
				this.resolvedCharset = Charset.forName(unquote(value));
			}
		}
		else if (!isQuotedString(value)) {
			checkToken(value);
		}
	}

	private boolean isQuotedString(String s) {
		if (s.length() < 2) {
			return false;
		}
		return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
	}

	protected String unquote(String s) {
		return (isQuotedString(s) ? s.substring(1, s.length() - 1) : s);
	}

	/**
	 * Indicates whether the {@linkplain #getType() type} is the wildcard character
	 * <code>&#42;</code> or not.
	 */
	public boolean isWildcardType() {
		return WILDCARD_TYPE.equals(getType());
	}

	/**
	 * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard
	 * character <code>&#42;</code> or the wildcard character followed by a suffix
	 * (for example, <code>&#42;+xml</code>).
	 * @return whether the subtype is a wildcard
	 */
	public boolean isWildcardSubtype() {
		String subtype = getSubtype();
		return (WILDCARD_TYPE.equals(subtype) || subtype.startsWith("*+"));
	}

	/**
	 * Indicates whether this MIME Type is concrete, i.e. whether neither the type
	 * nor the subtype is a wildcard character <code>&#42;</code>.
	 * @return whether this MIME Type is concrete
	 */
	public boolean isConcrete() {
		return !isWildcardType() && !isWildcardSubtype();
	}

	/**
	 * Return the primary type.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Return the subtype.
	 */
	public String getSubtype() {
		return this.subtype;
	}

	/**
	 * Return the subtype suffix as defined in RFC 6839.
	 * @since 5.3
	 */
	public @Nullable String getSubtypeSuffix() {
		int suffixIndex = this.subtype.lastIndexOf('+');
		if (suffixIndex != -1 && this.subtype.length() > suffixIndex) {
			return this.subtype.substring(suffixIndex + 1);
		}
		return null;
	}

	/**
	 * Return the character set, as indicated by a {@code charset} parameter, if any.
	 * @return the character set, or {@code null} if not available
	 * @since 4.3
	 */
	public @Nullable Charset getCharset() {
		return this.resolvedCharset;
	}

	/**
	 * Return a generic parameter value, given a parameter name.
	 * @param name the parameter name
	 * @return the parameter value, or {@code null} if not present
	 */
	public @Nullable String getParameter(String name) {
		return this.parameters.get(name);
	}

	/**
	 * Return all generic parameter values.
	 * @return a read-only map (possibly empty, never {@code null})
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * Indicate whether this MIME Type includes the given MIME Type.
	 * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html},
	 * and {@code application/*+xml} includes {@code application/soap+xml}, etc.
	 * This method is <b>not</b> symmetric.
	 * @param other the reference MIME Type with which to compare
	 * @return {@code true} if this MIME Type includes the given MIME Type;
	 * {@code false} otherwise
	 */
	public boolean includes(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType()) {
			// */* includes anything
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			if (isWildcardSubtype()) {
				// Wildcard with suffix, for example, application/*+xml
				int thisPlusIdx = getSubtype().lastIndexOf('+');
				if (thisPlusIdx == -1) {
					return true;
				}
				else {
					// application/*+xml includes application/soap+xml
					int otherPlusIdx = other.getSubtype().lastIndexOf('+');
					if (otherPlusIdx != -1) {
						String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
						String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
						String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
						if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Indicate whether this MIME Type is compatible with the given MIME Type.
	 * <p>For instance, {@code text/*} is compatible with {@code text/plain},
	 * {@code text/html}, and vice versa. In effect, this method is similar to
	 * {@link #includes}, except that it <b>is</b> symmetric.
	 * @param other the reference MIME Type with which to compare
	 * @return {@code true} if this MIME Type is compatible with the given MIME Type;
	 * {@code false} otherwise
	 */
	public boolean isCompatibleWith(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType() || other.isWildcardType()) {
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			if (isWildcardSubtype() || other.isWildcardSubtype()) {
				String thisSuffix = getSubtypeSuffix();
				String otherSuffix = other.getSubtypeSuffix();
				if (getSubtype().equals(WILDCARD_TYPE) || other.getSubtype().equals(WILDCARD_TYPE)) {
					return true;
				}
				else if (isWildcardSubtype() && thisSuffix != null) {
					return (thisSuffix.equals(other.getSubtype()) || thisSuffix.equals(otherSuffix));
				}
				else if (other.isWildcardSubtype() && otherSuffix != null) {
					return (getSubtype().equals(otherSuffix) || otherSuffix.equals(thisSuffix));
				}
			}
		}
		return false;
	}

	/**
	 * Similar to {@link #equals(Object)} but based on the type and subtype
	 * only, i.e. ignoring parameters.
	 * @param other the other mime type to compare to
	 * @return whether the two mime types have the same type and subtype
	 * @since 5.1.4
	 */
	public boolean equalsTypeAndSubtype(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		return this.type.equalsIgnoreCase(other.type) && this.subtype.equalsIgnoreCase(other.subtype);
	}

	/**
	 * Unlike {@link Collection#contains(Object)} which relies on
	 * {@link MimeType#equals(Object)}, this method only checks the type and the
	 * subtype, but otherwise ignores parameters.
	 * @param mimeTypes the list of mime types to perform the check against
	 * @return whether the list contains the given mime type
	 * @since 5.1.4
	 */
	public boolean isPresentIn(Collection<? extends MimeType> mimeTypes) {
		for (MimeType mimeType : mimeTypes) {
			if (mimeType.equalsTypeAndSubtype(this)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MimeType otherType &&
				this.type.equalsIgnoreCase(otherType.type) &&
				this.subtype.equalsIgnoreCase(otherType.subtype) &&
				parametersAreEqual(otherType)));
	}

	/**
	 * Determine if the parameters in this {@code MimeType} and the supplied
	 * {@code MimeType} are equal, performing case-insensitive comparisons
	 * for {@link Charset Charsets}.
	 * @since 4.2
	 */
	private boolean parametersAreEqual(MimeType other) {
		if (this.parameters.size() != other.parameters.size()) {
			return false;
		}

		for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
			String key = entry.getKey();
			if (!other.parameters.containsKey(key)) {
				return false;
			}
			if (PARAM_CHARSET.equals(key)) {
				if (!ObjectUtils.nullSafeEquals(getCharset(), other.getCharset())) {
					return false;
				}
			}
			else if (!ObjectUtils.nullSafeEquals(entry.getValue(), other.parameters.get(key))) {
				return false;
			}
		}

		return true;
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
		String value = this.toStringValue;
		if (value == null) {
			StringBuilder builder = new StringBuilder();
			appendTo(builder);
			value = builder.toString();
			this.toStringValue = value;
		}
		return value;
	}

	protected void appendTo(StringBuilder builder) {
		builder.append(this.type);
		builder.append('/');
		builder.append(this.subtype);
		appendTo(this.parameters, builder);
	}

	private void appendTo(Map<String, String> map, StringBuilder builder) {
		map.forEach((key, val) -> {
			builder.append(';');
			builder.append(key);
			builder.append('=');
			builder.append(val);
		});
	}

	/**
	 * Compares this MIME Type to another alphabetically.
	 * @param other the MIME Type to compare to
	 */
	@Override
	public int compareTo(MimeType other) {
		int comp = getType().compareToIgnoreCase(other.getType());
		if (comp != 0) {
			return comp;
		}
		comp = getSubtype().compareToIgnoreCase(other.getSubtype());
		if (comp != 0) {
			return comp;
		}
		comp = getParameters().size() - other.getParameters().size();
		if (comp != 0) {
			return comp;
		}

		TreeSet<String> thisAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		thisAttributes.addAll(getParameters().keySet());
		TreeSet<String> otherAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		otherAttributes.addAll(other.getParameters().keySet());
		Iterator<String> thisAttributesIterator = thisAttributes.iterator();
		Iterator<String> otherAttributesIterator = otherAttributes.iterator();

		while (thisAttributesIterator.hasNext()) {
			String thisAttribute = thisAttributesIterator.next();
			String otherAttribute = otherAttributesIterator.next();
			comp = thisAttribute.compareToIgnoreCase(otherAttribute);
			if (comp != 0) {
				return comp;
			}
			if (PARAM_CHARSET.equals(thisAttribute)) {
				Charset thisCharset = getCharset();
				Charset otherCharset = other.getCharset();
				if (thisCharset != otherCharset) {
					if (thisCharset == null) {
						return -1;
					}
					if (otherCharset == null) {
						return 1;
					}
					comp = thisCharset.compareTo(otherCharset);
					if (comp != 0) {
						return comp;
					}
				}
			}
			else {
				String thisValue = getParameters().get(thisAttribute);
				String otherValue = other.getParameters().get(otherAttribute);
				Assert.notNull(thisValue, "Parameter for " + thisAttribute + " must not be null");
				if (otherValue == null) {
					otherValue = "";
				}
				comp = thisValue.compareTo(otherValue);
				if (comp != 0) {
					return comp;
				}
			}
		}

		return 0;
	}

	/**
	 * Indicates whether this {@code MimeType} is more specific than the given
	 * type.
	 * <ol>
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
	public boolean isMoreSpecific(MimeType other) {
		Assert.notNull(other, "Other must not be null");
		boolean thisWildcard = isWildcardType();
		boolean otherWildcard = other.isWildcardType();
		if (thisWildcard && !otherWildcard) {  // */* > audio/*
			return false;
		}
		else if (!thisWildcard && otherWildcard) {  // audio/* < */*
			return true;
		}
		else {
			boolean thisWildcardSubtype = isWildcardSubtype();
			boolean otherWildcardSubtype = other.isWildcardSubtype();
			if (thisWildcardSubtype && !otherWildcardSubtype) {  // audio/* > audio/basic
				return false;
			}
			else if (!thisWildcardSubtype && otherWildcardSubtype) {  // audio/basic < audio/*
				return true;
			}
			else if (getType().equals(other.getType()) && getSubtype().equals(other.getSubtype())) {
				int paramsSize1 = getParameters().size();
				int paramsSize2 = other.getParameters().size();
				return paramsSize1 > paramsSize2;
			}
			else {
				return false;
			}
		}
	}

	/**
	 * Indicates whether this {@code MimeType} is less specific than the given type.
	 * <ol>
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
	public boolean isLessSpecific(MimeType other) {
		Assert.notNull(other, "Other must not be null");
		return other.isMoreSpecific(this);
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		String charsetName = getParameter(PARAM_CHARSET);
		if (charsetName != null) {
			this.resolvedCharset = Charset.forName(unquote(charsetName));
		}
	}


	/**
	 * Parse the given String value into a {@code MimeType} object,
	 * with this method name following the 'valueOf' naming convention
	 * (as supported by {@link org.springframework.core.convert.ConversionService}).
	 * @see MimeTypeUtils#parseMimeType(String)
	 */
	public static MimeType valueOf(String value) {
		return MimeTypeUtils.parseMimeType(value);
	}

	private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
		Map<String, String> map = new LinkedHashMap<>(parameters);
		map.put(PARAM_CHARSET, charset.name());
		return map;
	}

}
