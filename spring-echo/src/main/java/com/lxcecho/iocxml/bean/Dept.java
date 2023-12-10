package com.lxcecho.iocxml.bean;

import java.util.List;

/**
 * 部门类
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class Dept {

	/**
	 * 一个部门有很多员工
	 */
	private List<Emp> empList;

    private String dname;

    public String getDname() {
        return dname;
    }

    public void setDname(String dname) {
        this.dname = dname;
    }

    public List<Emp> getEmpList() {
        return empList;
    }

    public void setEmpList(List<Emp> empList) {
        this.empList = empList;
    }

    public void info() {
        System.out.println("部门名称："+dname);
        for (Emp emp:empList) {
            System.out.println(emp.getEname());
        }
    }
}
