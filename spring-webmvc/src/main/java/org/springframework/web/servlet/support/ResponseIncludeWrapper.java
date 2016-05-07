package org.springframework.web.servlet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Response wrapper used for server side include of handler methods.
 *
 * @Author Cagatay Kalan
 * @Since 4.3
 */
public class ResponseIncludeWrapper extends HttpServletResponseWrapper
{

    protected ByteArrayServletOutputStream captureServletOutputStream;
    protected ByteArrayServletOutputStream servletOutputStream;
    protected PrintWriter printWriter;

    private static final String CONTENT_TYPE = "content-type";
    private static final String LAST_MODIFIED = "last-modified";
    private static final DateFormat RFC1123_FORMAT;
    private static final String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    protected long lastModified = -1;
    private String contentType;

    private final ServletContext context;
    private final HttpServletRequest request;

    private static final Log logger = LogFactory.getLog(ResponseIncludeWrapper.class);

    static
    {
        RFC1123_FORMAT = new SimpleDateFormat(RFC1123_PATTERN, Locale.US);
        RFC1123_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public ResponseIncludeWrapper(HttpServletResponse response, HttpServletRequest request, ServletContext context)
    {
        super(response);
        this.request = request;
        this.context = context;
    }

    /**
     * Returns the value of the <code>last-modified</code> header field. The
     * result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the date the resource referenced by this
     *   <code>ResponseIncludeWrapper</code> was last modified, or -1 if not
     *   known.
     */
    public long getLastModified() {
        if (lastModified == -1) {
            // javadocs say to return -1 if date not known, if you want another
            // default, put it here
            return -1;
        }
        return lastModified;
    }

    /**
     * Returns the value of the <code>content-type</code> header field.
     *
     * @return the content type of the resource referenced by this
     *   <code>ResponseIncludeWrapper</code>, or <code>null</code> if not known.
     */
    @Override
    public String getContentType() {
        if (contentType == null) {
            String url = request.getRequestURI();
            String mime = context.getMimeType(url);
            if (mime != null) {
                setContentType(mime);
            } else {
                // return a safe value
                setContentType("application/x-octet-stream");
            }
        }
        return contentType;
    }

    /**
     * Flush the servletOutputStream or printWriter ( only one will be non-null )
     * This must be called after a requestDispatcher.include, since we can't
     * assume that the included servlet flushed its stream.
     */
    public void flushOutputStreamOrWriter() throws IOException
    {
        if (servletOutputStream != null)
        {
            servletOutputStream.flush();
        }
        if (printWriter != null)
        {
            printWriter.flush();
        }
    }


    /**
     * Return a printwriter, throws and exception if a OutputStream already
     * been returned.
     *
     * @return a PrintWriter object
     * @throws java.io.IOException if the outputstream already been called
     */
    @Override
    public PrintWriter getWriter() throws java.io.IOException
    {
        if (servletOutputStream == null)
        {
            if (printWriter == null)
            {
                if (captureServletOutputStream == null)
                    captureServletOutputStream = new ByteArrayServletOutputStream();
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
     * @throws java.io.IOException if the printwriter already been called
     */
    @Override
    public ServletOutputStream getOutputStream() throws java.io.IOException
    {
        if (printWriter == null)
        {
            if (servletOutputStream == null)
            {
                servletOutputStream = new ByteArrayServletOutputStream();
            }
            return servletOutputStream;
        }
        throw new IllegalStateException();
    }


    @Override
    public void sendRedirect(String location) throws IOException
    {
        if (isCommitted()) throw new IllegalStateException("Response already committed.");
        _getResponse().sendRedirect(location);
    }


    private HttpServletResponse _getResponse()
    {
        HttpServletResponse response = (HttpServletResponse) getResponse();
        while (response instanceof HttpServletResponseWrapper) {
            response = (HttpServletResponse) ((HttpServletResponseWrapper)response).getResponse();
        }
        return response;
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        _getResponse().sendError(sc);
        resetBuffer();
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        _getResponse().sendError(sc, msg);
        resetBuffer();
    }

    @Override
    public void setStatus(int status)
    {
        _getResponse().setStatus(status);
    }

    @Override
    public void resetBuffer()
    {
        if (printWriter != null)
            captureServletOutputStream.getBuffer().reset();
        else if (servletOutputStream != null)
            servletOutputStream.getBuffer().reset();

    }

    @Override
    public void reset()
    {
        resetBuffer();
    }

    @Override
    public void flushBuffer() throws IOException
    {
        flushOutputStreamOrWriter();
    }

    public String getContent() throws UnsupportedEncodingException
    {
        return getContent(null);
    }

    public String getContent(String encoding) throws UnsupportedEncodingException
    {
        ByteArrayOutputStream bos = null;
        if (printWriter != null)
            bos = captureServletOutputStream.getBuffer();
        else if (servletOutputStream != null)
            bos = servletOutputStream.getBuffer();

        if (encoding == null)
        {
            encoding = getCharacterEncoding();
        }

        if (bos != null)
            return bos.toString(encoding != null ? encoding : "UTF-8");

        return "";


    }

    /**
     * Sets the value of the <code>content-type</code> header field.
     *
     * @param mime a mime type
     */
    @Override
    public void setContentType(String mime)
    {
        contentType = mime;
        if (contentType != null)
        {
            getResponse().setContentType(contentType);
        }
    }


    @Override
    public void addDateHeader(String name, long value)
    {
        super.addDateHeader(name, value);
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            lastModified = value;
        }
    }

    @Override
    public void addHeader(String name, String value)
    {
        super.addHeader(name, value);
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            try
            {
                synchronized (RFC1123_FORMAT)
                {
                    lastModified = RFC1123_FORMAT.parse(value).getTime();
                }
            } catch (Throwable ignore)
            {
                logger.error(ignore);
            }
        } else if (lname.equals(CONTENT_TYPE))
        {
            contentType = value;
        }
    }

    @Override
    public void setDateHeader(String name, long value)
    {
        super.setDateHeader(name, value);
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            lastModified = value;
        }
    }

    @Override
    public void setHeader(String name, String value)
    {
        super.setHeader(name, value);
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            try
            {
                synchronized (RFC1123_FORMAT)
                {
                    lastModified = RFC1123_FORMAT.parse(value).getTime();
                }
            } catch (Throwable ignore)
            {
                logger.error(ignore);
            }
        } else if (lname.equals(CONTENT_TYPE))
        {
            contentType = value;
        }
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        _getResponse().addCookie(cookie);
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        _getResponse().addIntHeader(name,value);
    }


}
