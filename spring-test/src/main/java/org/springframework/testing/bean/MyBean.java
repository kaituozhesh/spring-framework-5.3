package org.springframework.testing.bean;

import java.io.Serializable;

/**
 * @Description:
 * @ClassName: MyBean
 * @Auther: lin
 * @Date: 2024/4/6 09:24
 * @Version: 1.0
 */
public class MyBean implements Serializable {

	private static final long serialVersionUID = -7755037882198487128L;
	private String name;

	private Integer age;

	public MyBean() {
	}

	public MyBean(String name, Integer age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
}
