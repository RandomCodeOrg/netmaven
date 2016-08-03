package com.github.randomcodeorg.netmaven.netmaven.compiler;

class MonoMCSCompiler extends CompliantNetCompiler {

	public MonoMCSCompiler(CompilationConfig config) {
		super(config);
	}

	@Override
	protected String getCompilerExecutable() {
		return "mcs";
	}

	@Override
	public void check() {
		
	}

}
