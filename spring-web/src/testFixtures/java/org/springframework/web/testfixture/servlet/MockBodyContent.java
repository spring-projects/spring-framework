/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.servlet;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import org.jspecify.annotations.Nullable;

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
		if (targetWriter instanceof JspWriter jspWriter) {
			return jspWriter;
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
