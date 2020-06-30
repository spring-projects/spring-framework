package org.springframework.context.annotation.foo;

/**
 * @author lifejwang11
 * @since 5.2
 */
public class SomeBean {
    private final SomeOtherBean other;

    public SomeBean(SomeOtherBean other) {
        this.other = other;
    }
}
