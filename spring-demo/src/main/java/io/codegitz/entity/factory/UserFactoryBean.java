package io.codegitz.entity.factory;

import io.codegitz.entity.User;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author 张观权
 * @date 2020/8/10 20:05
 **/
public class UserFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		return new User();
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}
}
