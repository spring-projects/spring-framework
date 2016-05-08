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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Response wrapper used for server side include of handler methods.
 * Response data for the included handler method is captured by a {@link ByteArrayServletOutputStream}
 * Included response has its own <code>content-type</code> and <code>last-modified</code> values.
 * <code>content-type</code> resolution falls back to the wrapped response if not set manually.{@see getContentType}
 *
 * Similarly, most of the properties of the original response can not be changed by the included methods while the following
 * actions are possible for a included handler method:
 *
 * - Redirect the original response
 * - Add/set headers on the original response except the <code>content-type</code> and <code>last-modified</code> headers
 * - Set status of the original response
 * - Send error to the original response
 *
 * To get the response stream as text, <code>getContent()</code> method must be used.
 *
 * @Author Cagatay Kalan
 * @Since 4.3.0
 *
 *
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


    private final HttpServletResponse responseDelegate;

    private static final Log logger = LogFactory.getLog(ResponseIncludeWrapper.class);

    static
    {
        RFC1123_FORMAT = new SimpleDateFormat(RFC1123_PATTERN, Locale.US);
        RFC1123_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public ResponseIncludeWrapper(HttpServletResponse response, HttpServletRequest request, ServletContext context)
    {
        super(response);
        this.responseDelegate = response;
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
     *   <code>ResponseIncludeWrapper</code>, or the content type of the original response if no content type
     *   set on this instance
     */
    @Override
    public String getContentType() {
        if (contentType == null) {
            setContentType(_getResponse().getContentType());
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


    /**
     * Redirects to wrapped response to a new location
     * @param location
     * @throws IOException
     */
    @Override
    public void sendRedirect(String location) throws IOException
    {
        _getResponse().sendRedirect(location);
    }


    /**
     * We need to store the original response in a separate field because during included request procecessing,
     * servlet container may wrap the response with its own implementation which prevents delegating actions to it.
     * Tomcat for instance, wraps the response with a ApplicationHttpResponse which prevents actions like sending redirects,
     * setting headers and etc. Because of that, we can not use the getResponse() for those actions.
     *
     * @return the original response wrapped by this instance.
     */
    private HttpServletResponse _getResponse()
    {
        return responseDelegate;
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        _getResponse().sendError(sc);
        resetBuffer();
    }

    /**
     * Delegates to the sendError method of the wrapped response.
     * @param sc status code
     * @param msg status message
     * @throws IOException
     */
    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        _getResponse().sendError(sc, msg);
        resetBuffer();
    }

    /**
     * Delegates to the setStatus method of the wrapped response.
     * @param status status code
     */
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

    /**
     * Gets the textual content of the included response by reading the response buffer with
     * the given encoding.
     *
     * @param encoding Character encoding to be used when converting stream data to text output.
     * If null, character encoding of the wrapped response is used. If character encoding of the
     * wrapped response is also null, then "UTF-8" is used.
     * @return Content of the included response.
     * @throws UnsupportedEncodingException
     */
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
     * Sets the value of the <code>content-type</code> value.
     * It does not affect the <code>content-type</code> of the wrapped response.
     * @param contentType
     */
    @Override
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }


    /**
     * Sets the <code>last-modified</code> header of this instance.
     * It does not affect the <code>last-modified</code> header of the wrapped response.
     * @param name
     * @param value
     */
    @Override
    public void addDateHeader(String name, long value)
    {
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            lastModified = value;
        }
    }

    /**
     * For <code>content-type</code> and <code>last-modified</code> headers,it stores the local values
     * on this instance. For other headers, header is added to the wrapped response.
     * @param name
     * @param value
     */
    @Override
    public void addHeader(String name, String value)
    {
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
        } else {
            _getResponse().addHeader(name,value);
        }
    }

    /**
     * Sets <code>last-modified</code> value of this instance.It does not affect
     * the <code>last-modified</code> header of the wrapped response.
     * @param name
     * @param value
     */
    @Override
    public void setDateHeader(String name, long value)
    {
        String lname = name.toLowerCase(Locale.ENGLISH);
        if (lname.equals(LAST_MODIFIED))
        {
            lastModified = value;
        }
    }

    /**
     * For <code>content-type</code> and <code>last-modified</code> headers,it sets the local values
     * on this instance. For other headers, header is set on the wrapped response.
     * @param name
     * @param value
     */
    @Override
    public void setHeader(String name, String value)
    {
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
        } else {
            _getResponse().setHeader(name,value);
        }
    }

    /**
     * Adds the cookie to the wrapped instance.This way, included responses can
     * add cookies to the parent response.
     * @param cookie
     */
    @Override
    public void addCookie(Cookie cookie)
    {
        _getResponse().addCookie(cookie);
    }

    /**
     * Sets the integer header on the wrapped response.
     * @param name
     * @param value
     */
    @Override
    public void addIntHeader(String name, int value)
    {
        _getResponse().addIntHeader(name,value);
    }

    /**
     * @return commit status of the wrapped response.
     */
    @Override
    public boolean isCommitted()
    {
        return _getResponse().isCommitted();
    }
}
