package org.springframework.http.client;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;

public final class OkHttpCallback2SettableFutureAdapter implements Callback {

    private final SettableListenableFuture<ClientHttpResponse> delegate;

    public OkHttpCallback2SettableFutureAdapter(SettableListenableFuture<ClientHttpResponse> future) {
        delegate = future;
    }

    @Override
    public void onFailure(Request request, IOException e) {
        delegate.setException(e);
    }

    @Override
    public void onResponse(Response response) {
        delegate.set(new OkHttpClientHttpResponse(response));
    }
}
