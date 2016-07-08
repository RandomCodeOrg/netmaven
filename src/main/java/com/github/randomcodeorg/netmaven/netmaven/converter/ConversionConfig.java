package com.github.randomcodeorg.netmaven.netmaven.converter;

import org.apache.maven.plugin.logging.Log;

public class ConversionConfig {

	private final String outputDirectory;
	private final Log log;
	private final String ikvmLocation;
	
	public ConversionConfig(Log log, String outputDirectory, String ikvmLocation) {
		this.outputDirectory = outputDirectory;
		this.log = log;
		this.ikvmLocation = ikvmLocation;
	}
	
	public String getOutputDirectory() {
		return outputDirectory;
	}
	
	public Log getLog() {
		return log;
	}
	
	public String getIkvmLocation() {
		return ikvmLocation;
	}

}
