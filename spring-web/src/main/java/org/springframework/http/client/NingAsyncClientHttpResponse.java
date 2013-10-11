package org.springframework.http.client;

import org.asynchttpclient.Response;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NingAsyncClientHttpResponse extends AbstractClientHttpResponse {

    private final Response response;

    private HttpHeaders headers;

    public NingAsyncClientHttpResponse(Response response) {
        this.response = response;
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return this.response.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return this.response.getStatusText();
    }

    @Override
    public void close() {}

    @Override
    public InputStream getBody() throws IOException {
        return this.response.getResponseBodyAsStream();
    }

    @Override
    public HttpHeaders getHeaders() {
        if (this.headers == null) {
            this.headers = new HttpHeaders();
            for (Map.Entry<String, List<String>> entry : this.response.getHeaders().entrySet()) {
                this.headers.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
            }
        }
        return this.headers;

    }
}
