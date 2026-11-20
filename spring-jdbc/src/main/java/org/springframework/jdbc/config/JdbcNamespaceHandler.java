/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link NamespaceHandler} for JDBC configuration namespace.
 * @author Oliver Gierke
 * @author Dave Syer
 */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("embedded-database", new EmbeddedDatabaseBeanDefinitionParser());
		registerBeanDefinitionParser("initialize-database", new InitializeDatabaseBeanDefinitionParser());
	}
}
