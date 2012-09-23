package test.beans;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * No operation method replacer implementation.
 *
 * <p>This is used to test the replaced-method parsing in isn't actually invoked</p>
 */
public class NoOpMethodReplacer implements MethodReplacer {

    public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
        return null;
    }
}
