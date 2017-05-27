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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * BeanFactoryPostProcessor for ChildOf annotation.
 * <p>
 * Taken from https://jira.spring.io/browse/SPR-6343 and slightly adjusted.
 */
public class ChildOfConfigurer implements BeanFactoryPostProcessor, PriorityOrdered {

    private int order = Ordered.LOWEST_PRECEDENCE;// default: same as non-Ordered

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
                                throw new FatalBeanException(String.format("%s is @ChildOf annotated, but no parent set."));
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
