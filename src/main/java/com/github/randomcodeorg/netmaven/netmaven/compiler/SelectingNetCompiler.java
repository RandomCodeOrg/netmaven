package com.github.randomcodeorg.netmaven.netmaven.compiler;

public class SelectingNetCompiler extends NetCompiler {

	private final NetCompiler selectedCompiler;

	public SelectingNetCompiler(CompilationConfig config) {
		super(config);
		NetCompiler comp = null;
		try {
			comp = new VisualCSCompiler(config);
			comp.check();
		} catch (CompilerNotFoundExeception e) {
			try {
				comp = new MonoMCSCompiler(config);
				comp.check();
			} catch (CompilerNotFoundExeception e1) {
				
			}
		}
		if(comp == null) throw new CompilerNotFoundExeception("The required compiler could not be found. Please install the Visual C# or Mono C# compiler.");
		this.selectedCompiler = comp;
	}

	@Override
	public String compile() {
		return selectedCompiler.compile();
	}

	@Override
	protected CompilationConfig getConfig() {
		return selectedCompiler.getConfig();
	}

	@Override
	public void check() {
		selectedCompiler.check();
	}

	@Override
	public String[] getFrameworkVersions() {
		return selectedCompiler.getFrameworkVersions();
	}
	
}
