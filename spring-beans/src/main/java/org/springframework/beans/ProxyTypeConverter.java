/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

/**
 * Provide Go-style interface and implement independent type conversion strategy, through the Proxy.
 * Using this to enable ProxyTypeConverter.
 *
 * <pre>
 * @Service
 * public class ProxyTypeConvertBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
 *     @Override
 *     public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
 *         beanFactory.setTypeConverter(new ProxyTypeConverter());
 *     }
 * }
 *
 * @Resource
 * @Qualifier("noticeService")
 * private Notable noticeService;
 *
 * NoticeService NOT implements Notable, But also inject success.
 * @Service
 * public class NoticeService {
 *     public String sayNotice() {
 *         return "notice";
 *     }
 * }
 * </pre>
 *
 * @author chenpeng
 * @see BeanFactoryPostProcessor
 * @see BeanWrapperImpl
 * @since 5.3.0
 */
public class ProxyTypeConverter extends TypeConverterSupport {

    public ProxyTypeConverter() {
        this.typeConverterDelegate = new TypeConverterDelegate(this);
        registerDefaultEditors();
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
        try {
            return super.convertIfNecessary(value, requiredType);
        } catch (TypeMismatchException e) {
            // check all methods
            this.checkAllMethods(value, requiredType, e);
            return getWrapperBean(value, requiredType);
        }
    }

    /**
     * get target type of object, using proxy
     *
     * @param value        original bean
     * @param requiredType target type
     * @return proxy bean for target type
     */
    private <T> T getWrapperBean(Object value, Class<T> requiredType) {
        Object o = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{requiredType}, (proxy, method, args) -> {
            Method beanMethod = value.getClass().getMethod(method.getName(), method.getParameterTypes());
            return beanMethod.invoke(value, args);
        });
        return super.convertIfNecessary(o, requiredType);
    }

    /**
     * check all methods for interface and original bean
     *
     * @param value        original bean
     * @param requiredType target type
     */
    private <T> void checkAllMethods(Object value, Class<T> requiredType, TypeMismatchException e) {
        if (!requiredType.isInterface()) {
            throw new ConversionNotSupportedException(value, requiredType, e);
        }

        Method[] methods = requiredType.getMethods();
        for (Method method : methods) {
            try {
                value.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException noSuchMethodException) {
                throw new ConversionNotSupportedException(value, requiredType, noSuchMethodException);
            }
        }
    }

}
