package io.codegitz.entity.factory;

import io.codegitz.entity.User;

/**
 * @author 张观权
 * @date 2020/8/10 19:39
 **/
public class StaticFactory {
	public static User getUser(){
		return new User();
	}
}
