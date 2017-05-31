package org.springframework.cache.jcache.interceptor;

import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.*;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author David Brimley
 */
public class CacheResultInterceptorTest extends AbstractCacheOperationTests<CacheResultOperation>  {

    public static final String EXPECTED_RESULT_FROM_METHOD_BODY = "ExpectedResultFromMethodBody";

    /**
     * When a Cache is unavailable (throwing exceptions) we should still expect the Method body to be called
     * and for the value to be returned.  Exceptions should be sent to the CacheErrorHandler for the pre method
     * cache get and for the post method cache put.
     *
     * In earlier versions of CacheResultInterceptor it would throw an exception from the cache on the post method
     * put to cache call, this exception short circuits the value returned from the method body.  It should instead
     * handle exceptions from post method cache put in the same way it handles exceptions from pre method cache get.
     */
    @Test
    public void willReturnAMethodValueWhenCacheIsInError() {
        TestErrorHandler testErrorHandler = new TestErrorHandler();
        CacheResultInterceptor cacheResultInterceptor = new CacheResultInterceptor(testErrorHandler);
        Object methodInvokeResult = cacheResultInterceptor.invoke(new TestCacheOperationInvocationContext(),
                new TestCacheOperationInvoker());
        assertEquals("Expected " + EXPECTED_RESULT_FROM_METHOD_BODY + " but got " + methodInvokeResult,
                EXPECTED_RESULT_FROM_METHOD_BODY,methodInvokeResult);
        assertTrue("Expected a call to the PutError handler on CacheErrorHandler",
                testErrorHandler.isPutErrorCalled());
        assertTrue("Expected a call to the GetError handler on CacheErrorHandler",
                testErrorHandler.isGetErrorCalled());
    }

    @Override
    protected CacheResultOperation createSimpleOperation() {
        CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
                SampleObject.class, "simpleGet", Long.class);

        return new CacheResultOperation(methodDetails, new FailingCacheResolver(), defaultKeyGenerator,
                defaultExceptionCacheResolver);
    }

    class TestCacheOperationInvoker implements CacheOperationInvoker {
        @Override
        public Object invoke() throws ThrowableWrapper {
            return EXPECTED_RESULT_FROM_METHOD_BODY;
        }
    }

    class TestCacheOperationInvocationContext implements CacheOperationInvocationContext {
        @Override
        public BasicOperation getOperation() {
            return createSimpleOperation();
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }
    }

    /**
     * The CacheErrorHandler that registers calls to Get and Put Error Handlers.
     */
    class TestErrorHandler implements CacheErrorHandler {

        private boolean getErrorCalled;
        private boolean putErrorCalled;

        public boolean isGetErrorCalled() {
            return getErrorCalled;
        }

        public boolean isPutErrorCalled() {
            return putErrorCalled;
        }

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            getErrorCalled=true;
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            putErrorCalled=true;
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {

        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {

        }
    }

    /**
     * The Cache Resolver to return the failing cache
     */
    class FailingCacheResolver implements CacheResolver {
        @Override
        public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
            List<Cache> myCaches = new ArrayList<Cache>();
            myCaches.add(new FailingCache());
            return myCaches;
        }
    }

    /**
     * The Cache that always throws Exceptions on the Get and Puts, simulating the Cache being down/unavailable.
     */
    class FailingCache implements Cache {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Object getNativeCache() {
            return null;
        }

        @Override
        public ValueWrapper get(Object key) {
            throw new RuntimeException("Cache not available");
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(Object key, Object value) {
            throw new RuntimeException("Cache not available");
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return null;
        }

        @Override
        public void evict(Object key) {

        }

        @Override
        public void clear() {

        }
    }

}