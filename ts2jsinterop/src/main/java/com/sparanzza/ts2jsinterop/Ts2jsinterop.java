package com.sparanzza.ts2jsinterop;

import java.io.*;

import static com.sparanzza.ts2jsinterop.Constants.CLOSE_PARENTHESIS;
import static com.sparanzza.ts2jsinterop.Constants.OPEN_PARENTHESIS;

public class Ts2jsinterop {
	
	public static void main(String[] args) {
		File file = new File(Ts2jsinterop.class.getClassLoader().getResource("babylon.module.d.ts").getFile());
		
		if (file.exists()) {
			System.out.println("### Init file module.ts ###");
			
			System.out.println("Absolute Path: " + file.getAbsolutePath());
			System.out.println("Is Directory: " + file.isDirectory());
			System.out.println("Parent Path: " + file.getParent());
			
			if (file.isFile()) {
				System.out.println("File size: " + file.length());
				System.out.println("File last modified " + file.lastModified());
			}
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String str;
				boolean inComment = false;
				while ((str = br.readLine()) != null && str.length() != 0) {
					
					// @formatter:off
					if (str.contains("/**")) { inComment = true; continue; }
					if (str.contains("*/")) { inComment = false; continue; }
					// @formatter:on
					
					if (!inComment) {
						if (isModule(str)) continue;
						if (isImports(str)) continue;
						if (isClass(str)) continue;
						if (isInterface(str)) continue;
						if (isMethod(str)) continue;
						if (avoidCases(str)) continue;
						isParam(str);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private static boolean avoidCases(String str) {
		if (str.trim().equals("}")) return true; // close statement
		if (str.contains("export")) return true; // export module from index
		return false;
	}
	
	private static boolean isImports(String str) {
		boolean isFound;
		if (isFound = str.contains("import")) {
			System.out.println("#IMPORT - " + str);
		}
		return isFound;
	}
	
	private static boolean isClass(String str) {
		boolean isFound;
		if (isFound = str.contains("Class") && !str.contains("(") ) {
			System.out.println("#CLASS - " + str);
		}
		return isFound;
	}
	
	private static boolean isInterface(String str) {
		boolean isFound;
		if (isFound = str.contains("Interface")) {
			System.out.println("#INTERFACE - " + str);
		}
		return isFound;
	}
	
	private static boolean isParam(String str) {
		System.out.println("#PARAM - " + str);
		return true;
	}
	
	private static boolean isMethod(String str) {
		boolean isFound;
		if (isFound = (str.contains(OPEN_PARENTHESIS) && str.contains(CLOSE_PARENTHESIS))) {
			System.out.println("#METHOD - " + str);
		}
		return isFound;
	}
	
	private static boolean isModule(String str) {
		boolean isFound;
		if (isFound = str.contains("declare module")) {
			System.out.println("#MODULE - " + str);
		}
		return isFound;
	}
	
}

