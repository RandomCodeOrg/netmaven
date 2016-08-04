package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;
import com.github.randomcodeorg.netmaven.netmaven.config.ProjectConfig;
import com.github.randomcodeorg.netmaven.netmaven.config.Reference;

@Execute(phase = LifecyclePhase.PROCESS_RESOURCES)
@Mojo(name = "config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, inheritByDefault = false)
public class NetMavenConfigMojo extends AbstractNetMavenMojo {

	@Parameter(required = true, property = "project", readonly = true)
	private MavenProject mavenProject;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private String outputDir;
	
	public NetMavenConfigMojo() {
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		String path = mavenProject.getFile().getParent();
		log.info("Location is: " + path);
		log.info("Searching for project files");
		Set<File> projects = searchProjectFiles(path);
		if (projects.size() == 0) {
			log.error("Could not find a project file.");
			throw new MojoExecutionException("Could not find a project file.");
		}
		if (projects.size() > 1) {
			log.error("Multiple project files were found.");
			throw new MojoExecutionException("Multiple project files were found.");
		}
		File projectFile = projects.iterator().next();
		log.info("Configuring project file: " + projectFile.getAbsolutePath());
		config(projectFile);
	}

	protected void config(File projectFile) throws MojoExecutionException, MojoFailureException {
		Document doc;

		SAXBuilder builder = new SAXBuilder();
		XMLOutputter xmlOutput = new XMLOutputter();

		xmlOutput.setFormat(Format.getPrettyFormat());

		try {
			doc = (Document) builder.build(projectFile);
		} catch (IOException | JDOMException e) {
			throw new MojoExecutionException("An I/O error occured reading the project file.", e);
		}
		Log log = getLog();
		log.debug(xmlOutput.outputString(doc));

		ProjectConfig pc = new ProjectConfig(doc);
		Path projectHomePath = projectFile.getParentFile().toPath();
		setupSourceDirectories(pc, projectHomePath);

		File generatedLibsDir = getGeneratedLibsDir();

		if (generatedLibsDir.exists()) {
			addGeneratedLibReferences(projectHomePath, pc, generatedLibsDir);
		}
		if (getAssemblies() != null && !getAssemblies().isEmpty()) {
			addAssemblies(projectHomePath, pc);
		}
		addDependencies(projectHomePath, pc);
		pc.save();
		log.info("Applying changes");
		log.debug(xmlOutput.outputString(doc));
		try {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(projectFile),
					StandardCharsets.UTF_8);
			xmlOutput.output(doc, writer);
			writer.close();
		} catch (IOException e) {
			throw new MojoExecutionException("An I/O error occured while appyling the changes.", e);
		}
		log.info("Completed. Refresh your project to apply the changes.");
	}

	protected void setupSourceDirectories(ProjectConfig config, Path projectHome) {
		getLog().info("Configuring source files");
		for (String s : mavenProject.getCompileSourceRoots()) {
			getLog().debug("Adding source files in: " + s);
			setupSourceDirectory(config, projectHome, new File(s));
		}
		for(String s : mavenProject.getTestCompileSourceRoots()){
			getLog().debug("Adding test source files in: " + s);
			setupSourceDirectory(config, projectHome, new File(s));
		}
	}

	protected void setupSourceDirectory(ProjectConfig config, Path projectHome, File f) {
		if (!f.exists())
			return;
		if (f.isFile()) {
			Path filePath = f.toPath();
			config.addSourceFile(projectHome.relativize(filePath).toString());
		} else if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				setupSourceDirectory(config, projectHome, child);
			}
		}
	}

	protected void addDependencies(Path projectHomePath, ProjectConfig pc) {
		Set<File> dependencies = new HashSet<>();
		for (Artifact artifact : mavenProject.getArtifacts()) {
			if (artifact.getFile().exists() && artifact.getFile().getName().endsWith(".dll")) {
				dependencies.add(artifact.getFile());
			}
		}
		if (dependencies.isEmpty())
			return;
		getLog().info("Configuring maven dependencies");
		for (File f : dependencies) {
			if (!pc.hasReference(f.getName().substring(0, f.getName().length() - ".dll".length()))) {
				pc.setReference(new Reference(projectHomePath, f));
			}
		}
	}

	protected void addAssemblies(Path projectHomePath, ProjectConfig pc) {
		if (getAssemblies() == null || getAssemblies().isEmpty())
			return;
		Log log = getLog();
		log.info("Configuring assembly references");
		for (String assembly : getAssemblies()) {
			if (!pc.hasReference(assembly)) {
				pc.setReference(new Reference(assembly, null));
			}
		}
	}

	protected void addGeneratedLibReferences(Path projectHomePath, ProjectConfig pc, File libsDir) {
		Log log = getLog();
		log.info("Configuring references to autogenerated libraries");
		String ref;

		for (File child : libsDir.listFiles()) {
			if (!child.isFile() || !child.getName().endsWith(".dll"))
				continue;
			ref = child.getName().substring(0, child.getName().length() - ".dll".length());
			if (!pc.hasReference(ref)) {
				pc.setReference(new Reference(ref, projectHomePath.relativize(child.toPath()).toString()));
			}
		}
	}

	protected File getGeneratedLibsDir() {
		String path = new PathBuilder(outputDir).sub("generated").sub("libs").build();
		return new File(path);
	}

	private Set<File> searchProjectFiles(String path) {
		Set<File> result = new HashSet<>();
		File parent = new File(path);
		if (!parent.exists() || !parent.isDirectory())
			return result;
		for (File child : parent.listFiles()) {
			if (isProjectFile(child)) {
				result.add(child);
			}
		}
		return result;
	}

	protected boolean isProjectFile(File f) {
		return f.exists() && f.isFile() && f.getName().endsWith(".csproj");
	}

}
