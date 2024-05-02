# di
在 AbstractBeanFactory 类中走 getBean() 方法可以利用指定名称、类型等信息获取到对应的 bean

然后走 getBean() 方法中的 doGetBean() 方法，如果单例表中有已经实例化的 bean，则直接获取

如果要获取的 bean 还没有在单例表中，则从父容器中去找，如果父容器也没有，则沿着当前容器的继承体系一直向上查找，找到了就直接获取

否则说明 bean 还没有创建，先确保当前 bean 依赖的所有已经被初始化，根据不同的模式（单例/原型/其他）创建一个新的 bean 并返回

实际上是走 AbstractAutowireCapableBeanFactory 类中的 createBean() 方法 -> doCreateBean() 方法

bean 优先使用容器的自动装配方法进行实例化，再选择走（有参/无参构造方法），默认走 JDK 的反射流程，也可以通过配置改用 Cglib 的流程 

bean 实例化后和 di 注入过程中有多处位置调用了调用 PostProcessor 后置处理器，可以做一些增强
 
在 di 注入之前会先缓存单例模式的半初始化 bean 对象，以防循环引用

获取解析 XML 文件中 bean 元素生成的 PropertyValues 元素值：依赖注入开始，首先处理 autowire 自动装配的注入，最后通过 applyPropertyValues() 方法注入最终值

di 注入完成的 bean 会注册记录下来

