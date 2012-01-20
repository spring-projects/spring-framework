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

package org.springframework.orm.hibernate3;

import java.io.Serializable;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer;
import org.junit.Test;

import org.springframework.beans.BeanUtils;

/**
 * Test for compatibility of Spring's BeanUtils and its BridgeMethodResolver use
 * when operating on a Hibernate-generated CGLIB proxy class.
 *
 * @author Arnout Engelen
 * @author Juergen Hoeller
 */
public class CglibProxyBridgeMethodTests {

	@Test
	public void introspectHibernateProxyForGenericClass() {
		BeanUtils.getPropertyDescriptor(CglibInstantieMedewerker.class, "organisatie");
		Class<?> clazz = CGLIBLazyInitializer.getProxyFactory(
				CglibInstantieMedewerker.class, new Class[] {HibernateProxy.class});
		BeanUtils.getPropertyDescriptor(clazz, "organisatie");
	}


	public interface CglibIOrganisatie {
	}


	public class CglibOrganisatie implements CglibIOrganisatie {
	}


	public class CglibInstantie extends CglibOrganisatie {
	}


	public interface CglibIOrganisatieMedewerker<T extends CglibIOrganisatie> {

		void setOrganisatie(T organisatie);
	}


	public class CglibOrganisatieMedewerker<T extends CglibOrganisatie> implements CglibIOrganisatieMedewerker<T> {

		public void setOrganisatie(CglibOrganisatie organisatie) {
		}
	}


	public class CglibInstantieMedewerker extends CglibOrganisatieMedewerker<CglibInstantie> implements Serializable {
	}

}
