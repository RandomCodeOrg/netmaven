package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.io.IOException;

public class MonoMCSCompiler extends NetCompiler {

	public MonoMCSCompiler(CompilationConfig config) {
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
		cb.add("mcs", "-o", releaseFile);
		cb.add("-target:" + outcome);
		for (String lib : config.getLibs()) {
			cb.add("/reference:" + lib);
		}
		for (File f : config.getDependencies()) {
			cb.add("/reference:" + f.getAbsolutePath());
		}
		for (String frameworkAssembly : config.getFrameworkAssemblies()) {
			if (frameworkAssembly.endsWith(".dll")) {
				cb.add("/reference:" + frameworkAssembly);
			} else {
				cb.add("/reference:" + frameworkAssembly + ".dll");
			}
		}
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

}
