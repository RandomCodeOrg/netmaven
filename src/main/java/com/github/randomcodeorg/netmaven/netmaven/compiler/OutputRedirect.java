package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.InputStream;
import java.util.Scanner;

import org.apache.maven.plugin.logging.Log;

public class OutputRedirect{

	private boolean isError;
	private Process process;
	private final Log log;
	
	public OutputRedirect(Process p, Log log, boolean isError){
		this.isError = isError;
		this.process = p;
		this.log = log;
	}
	
	public void start(){
		new Thread(new Runnable() {
			
			public void run() {
				if(isError){
					executeError();
				}else{
					executeOutput();
				}
			}
		}).start();
	}
	
	private void executeError(){
		executeOutput();
	}
	
	private void executeOutput(){
		InputStream in;
		if(isError) in = process.getErrorStream(); else in = process.getInputStream();
		Scanner sc = new Scanner(in);
		while(sc.hasNextLine()){
			if(isError){
				log.error(sc.nextLine());
			}else{
				log.info(sc.nextLine());
			}
		}
		sc.close();
	}
	
}
