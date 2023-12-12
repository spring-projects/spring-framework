package com.lxcecho.validator.two;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class User {

    @NotNull
    private String name;

    @Min(0)
    @Max(150)
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
