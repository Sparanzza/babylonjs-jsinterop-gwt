package com.sparanzza.ts2jsinterop;

import com.google.common.base.CharMatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.sparanzza.ts2jsinterop.Constants.*;
import static com.sparanzza.ts2jsinterop.SetTemplateData.STATE;
import static com.sparanzza.ts2jsinterop.SetTemplateData.state;

public class Ts2jsinterop {
	
	private static ExportLog elog;
	private static SetTemplateData td;
	private static BufferedReader br;
	
	public static void main(String[] args) {
		
		elog = new ExportLog("log.txt");
		
		File fileModuleTs = new File(Ts2jsinterop.class.getClassLoader().getResource("babylon.module.d.ts").getFile());
		
		if (fileModuleTs.exists()) {
			
			td = new SetTemplateData();
			
			System.out.println("### Init fileModuleTs module.ts ###");
			System.out.println("Absolute Path: " + fileModuleTs.getAbsolutePath());
			System.out.println("Is Directory: " + fileModuleTs.isDirectory());
			System.out.println("Parent Path: " + fileModuleTs.getParent());
			
			if (fileModuleTs.isFile()) {
				System.out.println("File size: " + fileModuleTs.length());
				System.out.println("File last modified " + fileModuleTs.lastModified());
			}
			
			try {
				String line;
				br = new BufferedReader(new FileReader(fileModuleTs));
				while ((line = br.readLine()) != null && line.length() != 0) {
					line = scapeComments(line);
					if (isDeclareIndex(line)) continue;
					if (isModule(line)) continue;
					if (isImports(line)) continue;
					if (isClass(line)) continue;
					if (isEnum(line)) continue;
					if (isInterface(line)) continue;
					if (isConstructor(line)) continue;
					// if (isMethod(line, br)) continue;
					if (isType(line)) continue;
					// isParam(line);
					
					endStatement(line);
					System.out.println("STATE: " + td.getState() + " line " + line);
				}
				
				elog.closeLog();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean isType(String line) throws IOException {
		if (line.contains(TYPESTRING)) {
			if (!line.contains(";")) return true; // one line
			state = STATE.TYPE;
			if (!line.contains(CLOSE_KEY)) {
				do {
					line = scapeComments(line);
				} while ((line = br.readLine()) != null && !line.contains(CLOSE_KEY));
			} else {}
			state = STATE.END_TYPE;
			return true;
		}
		return false;
	}
	
	private static String scapeComments(String line) throws IOException {
		do {
			// @formatter:off
			if (line.contains("/**") && line.contains("*/")) { state = STATE.END_COMMENT; continue; }
			if (line.contains("/**")) { state = STATE.COMMENT; continue; }
			if (line.contains("*/")) { state = STATE.END_COMMENT; }
			if (state != STATE.END_COMMENT && state != STATE.COMMENT) break;
			// @formatter:on
		} while ((line = br.readLine()) != null && state != STATE.END_COMMENT && state == STATE.COMMENT);
		return line;
	}
	
	private static boolean isEnum(String line) throws IOException {
		if (line.contains(ENUMSTRING) && line.contains(OPEN_KEY)) {
			elog.writeLogLine("# ENUM - " + line);
			Optional<String> name = Arrays.stream(line.trim().split(" ")).filter(s -> !s.contains(EXPORTSTRING) && !s.contains(ENUMSTRING) && !s.contains(OPEN_KEY)).findFirst();
			List<String> enumerates = new ArrayList<>();
			while ((line = br.readLine()) != null && !line.trim().equals(CLOSE_KEY)) {
				scapeComments(line);
				enumerates.add(Arrays.asList(line.split("=")).get(0).replaceAll(";", "").trim());
			}
			return td.setEnum(name, enumerates);
		}
		return false;
	}
	
	private static boolean isModule(String line) {
		if (line.contains("declare module")) {
			elog.writeLogLine("# MODULE - " + line);
			return td.setModule(line);
		}
		return false;
	}
	
	private static boolean isClass(String line) {
		if (line.contains(CLASSTRING) && line.contains(OPEN_KEY)) {
			state = STATE.CLASS;
			elog.writeLogLine("# CLASS - " + line);
			return td.setClass(line);
		}
		return false;
	}
	
	private static boolean isConstructor(String line) throws Exception {
		if (line.contains(CONSTRUCTORSTRING)) {
			state = STATE.CONSTRUCTOR;
			String paramsStr = "";
			if (line.contains("constructor();")) { // empty constructor
				paramsStr = "";
			} else if (line.contains("constructor(") && line.contains(");")) { // one line
				paramsStr = line.trim();
			} else if (line.contains("constructor(") && !line.contains(");")) { // multi line
				do {
					line = scapeComments(line);
					paramsStr = paramsStr + line.trim();
				} while ((line = br.readLine()) != null && !paramsStr.contains(");"));
			}
			// delete constructor word
			if (paramsStr != "") {
				paramsStr = CharMatcher.whitespace().removeFrom(paramsStr);
				int s = paramsStr.indexOf("constructor(");
				int e = paramsStr.indexOf(");");
				//
				// System.out.println("start " + s + " end " + e);
				paramsStr = paramsStr.substring(s + 12, paramsStr.length() - 2);
				paramsStr = paramsStr.replaceAll("\\?", "");
				paramsStr = paramsStr.replaceAll("\\*/", ""); // some line mix param with end comments
				// Split params by , character
				td.setConstructor(Arrays.asList(paramsStr.split(",")));
			} else {
				td.setConstructor(Collections.EMPTY_LIST); // empty constructor
			}
			// For now replace optional parameters in constructor
			elog.writeLogLine("# CONSTRUCTOR PARAMS OTHER LINE - " + paramsStr);
			state = STATE.END_CONSTRUCTOR;
			return true;
			
		} else {
			return false;
		}
	}
	
	private static boolean isDeclareIndex(String line) throws IOException {
		if (line.contains("declare") && line.contains("/index")) {
			elog.writeLogLine("# OMMITED DECLARE INDEX - " + line + "\n");
			
			while ((line = br.readLine()) != null && line.length() != 0) {
				elog.writeLogLine("# OMMITED - " + line);
				if (line.contains("}")) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static void endStatement(String line) {
		if (line.trim().equals("}")) td.endStatement();
		elog.writeLogLine("End Statement " + td.getState());
	}
	
	private static boolean isImports(String line) {
		if (line.contains("import")) {
			elog.writeLogLine("# IMPORT - " + line);
			return true;
		}
		return false;
	}
	
	private static boolean isInterface(String line) {
		if (line.contains("interface")) {
			elog.writeLogLine("# INTERFACE - " + line);
			td.setInterface(line);
			return true;
		}
		return false;
	}
	
	private static void isParam(String line) {
		elog.writeLogLine("# PARAM - " + line);
		td.setParam(line);
	}
	
	private static boolean isMethod(String line, BufferedReader br) {
		if (line.contains("<")) return false;
		if (line.contains("():")) { // method without function
			List<String> arr = Arrays.stream(line.replaceAll("():", " ").split(" ")).collect(Collectors.toList());// 0: method return 1: name 2: static /
			// private ...
			td.setMethod(arr, null); // no params
		} else {
			if (line.contains(OPEN_PARENTHESIS) && line.contains("):") && line.contains(";")) { //  method one line
				// line
				elog.writeLogLine("# METHOD - " + line);
				// tb.setMethod(line, null);
				
			} else if (line.contains(OPEN_PARENTHESIS)) { // multiline
				// need get all params
			}
		}
		return false;
	}
	
}


