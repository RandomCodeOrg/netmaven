package com.github.randomcodeorg.netmaven.netmaven.compiler;

public abstract class NetCompiler {

	private final CompilationConfig config;

	public NetCompiler(CompilationConfig config) {
		this.config = config;
	}

	public abstract String compile();

	protected CompilationConfig getConfig() {
		return config;
	}
	
	public abstract void check();

}
