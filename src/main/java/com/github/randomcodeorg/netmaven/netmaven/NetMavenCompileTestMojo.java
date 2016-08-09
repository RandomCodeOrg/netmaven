package com.github.randomcodeorg.netmaven.netmaven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationConfig;
import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationOutcome;
import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;

@Mojo(name = "compileTest", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST, inheritByDefault = true)
public class NetMavenCompileTestMojo extends AbstractNetMavenCompilationMojo {

	@Parameter(defaultValue = "${project.build.testSourceDirectory}", required = true, readonly = true)
	private String projectBuildDir;

	public NetMavenCompileTestMojo() {

	}

	@Override
	protected String getProjectBuildDir() {
		return projectBuildDir;
	}

	@Override
	protected String getArtifactName() {
		return "Test";
	}

	@Override
	protected boolean requiresRecycle(CompilationConfig config) {
		return false;
	}

	@Override
	protected boolean isArtifact(CompilationConfig config, String path) {
		return false;
	}

	@Override
	protected void applySettings(CompilationConfig config) {
		super.applySettings(config);
		String extension = config.getOutcome().getExtension();
		config.setOutcome(CompilationOutcome.DLL);
		config.getDependencies().add(new PathBuilder(config.getOutputDirectory()).sub("Debug." + extension).file());
	}

}
