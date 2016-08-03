package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.io.IOException;

public abstract class CompliantNetCompiler extends NetCompiler {

	public CompliantNetCompiler(CompilationConfig config) {
		super(config);
	}

	@Override
	public String compile() {
		CommandBuilder cb = new CommandBuilder();
		CompilationConfig config = getConfig();

		String outcome = "exe";
		String extension = "exe";
		switch (config.getOutcome()) {
		case DLL:
			outcome = "library";
			extension = "dll";
			break;
		default:
			outcome = "exe";
			extension = "exe";
			break;
		}
		String releaseFile = new PathBuilder(config.getOutputDirectory()).sub("Release." + extension).build();
		cb.add(getCompilerExecutable(), "-o", releaseFile);
		cb.add("-target:" + outcome);
		addReferences(cb, config);
		cb.add(getConfig().getSourceFiles());
		config.getLog().info("Executing compiler: mcs");
		config.getLog().debug(cb.toString());
		ExecuteableLink execLink = new ExecuteableLink(cb.build());
		try {
			if (!execLink.execute(config.getLog())) {
				throw new CompilationException();
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return releaseFile;
	}
	
	protected abstract String getCompilerExecutable();
	
	
	protected void addReferences(CommandBuilder commandBuilder, CompilationConfig config){
		for (String lib : config.getLibs()) {
			commandBuilder.add("/reference:" + lib);
		}
		for (File f : config.getDependencies()) {
			commandBuilder.add("/reference:" + f.getAbsolutePath());
		}
		for (String frameworkAssembly : config.getFrameworkAssemblies()) {
			if (frameworkAssembly.endsWith(".dll")) {
				commandBuilder.add("/reference:" + frameworkAssembly);
			} else {
				commandBuilder.add("/reference:" + frameworkAssembly + ".dll");
			}
		}
	}

}
