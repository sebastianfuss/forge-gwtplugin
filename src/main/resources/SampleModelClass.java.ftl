package ${basePackage}.model;

import javax.validation.constraints.NotNull;

public final class ${classPrefix}Model {

	@NotNull
	private String name;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
}