/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.servlet.handler.metadata;

import java.util.Collection;

import org.apache.commons.attributes.AttributeIndex;
import org.apache.commons.attributes.Attributes;

/**
 * Subclass of AbstractPathMapHandlerMapping that recognizes Commons Attributes
 * metadata attributes of type PathMap on application Controllers and automatically
 * wires them into the current servlet's WebApplicationContext.
 *
 * <p>
 * Controllers must have class attributes of the form:
 * <code>
 * &64;org.springframework.web.servlet.handler.commonsattributes.PathMap("/path.cgi")
 * </code>
 *
 * <p>The path must be mapped to the relevant Spring DispatcherServlet in /WEB-INF/web.xml.
 * It's possible to have multiple PathMap attributes on the one controller class.
 *
 * <p>To use this feature, you must compile application classes with Commons Attributes,
 * and run the Commons Attributes indexer tool on your application classes, which must
 * be in a Jar rather than in WEB-INF/classes.
 *
 * <p>Controllers instantiated by this class may have dependencies on middle tier
 * objects, expressed via JavaBean properties or constructor arguments. These will
 * be resolved automatically.
 *
 * <p>You will normally use this HandlerMapping with at most one DispatcherServlet in
 * your web application. Otherwise you'll end with one instance of the mapped controller
 * for each DispatcherServlet's context. You <i>might</i> want this--for example, if
 * one's using a .pdf mapping and a PDF view, and another a JSP view, or if using
 * different middle tier objects, but should understand the implications. All
 * Controllers with attributes will be picked up by each DispatcherServlet's context.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @deprecated as of Spring 2.5, in favor of annotation-based request mapping.
 * To be removed in Spring 3.0.
 */
public class CommonsPathMapHandlerMapping extends AbstractPathMapHandlerMapping {
	
	/**
	 * Use Commons Attributes AttributeIndex to get a Collection of Class
	 * objects with the required PathMap attribute. Protected so that it can
	 * be overridden during testing.
	 */
	protected Class[] getClassesWithPathMapAttributes() throws Exception {
		AttributeIndex ai = new AttributeIndex(getClass().getClassLoader());
		Collection classes = ai.getClasses(PathMap.class);
		return (Class[]) classes.toArray(new Class[classes.size()]);
	}
	
	/**
	 * Use Commons Attributes to find PathMap attributes for the given class.
	 * We know there's at least one, as the getClassNamesWithPathMapAttributes
	 * method return this class name.
	 */
	protected PathMap[] getPathMapAttributes(Class handlerClass) {
		Collection atts = Attributes.getAttributes(handlerClass, PathMap.class);
		return (PathMap[]) atts.toArray(new PathMap[atts.size()]);
	}

}
