package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationConfig;
import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationOutcome;
import com.github.randomcodeorg.netmaven.netmaven.compiler.NetCompiler;
import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;
import com.github.randomcodeorg.netmaven.netmaven.compiler.SelectingNetCompiler;
import com.github.randomcodeorg.netmaven.netmaven.nuget.NugetExpander2;

public abstract class AbstractNetMavenCompilationMojo extends AbstractNetMavenMojo {

	@Parameter(required = true, property = "project", readonly = true)
	private MavenProject mavenProject;

	@Parameter(name = "showDebugOutput", required = false)
	private boolean showDebugOutput = false;

	@Parameter(name = "showCompilerVersion", required = false)
	private boolean showCompilerVersion = false;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private String outputDir;

	public AbstractNetMavenCompilationMojo() {

	}

	protected MavenProject getProject() {
		return mavenProject;
	}

	protected abstract String getProjectBuildDir();

	protected String getProjectOutputDir() {
		return outputDir;
	}

	protected abstract String getArtifactName();

	protected void applySettings(CompilationConfig config) {
		config.setShowCompilerVersion(showCompilerVersion);
		config.setShowDebugOutput(showDebugOutput);
		config.setTargetFramework(getTargetFramework());
	}

	protected CompilationConfig createCompilationConfig(String projectBuildDir, String outputDir, String artifactName) throws MojoExecutionException {
		String packaging = getProject().getPackaging();
		CompilationOutcome outcome = CompilationOutcome.DLL;
		if ("exe".equals(packaging))
			outcome = CompilationOutcome.EXE;
		else if ("dll".equals(packaging))
			outcome = CompilationOutcome.DLL;

		List<File> dependencies = new ArrayList<>();
		addDependencies(dependencies, mavenProject);

		CompilationConfig config = new CompilationConfig(artifactName, getLog(), projectBuildDir,
				new PathBuilder(outputDir).sub("bin").build(), getAssemblies(), getLibDirectory(outputDir), outcome,
				dependencies);

		if (config.findSourceFiles().size() == 0) {
			getLog().info("No source files found!");
			return null;
		}
		File outputDirF = new File(config.getOutputDirectory());
		if (!outputDirF.exists())
			outputDirF.mkdirs();
		return config;
	}

	protected void addDependencies(Collection<File> dependencies, MavenProject mavenProject) throws MojoExecutionException {
		File f;
		NugetExpander2 expander = new NugetExpander2(getLogger());
		for (Artifact a : mavenProject.getArtifacts()) {
			f = a.getFile();
			if (f == null || !f.exists())
				continue;
			if (f.getName().endsWith(".dll"))
				dependencies.add(f);
			else if (f.getName().endsWith(".nuget")) {
				addNugetDependencies(expander, dependencies, f);
			}
		}
	}

	protected void addNugetDependencies(NugetExpander2 expander, Collection<File> dependencies, File nuget)
			throws MojoExecutionException {
		getLogger().debug("Adding nuget dependencies from file: %s", nuget.getAbsolutePath());
		File destination = new File(outputDir, "nuget");
		try {
			expander.expandDlls(getTargetFrameworkVersion(), nuget, destination, dependencies);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format(
					"Could not extract the contents of the nuget package at '%s'. Refer to the nested exception for more details.",
					nuget.getAbsolutePath()), e);
		}
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		String artifactName = getArtifactName();
		getLog().info("Executing NatMaven-Compiler");
		String projectBuildDir = getProjectBuildDir();
		getLog().info("Location is: " + projectBuildDir);
		String projectOutputDir = getProjectOutputDir();
		getLog().info("Output location is: " + projectOutputDir);
		getLog().info(String.format("Compiling %s...", artifactName));

		CompilationConfig config = createCompilationConfig(projectBuildDir, projectOutputDir, artifactName);
		if (config == null)
			return;
		applySettings(config);

		cycle(config);
	}

	protected void cycle(CompilationConfig config) {
		NetCompiler c = new SelectingNetCompiler(config);
		getLog().debug("Available framework versions:");
		for (String framework : c.getFrameworkVersions()) {
			getLog().debug("\t" + framework);
		}
		String filePath = c.compile();
		if (isArtifact(config, filePath))
			mavenProject.getArtifact().setFile(new File(filePath));
		if (requiresRecycle(config)) {
			setupRecycle(config);
			beforeRecycle(config);
			cycle(config);
		}
	}

	protected void beforeRecycle(CompilationConfig config) {

	}

	protected void setupRecycle(CompilationConfig config) {

	}

	protected boolean requiresRecycle(CompilationConfig config) {
		return false;
	}

	protected boolean isArtifact(CompilationConfig config, String path) {
		return path != null;
	}

	private String getLibDirectory(String outputDir) {
		String path = new PathBuilder(outputDir).sub("generated").sub("libs").build();
		return path;

	}

}
