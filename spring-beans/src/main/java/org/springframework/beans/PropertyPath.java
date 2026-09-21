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

package org.springframework.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A parsed bean property path, such as {@code "person.addresses[1].city"}.
 *
 * <p>Parses bean property paths for property access: it decides whether a given
 * string is a well-formed property path (see grammar) and where each path
 * segment begins and ends. The {@linkplain #canonicalName() canonical form}
 * is used for policy matching and error reporting.
 * The {@linkplain #segments() structured segment list} is used for property
 * navigation within beans.
 *
 * <p>The grammar is:
 * <pre>
 * PropertyPath := Segment ('.' Segment)*
 * Segment      := Name Index*                  -- Name may be empty only if at least one Index follows
 * Name         := char* excluding '.', '[', ']'
 * Index        := '[' Key ']'
 * Key          := QuotedKey | RawKey
 * QuotedKey    := "'" [^']* "'"  |  '"' [^"]* '"'
 * RawKey       := char* excluding quote characters, with balanced '[' / ']' nesting
 * </pre>
 *
 * <p>Invalid property paths are rejected with {@link InvalidPropertyPathException}.
 *
 * @author Brian Clozel
 * @since 7.1
 */
public final class PropertyPath {

	private final String canonicalName;

	private final List<Segment> segments;


	private PropertyPath(String canonicalName, List<Segment> segments) {
		this.canonicalName = canonicalName;
		this.segments = segments;
	}


	/**
	 * Parse the given property path.
	 * @param path the property path to parse; an empty string is a valid path
	 * with no segments
	 * @return the parsed property path
	 * @throws InvalidPropertyPathException if the given path is not a
	 * well-formed property path
	 */
	public static PropertyPath parse(String path) throws InvalidPropertyPathException {
		return parse(path, Options.UNLIMITED);
	}

	/**
	 * Parse the given property path, rejecting it if it exceeds the given {@code options}.
	 * @param path the property path to parse; an empty string is a valid path
	 * with no segments
	 * @param options the parsing options to apply
	 * @return the parsed property path
	 * @throws InvalidPropertyPathException if the given path is not a
	 * well-formed property path, or if it exceeds the given options
	 */
	public static PropertyPath parse(String path, Options options) throws InvalidPropertyPathException {
		Assert.notNull(path, "Property path must not be null");
		Assert.notNull(options, "Options must not be null");
		if (path.isEmpty()) {
			return new PropertyPath("", Collections.emptyList());
		}
		PropertyPath parsed = new Parser(path).parse();
		int nestingDepth = parsed.segments.size() - 1;
		if (nestingDepth > options.maxNestedPathDepth) {
			throw new InvalidPropertyPathException(path,
					"nesting depth exceeds the maximum of " + options.maxNestedPathDepth);
		}
		return parsed;
	}

	/**
	 * {@code path}'s canonical form, or {@code path} itself if it is not a
	 * well-formed property path (including a {@code null} path, for which
	 * this returns an empty string).
	 * <p>A convenience for callers with nothing better to fall back to than
	 * the original string, such as canonicalizing a user-supplied field name
	 * for display, comparison, or configuration matching, as opposed to
	 * {@link #parse(String)} itself, whose non-throwing behavior would be the
	 * wrong default for a caller that is about to navigate an object graph.
	 * @param path the property path to canonicalize, possibly {@code null}
	 * @return the canonical form of {@code path}, or {@code path} unchanged
	 * (or an empty string, if {@code path} is {@code null}) if it is not a
	 * well-formed property path
	 * @since 7.1
	 */
	public static String canonicalNameOrOriginal(@Nullable String path) {
		if (path == null) {
			return "";
		}
		try {
			return parse(path).canonicalName();
		}
		catch (InvalidPropertyPathException ex) {
			return path;
		}
	}

	/**
	 * Return the canonical string form of this path.
	 * <p>Unnecessary surrounding quotes are removed from keys:
	 * {@code map['key'].name} &rarr; {@code map[key].name}.
	 */
	public String canonicalName() {
		return this.canonicalName;
	}

	/**
	 * Return the segments of this path, in order, as an unmodifiable list.
	 */
	public List<Segment> segments() {
		return this.segments;
	}

	/**
	 * Return the sub-path made up of this path's segments from the given
	 * index to the end, such as the {@code "country.name"} sub-path of
	 * {@code "address.country.name"} from index 1.
	 * @param fromIndex the index of the first segment to include (inclusive)
	 * @return the sub-path starting at {@code fromIndex}
	 * @throws IndexOutOfBoundsException if {@code fromIndex} is negative
	 * @throws IllegalArgumentException if {@code fromIndex} is greater than
	 * {@link #segments()}{@code .size()}
	 */
	public PropertyPath subPath(int fromIndex) {
		if (fromIndex == 0) {
			return this;
		}
		List<Segment> subSegments = this.segments.subList(fromIndex, this.segments.size());
		StringBuilder subCanonicalName = new StringBuilder();
		for (int i = 0; i < subSegments.size(); i++) {
			if (i > 0) {
				subCanonicalName.append(PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR);
			}
			subCanonicalName.append(subSegments.get(i).toCanonicalName());
		}
		return new PropertyPath(subCanonicalName.toString(), subSegments);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PropertyPath that &&
				this.canonicalName.equals(that.canonicalName)));
	}

	@Override
	public int hashCode() {
		return this.canonicalName.hashCode();
	}

	@Override
	public String toString() {
		return this.canonicalName;
	}


	/**
	 * A dot-separated segment of a property path. It consists of a
	 * property name and the keys of any indexes applied to it, if the
	 * target property is an indexed collection.
	 * <p>For example, the path {@code "map[key].name"} has two segments:
	 * {@code Segment["map", ["key"]]} and {@code Segment["name", []]}.
	 * @param name the property name, which is empty only for a root-level
	 * indexed access such as {@code "[user]"}
	 * @param keys the keys of the indexes applied to the property, with any
	 * surrounding quotes removed; never {@code null}, possibly empty
	 */
	public record Segment(String name, List<String> keys) {

		public Segment(String name, List<String> keys) {
			Assert.notNull(name, "Segment name must not be null");
			Assert.notNull(keys, "Segment keys must not be null");
			this.name = name;
			this.keys = List.copyOf(keys);
		}

		/**
		 * The canonical format of this segment alone: its name, followed by each
		 * key wrapped in brackets, quoted only when necessary.
		 */
		public String toCanonicalName() {
			StringBuilder canonicalNameBuilder = new StringBuilder();
			canonicalNameBuilder.append(this.name);
			for (String key : this.keys) {
				appendCanonicalKey(canonicalNameBuilder, key);
			}
			return canonicalNameBuilder.toString();
		}

		/**
		 * Return a new segment instance with the same name, but dropping the last key.
		 */
		public Segment withoutLastKey() {
			if (this.keys.isEmpty()) {
				return this;
			}
			return new Segment(this.name, this.keys.subList(0, this.keys.size() - 1));
		}

		/**
		 * Append the canonical form of the given key, which is the key
		 * unquoted, or the key re-quoted if it contains illegal raw chars,
		 * such as {@code map['a]b']}.
		 */
		private void appendCanonicalKey(StringBuilder canonicalNameBuilder, String key) {
			char quoteChar = canonicalQuoteChar(key);
			canonicalNameBuilder.append(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
			if (quoteChar != 0) {
				canonicalNameBuilder.append(quoteChar);
			}
			canonicalNameBuilder.append(key);
			if (quoteChar != 0) {
				canonicalNameBuilder.append(quoteChar);
			}
			canonicalNameBuilder.append(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
		}

		/**
		 * Determine the quote character to surround the given key with in the
		 * canonical name, or {@code 0} if the key needs no quoting.
		 */
		private static char canonicalQuoteChar(String key) {
			int depth = 0;
			boolean balanced = true;
			boolean containsSingleQuote = false;
			boolean containsDoubleQuote = false;
			for (int i = 0; i < key.length(); i++) {
				switch (key.charAt(i)) {
					case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR -> depth++;
					case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR -> {
						if (--depth < 0) {
							balanced = false;
						}
					}
					case '\'' -> containsSingleQuote = true;
					case '"' -> containsDoubleQuote = true;
					default -> {
						// Ordinary key character.
					}
				}
			}
			if (balanced && depth == 0 && !containsSingleQuote && !containsDoubleQuote) {
				return 0;
			}
			return (containsSingleQuote && !containsDoubleQuote ? '"' : '\'');
		}
	}


	/**
	 * Options to customize the parsing for {@link PropertyPath}.
	 */
	public static final class Options {

		/**
		 * Options with no limit on nesting depth.
		 */
		public static final Options UNLIMITED = new Options(Integer.MAX_VALUE);

		private final int maxNestedPathDepth;

		private Options(int maxNestedPathDepth) {
			this.maxNestedPathDepth = maxNestedPathDepth;
		}

		/**
		 * Create an {@link Options} instance that rejects a path whose nesting
		 * depth exceeds the given maximum.
		 * @param maxNestedPathDepth the maximum nesting depth
		 */
		public static Options withMaxNestedPathDepth(int maxNestedPathDepth) {
			Assert.isTrue(maxNestedPathDepth >= 0, "'maxNestedPathDepth' must not be negative");
			return new Options(maxNestedPathDepth);
		}
	}


	/**
	 * Parser for a property path string.
	 * <p>Each stage of the enforced grammar is modeled by a separate {@link State}.
	 */
	private static final class Parser {

		private final String path;

		private final List<Segment> segments = new ArrayList<>(2);

		private final StringBuilder canonicalName;

		// Offset of the character currently being processed.
		private int pos;

		// Offset at which the current segment's name starts.
		private int segmentStart;

		// Offset at which the current segment's name ends, or -1 if not yet known.
		private int segmentNameEnd = -1;

		// Keys collected for the current segment
		private @Nullable List<String> keys;

		// Offset at which the current key starts.
		private int keyStart;

		// Bracket nesting depth within the current raw key.
		private int depth;

		// The quote character that opened the current quoted key.
		private char quoteChar;

		Parser(String path) {
			this.path = path;
			this.canonicalName = new StringBuilder(path.length());
		}

		PropertyPath parse() {
			State state = State.NAME;
			for (; this.pos < this.path.length(); this.pos++) {
				state = state.process(this.path.charAt(this.pos), this);
			}
			state.onEof(this);
			return new PropertyPath(this.canonicalName.toString(), Collections.unmodifiableList(this.segments));
		}

		private void addKey(String key) {
			List<String> keys = this.keys;
			if (keys == null) {
				keys = new ArrayList<>(2);
				this.keys = keys;
			}
			keys.add(key);
		}

		private void endSegment() {
			String name = this.path.substring(this.segmentStart, this.segmentNameEnd);
			List<String> keys = (this.keys != null ? this.keys : Collections.emptyList());
			if (name.isEmpty() && keys.isEmpty()) {
				throw new InvalidPropertyPathException(this.path,
						"empty path segment (at position " + this.segmentStart + ")");
			}
			if (!this.segments.isEmpty()) {
				this.canonicalName.append(PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR);
			}
			Segment segment = new Segment(name, keys);
			this.segments.add(segment);
			this.canonicalName.append(segment.toCanonicalName());
			this.keys = null;
			this.segmentNameEnd = -1;
		}

		private InvalidPropertyPathException error(String reason) {
			return new InvalidPropertyPathException(this.path, reason + " (at position " + this.pos + ")");
		}

		private InvalidPropertyPathException unclosedIndex() {
			return error("unclosed '" + PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR + "'");
		}
	}


	private enum State {

		// Reading a segment name, before any index of that segment
		NAME {
			@Override
			State process(char ch, Parser parser) {
				if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR) {
					parser.segmentNameEnd = parser.pos;
					parser.endSegment();
					parser.segmentStart = parser.pos + 1;
					return this;
				}
				if (ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
					parser.segmentNameEnd = parser.pos;
					return INDEX_OPEN;
				}
				if (ch == PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR) {
					throw parser.error("unexpected '" + PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR +
							"' without a matching '" + PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR + "'");
				}
				return this;
			}

			@Override
			void onEof(Parser parser) {
				parser.segmentNameEnd = parser.path.length();
				parser.endSegment();
			}
		},

		// Immediately after the "[" that opens an index
		INDEX_OPEN {
			@Override
			State process(char ch, Parser parser) {
				if (ch == PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR) {
					// An empty key, as in "map[]".
					parser.addKey("");
					return AFTER_INDEX;
				}
				if (ch == '\'' || ch == '"') {
					parser.quoteChar = ch;
					parser.keyStart = parser.pos + 1;
					return QUOTED_KEY;
				}
				parser.keyStart = parser.pos;
				// A key that itself opens a bracket level, as in "map[[a]]"
				parser.depth = (ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR ? 1 : 0);
				return RAW_KEY;
			}

			@Override
			void onEof(Parser parser) {
				throw parser.unclosedIndex();
			}
		},

		// Inside an unquoted key, tracking the depth of bracket nesting.
		RAW_KEY {
			@Override
			State process(char ch, Parser parser) {
				if (ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
					parser.depth++;
					return this;
				}
				if (ch == PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR) {
					if (parser.depth == 0) {
						parser.addKey(parser.path.substring(parser.keyStart, parser.pos));
						return AFTER_INDEX;
					}
					parser.depth--;
					return this;
				}
				if (ch == '\'' || ch == '"') {
					throw parser.error("unexpected quote '" + ch + "' in an unquoted key; " +
							"quote the whole key to use quote characters within it");
				}
				return this;
			}

			@Override
			void onEof(Parser parser) {
				throw parser.unclosedIndex();
			}
		},

		// Inside a quoted key, looking for the closing quote.
		QUOTED_KEY {
			@Override
			State process(char ch, Parser parser) {
				if (ch == parser.quoteChar) {
					parser.addKey(parser.path.substring(parser.keyStart, parser.pos));
					return QUOTE_CLOSED;
				}
				return this;
			}

			@Override
			void onEof(Parser parser) {
				throw parser.error("unterminated quote '" + parser.quoteChar + "'");
			}
		},

		// Immediately after a closing quote, only "]" is legal.
		QUOTE_CLOSED {
			@Override
			State process(char ch, Parser parser) {
				if (ch != PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR) {
					throw parser.error("unexpected '" + ch + "' after a closing quote; expected '" +
							PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR + "'");
				}
				return AFTER_INDEX;
			}

			@Override
			void onEof(Parser parser) {
				throw parser.unclosedIndex();
			}
		},

		// Immediately after an index's "]" , only "." or "[" are legal.
		AFTER_INDEX {
			@Override
			State process(char ch, Parser parser) {
				if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR) {
					parser.endSegment();
					parser.segmentStart = parser.pos + 1;
					return NAME;
				}
				if (ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
					return INDEX_OPEN;
				}
				throw parser.error("unexpected '" + ch + "' after an index; expected '" +
						PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR + "', '" +
						PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR + "', or the end of the path");
			}

			@Override
			void onEof(Parser parser) {
				parser.endSegment();
			}
		};

		abstract State process(char ch, Parser parser);

		abstract void onEof(Parser parser);
	}

}
