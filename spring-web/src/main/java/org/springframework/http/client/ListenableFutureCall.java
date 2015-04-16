package org.springframework.http.client;

import com.squareup.okhttp.Call;
import org.springframework.util.concurrent.SettableListenableFuture;

public final class ListenableFutureCall extends SettableListenableFuture<ClientHttpResponse> {

    private final Call call;

    public ListenableFutureCall(Call c) {
        call = c;
        call.enqueue(new OkHttpCallback2SettableFutureAdapter(this));
    }

    @Override
    protected void interruptTask() {
        call.cancel();
    }
}
