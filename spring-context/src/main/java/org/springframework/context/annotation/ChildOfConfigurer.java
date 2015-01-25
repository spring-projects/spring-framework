/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.context.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * BeanFactoryPostProcessor for ChildOf annotation.
 * <p>
 * Taken from https://jira.spring.io/browse/SPR-6343 and slightly adjusted.
 *
 * @author Jan Esser
 */
public class ChildOfConfigurer implements BeanFactoryPostProcessor, PriorityOrdered {

    private int order = Ordered.LOWEST_PRECEDENCE;// default: same as
                                                  // non-Ordered

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        final Iterator<String> iterator = beanFactory.getBeanNamesIterator();
        while (iterator.hasNext()) {
            try {
                final BeanDefinition bd = beanFactory.getBeanDefinition(iterator.next());
                final String beanClassName = bd.getBeanClassName();
                if (StringUtils.hasText(beanClassName)) {
                    try {
                        final ChildOf childOf = AnnotationUtils.findAnnotation(Class.forName(beanClassName), ChildOf.class);
                        if (childOf != null) {
                            final String parentName = childOf.parent();
                            if (StringUtils.hasText(parentName)) {
                                bd.setParentName(parentName);
                            } else
                                throw new FatalBeanException(String.format("%s is @ChildOf annotated, but no parent set.", beanClassName));
                        }
                    } catch (ClassNotFoundException e) {
                        throw new FatalBeanException("Unknown class defined.", e);
                    }
                }
            } catch (NoSuchBeanDefinitionException ex) {
                continue;
            }
        }
    }
}

