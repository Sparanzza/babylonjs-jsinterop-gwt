package com.sparanzza.ts2jsinterop;

import com.google.common.base.CharMatcher;
import com.sparanzza.ts2jsinterop.TemplateTsBuilder.STATE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static com.sparanzza.ts2jsinterop.Constants.CLOSE_PARENTHESIS;
import static com.sparanzza.ts2jsinterop.Constants.OPEN_PARENTHESIS;

public class Ts2jsinterop {
	
	private static ExportLog elog;
	private static TemplateTsBuilder tb;
	
	public static void main(String[] args) {
		
		elog = new ExportLog("log.txt");
		
		File fileModuleTs = new File(Ts2jsinterop.class.getClassLoader().getResource("babylon.module.d.ts").getFile());
		
		if (fileModuleTs.exists()) {
			
			tb = new TemplateTsBuilder();
			
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
				BufferedReader br = new BufferedReader(new FileReader(fileModuleTs));
				while ((line = br.readLine()) != null && line.length() != 0) {
					
					// @formatter:off
					if (isDeclareIndex(line, br)) continue;
					if (line.contains("/**") && line.contains("*/")) { tb.state = STATE.END_COMMENT; continue; }
					if (line.contains("/**")) { tb.state = STATE.COMMENT; continue; }
					if (line.contains("*/")) { tb.state = STATE.END_COMMENT; continue; }
					if (tb.state == STATE.COMMENT) continue;
					// @formatter:on
					
					if (isModule(line)) continue;
					if (isImports(line)) continue;
					if (isClass(line)) continue;
					if (isInterface(line)) continue;
					if (isConstructor(line, br)) continue;
					// if (isMethod(line)) continue;
					// isParam(line) continue;
					
					endStatement(line);
					System.out.println("STATE: " + tb.getState() + " line " + line);
				}
				
				elog.closeLog();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean isModule(String line) {
		if (line.contains("declare module")) {
			elog.writeLogLine("# MODULE - " + line);
			return tb.setModule(line);
		}
		return false;
	}
	
	private static boolean isClass(String line) {
		if (line.contains("class") && line.contains("{") && !line.contains("<")) {
			elog.writeLogLine("# CLASS - " + line);
			return tb.setClass(line);
		}
		return false;
	}
	
	private static boolean isConstructor(String line, BufferedReader br) throws Exception {
		
		if (line.contains("constructor")) {
			String paramsStr = "";
			if (line.contains("constructor();")) { // empty constructor
				paramsStr = "";
			} else if (line.contains("constructor(") && line.contains(");")) { // one line
				paramsStr = line.trim();
			} else if (line.contains("constructor(") && !line.contains(");")) { // multi line
				do {
					if (!(line.contains("/**") || line.contains("*") || line.contains("*/"))) paramsStr = paramsStr + line.trim();
				} while ((line = br.readLine()) != null && !line.contains(");"));
				assert line != null;
				paramsStr = paramsStr + line.trim();
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
				tb.setConstructor(Arrays.asList(paramsStr.split(",")));
				
			}else{
				// empty constructor
				tb.setConstructor(null);
			}
			// For now replace optional parameters in constructor
			elog.writeLogLine("# CONSTRUCTOR PARAMS OTHER LINE - " + paramsStr);
			
			
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean isDeclareIndex(String line, BufferedReader br) throws IOException {
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
		if (line.trim().equals("}")) tb.endStatement(); elog.writeLogLine("End Statement " + tb.getState());
	}
	
	private static boolean isImports(String line) {
		if (line.contains("import")) {
			elog.writeLogLine("# IMPORT - " + line);
			return true;
		}
		return false;
	}
	
	
	private static boolean isInterface(String line) {
		if (line.contains("interface") && !line.contains("<")) {
			elog.writeLogLine("# INTERFACE - " + line);
			tb.setInterface(line);
			return true;
		}
		return false;
	}
	
	private static void isParam(String line) {
		elog.writeLogLine("# PARAM - " + line);
	}
	
	private static boolean isMethod(String line) {
		
		if (line.contains(OPEN_PARENTHESIS) && line.contains(CLOSE_PARENTHESIS)) {
			elog.writeLogLine("# METHOD - " + line);
			return true;
		}
		return false;
	}
}


