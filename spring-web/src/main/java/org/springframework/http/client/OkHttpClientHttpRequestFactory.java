package org.springframework.http.client;

import com.squareup.okhttp.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.net.URI;

public final class OkHttpClientHttpRequestFactory implements
        ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {

    private final OkHttpClient client;
    private final boolean locallyBuilt;

    public OkHttpClientHttpRequestFactory() {
        client = new OkHttpClient();
        locallyBuilt = true;
    }

    public OkHttpClientHttpRequestFactory(OkHttpClient okHttpClient) {
        Assert.notNull(okHttpClient, "'okHttpClient' must not be null");
        client = okHttpClient;
        locallyBuilt = false;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
        return new OkHttpClientHttpRequest(client, uri, httpMethod);
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
        return new OkHttpAsyncClientHttpRequest(client, uri, httpMethod);
    }

    @Override
    public void destroy() throws Exception {
        if (locallyBuilt) {
            if (this.client.getCache() != null) {
                this.client.getCache().close();
            }
            this.client.getDispatcher().getExecutorService().shutdown();
        }
    }
}
