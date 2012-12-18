/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.Writer;

import org.apache.commons.logging.Log;

/**
 * {@code java.io.Writer} adapter for a Commons Logging {@code Log}.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public class CommonsLogWriter extends Writer {

	private final Log logger;

	private final StringBuilder buffer = new StringBuilder();


	/**
	 * Create a new CommonsLogWriter for the given Commons Logging logger.
	 * @param logger the Commons Logging logger to write to
	 */
	public CommonsLogWriter(Log logger) {
		Assert.notNull(logger, "Logger must not be null");
		this.logger = logger;
	}


	public void write(char ch) {
		if (ch == '\n' && this.buffer.length() > 0) {
			this.logger.debug(this.buffer.toString());
			this.buffer.setLength(0);
		}
		else {
			this.buffer.append(ch);
		}
	}

	@Override
	public void write(char[] buffer, int offset, int length) {
		for (int i = 0; i < length; i++) {
			char ch = buffer[offset + i];
			if (ch == '\n' && this.buffer.length() > 0) {
				this.logger.debug(this.buffer.toString());
				this.buffer.setLength(0);
			}
			else {
				this.buffer.append(ch);
			}
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

}
