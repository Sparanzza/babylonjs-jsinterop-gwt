package com.sparanzza.ts2jsinterop;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.sun.jdi.InterfaceType;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class TemplateTsBuilder {
	
	public static final String CLASSTRING = "class";
	public static final String INTERFACESTRING = "interface";
	public static final String EXTENDSTRING = "extend";
	public static final String EXPORTSTRING = "export";
	public static final String ABSTRACTSTRING = "abstract";
	
	public String moduleTitle = "";
	public STATE state;
	public List<TemplateClass> innerClasses;
	public TemplateClass tc;
	// Filters
	Predicate<String> filterModule = c -> {
		return !(c.contains("declare") || c.contains("module") || c.contains("{"));
	};
	Predicate<String> filterClass = c -> {
		return !(c.contains("export") || c.contains("class") || c.contains("{"));
	};
	private Builder builder;
	
	public TemplateTsBuilder() { state = STATE.INIT;}
	
	public boolean setModule(String t) {
		state = STATE.DECLARE_MODULE;
		Long result = Arrays.stream(t.trim().split(" ")).filter(filterModule).map(c -> {
			moduleTitle = c.replace("\"", "");
			return moduleTitle;
		}).count();
		return result == 1;
	}
	
	public boolean setClass(String line) {
		tc = new TemplateClass();
		AtomicReference<Integer> nParam = new AtomicReference<>(0);
		Arrays.stream(line.trim().split(" ")).filter(filterClass).forEach(c -> {
			// @formatter:off
					if (nParam.get() == 0 && c.equals("abstract")) { tc.isAbstract = true; return;}
					if (nParam.get() == 0) tc.classTitle = c;
					if (nParam.get() == 1 && c.equals("interface")) tc.isInterface = true;
					if (nParam.get() == 2 ) {
						Class myClass = null;
						try { myClass = Class.forName(c);
						} catch (ClassNotFoundException e) { e.printStackTrace(); }
						if(myClass != null){
							if (tc.isInterface) tc.interfaceTitles.add(myClass);
							else { tc.classExtend = myClass; }
						}
					}
			// @formatter:on
			nParam.set(nParam.get() + 1);
		});
		return true;
	}
	
	public Builder buildClass() {
		
		state = STATE.CLASS;
		builder = TypeSpec.classBuilder(tc.classTitle).addModifiers(Modifier.PUBLIC);
		if (tc.isAbstract) { builder.addModifiers(Modifier.ABSTRACT);}
		if (tc.isInterface) {
			if (tc.interfaceTitles.size() > 0) {
				tc.interfaceTitles.forEach( i -> builder.addSuperinterfaces(i));
			}
		} else {
			builder.superclass(tc.classExtend);
		}
	}
	
	public Builder buildInterface(String name) {
		state = STATE.CLASS;
		builder = TypeSpec.interfaceBuilder(name);
		return builder;
	}
	
	public void writeJavaFile(String _path) throws IOException {
		
		JavaFile javaFile = JavaFile.builder(moduleTitle, builder.build()).build();
		Path path = Paths.get(_path);
		javaFile.writeTo(path);
	}
	
	public void changeState() {
		if (state == STATE.CLASS) {
			state = STATE.END_CLASS;
			try {
				buildClass();
				writeJavaFile("./");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (state == STATE.END_CLASS) {
			state = STATE.END_MODULE;
			return;
		}
	}
	
	public static enum STATE {
		INIT, COMMENT, END_COMMENT, DECLARE_MODULE, MODULE_INDEX, END_MODULE, CLASS, CONSTRUCTOR, END_CLASS, INNERCLASS, METHOD, PARAM
	}
	
	class TemplateClass {
		public boolean isAbstract;
		public boolean isInterface;
		public String classTitle;
		public List<InterfaceType> interfaceTitles;
		public Class classExtend = null;
	}
}
