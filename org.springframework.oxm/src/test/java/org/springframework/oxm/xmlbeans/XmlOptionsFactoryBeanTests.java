/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.Collections;

import org.apache.xmlbeans.XmlOptions;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Arjen Poutsma
 */
public class XmlOptionsFactoryBeanTests {

    private XmlOptionsFactoryBean factoryBean = new XmlOptionsFactoryBean();

    @Test
	public void xmlOptionsFactoryBean() throws Exception {
        factoryBean.setOptions(Collections.singletonMap(XmlOptions.SAVE_PRETTY_PRINT, Boolean.TRUE));
        XmlOptions xmlOptions = factoryBean.getObject();
        assertNotNull("No XmlOptions returned", xmlOptions);
        assertTrue("Option not set", xmlOptions.hasOption(XmlOptions.SAVE_PRETTY_PRINT));
        assertFalse("Invalid option set", xmlOptions.hasOption(XmlOptions.LOAD_LINE_NUMBERS));
    }

}
