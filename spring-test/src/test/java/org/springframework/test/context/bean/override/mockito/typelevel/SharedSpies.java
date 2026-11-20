/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.typelevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoSpyBean(types = Service02.class)
@MockitoSpyBean(types = Service03.class)
@interface SharedSpies {
}
