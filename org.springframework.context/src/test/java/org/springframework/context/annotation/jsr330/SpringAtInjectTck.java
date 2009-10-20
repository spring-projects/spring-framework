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

package org.springframework.context.annotation.jsr330;

import junit.framework.Test;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SpringAtInjectTck {

	public static Test suite() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotatedBeanDefinitionReader bdr = new AnnotatedBeanDefinitionReader(ac);
		bdr.setScopeMetadataResolver(new ScopeMetadataResolver() {
			public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
				ScopeMetadata metadata = new ScopeMetadata();
				if (definition instanceof AnnotatedBeanDefinition) {
					AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
					metadata.setScopeName(annDef.getMetadata().hasAnnotation(javax.inject.Singleton.class.getName()) ?
							BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
				}
				return metadata;
			}
		});

		bdr.registerBean(Convertible.class);
		bdr.registerBean(DriversSeat.class, Drivers.class);
		bdr.registerBean(Seat.class, Primary.class);
		bdr.registerBean(V8Engine.class);
		bdr.registerBean(SpareTire.class, "spare");
		bdr.registerBean(Cupholder.class);
		bdr.registerBean(Tire.class, Primary.class);
		bdr.registerBean(FuelTank.class);

		ac.refresh();
		Car car = ac.getBean("convertible", Car.class);

		return Tck.testsFor(car, false, true);
	}

	public static Test suiteX() {
		GenericApplicationContext ac = new GenericApplicationContext();

		GenericBeanDefinition carDef = new GenericBeanDefinition();
		carDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		carDef.setBeanClass(Convertible.class);
		ac.registerBeanDefinition("car", carDef);

		GenericBeanDefinition driversSeatDef = new GenericBeanDefinition();
		driversSeatDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		driversSeatDef.setBeanClass(DriversSeat.class);
		driversSeatDef.addQualifier(new AutowireCandidateQualifier(Drivers.class));
		ac.registerBeanDefinition("driversSeat", driversSeatDef);

		GenericBeanDefinition seatDef = new GenericBeanDefinition();
		seatDef.setBeanClass(Seat.class);
		seatDef.setPrimary(true);
		ac.registerBeanDefinition("seat", seatDef);

		GenericBeanDefinition engineDef = new GenericBeanDefinition();
		engineDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		engineDef.setBeanClass(V8Engine.class);
		ac.registerBeanDefinition("engine", engineDef);

		GenericBeanDefinition spareDef = new GenericBeanDefinition();
		spareDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		spareDef.setBeanClass(SpareTire.class);
		spareDef.addQualifier(new AutowireCandidateQualifier(Drivers.class));
		ac.registerBeanDefinition("spare", spareDef);

		GenericBeanDefinition cupholderDef = new GenericBeanDefinition();
		cupholderDef.setBeanClass(Cupholder.class);
		ac.registerBeanDefinition("cupholder", cupholderDef);

		GenericBeanDefinition tireDef = new GenericBeanDefinition();
		tireDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		tireDef.setBeanClass(Tire.class);
		tireDef.setPrimary(true);
		ac.registerBeanDefinition("tire", tireDef);

		GenericBeanDefinition fuelTankDef = new GenericBeanDefinition();
		fuelTankDef.setBeanClass(FuelTank.class);
		ac.registerBeanDefinition("fuelTank", fuelTankDef);

		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		ac.refresh();
		Car car = ac.getBean("car", Car.class);
		return Tck.testsFor(car, false, true);
	}

}
