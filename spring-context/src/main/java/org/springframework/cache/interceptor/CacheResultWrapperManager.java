package org.springframework.cache.interceptor;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.*;

public class CacheResultWrapperManager {
  private Map<Class<?>, CacheResultWrapper> unwrapperByClass;

  public CacheResultWrapperManager() {
    unwrapperByClass = new HashMap<Class<?>, CacheResultWrapper>();

    List<CacheResultWrapper> unwrapperList = new ArrayList<CacheResultWrapper>();
    try {
      ClassUtils.forName("java.util.Optional", CacheAspectSupport.class.getClassLoader());
      unwrapperList.add(new OptionalUnWrapper());
    }
    catch (ClassNotFoundException ex) {
      // Java 8 not available - Optional references simply not supported then.
    }

    try {
      ClassUtils.forName("rx.Observable", CacheAspectSupport.class.getClassLoader());
      unwrapperList.add(new ObservableWrapper());
    }
    catch (ClassNotFoundException ex) {
      // RxJava not available
    }

    for(CacheResultWrapper unwrapper: unwrapperList) {
      unwrapperByClass.put(unwrapper.getWrapClass(), unwrapper);
    }
  }

  public Object wrap(Class<?> clazz, Object value) {
    if(value != null) {
      CacheResultWrapper unwrapper = unwrapperByClass.get(clazz);

      if (unwrapper != null) {
        return unwrapper.wrap(value);
      }
    }

    return value;
  }

  public Object asyncUnwrap(Object valueWrapped, AsyncWrapResult asyncResult) {
    if(valueWrapped != null) {
      CacheResultWrapper unwrapper = unwrapperByClass.get(valueWrapped.getClass());

      if (unwrapper != null) {
        return unwrapper.unwrap(valueWrapped, asyncResult);
      }
    }

    asyncResult.complete(valueWrapped);

    return valueWrapped;
  }


  /**
   * Inner class to avoid a hard dependency on Java 8.
   */
  private class OptionalUnWrapper implements CacheResultWrapper {

    @Override
    public Object unwrap(Object optionalObject, AsyncWrapResult asyncResult) {
      Optional<?> optional = (Optional<?>) optionalObject;
      if (!optional.isPresent()) {
        asyncResult.complete(null);
      } else {
        Object result = optional.get();
        Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
        asyncResult.complete(result);
      }
      return optionalObject;
    }

    @Override
    public Class<?> getWrapClass() {
      return Optional.class;
    }

    @Override
    public Object wrap(Object value) {
      return Optional.ofNullable(value);
    }
  }

  private class ObservableWrapper implements CacheResultWrapper {

    @Override
    public Object wrap(Object value) {
      if(value instanceof Iterable) {
        return Observable.from((Iterable<?>) value);

      } else {
        // Not sure if it's a good idea... At least a warning maybe be a good idea
        return Observable.just(value);
      }
    }

    @Override
    public Object unwrap(Object valueWrapped, final AsyncWrapResult asyncResult) {
      Observable<?> valueObservable = (Observable<?>) valueWrapped;

      final List<Object> values = new ArrayList<Object>();

      return valueObservable
          .doOnNext(new Action1<Object>() {
            @Override
            public void call(Object o) {
              values.add(o);
            }
          })
          .doOnCompleted(new Action0() {
            @Override
            public void call() {
              asyncResult.complete(values);
            }
          })
          .doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
              asyncResult.error(throwable);
            }
          });
    }

    @Override
    public Class<?> getWrapClass() {
      return Observable.class;
    }
  }
}
