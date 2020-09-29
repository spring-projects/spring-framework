package io.codegitz.customtag;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author 张观权
 * @date 2020/9/3 14:48
 **/
public class UserBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
	/**
	 * element对应的类
	 * @param element the {@code Element} that is being parsed
	 * @return
	 */
	protected Class<?> getBeanClass(Element element){
		return User.class;
	}

	/**
	 * 从element中获取对应的元素
	 * @param element
	 * @param beanDefinitionBuilder
	 */
	@Override
	protected void doParse(Element element, BeanDefinitionBuilder beanDefinitionBuilder){

		String userName = element.getAttribute("userName");
		String email = element.getAttribute("email");
		if (StringUtils.hasText(userName)){
			beanDefinitionBuilder.addPropertyValue("userName",userName);
		}

		if (StringUtils.hasText(email)){
			beanDefinitionBuilder.addPropertyValue("email",email);
		}
	}
}
