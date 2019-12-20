package org.springframework.web.context.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A copy of Spring-boot 2.1.8 {@link AnnotationConfigServletWebServerApplicationContext},
 * except that it does not use its own WebServer (other super class).
 * <p>
 * Also See {@link AnnotationConfigApplicationContext} (Same as this but uses
 * GenericApplicationContext instead of GenericWebApplicationContext)
 * <p>
 * In contrast to {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext},
 * this class uses the GenericWebApplicationContext, which allows registering beans using a Supplier
 * {@link #registerBean(Class, Supplier, BeanDefinitionCustomizer...)}
 * <p>
 * See javadoc of superclass {@link GenericWebApplicationContext} for more information.
 *
 * @author Elias Lerch
 * @see #register
 * @see #scan
 */
public class AnnotationConfigGenericWebApplicationContext extends GenericWebApplicationContext implements AnnotationConfigRegistry {

  private final AnnotatedBeanDefinitionReader reader;
  private final ClassPathBeanDefinitionScanner scanner;
  private final Set<Class<?>> annotatedClasses = new LinkedHashSet<>();
  private String[] basePackages;

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext} that needs
   * to be populated through {@link #register} calls and then manually
   * {@linkplain #refresh refreshed}.
   */
  public AnnotationConfigWebApplicationContext() {
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
  }

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext} with the
   * given {@code DefaultListableBeanFactory}. The context needs to be populated through
   * {@link #register} calls and then manually {@linkplain #refresh refreshed}.
   * @param beanFactory the DefaultListableBeanFactory instance to use for this context
   */
  public AnnotationConfigWebApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Delegates given environment to underlying {@link AnnotatedBeanDefinitionReader} and
   * {@link ClassPathBeanDefinitionScanner} members.
   */
  @Override
  public void setEnvironment(@Nonnull ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    this.reader.setEnvironment(environment);
    this.scanner.setEnvironment(environment);
  }

  /**
   * Provide a custom {@link BeanNameGenerator} for use with
   * {@link AnnotatedBeanDefinitionReader} and/or
   * {@link ClassPathBeanDefinitionScanner}, if any.
   * <p>
   * Default is
   * {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
   * <p>
   * Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   * @param beanNameGenerator the bean name generator
   * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
   * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
   */
  public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
    this.reader.setBeanNameGenerator(beanNameGenerator);
    this.scanner.setBeanNameGenerator(beanNameGenerator);
    this.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
        beanNameGenerator);
  }

  /**
   * Set the {@link ScopeMetadataResolver} to use for detected bean classes.
   * <p>
   * The default is an {@link AnnotationScopeMetadataResolver}.
   * <p>
   * Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   * @param scopeMetadataResolver the scope metadata resolver
   */
  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    this.reader.setScopeMetadataResolver(scopeMetadataResolver);
    this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
  }

  /**
   * Register one or more annotated classes to be processed. Note that
   * {@link #refresh()} must be called in order for the context to fully process the new
   * class.
   * <p>
   * Calls to {@code #register} are idempotent; adding the same annotated class more
   * than once has no additional effect.
   * @param annotatedClasses one or more annotated classes, e.g. {@code @Configuration}
   * classes
   * @see #scan(String...)
   * @see #refresh()
   */
  @Override
  public final void register(Class<?>... annotatedClasses) {
    Assert.notEmpty(annotatedClasses, "At least one annotated class must be specified");
    this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
  }

  /**
   * Perform a scan within the specified base packages. Note that {@link #refresh()}
   * must be called in order for the context to fully process the new class.
   * @param basePackages the packages to check for annotated classes
   * @see #register(Class...)
   * @see #refresh()
   */
  @Override
  public final void scan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    this.basePackages = basePackages;
  }

  @Override
  protected void prepareRefresh() {
    this.scanner.clearCache();
    super.prepareRefresh();
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    if (this.basePackages != null && this.basePackages.length > 0) {
      this.scanner.scan(this.basePackages);
    }
    if (!this.annotatedClasses.isEmpty()) {
      this.reader.register(ClassUtils.toClassArray(this.annotatedClasses));
    }
  }
}
