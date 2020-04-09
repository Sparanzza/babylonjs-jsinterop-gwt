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
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.sparanzza.ts2jsinterop.Constants.*;

public class SetTemplateData {
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
	
	public SetTemplateData() {
		state = STATE.INIT;
		t = new TemplateBuilder();
	}
	
	public static TypeName getClazzType(String clazz) {
		switch (clazz) {
			case "number":
				ClassName.get(Number.class);
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
				t.clazz.pClazz.genType = "T";
				return line.replaceAll("<T>", "");
			} else {
				String genericPArameters = line.substring(line.indexOf("<"), line.lastIndexOf(">"));
				genericPArameters.split(" ");
				// get clean class withouth generic parameters
				return line.substring(0, line.indexOf("<"));
			}
		} else {
			return line;
		}
	}
	
	public void buildClass() {
		builder = TypeSpec.classBuilder(t.clazz.pClazz.name).addModifiers(Modifier.PUBLIC);
		if (t.isAbstract) {
			builder.addModifiers(Modifier.ABSTRACT);
		}
		if (t.isInterface) {
			for (ParamBuilder i : t.clazz.implementInterface) {
				builder.addSuperinterface(ClassName.get("android.graphics.drawable", i.name));
			}
		}
		if (t.clazz.pExtend != null) {
			builder.superclass(TypeVariableName.get(t.clazz.pExtend.name));
		}
		
		// https://stackoverflow.com/questions/29117410/how-to-add-the-any-type-questionmark-in-javapoet
		if (t.clazz.pClazz.genType != "") {
			builder.addTypeVariable(TypeVariableName.get(t.clazz.pClazz.genType));
		}
		
		builder.addMethods(t.buildConstructorList());
		state = STATE.END_CLASS;
	}
	
	public void setInterface(String line) {
		if (line.contains("<")) return;
		insideClassOrInterface = true;
		buildInterface = true;
		state = STATE.INTERFACE;
		
		t = new TemplateBuilder();
		Arrays.stream(line.trim().split(" ")).filter(filterInterface).forEach(c -> {
			switch (c.trim()) {
				case ABSTRACTSTRING:
					t.isAbstract = true;
					break;
				case EXTENDSTRING:
					state = STATE.EXTEND_CLASS;
					break;
				default:
					if (state == STATE.INTERFACE) {t.clazz.pClazz.name += c;}
					if (state == STATE.EXTEND_CLASS) {t.clazz.pExtend.name = c;}
			}
		});
		state = STATE.INTERFACE;
	}
	
	public void setConstructor(List<String> params) {
		Map<String, TypeName> mapParams = params.stream().map(p -> p.split(":")).collect(Collectors.toMap(e -> e[0], e -> getClazzType(e[1])));
		t.addConstructor(mapParams);
	}
	
	public void buildInterface() {
		state = STATE.INTERFACE;
		builder = TypeSpec.interfaceBuilder(t.clazz.pClazz.name).addModifiers(Modifier.PUBLIC);
		if (t.clazz.pExtend != null) {
			System.out.println("super class ... " + t.clazz.pClazz.name);
			builder.superclass(TypeVariableName.get(t.clazz.pClazz.name));
		}
	}
	
	public boolean setEnum(Optional<String> name, List<String> enumerates) {
		state = STATE.ENUM;
		builder.addType(t.enumBuild(name.get(), enumerates));
		return false;
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
		if (state == STATE.END_CLASS) return false;
		System.out.println(t.toString());
		if (insideClassOrInterface && t.clazz != null) {
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
				} else if (state == STATE.ENUM) {
					state = STATE.END_ENUM;
					// TODO IN REFACTOR
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
		ENUM,
		END_ENUM,
		TYPE,
		END_TYPE,
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
		END_CONSTRUCTOR,
		INNERCLASS,
		METHOD,
		PARAM;
	}
	// @formatter:on
}
