package org.springframework.tests.sample.beans;

/**
 * @author luzhonghao
 * @since 2016.05.28
 */
public class TestGenericBean<T> {
    private T name;

    public T getName() {
        return name;
    }

    public void setName(T name) {
        this.name = name;
    }
}
