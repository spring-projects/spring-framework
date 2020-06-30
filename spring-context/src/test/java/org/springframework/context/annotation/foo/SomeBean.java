package org.springframework.context.annotation.foo;

public class SomeBean {
    private final SomeOtherBean other;

    public SomeBean(SomeOtherBean other) {
        this.other = other;
    }
}
