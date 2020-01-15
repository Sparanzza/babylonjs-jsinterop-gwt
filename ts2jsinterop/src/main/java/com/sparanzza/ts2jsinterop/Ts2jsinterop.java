package com.sparanzza.ts2jsinterop;

import java.io.*;

import static com.sparanzza.ts2jsinterop.Constants.CLOSE_PARENTHESIS;
import static com.sparanzza.ts2jsinterop.Constants.OPEN_PARENTHESIS;

public class Ts2jsinterop {
	
	static STATE st = STATE.INIT;
	static BufferedWriter bwLog = null;
	
	public static void main(String[] args) {
		File outLog = new File("log.txt");
		
		
		File fileModuleTs = new File(Ts2jsinterop.class.getClassLoader().getResource("babylon.module.d.ts").getFile());
		if (fileModuleTs.exists()) {
			
			System.out.println("### Init fileModuleTs module.ts ###");
			System.out.println("Absolute Path: " + fileModuleTs.getAbsolutePath());
			System.out.println("Is Directory: " + fileModuleTs.isDirectory());
			System.out.println("Parent Path: " + fileModuleTs.getParent());
			
			if (fileModuleTs.isFile()) {
				System.out.println("File size: " + fileModuleTs.length());
				System.out.println("File last modified " + fileModuleTs.lastModified());
			}
			
			try {
				bwLog = new BufferedWriter(new FileWriter(outLog));
				
				BufferedReader br = new BufferedReader(new FileReader(fileModuleTs));
				String line;
				
				while ((line = br.readLine()) != null && line.length() != 0) {
					
					if (isDeclareIndex(line, br)) continue;
					// @formatter:off
					if (line.contains("/**")) { st = STATE.COMMENT; continue; }
					if (line.contains("*/")) { st = STATE.END_COMMENT; continue; }
					// @formatter:on
					if (st == STATE.COMMENT) continue;
					
					if (isModule(line)) continue;
					if (isImports(line)) continue;
					if (isClass(line)) continue;
					if (isInterface(line)) continue;
					if (isConstructor(line)) continue;
					if (isMethod(line)) continue;
					if (avoidCases(line)) continue;
					isParam(line);
					
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					// Close the writer regardless of what happens...
					bwLog.close();
				} catch (Exception e) {
				}
			}
		}
		
	}
	
	private static boolean isConstructor(String line) throws IOException {
		if (line.contains("import")) {
			bwLog.write("# CONSTRUCTOR - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isDeclareIndex(String line, BufferedReader br) throws IOException {
		if (line.contains("declare") && line.contains("/index")) {
			bwLog.write("# OMMITED DECLARE INDEX - " + line + "\n");
			
			while ((line = br.readLine()) != null && line.length() != 0) {
				bwLog.write("# OMMITED - " + line + "\n");
				if (line.contains("}")) {
					st = STATE.END_MODULE;
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static boolean avoidCases(String line) {
		if (line.trim().equals("}")) return true; // close statement
//		if (str.contains("export")) return true; // export module from index
		return false;
	}
	
	private static boolean isImports(String line) throws IOException {
		if (line.contains("import")) {
			bwLog.write("# IMPORT - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isClass(String line) throws IOException {
		if (line.contains("class") && line.contains("{")) {
			bwLog.write("# CLASS - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isInterface(String line) throws IOException {
		if (line.contains("interface")) {
			bwLog.write("# INTERFACE - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isParam(String line) throws IOException {
		bwLog.write("# PARAM - " + line + "\n");
		return true;
	}
	
	private static boolean isMethod(String line) throws IOException {
		
		if (line.contains(OPEN_PARENTHESIS) && line.contains(CLOSE_PARENTHESIS)) {
			bwLog.write("# METHOD - " + line + "\n");
			return true;
		}
		return false;
	}
	
	private static boolean isModule(String line) throws IOException {
		if (line.contains("declare module")) {
			bwLog.write("# MODULE - " + line + "\n");
			return true;
		}
		return false;
	}
	
	enum STATE {
		INIT, COMMENT, END_COMMENT, DECLARE_MODULE, MODULE_INDEX, END_MODULE, CLASS, CONSTRUCTOR, ENDCLASS, INNERCLASS, METHOD, PARAM
	}
	
}


