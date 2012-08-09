/*
 * Copyright 2010 the original author or authors.
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
package org.springframework.context.groovy

import org.springframework.context.groovy.DynamicElementReader;
import org.w3c.dom.Element

public class DynamicElementReaderTests extends GroovyTestCase{

    void testReadMethodToElement() {

        def elementReader = new DynamicElementReader("jee", [jee:"http://www.springframework.org/schema/jee"])

        try {
            elementReader.'jndi-lookup'(id:"dataSource", 'jndi-name':"jdbc/petstore")
        }
        catch (e) {
            assertEquals """Configuration problem: No namespace handler found for element <jee:jndi-lookup id='dataSource' jndi-name='jdbc/petstore' xmlns:jee='http://www.springframework.org/schema/jee'/>
Offending resource: resource loaded from byte array""", e.message            
        }

    }

}