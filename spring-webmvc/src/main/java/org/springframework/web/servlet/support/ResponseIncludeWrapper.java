package org.springframework.web.servlet.support;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Response wrapper used for server side include of handler methods.
 * @Author Cagatay Kalan
 * @Since 4.3
 */
public class ResponseIncludeWrapper extends HttpServletResponseWrapper
{

    protected final ServletOutputStream captureServletOutputStream;
    protected ServletOutputStream servletOutputStream;
    protected PrintWriter printWriter;

    public ResponseIncludeWrapper(HttpServletResponse response, ServletOutputStream captureServletOutputStream)
    {
        super(response);
        this.captureServletOutputStream = captureServletOutputStream;
    }

    /**
     * Flush the servletOutputStream or printWriter ( only one will be non-null )
     * This must be called after a requestDispatcher.include, since we can't
     * assume that the included servlet flushed its stream.
     */
    public void flushOutputStreamOrWriter() throws IOException
    {
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }


    /**
     * Return a printwriter, throws and exception if a OutputStream already
     * been returned.
     *
     * @return a PrintWriter object
     * @exception java.io.IOException
     *                if the outputstream already been called
     */
    @Override
    public PrintWriter getWriter() throws java.io.IOException {
        if (servletOutputStream == null) {
            if (printWriter == null) {
                setCharacterEncoding(getCharacterEncoding());
                printWriter = new PrintWriter(
                        new OutputStreamWriter(captureServletOutputStream,
                                getCharacterEncoding()));
            }
            return printWriter;
        }
        throw new IllegalStateException();
    }


    /**
     * Return a OutputStream, throws and exception if a printwriter already
     * been returned.
     *
     * @return a OutputStream object
     * @exception java.io.IOException
     *                if the printwriter already been called
     */
    @Override
    public ServletOutputStream getOutputStream() throws java.io.IOException {
        if (printWriter == null) {
            if (servletOutputStream == null) {
                servletOutputStream = captureServletOutputStream;
            }
            return servletOutputStream;
        }
        throw new IllegalStateException();
    }

}
