package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationConfig;
import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationOutcome;
import com.github.randomcodeorg.netmaven.netmaven.compiler.NetCompiler;
import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;
import com.github.randomcodeorg.netmaven.netmaven.compiler.SelectingNetCompiler;

@Mojo(name = "netmavenCompile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, inheritByDefault = true)
public class NetMavenCompileMojo extends AbstractNetMavenMojo {

	@Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
	private String projectBuildDir;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private String outputDir;

	@Component
	private MavenProjectHelper projectHelper;

	@Parameter(required = true, property = "project", readonly = true)
	private MavenProject mavenProject;

	public NetMavenCompileMojo() {
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Executing NatMaven-Compiler");
		getLog().info("Location is: " + projectBuildDir);

		String packaging = mavenProject.getPackaging();
		CompilationOutcome outcome = CompilationOutcome.DLL;
		if ("exe".equals(packaging))
			outcome = CompilationOutcome.EXE;
		else if ("dll".equals(packaging))
			outcome = CompilationOutcome.DLL;

		List<File> dependencies = new ArrayList<>();
		File f;
		for (Artifact a : mavenProject.getArtifacts()) {
			f = a.getFile();
			if (f != null && f.exists() && f.getName().endsWith(".dll"))
				dependencies.add(f);
		}

		CompilationConfig config = new CompilationConfig(getLog(), projectBuildDir,
				new PathBuilder(outputDir).sub("bin").build(), getAssemblies(), getLibDirectory(), outcome,
				dependencies);

		if (config.findSourceFiles().size() == 0) {
			getLog().info("No source files found!");
			return;
		}
		File outputDir = new File(config.getOutputDirectory());
		if (!outputDir.exists())
			outputDir.mkdirs();
		NetCompiler c = new SelectingNetCompiler(config);
		String filePath = c.compile();
		mavenProject.getArtifact().setFile(new File(filePath));
	}

	private String getLibDirectory() {
		String path = new PathBuilder(outputDir).sub("generated").sub("libs").build();
		return path;

	}

}
