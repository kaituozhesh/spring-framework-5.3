package org.springframework.testing.seltEditor;

/**
 * @Description:
 * @ClassName: Customer
 * @Auther: lin
 * @Date: 2024/6/13 20:06
 * @Version: 1.0
 */
public class Customer {
	private String name;
	private Address address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Customer{" +
				"name='" + name + '\'' +
				", address=" + address +
				'}';
	}
}
