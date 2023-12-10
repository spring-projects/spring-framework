package com.lxcecho.iocxml.bean;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class MyFactoryBean implements FactoryBean<User> {
    @Override
    public User getObject() throws Exception {
        return new User();
    }

    @Override
    public Class<?> getObjectType() {
        return User.class;
    }
}
