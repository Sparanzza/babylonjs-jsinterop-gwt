package com.sparanzza.ts2jsinterop;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TemplateBuilder {
	public boolean isAbstract;
	public ClazzBuilder clazz;
	public boolean isInterface;
	public List<ClazzBuilder> interfaces;
	public List<MethodBuilder> methods;
	public Map<String, ParamBuilder> params;
	private List<MethodBuilder> constructors;
	
	public TemplateBuilder() {
		isAbstract = isInterface = false;
		clazz = new ClazzBuilder();
		constructors = new ArrayList<>();
	}
	
	public TemplateBuilder(String nameClazz) {
		isAbstract = isInterface = false;
		clazz = new ClazzBuilder(nameClazz);
		constructors = new ArrayList<>();
	}
	
	
	public String toString() {
		return this.clazz.toString();
	}
	
	public TemplateBuilder addConstructor(Map<String, TypeName> params) {
		this.constructors.add(new MethodBuilder("", params, null));
		return this;
	}
	
	public List<MethodSpec> buildConstructorList() {
		return getConstructors().stream().map(MethodBuilder::buildConstructor).collect(Collectors.toList());
	}
	
	public TypeSpec enumBuild(String name, List<String> enumerates) {
		TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name).addModifiers(Modifier.PUBLIC);
		enumerates.stream().forEach(enumBuilder::addEnumConstant);
		return enumBuilder.build();
	}
	
	public List<MethodBuilder> getConstructors() { return this.constructors;}
	
	enum ModifierTypes {PUBLIC, PROTECTED, PRIVATE, DEFAULT;}
	
	class ClazzBuilder {
		public ModifierTypes modifier;
		public ParamBuilder pClazz;
		public ParamBuilder pExtend;
		public List<ParamBuilder> implementInterface;
		
		public ClazzBuilder() {
			modifier = ModifierTypes.PUBLIC;
			pClazz = new ParamBuilder();
			implementInterface = new ArrayList<>();
		}
		
		public ClazzBuilder(String nameClazz) {
			modifier = ModifierTypes.PUBLIC;
			pClazz = new ParamBuilder(nameClazz);
			implementInterface = new ArrayList<>();
		}
		
		public ClazzBuilder addImplementInterface(String name) {
			implementInterface.add(new ParamBuilder(name));
			return this;
		}
		
		public String toString() {
			return "[CLASS] pClass: { name: " + this.pClazz.name + ", type: " + this.pClazz.type + ", genType: " + this.pClazz.genType + ", " + "genTypeExtend: " + this.pClazz.genTypeExtend + "}";
		}
	}
	
	class ParamBuilder {
		public String name;
		public String genType;
		public String genTypeExtend;
		public String genTypeImplements;
		private TypeName type;
		private boolean optional;
		
		//TODO define Function to generate lambda
		
		public ParamBuilder() {
			name = genType = genTypeExtend = genTypeImplements = "";
			type = ClassName.OBJECT;
		}
		
		public ParamBuilder(String n) {
			name = n;
			genType = genTypeExtend = genTypeImplements = "";
		}
		
		public ParamBuilder(String name, TypeName type) {
			this.name = name;
			this.type = type;
		}
		
		public TypeName getType() {return this.type;}
		
		public ParamBuilder setType(TypeName clazz) {
			this.type = clazz;
			return this;
		}
		
		public boolean getOptional() {
			return this.optional;
		}
		
		public ParamBuilder setOptional(boolean opt) {
			this.optional = opt;
			return this;
		}
	}
	
	class MethodBuilder {
		public ModifierTypes modifier;
		
		//is empty -> Constructor
		public String name;
		public TypeName returnType;
		public List<ParamBuilder> paramsList;
		
		public MethodBuilder(String name, Map<String, TypeName> params, TypeName returnType) {
			this.name = name;
			paramsList = new ArrayList<>();
			params.forEach(this::addParam);
			this.returnType = returnType;
			
		}
		
		public MethodBuilder addParam(String name, TypeName type) {
			paramsList.add(new ParamBuilder(name, type));
			return this;
		}
		
		public MethodSpec buildConstructor() {
			MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
			paramsList.stream().forEach(p -> constructorBuilder.addParameter(p.type, p.name));
			return constructorBuilder.build();
		}
		
		// TODO buildMethod()
		
	}
}


