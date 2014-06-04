package source.source.spring.ch4;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class UserNamespacesHandlers extends NamespaceHandlerSupport {
	public void init() {
		registerBeanDefinitionParser("user", new UserNamespacesprase());
	}
}