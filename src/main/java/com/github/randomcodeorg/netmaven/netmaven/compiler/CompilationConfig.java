package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class CompilationConfig {

	private final Log log;
	private final String sourceLocation;
	private final String outputDirectory;
	private List<File> sourceFiles;
	private List<String> frameworkAssemblies;
	private final String libLocation;
	private List<String> libs;
	private CompilationOutcome outcome;
	private final List<File> dependencies;
	private String targetFramework;
	private boolean showDebugOutput = false;
	private boolean showCompilerVersion = false;
	private boolean compileDebugVersion = false;
	
	private String artifactName;

	public CompilationConfig(String artifactName, Log log, String sourceLocation, String outputDirectory, List<String> frameworkAssemblies,
			String libLocation, CompilationOutcome outcome, List<File> dependencies) {
		this.log = log;
		this.sourceLocation = sourceLocation;
		this.outputDirectory = outputDirectory;
		this.frameworkAssemblies = frameworkAssemblies;
		this.libLocation = libLocation;
		this.outcome = outcome;
		this.dependencies = dependencies;
		this.artifactName = artifactName;
	}

	public boolean getCompileDebugVersion(){
		return compileDebugVersion;
	}
	
	public void setCompileDebugVersion(boolean value){
		compileDebugVersion = value;
	}
	
	public String getArtifactName(){
		return artifactName;
	}
	
	public void setArtifactName(String name){
		this.artifactName = name;
	}
	
	public CompilationOutcome getOutcome() {
		return outcome;
	}
	
	public void setOutcome(CompilationOutcome outcome){
		this.outcome = outcome;
	}
	
	public boolean getShowDebugOutput(){
		return showDebugOutput;
	}
	
	public void setShowDebugOutput(boolean value){
		showDebugOutput = value;
	}
	
	public boolean getShowCompilerVersion(){
		return showCompilerVersion;
	}
	
	public void setShowCompilerVersion(boolean value){
		showCompilerVersion = value;
	}

	private List<File> findSourceFiles(List<File> files, File current) {
		if (current.isFile() && current.getName().endsWith(".cs")) {
			log.debug("Found source file: " + current.getAbsolutePath());
			files.add(current);
		} else if (current.isDirectory()) {
			for (File child : current.listFiles()) {
				findSourceFiles(files, child);
			}
		}
		return files;
	}

	public List<File> findSourceFiles() {
		if (sourceFiles != null)
			return sourceFiles;
		sourceFiles = new ArrayList<>();
		return findSourceFiles(sourceFiles, new File(sourceLocation));
	}

	public List<String> getSourceFiles() {

		List<File> files = findSourceFiles();
		List<String> result = new ArrayList<>();
		for (File f : files) {
			result.add(f.getAbsolutePath());
		}
		return result;
	}

	public Log getLog() {
		return log;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public List<String> getFrameworkAssemblies() {
		return frameworkAssemblies;
	}

	public List<String> getLibs() {
		if (libs != null)
			return libs;
		libs = new ArrayList<>();
		findLibs(new File(libLocation), libs);
		return libs;
	}

	public String getLibsDirectory() {
		return libLocation;
	}

	public List<File> getDependencies() {
		return dependencies;
	}
	
	private void findLibs(File current, List<String> libs) {
		if (!current.exists())
			return;
		if (current.isFile() && current.getName().endsWith(".dll")) {
			libs.add(current.getAbsolutePath());
		} else if (current.isDirectory()) {
			for (File child : current.listFiles()) {
				findLibs(child, libs);
			}
		}
	}
	
	
	public String getTargetFramework(){
		return targetFramework;
	}

	public void setTargetFramework(String targetFramework){
		this.targetFramework = targetFramework;
	}
	
	public boolean hasTargetFramework(){
		return targetFramework != null;
	}
	
}
