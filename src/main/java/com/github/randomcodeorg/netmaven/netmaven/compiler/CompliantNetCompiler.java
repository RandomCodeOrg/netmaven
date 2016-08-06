package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;

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
		buildReferences(cb, config);
		buildLibraryPaths(cb, config);
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
				throw new CompilationException(
						"The compilation failed. Please refer to the build output for more details.");
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

	protected LogLevelSelector getLogLevelSelector() {
		return new SimpleLogLevelSelector();
	}

	protected abstract String getCompilerExecutable();

	protected void addCompilerSpecificOptions(CommandBuilder commanBuilder) {

	}

	protected void addOutcome(CompilationConfig config, CommandBuilder commanBuilder, String outcome) {
		commanBuilder.add("-target:" + outcome);
	}

	protected void addOutput(CompilationConfig config, CommandBuilder commandBuilder, String releaseFile) {
		commandBuilder.add("-o", releaseFile);
	}

	protected void addReferences(Set<String> references) {
		CompilationConfig c = getConfig();
		references.addAll(c.getLibs());
		for (File f : c.getDependencies())
			references.add(f.getAbsolutePath());
		for (String frameworkAssembly : c.getFrameworkAssemblies()) {
			if (frameworkAssembly.endsWith(".dll")) {
				references.add(frameworkAssembly);
			} else {
				references.add(frameworkAssembly + ".dll");
			}
		}
	}

	protected void buildReferences(CommandBuilder commandBuilder, CompilationConfig config) {
		Set<String> refs = new HashSet<>();
		addReferences(refs);
		if (refs.isEmpty())
			return;
		commandBuilder.add(buildCSParameter("r", refs));
	}

	protected abstract File getLibraryPathForFramework(String version);

	protected void addLibraryPaths(Set<String> libraryPaths) {
		if (getConfig().hasTargetFramework()) {
			libraryPaths.add(getLibraryPathForFramework(getConfig().getTargetFramework()).getAbsolutePath());
		}
	}

	protected String buildCSParameter(String paramater, Set<String> values) {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (String p : values) {
			if (!isFirst)
				sb.append(",");
			else
				isFirst = false;
			if (p.contains(" ")) {
				sb.append(String.format("\"%s\"", p));
			} else {
				sb.append(p);
			}
		}
		return String.format("/%s:%s", paramater, sb.toString());
	}

	protected void buildLibraryPaths(CommandBuilder commandBuilder, CompilationConfig config) {
		Set<String> libPaths = new HashSet<>();
		addLibraryPaths(libPaths);
		if (libPaths.isEmpty())
			return;
		commandBuilder.add(buildCSParameter("lib", libPaths));
	}

}
