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

package org.springframework.orm.jpa.openjpa;

/**
 * Test that AspectJ weaving (in particular the currently shipped aspects) work with JPA (see SPR-3873 for more details). 
 *
 * @author Ramnivas Laddad
 */
@org.junit.Ignore // TODO SPR-8116 this test causes gradle to hang.
// when run independently e.g. `./gradlew :spring-orm:test -Dtest.single=OpenJpaEntity...`
// it works fine. When run together with all other tests e.g. `./gradlew :spring-orm:test`
// it hangs on the 'testCanSerializeProxies' test method. Note that this test DOES pass in
// Eclipse, even when the entire 'spring-orm' module is run. Run gradle with '-i' to
// get more details when reproducing the hanging test.
public class OpenJpaEntityManagerFactoryWithAspectJWeavingIntegrationTests extends OpenJpaEntityManagerFactoryIntegrationTests {

	protected String[] getConfigLocations() {
		return new String[] {
				"/org/springframework/orm/jpa/openjpa/openjpa-manager-aspectj-weaving.xml", 
				"/org/springframework/orm/jpa/memdb.xml",
				"/org/springframework/orm/jpa/inject.xml"};
	}

}
