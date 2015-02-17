package org.springframework.http.client;

import com.squareup.okhttp.OkHttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.springframework.http.client.OkHttpRequestHelper.buildRequest;
import static org.springframework.http.client.OkHttpRequestHelper.getRequestBody;

public final class OkHttpClientHttpRequest extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

    private final OkHttpClient client;
    private final URI uri;
    private final HttpMethod httpMethod;
    private final OkHttpRequestBody body;

    public OkHttpClientHttpRequest(OkHttpClient okHttpClient, URI u, HttpMethod method) {
        client = okHttpClient;
        uri = u;
        httpMethod = method;
        body = new OkHttpRequestBody();
    }

    @Override
    public HttpMethod getMethod() {
        return httpMethod;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public void setBody(Body b) {
        assertNotExecuted();
        body.setBody(b);
    }

    @Override
    protected OutputStream getBodyInternal(HttpHeaders ignored) {
        return body.getAsBuffer();
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        return new OkHttpClientHttpResponse(
                client.newCall(buildRequest(httpMethod, uri, headers, getRequestBody(body))).execute());
    }
}
