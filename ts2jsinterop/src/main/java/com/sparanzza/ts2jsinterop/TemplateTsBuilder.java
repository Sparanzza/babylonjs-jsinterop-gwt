package com.sparanzza.ts2jsinterop;

import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class TemplateTsBuilder {
	public static final String CLASSTRING = "class";
	public static final String INTERFACESTRING = "interface";
	public static final String IMPLEMENTSSTRING = "implements";
	public static final String EXTENDSTRING = "extend";
	public static final String EXPORTSTRING = "export";
	public static final String ABSTRACTSTRING = "abstract";
	public static final String MODULESTRING = "module";
	public static final String DECLARESTRING = "declare";
	public static STATE state;
	private final String pathBuild = "../core/src/main/java/";
	private final String groupId = "com.sparanzza.";
	public String moduleTitle = "";
	public List<TemplateClass> innerClasses;
	public TemplateClass template;
	
	// Filters
	Predicate<String> filterModule = c -> {
		return !(c.contains(DECLARESTRING) || c.contains(MODULESTRING) || c.contains("{"));
	};
	Predicate<String> filterClass = c -> {
		return !(c.contains(EXPORTSTRING) || c.contains(CLASSTRING) || c.contains("{"));
	};
	Predicate<String> filterInterface = c -> {
		return !(c.contains(EXPORTSTRING) || c.contains(INTERFACESTRING) || c.contains("{"));
	};
	private Builder builder;
	
	public TemplateTsBuilder() { state = STATE.INIT;}
	
	public static TypeName getClazz(String clazz) {
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
	
	public boolean setModule(String t) {
		System.out.println("setModule " + t);
		state = STATE.DECLARE_MODULE;
		Long result = Arrays.stream(t.trim().split(" ")).filter(filterModule).map(c -> {
			moduleTitle = c.replace("\"", "");
			return moduleTitle;
		}).count();
		
		return result == 1;
	}
	
	public boolean setClass(String line) {
		template = new TemplateClass();
		state = STATE.CLASS;
		Arrays.stream(line.trim().split(" ")).filter(filterClass).peek(x -> System.out.println("word is " + x + " state is " + state)).forEach(c -> {
			switch (c.trim()) {
				case ABSTRACTSTRING:
					template.isAbstract = true;
					break;
				case EXTENDSTRING:
					state = STATE.EXTEND_CLASS;
					break;
				case IMPLEMENTSSTRING:
					template.isInterface = true;
					state = STATE.IMPLEMENTS;
					break;
				default:
					if (state == STATE.CLASS) {template.clazzName = c;}
					if (state == STATE.IMPLEMENTS) {template.interfaceTitles.add(c);}
					if (state == STATE.EXTEND_CLASS) {template.classExtend = TypeVariableName.get(c);}
			}
		});
		state = STATE.CLASS;
		return true;
	}
	
	public void setInterface(String line) {
		template = new TemplateClass();
		state = STATE.INTERFACE;
		Arrays.stream(line.trim().split(" ")).filter(filterInterface).peek(x -> System.out.println("word is " + x + " state is " + state)).forEach(c -> {
			switch (c.trim()) {
				case ABSTRACTSTRING:
					template.isAbstract = true;
					break;
				case EXTENDSTRING:
					state = STATE.EXTEND_CLASS;
					break;
				default:
					if (state == STATE.INTERFACE) {template.clazzName = c;}
					if (state == STATE.EXTEND_CLASS) {template.classExtend = TypeVariableName.get(c);}
			}
		});
		state = STATE.INTERFACE;
	}
	
	public void setConstructor(List<String> params) {
		MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
		if (params != null) params.stream().map(e -> e.split(":")).forEach(param -> {
			System.out.println("param " + param[1] + " - " + param[0]);
			constructorBuilder.addParameter(getClazz(param[1]), param[0]);
		}); template.constructor = constructorBuilder.build();
	}
	
	public void buildClass() {
		state = STATE.CLASS;
		builder = TypeSpec.classBuilder(template.clazzName).addModifiers(Modifier.PUBLIC);
		if (template.isAbstract) {
			builder.addModifiers(Modifier.ABSTRACT);
		}
		if (template.isInterface) {
			System.out.println("is interface --------- " + template.isInterface + " " + template.interfaceTitles.toString());
			for (String i : template.interfaceTitles) {
				builder.addSuperinterface(ClassName.get("android.graphics.drawable", i));
			}
		}
		if (template.classExtend != null) {
			System.out.println("super class ... " + template.classExtend.name);
			builder.superclass(template.classExtend);
		}
		if (template.constructor != null) builder.addMethod(template.constructor);
		
	}
	
	public void buildInterface() {
		state = STATE.INTERFACE;
		builder = TypeSpec.interfaceBuilder(template.clazzName).addModifiers(Modifier.PUBLIC);
		if (template.classExtend != null) {
			System.out.println("super class ... " + template.classExtend.name);
			builder.superclass(template.classExtend);
		}
	}
	
	public void writeJavaFile(String _path) throws IOException {
		JavaFile javaFile = JavaFile.builder(cleanPackage(moduleTitle), builder.build()).build();
		Path path = Paths.get(_path);
		javaFile.writeTo(path);
	}
	
	private String cleanPackage(String moduleTitle) {
		int iLastSlash = moduleTitle.lastIndexOf("/");
		return groupId + moduleTitle.substring(0, iLastSlash).replace("/", ".");
	}
	
	public boolean endStatement() {
		if ((state == STATE.CLASS || state == STATE.INTERFACE) && template != null) {
			try {
				System.out.println("Building " + state + " ... " + template.clazzName);
				if (state == STATE.INTERFACE) {
					state = STATE.END_INTERFACE;
					buildInterface();
				}
				if (state == STATE.CLASS) {
					state = STATE.END_CLASS;
					buildClass();
				}
				System.out.println("writing file ... " + moduleTitle);
				writeJavaFile(pathBuild);
				template = null;
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		if (state == STATE.END_CLASS || state == state.END_INTERFACE || state == state.END_COMMENT) {
			state = STATE.END_MODULE;
		}
		
		return true;
	}
	
	public STATE getState() {
		return state;
	}
	
	public static enum STATE {
		INIT, COMMENT, END_COMMENT, DECLARE_MODULE, MODULE_INDEX, END_INTERFACE, END_MODULE, CLASS, EXTEND_CLASS, END_CLASS, IMPLEMENTS, INTERFACE, CONSTRUCTOR, INNERCLASS, METHOD, PARAM
	}
	
	class TemplateClass {
		public boolean isAbstract;
		public boolean isInterface;
		public String clazzName;
		public List<String> interfaceTitles = new ArrayList<>();
		public TypeVariableName classExtend = null;
		public MethodSpec constructor = null;
	}
	
}
