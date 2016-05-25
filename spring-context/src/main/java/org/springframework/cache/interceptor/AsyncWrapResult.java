package org.springframework.cache.interceptor;

public class AsyncWrapResult {
  private CallBack callBack;

  public AsyncWrapResult(CallBack callBack) {
    this.callBack = callBack;
  }

  public void complete(Object value) {
    callBack.onValue(value);
  }

  public void error(Throwable throwable) {
    callBack.onError(throwable);
  }

  interface CallBack {
    void onValue(Object value);

    void onError(Throwable throwable);
  }

}
