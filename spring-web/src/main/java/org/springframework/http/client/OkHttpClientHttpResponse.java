package org.springframework.http.client;

import com.squareup.okhttp.Response;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;

public final class OkHttpClientHttpResponse extends AbstractClientHttpResponse {

    private final Response response;
    private final HttpHeaders httpHeaders;

    public OkHttpClientHttpResponse(Response r) {
        response = r;
        httpHeaders = new HttpHeaders();
        for (String key : response.headers().names()) {
            for (String value : response.headers(key)) {
                httpHeaders.add(key, value);
            }
        }
    }

    @Override
    public int getRawStatusCode() {
        return response.code();
    }

    @Override
    public String getStatusText() {
        return response.message();
    }

    @Override
    public void close() {
        try {
            response.body().close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public InputStream getBody() {
        return response.body().byteStream();
    }

    @Override
    public HttpHeaders getHeaders() {
        return httpHeaders;
    }
}
