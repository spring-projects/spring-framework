package com.lm.dao;

import org.springframework.stereotype.Repository;
/**
 * @author Daniel
 * @Description
 * @Date 2020/6/14 0:36
 **/

@Repository("dao")
public class UserDao {
	public void pringInfo(){
		System.out.println("User dao");
	}
}