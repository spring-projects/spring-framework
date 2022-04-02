
package com.ysj.bean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


@Component
public class FirstBeanPostProcess{
	@Resource
	private FirstBean firstBean;

	@PostConstruct
	private void Esa(){
		System.out.println("ssss");
	}
}

