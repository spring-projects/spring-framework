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

package org.springframework.mock.web.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * Mock implementation of the {@link javax.servlet.jsp.tagext.BodyContent} class.
 *
 * <p>Used for testing the web framework; only necessary for testing
 * applications when testing custom JSP tags.
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
	public MockBodyContent(String content, HttpServletResponse response, Writer targetWriter) {
		super(adaptJspWriter(targetWriter, response));
		this.content = content;
	}

	private static JspWriter adaptJspWriter(Writer targetWriter, HttpServletResponse response) {
		if (targetWriter instanceof JspWriter) {
			return (JspWriter) targetWriter;
		}
		else {
			return new MockJspWriter(response, targetWriter);
		}
	}


	public Reader getReader() {
		return new StringReader(this.content);
	}

	public String getString() {
		return this.content;
	}

	public void writeOut(Writer writer) throws IOException {
		writer.write(this.content);
	}


	//---------------------------------------------------------------------
	// Delegating implementations of JspWriter's abstract methods
	//---------------------------------------------------------------------

	public void clear() throws IOException {
		getEnclosingWriter().clear();
	}

	public void clearBuffer() throws IOException {
		getEnclosingWriter().clearBuffer();
	}

	public void close() throws IOException {
		getEnclosingWriter().close();
	}

	public int getRemaining() {
		return getEnclosingWriter().getRemaining();
	}

	public void newLine() throws IOException {
		getEnclosingWriter().println();
	}

	public void write(char value[], int offset, int length) throws IOException {
		getEnclosingWriter().write(value, offset, length);
	}

	public void print(boolean value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(char value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(char[] value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(double value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(float value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(int value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(long value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(Object value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void print(String value) throws IOException {
		getEnclosingWriter().print(value);
	}

	public void println() throws IOException {
		getEnclosingWriter().println();
	}

	public void println(boolean value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(char value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(char[] value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(double value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(float value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(int value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(long value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(Object value) throws IOException {
		getEnclosingWriter().println(value);
	}

	public void println(String value) throws IOException {
		getEnclosingWriter().println(value);
	}

}
