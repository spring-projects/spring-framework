/*
 * Copyright 2002-2016 the original author or authors.
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



package org.springframework.web.servlet.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Iterator;
import java.util.LinkedList;



/**
 * Exact copy of the org.grails.buffer.StreamByteBuffer class from Grails framework. https://github.com/grails/grails-core
 *
 * An in-memory buffer that provides OutputStream and InputStream interfaces.
 *
 * This is more efficient than using ByteArrayOutputStream/ByteArrayInputStream
 *
 * This is not thread-safe, it is intended to be used by a single Thread.
 *
 * @author Lari Hotari, Sagire Software Oy
 * @since 4.3.0
 */
public class StreamByteBuffer {

    private static final int DEFAULT_CHUNK_SIZE = 8192;

    private LinkedList<StreamByteBufferChunk> chunks = new LinkedList<StreamByteBufferChunk>();
    private StreamByteBufferChunk currentWriteChunk;
    private StreamByteBufferChunk currentReadChunk = null;
    private int chunkSize;
    private StreamByteBufferOutputStream output;
    private StreamByteBufferInputStream input;
    private int totalBytesUnreadInList = 0;
    private int totalBytesUnreadInIterator = 0;
    private ReadMode readMode;
    private Iterator<StreamByteBufferChunk> readIterator;

    public enum ReadMode {
        REMOVE_AFTER_READING,
        RETAIN_AFTER_READING
    }

    public StreamByteBuffer() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public StreamByteBuffer(int chunkSize) {
        this(chunkSize, ReadMode.REMOVE_AFTER_READING);
    }

    public StreamByteBuffer(int chunkSize, ReadMode readMode) {
        this.chunkSize = chunkSize;
        this.readMode = readMode;
        currentWriteChunk = new StreamByteBufferChunk(chunkSize);
        output = new StreamByteBufferOutputStream();
        input = new StreamByteBufferInputStream();
    }

    public OutputStream getOutputStream() {
        return output;
    }

    public InputStream getInputStream() {
        return input;
    }

    public void writeTo(OutputStream target) throws IOException
    {
        while (prepareRead() != -1) {
            currentReadChunk.writeTo(target);
        }
    }

    public byte[] readAsByteArray() {
        byte[] buf = new byte[totalBytesUnread()];
        input.readImpl(buf, 0, buf.length);
        return buf;
    }

    public String readAsString(String encoding) throws CharacterCodingException
    {
        Charset charset = Charset.forName(encoding);
        return readAsString(charset);
    }

    public String readAsString(Charset charset) throws CharacterCodingException {
        int unreadSize = totalBytesUnread();
        if (unreadSize > 0) {
            CharsetDecoder decoder = charset.newDecoder().onMalformedInput(
                    CodingErrorAction.REPLACE).onUnmappableCharacter(
                    CodingErrorAction.REPLACE);
            CharBuffer charbuffer = CharBuffer.allocate(unreadSize);
            ByteBuffer buf = null;
            while (prepareRead() != -1) {
                buf = currentReadChunk.readToNioBuffer();
                boolean endOfInput = (prepareRead() == -1);
                CoderResult result = decoder.decode(buf, charbuffer, endOfInput);
                if (endOfInput) {
                    if (!result.isUnderflow()) {
                        result.throwException();
                    }
                }
            }
            CoderResult result = decoder.flush(charbuffer);
            if (buf.hasRemaining()) {
                throw new IllegalStateException("There's a bug here, buffer wasn't read fully.");
            }
            if (!result.isUnderflow()) {
                result.throwException();
            }
            charbuffer.flip();
            String str;
            if (charbuffer.hasArray()) {
                int len = charbuffer.remaining();
                char[] ch = charbuffer.array();
                if (len != ch.length) {
                    ch = (char[])subarray(ch, 0, len);
                }
                str = new String(ch);
            }
            else {
                str = charbuffer.toString();
            }
            return str;
        }
        return null;
    }

    public int totalBytesUnread() {
        int total = 0;
        if (readMode == ReadMode.REMOVE_AFTER_READING) {
            total = totalBytesUnreadInList;
        }
        else if (readMode == ReadMode.RETAIN_AFTER_READING) {
            prepareRetainAfterReading();
            total = totalBytesUnreadInIterator;
        }
        if (currentReadChunk != null) {
            total += currentReadChunk.bytesUnread();
        }
        if (currentWriteChunk != currentReadChunk && currentWriteChunk != null) {
            if (readMode == ReadMode.REMOVE_AFTER_READING) {
                total += currentWriteChunk.bytesUnread();
            }
            else if (readMode == ReadMode.RETAIN_AFTER_READING) {
                total += currentWriteChunk.bytesUsed();
            }
        }
        return total;
    }

    protected int allocateSpace() {
        int spaceLeft = currentWriteChunk.spaceLeft();
        if (spaceLeft == 0) {
            chunks.add(currentWriteChunk);
            totalBytesUnreadInList += currentWriteChunk.bytesUnread();
            currentWriteChunk = new StreamByteBufferChunk(chunkSize);
            spaceLeft = currentWriteChunk.spaceLeft();
        }
        return spaceLeft;
    }

    protected int prepareRead() {
        prepareRetainAfterReading();
        int bytesUnread = (currentReadChunk != null) ? currentReadChunk.bytesUnread() : 0;
        if (bytesUnread == 0) {
            if (readMode == ReadMode.REMOVE_AFTER_READING && !chunks.isEmpty()) {
                currentReadChunk = chunks.removeFirst();
                bytesUnread = currentReadChunk.bytesUnread();
                totalBytesUnreadInList -= bytesUnread;
            }
            else if (readMode == ReadMode.RETAIN_AFTER_READING && readIterator.hasNext()) {
                currentReadChunk = readIterator.next();
                currentReadChunk.reset();
                bytesUnread = currentReadChunk.bytesUnread();
                totalBytesUnreadInIterator -= bytesUnread;
            }
            else if (currentReadChunk != currentWriteChunk) {
                currentReadChunk = currentWriteChunk;
                bytesUnread = currentReadChunk.bytesUnread();
            }
            else {
                bytesUnread = -1;
            }
        }
        return bytesUnread;
    }

    public void reset() {
        if (readMode == ReadMode.RETAIN_AFTER_READING) {
            readIterator = null;
            prepareRetainAfterReading();
            if (currentWriteChunk != null) {
                currentWriteChunk.reset();
            }
        }
    }

    private void prepareRetainAfterReading() {
        if (readMode == ReadMode.RETAIN_AFTER_READING && readIterator == null) {
            readIterator = chunks.iterator();
            totalBytesUnreadInIterator = totalBytesUnreadInList;
            currentReadChunk = null;
        }
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public void setReadMode(ReadMode readMode) {
        this.readMode = readMode;
    }

    public void retainAfterReadingMode() {
        setReadMode(ReadMode.RETAIN_AFTER_READING);
    }

    class StreamByteBufferChunk {
        private int pointer = 0;
        private byte[] buffer;
        private int size;
        private int used = 0;

        public StreamByteBufferChunk(int size) {
            this.size = size;
            buffer = new byte[size];
        }

        public ByteBuffer readToNioBuffer() {
            if (pointer < used) {
                ByteBuffer result;
                if (pointer > 0 || used < size) {
                    result = ByteBuffer.wrap(buffer, pointer, used - pointer);
                }
                else {
                    result = ByteBuffer.wrap(buffer);
                }
                pointer = used;
                return result;
            }

            return null;
        }

        public boolean write(byte b) {
            if (used < size) {
                buffer[used++] = b;
                return true;
            }

            return false;
        }

        public void write(byte[] b, int off, int len) {
            System.arraycopy(b, off, buffer, used, len);
            used = used + len;
        }

        public void read(byte[] b, int off, int len) {
            System.arraycopy(buffer, pointer, b, off, len);
            pointer = pointer + len;
        }

        public void writeTo(OutputStream target) throws IOException {
            if (pointer < used) {
                target.write(buffer, pointer, used - pointer);
                pointer = used;
            }
        }

        public void reset() {
            pointer = 0;
        }

        public int bytesUsed() {
            return used;
        }

        public int bytesUnread() {
            return used - pointer;
        }

        public int read() {
            if (pointer < used) {
                return buffer[pointer++] & 0xff;
            }

            return -1;
        }

        public int spaceLeft() {
            return size - used;
        }
    }

    class StreamByteBufferOutputStream extends OutputStream {
        private boolean closed = false;

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return;
            }

            int bytesLeft = len;
            int currentOffset = off;
            while (bytesLeft > 0) {
                int spaceLeft = allocateSpace();
                int writeBytes = Math.min(spaceLeft, bytesLeft);
                currentWriteChunk.write(b, currentOffset, writeBytes);
                bytesLeft -= writeBytes;
                currentOffset += writeBytes;
            }
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void write(int b) throws IOException {
            allocateSpace();
            currentWriteChunk.write((byte) b);
        }

        public StreamByteBuffer getBuffer() {
            return StreamByteBuffer.this;
        }
    }

    class StreamByteBufferInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            prepareRead();
            return currentReadChunk.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return readImpl(b, off, len);
        }

        int readImpl(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return 0;
            }

            int bytesLeft = len;
            int currentOffset = off;
            int bytesUnread = prepareRead();
            int totalBytesRead = 0;
            while (bytesLeft > 0 && bytesUnread != -1) {
                int readBytes = Math.min(bytesUnread, bytesLeft);
                currentReadChunk.read(b, currentOffset, readBytes);
                bytesLeft -= readBytes;
                currentOffset += readBytes;
                totalBytesRead += readBytes;
                bytesUnread = prepareRead();
            }
            if (totalBytesRead > 0) {
                return totalBytesRead;
            }

            return -1;
        }

        @Override
        public synchronized void reset() throws IOException {
            if (readMode==ReadMode.RETAIN_AFTER_READING) {
                StreamByteBuffer.this.reset();
            }
            else {
                // reset isn't supported in ReadMode.REMOVE_AFTER_READING
                super.reset();
            }
        }

        @Override
        public int available() throws IOException {
            return totalBytesUnread();
        }

        public StreamByteBuffer getBuffer() {
            return StreamByteBuffer.this;
        }
    }

    public void clear() {
        chunks.clear();
        currentReadChunk = null;
        totalBytesUnreadInList = 0;
        totalBytesUnreadInIterator = 0;
        currentWriteChunk = new StreamByteBufferChunk(chunkSize);
        readIterator = null;
    }

    /**
     * Returns the subarray of an existing array
     *
     * @param args The array object
     * @param start The start index (inclusive)
     * @param end The end index (exclusive)
     * @return The new array
     */
    static Object subarray(Object args, int start, int end) {
        int len = Array.getLength(args);

        if(start < 0) start = 0;
        if(end > len) end = len;

        Class<?> type = args.getClass().getComponentType();

        int newLen = end - start;
        if(newLen <= 0) {
            return Array.newInstance(type,0);
        }
        else {
            Object newArray = Array.newInstance(type, newLen );
            System.arraycopy(args, start, newArray,0, newLen);
            return newArray;
        }
    }


}

