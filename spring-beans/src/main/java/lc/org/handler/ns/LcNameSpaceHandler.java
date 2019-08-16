package lc.org.handler.ns;

import lc.org.parser.LcElementParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author : liuc
 * @date : 2019/4/10 14:50
 * @description :
 */
public class LcNameSpaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		//注册解析器,第一参数为元素名称,例如:<lc:custom  lc为命名空间 custom为元素名称
		registerBeanDefinitionParser("custom",new LcElementParser());
	}
}
