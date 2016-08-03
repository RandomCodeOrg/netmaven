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
		cb.add(getCompilerExecutable());
		addOutput(config, cb, releaseFile);
		addOutcome(config, cb, outcome);
		addReferences(cb, config);
		addCompilerSpecificOptions(cb);
		cb.add(getConfig().getSourceFiles());
		config.getLog().info("Executing compiler: " + getCompilerExecutable());
		config.getLog().debug(cb.toString());
		config.getLog().info("");
		config.getLog().info("");
		ExecuteableLink execLink = new ExecuteableLink(getLogLevelSelector(), cb.build());
		try {
			if (!execLink.execute(config.getLog())) {
				config.getLog().info("");
				config.getLog().info("");
				throw new CompilationException("The compilation failed. Please refer to the build output for more details.");
			}

		} catch (IOException | InterruptedException e) {
			config.getLog().info("");
			config.getLog().info("");
			e.printStackTrace();
		}
		config.getLog().info("");
		config.getLog().info("");
		return releaseFile;
	}
	
	protected LogLevelSelector getLogLevelSelector(){
		return new SimpleLogLevelSelector();
	}
	
	protected abstract String getCompilerExecutable();
	
	protected void addCompilerSpecificOptions(CommandBuilder commanBuilder){
		
	}
	
	protected void addOutcome(CompilationConfig config, CommandBuilder commanBuilder, String outcome){
		commanBuilder.add("-target:" + outcome);
	}
	
	protected void addOutput(CompilationConfig config, CommandBuilder commandBuilder, String releaseFile){
		commandBuilder.add("-o", releaseFile);
	}
	
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
