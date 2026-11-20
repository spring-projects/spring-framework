/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mock.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspWriter;
import org.jspecify.annotations.Nullable;

/**
 * Mock implementation of the {@link jakarta.servlet.jsp.JspWriter} class.
 * Only necessary for testing applications when testing custom JSP tags.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class MockJspWriter extends JspWriter {

	private final HttpServletResponse response;

	private @Nullable PrintWriter targetWriter;


	/**
	 * Create a MockJspWriter for the given response,
	 * using the response's default Writer.
	 * @param response the servlet response to wrap
	 */
	public MockJspWriter(HttpServletResponse response) {
		this(response, null);
	}

	/**
	 * Create a MockJspWriter for the given plain Writer.
	 * @param targetWriter the target Writer to wrap
	 */
	public MockJspWriter(Writer targetWriter) {
		this(null, targetWriter);
	}

	/**
	 * Create a MockJspWriter for the given response.
	 * @param response the servlet response to wrap
	 * @param targetWriter the target Writer to wrap
	 */
	public MockJspWriter(@Nullable HttpServletResponse response, @Nullable Writer targetWriter) {
		super(DEFAULT_BUFFER, true);
		this.response = (response != null ? response : new MockHttpServletResponse());
		if (targetWriter instanceof PrintWriter printWriter) {
			this.targetWriter = printWriter;
		}
		else if (targetWriter != null) {
			this.targetWriter = new PrintWriter(targetWriter);
		}
	}

	/**
	 * Lazily initialize the target Writer.
	 */
	protected PrintWriter getTargetWriter() throws IOException {
		if (this.targetWriter == null) {
			this.targetWriter = this.response.getWriter();
		}
		return this.targetWriter;
	}


	@Override
	public void clear() throws IOException {
		if (this.response.isCommitted()) {
			throw new IOException("Response already committed");
		}
		this.response.resetBuffer();
	}

	@Override
	public void clearBuffer() throws IOException {
	}

	@Override
	public void flush() throws IOException {
		this.response.flushBuffer();
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public int getRemaining() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void newLine() throws IOException {
		getTargetWriter().println();
	}

	@Override
	public void write(char[] value, int offset, int length) throws IOException {
		getTargetWriter().write(value, offset, length);
	}

	@Override
	public void print(boolean value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(char value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(char[] value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(double value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(float value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(int value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(long value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(Object value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(String value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void println() throws IOException {
		getTargetWriter().println();
	}

	@Override
	public void println(boolean value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(char value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(char[] value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(double value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(float value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(int value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(long value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(Object value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(String value) throws IOException {
		getTargetWriter().println(value);
	}

}
