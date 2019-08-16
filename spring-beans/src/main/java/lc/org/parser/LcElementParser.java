package lc.org.parser;

import lc.org.bean.LcCustomer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author : liuc
 * @date : 2019/4/10 15:18
 * @description :
 */
public class LcElementParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return LcCustomer.class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String name = element.getAttribute("name");
		String hobby = element.getAttribute("hobby");
		if(!StringUtils.hasText(name)){
			throw new NullPointerException("lc customer name can not be null");
		}
		if(!StringUtils.hasText(hobby)){
			throw new NullPointerException("lc customer hobby can not be null");
		}
		builder.addPropertyValue("name",name);
		builder.addPropertyValue("hobby",hobby);
	}
}
