package com.debug.spring;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:
 */
//@Component
public class User
{
    //@Value(value = "github")
    private String name;
   // @Value(value = "male")
    private String gender;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }
}
