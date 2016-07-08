package com.github.randomcodeorg.netmaven.netmaven.converter;

import java.io.File;

public abstract class NetmavenConverter {

	private final ConversionConfig config;
	
	public NetmavenConverter(ConversionConfig config) {
		this.config = config;
	}
	
	
	public final ConversionConfig getConfig() {
		return config;
	}
	
	public abstract void convert(Iterable<File> files);

}
