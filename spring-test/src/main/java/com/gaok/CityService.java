package com.gaok;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : gaokang
 * @date : 2020/5/2
 */
@Component
public class CityService {

	@Autowired
	private CountyService countyService;

	public CityService(){
		System.out.println("init cityService");
	}
}
