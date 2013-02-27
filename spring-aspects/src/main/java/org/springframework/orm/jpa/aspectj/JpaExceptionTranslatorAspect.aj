/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.springframework.orm.jpa.aspectj;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

public aspect JpaExceptionTranslatorAspect {
    pointcut entityManagerCall(): call(* EntityManager.*(..)) || call(* EntityManagerFactory.*(..)) || call(* EntityTransaction.*(..)) || call(* Query.*(..));

    after() throwing(RuntimeException re): entityManagerCall() {
    	DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(re);
    	if (dex != null) {
    		throw dex;
    	} else {
    		throw re;
    	}
    }
}