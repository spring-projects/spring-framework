/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.client;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import org.springframework.http.StreamingHttpOutputMessage.Body;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Luciano Leggieri
 */
public final class OkHttpRequestBody extends RequestBody {
    private final AtomicReference<Body> body = new AtomicReference<Body>();
    private final Buffer buffer = new Buffer();

    public OutputStream getAsBuffer() {
        return buffer.outputStream();
    }

    public void setBody(Body b) {
        body.set(b);
        buffer.clear();
    }

    @Override
    public final MediaType contentType() {
        return null;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Body b = body.getAndSet(null);
        if (b != null) {
            b.writeTo(sink.outputStream());
        } else {
            sink.writeAll(buffer);
            buffer.clear();
        }
    }

    public boolean isEmpty() {
        return (body.get() == null && buffer.size() == 0);
    }
}
