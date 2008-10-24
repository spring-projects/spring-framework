/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;

/**
 * Extension of the standard JPA EntityManagerFactory interface, linking in
 * Spring's EntityManagerFactoryPlusOperations interface which defines
 * additional operations (beyond JPA 1.0) in a vendor-independent fashion.
 *
 * @author Rod Johnson
 * @since 2.0
 * @see javax.persistence.EntityManager
 */
public interface EntityManagerFactoryPlus extends EntityManagerFactory, EntityManagerFactoryPlusOperations {

}
