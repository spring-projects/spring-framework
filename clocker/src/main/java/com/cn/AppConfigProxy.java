package com.cn;

import com.cn.mayf.service.BeanService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author mayf
 * @Date 2021/3/7 16:55
 */
@Configuration
//@ComponentScan("com.cn")
@MapperScan(value = "com.cn.mayf.mapper")
public class AppConfigProxy implements BeanFactoryAware {

//	@Bean(name = "sqlSessionFactory")
//	public SqlSessionFactoryBean sqlSessionFactoryBean(){
//		System.out.println("init sqlSessionFactoryBean()");
//		return new SqlSessionFactoryBean();
//	}

	@Bean
	public ServiceDemo serviceDemo(){
		System.out.println("init serviceDemo()");
		return new ServiceDemo();
	}

	@Bean
	public BeanService beanDemo(){
		serviceDemo();
		return new BeanService();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

	}


//	@Bean("mapperScannerConfigurer")
//	public MapperScannerConfigurer mapperScannerConfigurer(){
////		serviceDemo();
//		MapperScannerConfigurer scanner = new MapperScannerConfigurer();
//		scanner.setBasePackage("com.cn.mayf.mapper");
//		return scanner;
//	}
}
