package io.codegitz.customtag;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author 张观权
 * @date 2020/9/3 14:55
 **/
public class MyNamespaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("user",new UserBeanDefinitionParser());
	}
}
