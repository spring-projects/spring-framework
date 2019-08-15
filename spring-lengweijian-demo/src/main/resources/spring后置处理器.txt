spring后置处理器
一、InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()

    InstantiationAwareBeanPostProcessor接口继承BeanPostProcessor接口，它内部扩展了三个方法。
    它的作用主要在于目标对象实例化过程中需要处理的事情，包括实例化对象的前后过程以及实例的属性设置，

    postProcessBeforeInstantiation

    postProcessAfterInstantiation  boolean  判断要不要填充

    postProcessPropertyValues

    应用场景：
__________________________________________________________________________________
二、SmartInstantiationAwareBeanPostProcessor.determineCandidateConstructors()  -----> 推断当前要使用那个构造方法

    predictBeanType

    determineCandidateConstructors

    getEarlyBeanReference
__________________________________________________________________________________
三、MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition  ----缓存注解信息
__________________________________________________________________________________
四、SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference()  -------> 得到一个体现暴露的对象
__________________________________________________________________________________
五、InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()  ------> 判断你的bean是否需要完成属性填充
__________________________________________________________________________________
六、InstantiationAwareBeanPostProcessor.postProcessPropertyValues()  --完成自动注入
__________________________________________________________________________________
七、BeanPostProcessor.postProcessBeforeInitialization()  -------> 初始化之前
__________________________________________________________________________________
八、BeanPostProcessor.postProcessAfterInitialization()   ------------- > 初始化过程，bean实例化并赋值后执行
__________________________________________________________________________________
九、bean销毁的一个后置处理器
用得不多。
__________________________________________________________________________________
5个后置处理器被调用9次..