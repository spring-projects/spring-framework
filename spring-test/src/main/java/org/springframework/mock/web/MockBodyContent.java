/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.mock.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;

import org.springframework.lang.Nullable;

/**
 * Mock implementation of the {@link jakarta.servlet.jsp.tagext.BodyContent} class.
 * Only necessary for testing applications when testing custom JSP tags.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class MockBodyContent extends BodyContent {

	private final String content;


	/**
	 * Create a MockBodyContent for the given response.
	 * @param content the body content to expose
	 * @param response the servlet response to wrap
	 */
	public MockBodyContent(String content, HttpServletResponse response) {
		this(content, response, null);
	}

	/**
	 * Create a MockBodyContent for the given response.
	 * @param content the body content to expose
	 * @param targetWriter the target Writer to wrap
	 */
	public MockBodyContent(String content, Writer targetWriter) {
		this(content, null, targetWriter);
	}

	/**
	 * Create a MockBodyContent for the given response.
	 * @param content the body content to expose
	 * @param response the servlet response to wrap
	 * @param targetWriter the target Writer to wrap
	 */
	public MockBodyContent(String content, @Nullable HttpServletResponse response, @Nullable Writer targetWriter) {
		super(adaptJspWriter(targetWriter, response));
		this.content = content;
	}

	private static JspWriter adaptJspWriter(@Nullable Writer targetWriter, @Nullable HttpServletResponse response) {
		if (targetWriter instanceof JspWriter) {
			return (JspWriter) targetWriter;
		}
		else {
			return new MockJspWriter(response, targetWriter);
		}
	}


	@Override
	public Reader getReader() {
		return new StringReader(this.content);
	}

	@Override
	public String getString() {
		return this.content;
	}

	@Override
	public void writeOut(Writer writer) throws IOException {
		writer.write(this.content);
	}


	//---------------------------------------------------------------------
	// Delegating implementations of JspWriter's abstract methods
	//---------------------------------------------------------------------

	@Override
	public void clear() throws IOException {
		getEnclosingWriter().clear();
	}

	@Override
	public void clearBuffer() throws IOException {
		getEnclosingWriter().clearBuffer();
	}

	@Override
	public void close() throws IOException {
		getEnclosingWriter().close();
	}

	@Override
	public int getRemaining() {
		return getEnclosingWriter().getRemaining();
	}

	@Override
	public void newLine() throws IOException {
		getEnclosingWriter().println();
	}

	@Override
	public void write(char[] value, int offset, int length) throws IOException {
		getEnclosingWriter().write(value, offset, length);
	}

	@Override
	public void print(boolean value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(char value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(char[] value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(double value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(float value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(int value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(long value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(Object value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(String value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void println() throws IOException {
		getEnclosingWriter().println();
	}

	@Override
	public void println(boolean value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(char value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(char[] value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(double value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(float value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(int value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(long value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(Object value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(String value) throws IOException {
		getEnclosingWriter().println(value);
	}

}
