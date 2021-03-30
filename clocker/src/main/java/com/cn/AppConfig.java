package com.cn;

import com.cn.mayf.beanfactorypostprocessor.MyBeanDefinitionRegistrar;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author mayf
 * @Date 2021/3/7 16:55
 */
@Configuration
@Import(MyBeanDefinitionRegistrar.class)
@ComponentScan("com.cn")
// Exception in thread "main" java.lang.annotation.AnnotationFormatError: Invalid default: public abstract java.lang.Class org.mybatis.spring.annotation.MapperScan.factoryBean()
@MapperScan(value = "com.cn.mayf.mapper")
public class AppConfig {

//	@Bean(name = "sqlSessionFactory")
//	public SqlSessionFactoryBean sqlSessionFactoryBean(){
//		System.out.println("init sqlSessionFactoryBean()");
//		return new SqlSessionFactoryBean();
//	}

//	@Bean
//	public ServiceDemo serviceDemo(){
//		System.out.println("init serviceDemo()");
//		return new ServiceDemo();
//	}
//
//	@Bean
//	public BeanService beanDemo(){
//		serviceDemo();
//		return new BeanService();
//	}

//	@Bean("mapperScannerConfigurer")
//	public MapperScannerConfigurer mapperScannerConfigurer(){
////		serviceDemo();
//		MapperScannerConfigurer scanner = new MapperScannerConfigurer();
//		scanner.setBasePackage("com.cn.mayf.mapper");
//		return scanner;
//	}
}
