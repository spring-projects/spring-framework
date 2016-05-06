package org.springframework.web.servlet.support;


import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;

/**
 * Simple ServletOutputStream implementation which captures servlet response during server side includes
 * used by the {@link ResponseIncludeWrapper}.Response data is stored in an in-memory buffer.
 * Internally {@link ByteArrayOutputStream} is used for the buffer.Please note that, because of the
 * limitations of the {@link ByteArrayOutputStream}, performance may be less than optimal for too many write
 * operations.
 *
 *
 * @Author Cagatay Kalan
 * @Since 4.3
 *
 * @see {@link ResponseIncludeWrapper}
 */
public class ByteArrayServletOutputStream extends ServletOutputStream
{
    /**
     * Our buffer to hold the stream.
     */
    protected final ByteArrayOutputStream buf;


    /**
     * Construct a new ServletOutputStream.
     */
    public ByteArrayServletOutputStream() {
        buf = new ByteArrayOutputStream();
    }

    @Override
    public boolean isReady()
    {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener)
    {
        // do nothing
    }


    /**
     * @return the byte array.
     */
    public byte[] toByteArray() {
        return buf.toByteArray();
    }


    /**
     * Write to our buffer.
     *
     * @param b The parameter to write
     */
    @Override
    public void write(int b) {
        buf.write(b);
    }

    public ByteArrayOutputStream getBuffer() {
        return buf;
    }


}
