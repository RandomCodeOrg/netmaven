package com.github.randomcodeorg.netmaven.netmaven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationConfig;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, inheritByDefault = true)
public class NetMavenCompileMojo extends AbstractNetMavenCompilationMojo {

	@Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
	private String projectBuildDir;

	public NetMavenCompileMojo() {
	}

	@Override
	protected String getProjectBuildDir() {
		return projectBuildDir;
	}

	@Override
	protected String getArtifactName() {
		return "Release";
	}
	
	@Override
	protected boolean requiresRecycle(CompilationConfig config) {
		return config.getArtifactName().equals("Release");
	}
	
	@Override
	protected void setupRecycle(CompilationConfig config) {
		super.setupRecycle(config);
		config.setArtifactName("Debug");
		config.setCompileDebugVersion(true);
	}
	
	@Override
	protected boolean isArtifact(CompilationConfig config, String path) {
		if(path == null) return false;
		if("Release".equalsIgnoreCase(config.getArtifactName())) return true;
		return false;
	}
	
	@Override
	protected void beforeRecycle(CompilationConfig config) {
		super.beforeRecycle(config);
		getLog().info("Compiling Debug...");
	}

}
