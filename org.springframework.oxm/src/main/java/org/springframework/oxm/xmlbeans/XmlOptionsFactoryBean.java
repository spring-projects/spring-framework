/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.oxm.xmlbeans;

import java.util.Iterator;
import java.util.Map;

import org.apache.xmlbeans.XmlOptions;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Factory bean that configures an XMLBeans <code>XmlOptions</code> object and provides it as a bean reference.
 * <p/>
 * Typical usage will be to set XMLBeans options on this bean, and refer to it in the <code>XmlBeansMarshaller</code>.
 *
 * @author Arjen Poutsma
 * @see XmlOptions
 * @see #setOptions(java.util.Map)
 * @see XmlBeansMarshaller#setXmlOptions(org.apache.xmlbeans.XmlOptions)
 * @since 1.0.0
 */
public class XmlOptionsFactoryBean implements FactoryBean, InitializingBean {

    private XmlOptions xmlOptions;

    private Map options;

    /** Returns the singleton <code>XmlOptions</code>. */
    public Object getObject() throws Exception {
        return xmlOptions;
    }

    /** Returns the class of <code>XmlOptions</code>. */
    public Class getObjectType() {
        return XmlOptions.class;
    }

    /** Returns <code>true</code>. */
    public boolean isSingleton() {
        return true;
    }

    /**
     * Sets options on the underlying <code>XmlOptions</code> object. The keys of the supplied map should be one of the
     * string constants defined in <code>XmlOptions</code>, the values vary per option.
     *
     * @see XmlOptions#put(Object,Object)
     * @see XmlOptions#SAVE_PRETTY_PRINT
     * @see XmlOptions#LOAD_STRIP_COMMENTS
     */
    public void setOptions(Map options) {
        this.options = options;
    }

    public void afterPropertiesSet() throws Exception {
        xmlOptions = new XmlOptions();
        if (options != null) {
            for (Iterator iterator = options.keySet().iterator(); iterator.hasNext();) {
                Object option = iterator.next();
                xmlOptions.put(option, options.get(option));
            }
        }
    }
}
