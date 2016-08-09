package com.github.randomcodeorg.netmaven.netmaven.compiler;

public enum CompilationOutcome {
	
	EXE("exe"),
	DLL("dll");

	private final String extension;
	
	private CompilationOutcome(String extension){
		this.extension = extension;
	}
	
	public String getExtension(){
		return extension;
	}
}
