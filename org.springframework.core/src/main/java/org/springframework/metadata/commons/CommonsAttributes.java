/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.metadata.commons;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.metadata.Attributes;

/**
 * Implementation of the Spring Attributes facade for Commons Attributes.
 *
 * <p>Please see the
 * <a href="http://jakarta.apache.org/commons/sandbox/attributes">
 * Commons Attributes documentation</a> for information on how to use the
 * attribute compiler.
 *
 * <p>As of December 2003, follow the Javadocs to the AttributeCompiler class
 * to see how the Ant task works. Note that you need to put the following jars
 * in your $ANT_HOME/lib directory for the Common Attributes compiler to work:
 * <ul>
 * <li>Commons Attributes compiler jar
 * <li>the xjavadoc Jar (from XDoclet)
 * <li>commons-collection.jar (from Jakarta Commons)
 * </ul>
 *
 * <p>You need to perform the attribute compilation step before compiling your source.
 *
 * <p>See build.xml in the tests for package org.springframework.aop.autoproxy.metadata
 * for an example of the required Ant scripting. The header of this build script
 * includes some quick, and hopefully useful, hints on using Commons Attributes.
 * The source files in the same package (TxClass and TxClassWithClassAttribute)
 * illustrate attribute usage in source files.
 *
 * <p>The Spring Framework project does not provide support usage of specific
 * attributes implementations. Please refer to the appropriate site and mailing
 * list of the attributes implementation.
 *
 * @author Rod Johnson
 */
public class CommonsAttributes implements Attributes {
	
	/*
	 * Commons Attributes caches attributes, so we don't need to cache here
	 * as well.
	 */

	public Collection getAttributes(Class targetClass) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetClass);
	}

	public Collection getAttributes(Class targetClass, Class filter) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetClass, filter);
	}

	public Collection getAttributes(Method targetMethod) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetMethod);
	}

	public Collection getAttributes(Method targetMethod, Class filter) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetMethod, filter);
	}

	public Collection getAttributes(Field targetField) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetField);
	}

	public Collection getAttributes(Field targetField, Class filter) {
		return org.apache.commons.attributes.Attributes.getAttributes(targetField, filter);
	}

}
