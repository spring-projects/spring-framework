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

package org.springframework.aot.generate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * A single generated resource.
 *
 * @author Stephane Nicoll
 * @since 7.1
 * @see GeneratedResources
 */
public class GeneratedResource {

	private final String path;

	private final Content content;

	/**
	 * Create a new, empty instance with the given {@code path}.
	 * @param path the absolute path of this resource on the classpath
	 */
	GeneratedResource(String path) {
		this.path = path;
		this.content = new DefaultContent();
	}

	/**
	 * Return the absolute path of this resource on the classpath.
	 * <p>The path returned by this method does not have a leading slash and is
	 * suitable for generated code that use {@link ClassLoader#getResource(String)}.
	 * @return the path of this resource on the classpath
	 * @see #hasContent()
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Whether this instance has content associated to it. Instances that do
	 * not have content are not contributed to {@link GeneratedFiles}.
	 * @return {@code true} if this instance has content
	 * @see #handle(Consumer)
	 */
	public boolean hasContent() {
		return this.content.exists();
	}

	/**
	 * Handle this instance, using the provided {@link Content}.
	 * <p>Generated resources are usually written only once, but there are cases
	 * where they need to be stored in a unique path and several rounds of
	 * AOT processing may touch the same file. For cases like this use
	 * {@code createOrValidate} as only one attempt to create the content is
	 * allowed.
	 * @param content the content to use
	 */
	public void handle(Consumer<Content> content) {
		content.accept(this.content);
	}

	void writeTo(GeneratedFiles generatedFiles) {
		if (hasContent()) {
			generatedFiles.addFile(Kind.RESOURCE, this.path, this.content);
		}
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", GeneratedResource.class.getSimpleName() + "[", "]")
				.add("path='" + this.path + "'")
				.toString();
	}

	/**
	 * Abstraction of the content of a generated resource.
	 */
	public interface Content extends InputStreamSource {

		/**
		 * Specify if this content exists. Use this as a check before calling
		 * {@link #getInputStream()}.
		 * @return {@code true} if this instance has been created
		 */
		boolean exists();

		/**
		 * Create this instance with the content from the given {@link CharSequence}.
		 * @param content the content
		 * @throws IllegalStateException if this instance has already been created
		 */
		void create(CharSequence content);

		/**
		 * Create this instance with the content from the given {@link CharSequence}
		 * if it does not exist or validate that the existing content matches.
		 * @param content the content
		 * @throws IllegalArgumentException if this instance has already been created,
		 * but its content does not match the given {@code content}
		 */
		void createOrValidate(CharSequence content);

		/**
		 * Create this instance with the content written to an {@link Appendable}
		 * passed to the given {@link ThrowingConsumer}.
		 * @param content a {@link ThrowingConsumer} that accepts an
		 * {@link Appendable} which will receive the content
		 * @throws IllegalStateException if this instance has already been created
		 */
		void create(ThrowingConsumer<Appendable> content);

		/**
		 * Create this instance with the content written to an {@link Appendable}
		 * passed to the given {@link ThrowingConsumer} if it does not exist or
		 * validate that the existing content matches.
		 * @param content a {@link ThrowingConsumer} that accepts an
		 * {@link Appendable} which will receive the content
		 * @throws IllegalArgumentException if this instance has already been created,
		 * but its content does not match the given {@code content}
		 */
		void createOrValidate(ThrowingConsumer<Appendable> content);

		/**
		 * Create this instance with the content from the given {@link InputStreamSource}.
		 * @param content an {@link InputStreamSource} that will provide an input
		 * stream containing the content
		 * @throws IllegalStateException if this instance has already been created
		 */
		void create(InputStreamSource content);

		/**
		 * Create this instance with the content from the given {@link InputStreamSource}
		 * if it does not exist or validate that the existing content matches.
		 * @param content an {@link InputStreamSource} that will provide an input
		 * stream containing the content
		 * @throws IllegalArgumentException if this instance has already been created,
		 * but its content does not match the given {@code content}
		 */
		void createOrValidate(InputStreamSource content);

	}

	private final class DefaultContent implements Content {

		private @Nullable InputStreamSource source;

		@Override
		public InputStream getInputStream() throws IOException {
			Assert.state(this.source != null, "No content is set for " + GeneratedResource.this);
			return this.source.getInputStream();
		}

		@Override
		public boolean exists() {
			return (this.source != null);
		}

		@Override
		public void create(CharSequence content) {
			create(appendable -> appendable.append(content));
		}

		@Override
		public void createOrValidate(CharSequence content) {
			createOrValidate(appendable -> appendable.append(content));
		}

		@Override
		public void create(ThrowingConsumer<Appendable> content) {
			create(new AppendableConsumerInputStreamSource(content));
		}

		@Override
		public void createOrValidate(ThrowingConsumer<Appendable> content) {
			createOrValidate(new AppendableConsumerInputStreamSource(content));
		}

		@Override
		public void create(InputStreamSource content) {
			Assert.notNull(content, "'content' must not be null");
			if (exists()) {
				throw new IllegalStateException(
						"Content for generated resource at '%s' has already been set".formatted(getPath()));
			}
			this.source = content;
		}

		@Override
		public void createOrValidate(InputStreamSource content) {
			Assert.notNull(content, "'content' must not be null");
			if (this.source == null) {
				this.source = content;
			}
			if (!hasSomeContentAs(content)) {
				throw new IllegalArgumentException(
						"Content for generated resource at '%s' differs from the content that has already been written"
								.formatted(getPath()));
			}
		}

		private boolean hasSomeContentAs(InputStreamSource content) {
			try (InputStream input1 = getInputStream(); InputStream input2 = content.getInputStream()) {
				if (input1 == input2) {
					return true;
				}
				byte[] buffer1 = new byte[8192];
				byte[] buffer2 = new byte[8192];
				int count1;
				int count2;
				while ((count1 = input1.read(buffer1)) != -1) {
					count2 = input2.read(buffer2);
					if (count1 != count2) {
						return false;
					}
					if (!Arrays.equals(buffer1, 0, count1, buffer2, 0, count2)) {
						return false;
					}
				}
				return input2.read() == -1;
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to validate content for generated resource at '%s'"
						.formatted(getPath()), ex);
			}

		}

	}
}
