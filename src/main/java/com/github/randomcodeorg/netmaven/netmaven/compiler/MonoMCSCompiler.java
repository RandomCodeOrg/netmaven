package com.github.randomcodeorg.netmaven.netmaven.compiler;

public class MonoMCSCompiler extends CompliantNetCompiler {

	public MonoMCSCompiler(CompilationConfig config) {
		super(config);
	}

	@Override
	protected String getCompilerExecutable() {
		return "mcs";
	}

}
