package com.sparanzza.ts2jsinterop;

import com.sparanzza.ts2jsinterop.TemplateBuilder.ParamBuilder;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.sparanzza.ts2jsinterop.Constants.*;

public class BuilderProcessor {
	public static STATE state;
	public String moduleScope = "";
	public TemplateBuilder t;
	public boolean insideClassOrInterface = false;
	public boolean buildClass = false;
	public boolean buildInterface = false;
	// @formatter:off
	// Filters
	Predicate<String> filterModule = c -> { return !(c.contains(DECLARESTRING) || c.contains(MODULESTRING) || c.contains("{")); };
	Predicate<String> filterClass = c -> { return !(c.contains(EXPORTSTRING) || c.contains(CLASSTRING) || c.contains("{")); };
	Predicate<String> filterInterface = c -> { return !(c.contains(EXPORTSTRING) || c.contains(INTERFACESTRING) || c.contains("{")); };
	private Builder builder;
	
	// @formatter:on
	
	public BuilderProcessor() { state = STATE.INIT;}
	
	public boolean setModule(String t) {
		System.out.println("setModule " + t);
		state = STATE.DECLARE_MODULE;
		Long result = Arrays.stream(t.trim().split(" ")).filter(filterModule).map(c -> {
			moduleScope = c.replace("\"", "");
			return moduleScope;
		}).count();
		
		return result == 1;
	}
	
	public boolean setClass(String line) {
		t = new TemplateBuilder();
		state = STATE.CLASS;
		insideClassOrInterface = true;
		buildClass = true;
		Arrays.stream(line.trim().split(" ")).filter(filterClass).forEach(c -> {
			switch (c.trim()) {
				case ABSTRACTSTRING:
					t.isAbstract = true;
					state = STATE.ABSTRACT_CLASS;
					break;
				case EXTENDSTRING:
					state = STATE.EXTEND_CLASS;
					break;
				case IMPLEMENTSSTRING:
					t.isInterface = true;
					state = STATE.IMPLEMENTS;
					break;
				default:
					if (state == STATE.CLASS) {
						System.out.println("CLASS " + c);
						c = extractGeneric(c);
						t.clazz.pClazz.name = c;
					}
					if (state == STATE.IMPLEMENTS) {
						System.out.println("IMPLEMENTS  " + c);
						t.clazz.addImplementInterface(c);
					}
					if (state == STATE.EXTEND_CLASS) {
						System.out.println("EXTEND_CLASS  " + c);
						t.clazz.pExtend.name = c;
					}
			}
		});
		state = STATE.CLASS;
		return true;
	}
	
	public String extractGeneric(String line) {
		// https://square.github.io/javapoet/1.x/javapoet/com/squareup/javapoet/TypeVariableName.html#get-java.lang.String-com.squareup.javapoet.TypeName...-
		if (line.contains("<") && line.contains(">")) {
			if (line.contains("<T>")) {
				t.typeGenericsParam = new String[]{"T"};
				return line.replaceAll("<T>", "");
			} else {
				String genericPArameters = line.substring(line.indexOf("<"), line.lastIndexOf(">"));
				t.typeGenericsParam = genericPArameters.split(" ");
				// get clean class withouth generic parameters
				return line.substring(0, line.indexOf("<"));
			}
		} else {
			return line;
		}
	}
	
	public void buildClass() {
		state = STATE.CLASS;
		builder = TypeSpec.classBuilder(t.clazz.pClazz.name).addModifiers(Modifier.PUBLIC);
		if (t.isAbstract) {
			builder.addModifiers(Modifier.ABSTRACT);
		}
		if (t.isInterface) {
			for (ParamBuilder i : t.clazz.implementInterface) {
				builder.addSuperinterface(ClassName.get("android.graphics.drawable", i.name));
			}
		}
		if (t.classExtend != null) {
			builder.superclass(TypeVariableName.get(c));
		}
		
		// https://stackoverflow.com/questions/29117410/how-to-add-the-any-type-questionmark-in-javapoet
		if (t.typeGenericsParam != null) {
			if (t.typeGenericsParam[0].equals("T")) {
				builder.addTypeVariable(TypeVariableName.get(t.typeGenericsParam[0]));
			} else {
				builder.addTypeVariable(TypeVariableName.get(t.typeGenericsParam[0]));
			}
		}
		if (t.constructor != null) builder.addMethod(t.constructor);
	}
	
	public void setInterface(String line) {
		if (line.contains("<")) return;
		insideClassOrInterface = true;
		buildInterface = true;
		state = STATE.INTERFACE;
		
		t = new TemplateBuilder();
		Arrays.stream(line.trim().split(" ")).filter(filterInterface).peek(x -> System.out.println("word is " + x + " state is " + state)).forEach(c -> {
			switch (c.trim()) {
				case ABSTRACTSTRING:
					t.isAbstract = true;
					break;
				case EXTENDSTRING:
					state = STATE.EXTEND_CLASS;
					break;
				default:
					if (state == STATE.INTERFACE) {t.clazzName = c;}
					if (state == STATE.EXTEND_CLASS) {t.classExtend = TypeVariableName.get(c);}
			}
		});
		state = STATE.INTERFACE;
	}
	
	public void setConstructor(List<String> params) {
		MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
		if (params != null) params.stream().map(e -> e.split(":")).forEach(param -> {
			System.out.println("param " + param[1] + " - " + param[0]);
			constructorBuilder.addParameter(getClazz(param[1]), param[0]);
		});
		t.constructor = constructorBuilder.build();
	}
	
	public void buildInterface() {
		state = STATE.INTERFACE;
		builder = TypeSpec.interfaceBuilder(t.clazzName).addModifiers(Modifier.PUBLIC);
		if (t.classExtend != null) {
			System.out.println("super class ... " + t.classExtend.name);
			builder.superclass(t.classExtend);
		}
	}
	
	public void writeJavaFile(String _path) throws IOException {
		JavaFile javaFile = JavaFile.builder(cleanPackage(moduleScope), builder.build()).build();
		Path path = Paths.get(_path);
		javaFile.writeTo(path);
	}
	
	private String cleanPackage(String moduleTitle) {
		int iLastSlash = moduleTitle.lastIndexOf("/");
		return GROUPID + moduleTitle.substring(0, iLastSlash).replace("/", ".");
	}
	
	public boolean endStatement() {
		if (insideClassOrInterface && t != null) {
			try {
				System.out.println("Building " + state + " ... " + t.clazz.pClazz.name);
				if (buildInterface) {
					state = STATE.END_INTERFACE;
					buildInterface = false;
					buildInterface();
					
				} else if (buildClass) {
					state = STATE.END_CLASS;
					buildClass = false;
					buildClass();
				}
				System.out.println("writing file ... " + moduleScope);
				writeJavaFile(PATH_BUILD);
				insideClassOrInterface = false;
				t = null;
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
	
	public void setMethod(List<String> line, String[] params) {
		state = STATE.METHOD;
	}
	
	public void setParam(String line) {
		state = STATE.PARAM;
	}
	
	// @formatter:off
	public static enum STATE {
		INIT,
		COMMENT,
		END_COMMENT,
		DECLARE_MODULE,
		MODULE_INDEX,
		END_INTERFACE,
		END_MODULE,
		CLASS,
		EXTEND_CLASS,
		ABSTRACT_CLASS,
		END_CLASS,
		IMPLEMENTS,
		INTERFACE,
		CONSTRUCTOR,
		INNERCLASS,
		METHOD,
		PARAM
	}
	// @formatter:on
}
