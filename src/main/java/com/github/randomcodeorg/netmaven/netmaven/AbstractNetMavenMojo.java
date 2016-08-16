package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;
import com.github.randomcodeorg.netmaven.netmaven.nuget.NugetExpander2;

public abstract class AbstractNetMavenMojo extends AbstractMojo {

	@Parameter(name = "assemblies")
	private List<String> assemblies;

	@Parameter(name = "ikvmHome", required = false)
	private String ikvmLocation;
	
	@Parameter(name = "targetFramework", required=false)
	private String targetFramework;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repoSession;
	
	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private String outputDir;

	private InternalLogger logger;
	
	private FrameworkVersion targetFrameworkVersion;
	
	public AbstractNetMavenMojo() {

	}
	
	protected InternalLogger getLogger(){
		if(logger == null){
			logger = new MavenRedirectLogger(getLog());
		}
		return logger;
	}

	protected FrameworkVersion getTargetFrameworkVersion() throws MojoExecutionException{
		if(targetFrameworkVersion == null){
			try {
				targetFrameworkVersion = FrameworkVersion.parse(targetFramework);
			} catch (ParseException e) {
				throw new MojoExecutionException("The specified target framework version is invalid.", e);
			}
		}
		return targetFrameworkVersion;
	}

	protected List<String> getAssemblies() {
		if (assemblies != null) {
			if (!assemblies.contains("System"))
				assemblies.add("System");
		} else {
			assemblies = new ArrayList<>();
			assemblies.add("System");
		}
		return assemblies;
	}

	public String getIKVMLibraryPath() {
		if (checkIKVM(ikvmLocation)) {
			return ikvmLocation;
		}
		String ikvmHome = System.getenv("IKVM_HOME");
		if (checkIKVM(ikvmHome)) {
			return ikvmHome;
		}
		String mavenIKVM = getDefaultIKVMPath();
		if (checkIKVM(mavenIKVM))
			return mavenIKVM;
		return null;
	}

	protected String getDefaultIKVMPath() {
		return new PathBuilder(repoSession.getLocalRepository().getBasedir().getParentFile().getAbsolutePath())
				.sub("tools").sub("IKVM-lib").build();
	}

	protected boolean checkIKVM(String path) {
		if (path == null || path.isEmpty() || !new File(path).exists())
			return false;
		if (!directoryExists(PathBuilder.create(path).sub("bin").build()))
			return false;
		if (!directoryExists(PathBuilder.create(path).sub("bin-x64").build()))
			return false;
		if (!directoryExists(PathBuilder.create(path).sub("bin-x86").build()))
			return false;
		if (!directoryExists(PathBuilder.create(path).sub("lib").build()))
			return false;
		return true;
	}

	protected boolean directoryExists(String path) {
		if (path == null)
			return false;
		File f = new File(path);
		return f.exists() && f.isDirectory();
	}

	protected String getTargetFramework(){
		return targetFramework;
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
}
