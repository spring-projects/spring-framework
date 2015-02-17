package org.springframework.http.client;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import org.springframework.http.StreamingHttpOutputMessage.Body;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

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
