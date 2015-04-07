/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.beans.PropertyEditorSupport;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.util.StringUtils;

/**
 * Property editor that converts a String into a {@link TransactionAttributeSource}.
 * The transaction attribute string must be parseable by the
 * {@link TransactionAttributeEditor} in this package.
 *
 * <p>Strings are in property syntax, with the form:<br>
 * {@code FQCN.methodName=&lt;transaction attribute string&gt;}
 *
 * <p>For example:<br>
 * {@code com.mycompany.mycode.MyClass.myMethod=PROPAGATION_MANDATORY,ISOLATION_DEFAULT}
 *
 * <p><b>NOTE:</b> The specified class must be the one where the methods are
 * defined; in case of implementing an interface, the interface class name.
 *
 * <p>Note: Will register all overloaded methods for a given name.
 * Does not support explicit registration of certain overloaded methods.
 * Supports "xxx*" mappings, e.g. "notify*" for "notify" and "notifyAll".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 26.04.2003
 * @see TransactionAttributeEditor
 */
public class TransactionAttributeSourceEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
		if (StringUtils.hasLength(text)) {
			// Use properties editor to tokenize the hold string.
			PropertiesEditor propertiesEditor = new PropertiesEditor();
			propertiesEditor.setAsText(text);
			Properties props = (Properties) propertiesEditor.getValue();

			// Now we have properties, process each one individually.
			TransactionAttributeEditor tae = new TransactionAttributeEditor();
			Enumeration<?> propNames = props.propertyNames();
			while (propNames.hasMoreElements()) {
				String name = (String) propNames.nextElement();
				String value = props.getProperty(name);
				// Convert value to a transaction attribute.
				tae.setAsText(value);
				TransactionAttribute attr = (TransactionAttribute) tae.getValue();
				// Register name and attribute.
				source.addTransactionalMethod(name, attr);
			}
		}
		setValue(source);
	}

}
