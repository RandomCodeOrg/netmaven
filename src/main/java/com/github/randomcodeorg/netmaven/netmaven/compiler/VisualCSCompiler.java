package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.util.regex.Pattern;

class VisualCSCompiler extends CompliantNetCompiler {

	public VisualCSCompiler(CompilationConfig config) {
		super(config);
	}

	private File executable;

	@Override
	protected String getCompilerExecutable() {
		if (executable == null) {
			executable = searchExecutable();
		}
		return executable.getAbsolutePath();
	}

	protected File searchExecutable() {
		String[] locations = { "C:\\Windows\\Microsoft.NET\\Framework64", "C:\\Windows\\Microsoft.NET\\Framework" };
		CompilationConfig config = getConfig();
		File tmp = null;
		for (String location : locations) {
			tmp = searchAt(config, new File(location));
			if (tmp != null)
				break;
		}
		if (tmp == null)
			throwNotFound();
		return tmp;
	}
	
	@Override
	protected LogLevelSelector getLogLevelSelector() {
		String warningRegex = ".*: warning CS[0-9]+: .*";
		String errorRegex = ".*: error CS[0-9]+: .*";
		return new RegexLogLevelSelector(errorRegex, warningRegex);
	}
	
	@Override
	protected void addCompilerSpecificOptions(CommandBuilder commanBuilder) {
		commanBuilder.add("/nologo");
	}
	
	@Override
	protected void addOutcome(CompilationConfig config, CommandBuilder commanBuilder, String outcome) {
		commanBuilder.add("/target:" + outcome);
	}

	@Override
	protected void addOutput(CompilationConfig config, CommandBuilder commandBuilder, String releaseFile) {
		if(releaseFile.contains(" ") && !releaseFile.startsWith("\"")) releaseFile = String.format("\"%s\"", releaseFile);
		commandBuilder.add( String.format("/out:%s", releaseFile) );
	}
	
	protected File searchAt(CompilationConfig config, File f) {
		if (!f.exists() || !f.isDirectory())
			return null;
		File versionDir = null;
		for (File child : f.listFiles()) {
			if (!child.isDirectory() || !child.getName().startsWith("v"))
				continue;
			if (check(config, versionDir, child)) {
				versionDir = child;
			}
		}
		if (versionDir == null)
			return null;
		return PathBuilder.create(versionDir.getAbsolutePath()).sub("csc.exe").file();
	}

	protected boolean check(CompilationConfig config, File currentV, File newV) {
		if (config.hasNetVersion()) {
			Pattern pattern = Pattern.compile(config.getNetVersion());
			return pattern.matcher(newV.getName()).matches();
		} else {
			if (currentV == null)
				return true;
			return newV.getName().compareTo(currentV.getName()) > 0;
		}
	}

	protected void throwNotFound() {
		throw new CompilerNotFoundExeception("Could not find the comiler executable.");
	}

	@Override
	public void check() {
		getCompilerExecutable();
	}

}
