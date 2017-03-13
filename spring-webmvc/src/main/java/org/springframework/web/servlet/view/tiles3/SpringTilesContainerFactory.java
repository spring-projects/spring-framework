package org.springframework.web.servlet.view.tiles3;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.jsp.JspFactory;

import org.apache.tiles.TilesContainer;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.ApplicationContextAware;
import org.apache.tiles.request.ApplicationResource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer.TilesElActivator;

/**
 * @author Torsten Krah
 */
class SpringTilesContainerFactory extends BasicTilesContainerFactory {

    private TilesConfigurer tilesConfigurer;

    public SpringTilesContainerFactory(TilesConfigurer tilesConfigurer) {
        this.tilesConfigurer = tilesConfigurer;
    }

    @Override
    protected TilesContainer createDecoratedContainer(TilesContainer originalContainer, ApplicationContext context) {
        return (tilesConfigurer.useMutableTilesContainer ? new CachingTilesContainer(originalContainer) : originalContainer);
    }

    @Override
    protected List<ApplicationResource> getSources(ApplicationContext applicationContext) {
        if (tilesConfigurer.definitions != null) {
            List<ApplicationResource> result = new LinkedList<>();
            for (String definition : tilesConfigurer.definitions) {
                Collection<ApplicationResource> resources = applicationContext.getResources(definition);
                if (resources != null) {
                    result.addAll(resources);
                }
            }
            return result;
        } else {
            return super.getSources(applicationContext);
        }
    }

    @Override
    protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(ApplicationContext applicationContext,
                                                                        LocaleResolver resolver) {
        BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(applicationContext, resolver);
        if (tilesConfigurer.checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
            ((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(true);
        }
        return dao;
    }

    @Override
    protected DefinitionsReader createDefinitionsReader(ApplicationContext context) {
        DigesterDefinitionsReader reader = (DigesterDefinitionsReader) super.createDefinitionsReader(context);
        reader.setValidating(tilesConfigurer.validateDefinitions);
        return reader;
    }

    @Override
    protected DefinitionsFactory createDefinitionsFactory(ApplicationContext applicationContext,
                                                          LocaleResolver resolver) {

        if (tilesConfigurer.definitionsFactoryClass != null) {
            DefinitionsFactory factory = BeanUtils.instantiateClass(tilesConfigurer.definitionsFactoryClass);
            if (factory instanceof ApplicationContextAware) {
                ((ApplicationContextAware) factory).setApplicationContext(applicationContext);
            }
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
            if (bw.isWritableProperty("localeResolver")) {
                bw.setPropertyValue("localeResolver", resolver);
            }
            if (bw.isWritableProperty("definitionDAO")) {
                bw.setPropertyValue("definitionDAO", createLocaleDefinitionDao(applicationContext, resolver));
            }
            return factory;
        } else {
            return super.createDefinitionsFactory(applicationContext, resolver);
        }
    }

    @Override
    protected PreparerFactory createPreparerFactory(ApplicationContext context) {
        if (tilesConfigurer.preparerFactoryClass != null) {
            return BeanUtils.instantiateClass(tilesConfigurer.preparerFactoryClass);
        } else {
            return super.createPreparerFactory(context);
        }
    }

    @Override
    protected LocaleResolver createLocaleResolver(ApplicationContext context) {
        return new SpringLocaleResolver();
    }

    @Override
    protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(ApplicationContext context,
                                                                        LocaleResolver resolver) {
        AttributeEvaluator evaluator;
        if (TilesConfigurer.tilesElPresent && JspFactory.getDefaultFactory() != null) {
            evaluator = new TilesElActivator().createEvaluator(tilesConfigurer.servletContext);
        } else {
            evaluator = new DirectAttributeEvaluator();
        }
        return new BasicAttributeEvaluatorFactory(evaluator);
    }
}
