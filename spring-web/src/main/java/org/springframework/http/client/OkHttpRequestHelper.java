package org.springframework.http.client;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;

final class OkHttpRequestHelper {
    public OkHttpRequestHelper() {}

    static Request buildRequest(HttpMethod method, URI uri, HttpHeaders headers, RequestBody body) throws MalformedURLException {
        Request.Builder builder = new Request.Builder().method(method.name(), body);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                builder.addHeader(entry.getKey(), value);
            }
        }
        return builder.url(uri.toURL()).build();
    }

    static OkHttpRequestBody getRequestBody(OkHttpRequestBody body) {
        return body.isEmpty()?null:body;
    }
}
