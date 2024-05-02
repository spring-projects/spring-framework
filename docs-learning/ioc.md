# ioc
SimpleBeanDefinitionRegistry 类中有：

private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

这是 ioc 容器的实际体现

注册 bean 的核心方法是 DefaultListableBeanFactory 类中的 registerBeanDefinition 方法：

根据 beanName 拿到对应的 beanDefinition -> 看看有没有重名的 beanDefinition 存在：若有重名的且不允许覆盖，则直接抛出异常；若有重名的且允许覆盖，则覆盖旧值 -> 如果有 bean 正在创建，加上 synchronized 让其他线程暂时不能向单例表中添加新的 bean -> 向 beanDefinitionMap 添加对应内容