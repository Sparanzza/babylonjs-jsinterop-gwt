package com.sparanzza.ts2jsinterop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportLog {
	
	File outLog;
	BufferedWriter bw;
	
	public ExportLog(String filename) {
		outLog = new File(filename);
		initBuffered(outLog);
	}
	
	public void writeLogLine(String line) {
		try {
			bw.write(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeLog() {
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initBuffered(File file) {
		try {
			bw = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
