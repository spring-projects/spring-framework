## Spring源码分析
### 测试代码
```
public static void main(String[] args) {
    ApplicationContext ac = new ClassPathXmlApplicationContext("spring-context.xml");
    PersonService person = ac.getBean(PersonService.class);
    person.print();
}
```
###执行过程
1. 创建ClassPathXmlApplicationContext对象
    - super(parent);
        - this.resourcePatternResolver = getResourcePatternResolver();
            > 返回 ResourcePatternResolver 用于通过文件路径初始化Resource实例
        - setParent(parent);
            > 设置父容器，并将父容器的 Environment 合并到当前容器
    - setConfigLocations(configLocations);
        > 将输入的文件路径替换掉环境变量，并赋值给this.configLocations
    - refresh();
        > 加载或刷新配置中的持久化描述，可能是XML文件，配置文件或关系数据库schema
        - prepareRefresh();
            > 刷新前的准备
            - this.startupDate = System.currentTimeMillis();
            - this.closed.set(false);
            - this.active.set(true);
            - initPropertySources();
        - ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
        - prepareBeanFactory(beanFactory);
        - postProcessBeanFactory(beanFactory);
            > 后置处理器1
                                                   >
                                                   >
        - 后置处理器2: invokeBeanFactoryPostProcessors(beanFactory);
        - 后置处理器3: registerBeanPostProcessors(beanFactory);
        - 后置处理器4: initMessageSource();
        - 后置处理器5: initApplicationEventMulticaster();
        - 后置处理器6: onRefresh();
        - 后置处理器7: registerListeners();
        - 后置处理器8: finishBeanFactoryInitialization(beanFactory);
        - 后置处理器9: finishRefresh();
2. 获取Bean
3. 调用Bean方法