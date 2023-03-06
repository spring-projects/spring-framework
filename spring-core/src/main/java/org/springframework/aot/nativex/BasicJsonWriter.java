/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.nativex;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.aot.hint.TypeReference;

/**
 * Very basic json writer for the purposes of translating runtime hints to native
 * configuration.
 *
 * @author Stephane Nicoll
 */
class BasicJsonWriter {

	private final IndentingWriter writer;

	/**
	 * Create a new instance with the specified indent value.
	 * @param writer the writer to use
	 * @param singleIndent the value of one indent
	 */
	public BasicJsonWriter(Writer writer, String singleIndent) {
		this.writer = new IndentingWriter(writer, singleIndent);
	}

	/**
	 * Create a new instance using two whitespaces for the indent.
	 * @param writer the writer to use
	 */
	public BasicJsonWriter(Writer writer) {
		this(writer, "  ");
	}


	/**
	 * Write an object with the specified attributes. Each attribute is
	 * written according to its value type:
	 * <ul>
	 * <li>Map: write the value as a nested object</li>
	 * <li>List: write the value as a nested array</li>
	 * <li>Otherwise, write a single value</li>
	 * </ul>
	 * @param attributes the attributes of the object
	 */
	public void writeObject(Map<String, Object> attributes) {
		writeObject(attributes, true);
	}

	/**
	 * Write an array with the specified items. Each item in the
	 * list is written either as a nested object or as an attribute
	 * depending on its type.
	 * @param items the items to write
	 * @see #writeObject(Map)
	 */
	public void writeArray(List<?> items) {
		writeArray(items, true);
	}

	private void writeObject(Map<String, Object> attributes, boolean newLine) {
		if (attributes.isEmpty()) {
			this.writer.print("{ }");
		}
		else {
			this.writer.println("{").indented(writeAll(attributes.entrySet().iterator(),
					entry -> writeAttribute(entry.getKey(), entry.getValue()))).print("}");
		}
		if (newLine) {
			this.writer.println();
		}
	}

	private void writeArray(List<?> items, boolean newLine) {
		if (items.isEmpty()) {
			this.writer.print("[ ]");
		}
		else {
			this.writer.println("[")
					.indented(writeAll(items.iterator(), this::writeValue)).print("]");
		}
		if (newLine) {
			this.writer.println();
		}
	}

	private <T> Runnable writeAll(Iterator<T> it, Consumer<T> writer) {
		return () -> {
			while (it.hasNext()) {
				writer.accept(it.next());
				if (it.hasNext()) {
					this.writer.println(",");
				}
				else {
					this.writer.println();
				}
			}
		};
	}

	private void writeAttribute(String name, Object value) {
		this.writer.print(quote(name) + ": ");
		writeValue(value);
	}

	@SuppressWarnings("unchecked")
	private void writeValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			writeObject((Map<String, Object>) map, false);
		}
		else if (value instanceof List<?> list) {
			writeArray(list, false);
		}
		else if (value instanceof TypeReference typeReference) {
			this.writer.print(quote(typeReference.getName()));
		}
		else if (value instanceof CharSequence string) {
			this.writer.print(quote(escape(string)));
		}
		else if (value instanceof Boolean flag) {
			this.writer.print(Boolean.toString(flag));
		}
		else {
			throw new IllegalStateException("unsupported type: " + value.getClass());
		}
	}

	private String quote(String name) {
		return "\"" + name + "\"";
	}


	private static String escape(CharSequence input) {
		StringBuilder builder = new StringBuilder();
		input.chars().forEach(c -> builder.append(
			switch (c) {
				case '"' -> "\\\"";
				case '\\' -> "\\\\";
				case '/' -> "\\/";
				case '\b' -> "\\b";
				case '\f' -> "\\f";
				case '\n' -> "\\n";
				case '\r' -> "\\r";
				case '\t' -> "\\t";
				default -> {
					if (c <= 0x1F) {
						yield String.format("\\u%04x", c);
					}
					else {
						yield (char) c;
					}
				}
			}
		));
		return builder.toString();
	}


	static class IndentingWriter extends Writer {

		private final Writer out;

		private final String singleIndent;

		private int level = 0;

		private String currentIndent = "";

		private boolean prependIndent = false;

		IndentingWriter(Writer out, String singleIndent) {
			this.out = out;
			this.singleIndent = singleIndent;
		}

		/**
		 * Write the specified text.
		 * @param string the content to write
		 */
		public IndentingWriter print(String string) {
			write(string.toCharArray(), 0, string.length());
			return this;
		}

		/**
		 * Write the specified text and append a new line.
		 * @param string the content to write
		 */
		public IndentingWriter println(String string) {
			write(string.toCharArray(), 0, string.length());
			return println();
		}

		/**
		 * Write a new line.
		 */
		public IndentingWriter println() {
			String separator = System.lineSeparator();
			try {
				this.out.write(separator.toCharArray(), 0, separator.length());
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
			this.prependIndent = true;
			return this;
		}

		/**
		 * Increase the indentation level and execute the {@link Runnable}. Decrease the
		 * indentation level on completion.
		 * @param runnable the code to execute within an extra indentation level
		 */
		public IndentingWriter indented(Runnable runnable) {
			indent();
			runnable.run();
			return outdent();
		}

		/**
		 * Increase the indentation level.
		 */
		private IndentingWriter indent() {
			this.level++;
			return refreshIndent();
		}

		/**
		 * Decrease the indentation level.
		 */
		private IndentingWriter outdent() {
			this.level--;
			return refreshIndent();
		}

		private IndentingWriter refreshIndent() {
			this.currentIndent = this.singleIndent.repeat(Math.max(0, this.level));
			return this;
		}

		@Override
		public void write(char[] chars, int offset, int length) {
			try {
				if (this.prependIndent) {
					this.out.write(this.currentIndent.toCharArray(), 0, this.currentIndent.length());
					this.prependIndent = false;
				}
				this.out.write(chars, offset, length);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public void flush() throws IOException {
			this.out.flush();
		}

		@Override
		public void close() throws IOException {
			this.out.close();
		}

	}

}
