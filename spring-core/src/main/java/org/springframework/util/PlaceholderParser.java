/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * Parser for Strings that have placeholder values in them. In its simplest form,
 * a placeholder takes the form of {@code ${name}}, where {@code name} is the key
 * that can be resolved using a {@link PlaceholderResolver PlaceholderResolver},
 * <code>${</code> the prefix, and <code>}</code> the suffix.
 *
 * <p>A placeholder can also have a default value if its key does not represent a
 * known property. The default value is separated from the key using a
 * {@code separator}. For instance {@code ${name:John}} resolves to {@code John} if
 * the placeholder resolver does not provide a value for the {@code name}
 * property.
 *
 * <p>Placeholders can also have a more complex structure, and the resolution of
 * a given key can involve the resolution of nested placeholders. Default values
 * can also have placeholders.
 *
 * <p>For situations where the syntax of a valid placeholder matches a String that
 * must be rendered as is, the placeholder can be escaped using an {@code escape}
 * character. For instance {@code \${name}} resolves as {@code ${name}}.
 *
 * <p>The prefix, suffix, separator, and escape characters are configurable. Only
 * the prefix and suffix are mandatory, and the support for default values or
 * escaping is conditional on providing non-null values for them.
 *
 * <p>This parser makes sure to resolves placeholders as lazily as possible.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
final class PlaceholderParser {

	private static final Log logger = LogFactory.getLog(PlaceholderParser.class);

	private static final Map<String, String> wellKnownSimplePrefixes = Map.of(
			"}", "{",
			"]", "[",
			")", "("
		);


	private final String prefix;

	private final String suffix;

	private final String simplePrefix;

	@Nullable
	private final String separator;

	private final boolean ignoreUnresolvablePlaceholders;

	@Nullable
	private final Character escape;


	/**
	 * Create an instance using the specified input for the parser.
	 * @param prefix the prefix that denotes the start of a placeholder
	 * @param suffix the suffix that denotes the end of a placeholder
	 * @param separator the separating character between the placeholder
	 * variable and the associated default value, if any
	 * @param escape the character to use at the beginning of a placeholder
	 * prefix or separator to escape it and render it as is
	 * @param ignoreUnresolvablePlaceholders whether unresolvable placeholders
	 * should be ignored ({@code true}) or cause an exception ({@code false})
	 */
	PlaceholderParser(String prefix, String suffix, @Nullable String separator,
			@Nullable Character escape, boolean ignoreUnresolvablePlaceholders) {
		this.prefix = prefix;
		this.suffix = suffix;
		String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.suffix);
		if (simplePrefixForSuffix != null && this.prefix.endsWith(simplePrefixForSuffix)) {
			this.simplePrefix = simplePrefixForSuffix;
		}
		else {
			this.simplePrefix = this.prefix;
		}
		this.separator = separator;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
		this.escape = escape;
	}

	/**
	 * Replace all placeholders of format {@code ${name}} with the value returned
	 * from the supplied {@link PlaceholderResolver}.
	 * @param value the value containing the placeholders to be replaced
	 * @param placeholderResolver the {@code PlaceholderResolver} to use for replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
		Assert.notNull(value, "'value' must not be null");
		ParsedValue parsedValue = parse(value);
		PartResolutionContext resolutionContext = new PartResolutionContext(placeholderResolver,
				this.prefix, this.suffix, this.ignoreUnresolvablePlaceholders,
				candidate -> parse(candidate, false));
		return parsedValue.resolve(resolutionContext);
	}

	/**
	 * Parse the specified value.
	 * @param value the value containing the placeholders to be replaced
	 * @return the different parts that have been identified
	 */
	ParsedValue parse(String value) {
		List<Part> parts = parse(value, false);
		return new ParsedValue(value, parts);
	}

	private List<Part> parse(String value, boolean inPlaceholder) {
		LinkedList<Part> parts = new LinkedList<>();
		int startIndex = nextStartPrefix(value, 0);
		if (startIndex == -1) {
			Part part = (inPlaceholder ? createSimplePlaceholderPart(value) : new TextPart(value));
			parts.add(part);
			return parts;
		}
		int position = 0;
		while (startIndex != -1) {
			int endIndex = nextValidEndPrefix(value, startIndex);
			if (endIndex == -1) { // Not a valid placeholder, consume the prefix and continue
				addText(value, position, startIndex + this.prefix.length(), parts);
				position = startIndex + this.prefix.length();
				startIndex = nextStartPrefix(value, position);
			}
			else if (isEscaped(value, startIndex)) { // Not a valid index, accumulate and skip the escape character
				addText(value, position, startIndex - 1, parts);
				addText(value, startIndex, startIndex + this.prefix.length(), parts);
				position = startIndex + this.prefix.length();
				startIndex = nextStartPrefix(value, position);
			}
			else { // Found valid placeholder, recursive parsing
				addText(value, position, startIndex, parts);
				String placeholder = value.substring(startIndex + this.prefix.length(), endIndex);
				List<Part> placeholderParts = parse(placeholder, true);
				parts.addAll(placeholderParts);
				startIndex = nextStartPrefix(value, endIndex + this.suffix.length());
				position = endIndex + this.suffix.length();
			}
		}
		// Add rest of text if necessary
		addText(value, position, value.length(), parts);
		return (inPlaceholder ? List.of(createNestedPlaceholderPart(value, parts)) : parts);
	}

	private SimplePlaceholderPart createSimplePlaceholderPart(String text) {
		String[] keyAndDefault = splitKeyAndDefault(text);
		return ((keyAndDefault != null) ? new SimplePlaceholderPart(text, keyAndDefault[0], keyAndDefault[1]) :
				new SimplePlaceholderPart(text, text, null));
	}

	private NestedPlaceholderPart createNestedPlaceholderPart(String text, List<Part> parts) {
		if (this.separator == null) {
			return new NestedPlaceholderPart(text, parts, null);
		}
		List<Part> keyParts = new ArrayList<>();
		List<Part> defaultParts = new ArrayList<>();
		for (int i = 0; i < parts.size(); i++) {
			Part part = parts.get(i);
			if (!(part instanceof TextPart)) {
				keyParts.add(part);
			}
			else {
				String candidate = part.text();
				String[] keyAndDefault = splitKeyAndDefault(candidate);
				if (keyAndDefault != null) {
					keyParts.add(new TextPart(keyAndDefault[0]));
					if (keyAndDefault[1] != null) {
						defaultParts.add(new TextPart(keyAndDefault[1]));
					}
					defaultParts.addAll(parts.subList(i + 1, parts.size()));
					return new NestedPlaceholderPart(text, keyParts, defaultParts);
				}
				else {
					keyParts.add(part);
				}
			}
		}
		// No separator found
		return new NestedPlaceholderPart(text, parts, null);
	}

	@Nullable
	private String[] splitKeyAndDefault(String value) {
		if (this.separator == null || !value.contains(this.separator)) {
			return null;
		}
		int position = 0;
		int index = value.indexOf(this.separator, position);
		StringBuilder buffer = new StringBuilder();
		while (index != -1) {
			if (isEscaped(value, index)) {
				// Accumulate, without the escape character.
				buffer.append(value, position, index - 1);
				buffer.append(value, index, index + this.separator.length());
				position = index + this.separator.length();
				index = value.indexOf(this.separator, position);
			}
			else {
				buffer.append(value, position, index);
				String key = buffer.toString();
				String fallback = value.substring(index + this.separator.length());
				return new String[] { key, fallback };
			}
		}
		buffer.append(value, position, value.length());
		return new String[] { buffer.toString(), null };
	}

	private static void addText(String value, int start, int end, LinkedList<Part> parts) {
		if (start > end) {
			return;
		}
		String text = value.substring(start, end);
		if (!text.isEmpty()) {
			if (!parts.isEmpty()) {
				Part current = parts.removeLast();
				if (current instanceof TextPart textPart) {
					parts.add(new TextPart(textPart.text + text));
				}
				else {
					parts.add(current);
					parts.add(new TextPart(text));
				}
			}
			else {
				parts.add(new TextPart(text));
			}
		}
	}


	private int nextStartPrefix(String value, int index) {
		return value.indexOf(this.prefix, index);
	}

	private int nextValidEndPrefix(String value, int startIndex) {
		int index = startIndex + this.prefix.length();
		int withinNestedPlaceholder = 0;
		while (index < value.length()) {
			if (StringUtils.substringMatch(value, index, this.suffix)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + this.suffix.length();
				}
				else {
					return index;
				}
			}
			else if (StringUtils.substringMatch(value, index, this.simplePrefix)) {
				withinNestedPlaceholder++;
				index = index + this.simplePrefix.length();
			}
			else {
				index++;
			}
		}
		return -1;
	}

	private boolean isEscaped(String value, int index) {
		return (this.escape != null && index > 0 && value.charAt(index - 1) == this.escape);
	}

	/**
	 * Provide the necessary context to handle and resolve underlying placeholders.
	 */
	static class PartResolutionContext implements PlaceholderResolver {

		private final String prefix;

		private final String suffix;

		private final boolean ignoreUnresolvablePlaceholders;

		private final Function<String, List<Part>> parser;

		private final PlaceholderResolver resolver;

		@Nullable
		private Set<String> visitedPlaceholders;


		PartResolutionContext(PlaceholderResolver resolver, String prefix, String suffix,
				boolean ignoreUnresolvablePlaceholders, Function<String, List<Part>> parser) {
			this.prefix = prefix;
			this.suffix = suffix;
			this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
			this.parser = parser;
			this.resolver = resolver;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			String value = this.resolver.resolvePlaceholder(placeholderName);
			if (value != null && logger.isTraceEnabled()) {
				logger.trace("Resolved placeholder '" + placeholderName + "'");
			}
			return value;
		}

		public String handleUnresolvablePlaceholder(String key, String text) {
			if (this.ignoreUnresolvablePlaceholders) {
				return toPlaceholderText(key);
			}
			String originalValue = (!key.equals(text) ? toPlaceholderText(text) : null);
			throw new PlaceholderResolutionException(
					"Could not resolve placeholder '%s'".formatted(key), key, originalValue);
		}

		private String toPlaceholderText(String text) {
			return this.prefix + text + this.suffix;
		}

		public List<Part> parse(String text) {
			return this.parser.apply(text);
		}

		public void flagPlaceholderAsVisited(String placeholder) {
			if (this.visitedPlaceholders == null) {
				this.visitedPlaceholders = new HashSet<>(4);
			}
			if (!this.visitedPlaceholders.add(placeholder)) {
				throw new PlaceholderResolutionException(
						"Circular placeholder reference '%s'".formatted(placeholder), placeholder, null);
			}
		}

		public void removePlaceholder(String placeholder) {
			Assert.state(this.visitedPlaceholders != null, "Visited placeholders must not be null");
			this.visitedPlaceholders.remove(placeholder);
		}

	}


	/**
	 * A part is a section of a String containing placeholders to replace.
	 */
	interface Part {

		/**
		 * Resolve this part using the specified {@link PartResolutionContext}.
		 * @param resolutionContext the context to use
		 * @return the resolved part
		 */
		String resolve(PartResolutionContext resolutionContext);

		/**
		 * Provide a textual representation of this part.
		 * @return the raw text that this part defines
		 */
		String text();

		/**
		 * Return a String that appends the resolution of the specified parts.
		 * @param parts the parts to resolve
		 * @param resolutionContext the context to use for the resolution
		 * @return a concatenation of the supplied parts with placeholders replaced inline
		 */
		static String resolveAll(Iterable<Part> parts, PartResolutionContext resolutionContext) {
			StringBuilder sb = new StringBuilder();
			for (Part part : parts) {
				sb.append(part.resolve(resolutionContext));
			}
			return sb.toString();
		}

	}

	/**
	 * A representation of the parsing of an input string.
	 * @param text the raw input string
	 * @param parts the parts that appear in the string, in order
	 */
	record ParsedValue(String text, List<Part> parts) {

		public String resolve(PartResolutionContext resolutionContext) {
			try {
				return Part.resolveAll(this.parts, resolutionContext);
			}
			catch (PlaceholderResolutionException ex) {
				throw ex.withValue(this.text);
			}
		}

	}

	/**
	 * A {@link Part} implementation that does not contain a valid placeholder.
	 * @param text the raw (and resolved) text
	 */
	record TextPart(String text) implements Part {

		@Override
		public String resolve(PartResolutionContext resolutionContext) {
			return this.text;
		}

	}

	/**
	 * A {@link Part} implementation that represents a single placeholder with
	 * a hard-coded fallback.
	 * @param text the raw text
	 * @param key the key of the placeholder
	 * @param fallback the fallback to use, if any
	 */
	record SimplePlaceholderPart(String text, String key, @Nullable String fallback) implements Part {

		@Override
		public String resolve(PartResolutionContext resolutionContext) {
			String resolvedValue = resolveToText(resolutionContext, this.key);
			if (resolvedValue != null) {
				return resolvedValue;
			}
			else if (this.fallback != null) {
				return this.fallback;
			}
			return resolutionContext.handleUnresolvablePlaceholder(this.key, this.text);
		}

		@Nullable
		private String resolveToText(PartResolutionContext resolutionContext, String text) {
			String resolvedValue = resolutionContext.resolvePlaceholder(text);
			if (resolvedValue != null) {
				resolutionContext.flagPlaceholderAsVisited(text);
				// Let's check if we need to recursively resolve that value
				List<Part> nestedParts = resolutionContext.parse(resolvedValue);
				String value = toText(nestedParts);
				if (!isTextOnly(nestedParts)) {
					value = new ParsedValue(resolvedValue, nestedParts).resolve(resolutionContext);
				}
				resolutionContext.removePlaceholder(text);
				return value;
			}
			// Not found
			return null;
		}

		private boolean isTextOnly(List<Part> parts) {
			return parts.stream().allMatch(TextPart.class::isInstance);
		}

		private String toText(List<Part> parts) {
			StringBuilder sb = new StringBuilder();
			parts.forEach(part -> sb.append(part.text()));
			return sb.toString();
		}

	}

	/**
	 * A {@link Part} implementation that represents a single placeholder
	 * containing nested placeholders.
	 * @param text the raw text of the root placeholder
	 * @param keyParts the parts of the key
	 * @param defaultParts the parts of the fallback, if any
	 */
	record NestedPlaceholderPart(String text, List<Part> keyParts, @Nullable List<Part> defaultParts) implements Part {

		@Override
		public String resolve(PartResolutionContext resolutionContext) {
			String resolvedKey = Part.resolveAll(this.keyParts, resolutionContext);
			String value = resolutionContext.resolvePlaceholder(resolvedKey);
			if (value != null) {
				return value;
			}
			else if (this.defaultParts != null) {
				return Part.resolveAll(this.defaultParts, resolutionContext);
			}
			return resolutionContext.handleUnresolvablePlaceholder(resolvedKey, this.text);
		}

	}

}
