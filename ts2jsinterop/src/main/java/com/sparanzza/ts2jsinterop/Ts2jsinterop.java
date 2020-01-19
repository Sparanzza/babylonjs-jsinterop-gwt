package com.sparanzza.ts2jsinterop;

import com.sparanzza.ts2jsinterop.TemplateTsBuilder.STATE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
					if (line.contains("/**")) { tb.state = STATE.COMMENT; continue; }
					if (line.contains("*/")) { tb.state = STATE.END_COMMENT; continue; }
					if (tb.state == STATE.COMMENT) continue;
					// @formatter:on
					
					if (isModule(line)) continue;
					if (isImports(line)) continue;
					if (isClass(line)) continue;
					if (isInterface(line)) continue;
					if (isConstructor(line)) continue;
					if (isMethod(line)) continue;
					if (avoidCases(line)) continue;
					isParam(line);
					
				}
				
				elog.closeLog();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean isModule(String line) {
		if (line.contains("declare module")) {
			elog.writeLog("# MODULE - " + line + "\n");
			return tb.setModule(line);
		}
		return false;
	}
	
	private static boolean isClass(String line) {
		if (line.contains("class") && line.contains("{") && !line.contains("<")) {
			elog.writeLog("# CLASS - " + line + "\n");
			return tb.setClass(line);
		}
		return false;
	}
	private static boolean isConstructor(String line) {
		if (line.contains("import")) {
			elog.writeLog("# CONSTRUCTOR - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isDeclareIndex(String line, BufferedReader br) throws IOException {
		if (line.contains("declare") && line.contains("/index")) {
			elog.writeLog("# OMMITED DECLARE INDEX - " + line + "\n");
			
			while ((line = br.readLine()) != null && line.length() != 0) {
				elog.writeLog("# OMMITED - " + line + "\n");
				if (line.contains("}")) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static boolean avoidCases(String line) {
		if (line.trim().equals("}")) tb.changeState();
		return false;
	}
	
	private static boolean isImports(String line) {
		if (line.contains("import")) {
			elog.writeLog("# IMPORT - " + line + "\n");
			return true;
		}
		return false;
	}
	
	
	private static boolean isInterface(String line) {
		if (line.contains("interface")) {
			elog.writeLog("# INTERFACE - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static void isParam(String line) {
		elog.writeLog("# PARAM - " + line + "\n");
	}
	
	private static boolean isMethod(String line) {
		
		if (line.contains(OPEN_PARENTHESIS) && line.contains(CLOSE_PARENTHESIS)) {
			elog.writeLog("# METHOD - " + line + "\n");
			return true;
		}
		return false;
	}
}


