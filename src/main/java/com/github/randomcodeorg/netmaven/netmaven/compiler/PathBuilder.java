package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;

public class PathBuilder {

	private String path;
	private final String SEPARATOR = File.separator;
	
	public PathBuilder(String path) {
		this.path = path;
	}
	
	public PathBuilder sub(String name){
		if(path.endsWith(SEPARATOR) && name.startsWith(SEPARATOR)){
			path += name.substring(1);
			return this;
		}
		if(path.endsWith(SEPARATOR) || name.startsWith(SEPARATOR)){
			path += name;
			return this;
		}
		path += SEPARATOR + name;
		return this;
	}
	
	public String build(){
		return path;
	}

}
