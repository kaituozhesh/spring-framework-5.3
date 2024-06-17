package org.springframework.testing.seltEditor;

import java.beans.PropertyEditorSupport;

/**
 * @Description:
 * @ClassName: AddressPropertyEditor
 * @Auther: lin
 * @Date: 2024/6/13 20:07
 * @Version: 1.0
 */
public class AddressPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] s = text.split("_");
		Address address = new Address();
		address.setProvince(s[0]);
		address.setCity(s[1]);
		address.setTown(s[2]);
		this.setValue(address);
	}
}
