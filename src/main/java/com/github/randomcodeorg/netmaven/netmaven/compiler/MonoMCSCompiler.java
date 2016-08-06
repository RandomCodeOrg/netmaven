package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class MonoMCSCompiler extends CompliantNetCompiler {

	private static final Pattern VERSION_DIR_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+$");
	private static final String[] SEARCH_PATHS = new String[] { "/usr/lib/mono", "/usr/local/lib/mono",
			"/Library/Frameworks/Mono.framework/Versions/Current/lib/mono" };

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

	@Override
	public String[] getFrameworkVersions() {
		Map<String, File> frameworks = findFrameworks();
		String[] result = new String[frameworks.size()];
		int i = 0;
		for (String s : frameworks.keySet()) {
			result[i] = s;
			i++;
		}
		return result;
	}

	protected Map<String, File> findFrameworks() {
		Map<String, File> result = new HashMap<>();
		for (String path : SEARCH_PATHS)
			find(result, new File(path));
		return result;
	}

	protected void find(Map<String, File> result, File f) {
		if (f == null || !f.exists() || !f.isDirectory())
			return;
		for (File child : f.listFiles()) {
			if (!child.isDirectory())
				continue;
			if (VERSION_DIR_PATTERN.matcher(child.getName()).matches()) {
				result.put(child.getName(), child);
			}
		}
	}
	
	@Override
	protected File getLibraryPathForFramework(String version) {
		Map<String, File> frameworks = findFrameworks();
		if(frameworks.containsKey(version)){
			return frameworks.get(version);
		}else{
			throw new CompilationException("Could not find target framework.");
		}
	}
	
	@Override
	protected void addOutput(CompilationConfig config, CommandBuilder commandBuilder, String releaseFile) {
		if(releaseFile.contains(" ") && !releaseFile.startsWith("\"")) releaseFile = String.format("\"%s\"", releaseFile);
		commandBuilder.add( String.format("/out:%s", releaseFile) );
	}

}
