package com.sparanzza.ts2jsinterop;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;


public class TemplateBuilder {
	public boolean isAbstract;
	public ClazzBuilder clazz;
	public boolean isInterface;
	public List<ClazzBuilder> interfaces;
	public List<MethodBuilder> constructors;
	public List<MethodBuilder> methods;
	public Map<String, ParamBuilder> params;
	
	public TemplateBuilder() {
		isAbstract = false;
		isInterface = false;
		clazz = new ClazzBuilder();
	}
	
	public TemplateBuilder(String nameClazz) {
		isAbstract = false;
		isInterface = false;
		clazz = new ClazzBuilder(nameClazz);
	}
	
	public static TypeName getClazzType(String clazz) {
		switch (clazz) {
			case "number":
				return ClassName.FLOAT;
			case "string":
				return ClassName.get(String.class);
			case "boolean":
				return ClassName.BOOLEAN;
			case "Object":
				return ClassName.OBJECT;
			case "any":
				return ClassName.OBJECT;
			case "undefined":
				return null;
			case "null":
				return null;
			default:
				return ClassName.get("", clazz);
		}
	}
	
	enum ModifierTypes {PUBLIC, PROTECTED, PRIVATE, DEFAULT;}
	
	class ClazzBuilder {
		public ModifierTypes modifier;
		public ParamBuilder pClazz;
		public ParamBuilder pExtend;
		public List<ParamBuilder> implementInterface;
		
		public ClazzBuilder() {}
		
		public ClazzBuilder(String nameClazz) {
			modifier = ModifierTypes.DEFAULT;
			pClazz = new ParamBuilder(nameClazz);
			pExtend = new ParamBuilder();
		}
		
		public ClazzBuilder addImplementInterface(String name) {
			implementInterface.add(new ParamBuilder(name));
			return this;
		}
	}
	
	class ParamBuilder {
		public String name;
		public String genType;
		public String genTypeExtend;
		public String genTypeImplements;
		//TODO define Function to generate lambda
		
		public ParamBuilder() {
			name = genType = genTypeExtend = genTypeImplements = "";
		}
		
		public ParamBuilder(String n) {
			name = n;
			genType = genTypeExtend = genTypeImplements = "";
		}
	}
	
	class MethodBuilder {
		public ModifierTypes modifier;
		public String name;
		public ParamBuilder returnType;
		public Map<String, ParamBuilder> paramsMethodList;
	}
}


