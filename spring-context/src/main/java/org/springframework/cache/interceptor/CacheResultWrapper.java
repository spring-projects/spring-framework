package org.springframework.cache.interceptor;

interface CacheResultWrapper {
  /**
   * Wraps to the wrapping target class
   * @param value the value to wrap
   * @return the value wrapped
   */
  Object wrap(Object value);

  /**
   * Unwraps a value and returns it decorated if it needs in order to
   * notify the result, this will be the case if the wrapped value is not
   * available at the moment (it is calculated asynchronously
   * @param valueWrapped the value wrapped
   * @param asyncResult it will call it when the value it's available
   * @return the same value wrapped or a version decorated.
   */
  Object unwrap(Object valueWrapped, AsyncWrapResult asyncResult);

  Class<?> getWrapClass();
}
