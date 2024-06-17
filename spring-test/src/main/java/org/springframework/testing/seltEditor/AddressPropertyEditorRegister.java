package org.springframework.testing.seltEditor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditor;

/**
 * @Description:
 * @ClassName: AddressPropertyEditorRegister
 * @Auther: lin
 * @Date: 2024/6/13 20:08
 * @Version: 1.0
 */
public class AddressPropertyEditorRegister implements PropertyEditorRegistrar {

	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(Address.class, new AddressPropertyEditor());
	}
}
