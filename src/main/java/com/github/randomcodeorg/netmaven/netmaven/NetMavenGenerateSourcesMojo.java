package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;
import com.github.randomcodeorg.netmaven.netmaven.converter.ConversionConfig;
import com.github.randomcodeorg.netmaven.netmaven.converter.IkvmInclusionProfile;
import com.github.randomcodeorg.netmaven.netmaven.converter.NetmavenMonoConverter;

@Mojo(name = "netmavenGenerateSources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, inheritByDefault = true)
public class NetMavenGenerateSourcesMojo extends AbstractNetMavenMojo {

	@Parameter(required = true, property = "project", readonly = true)
	private MavenProject mavenProject;

	@Parameter(name = "ikvmHome", required = false)
	private String ikvmLocation;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private String outputDir;

	@Parameter(name = "ikvmRequired", required = false)
	private boolean ikvmRequired = false;

	public NetMavenGenerateSourcesMojo() {

	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		String ikvmHome = getIKVMLibraryPath();
		if (ikvmHome == null) {
			throw new MojoFailureException(
					"The path to the IKVM executables is missing. Specify it using <ikvmHome>PATH</ikvmHome>");
		}
		log.info("Using IKVM at " + ikvmHome);
		singleLibraryStrategy(log, ikvmHome);
	}

	private void singleLibraryStrategy(Log log, String ikvmHome) {
		log.info("Using single library strategy");
		log.info("Inspecting dependencies");
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		Set<File> jarFiles = new HashSet<>();
		for (Artifact a : artifacts) {
			if (a.getFile().getName().endsWith(".jar")) {
				log.debug(String.format("%s at '%s' will be included", a, a.getFile().getAbsolutePath()));
				jarFiles.add(a.getFile());
			}
		}

		String path = buildOutputDir();

		if (jarFiles.size() == 0) {
			log.info("No libraries to convert");
			if (ikvmRequired)
				copyIkvmLibraries(log, ikvmHome, path, IkvmInclusionProfile.FULL);
			return;
		}

		ConversionConfig config = new ConversionConfig(log, path, ikvmHome);
		NetmavenMonoConverter converter = new NetmavenMonoConverter(config);
		converter.convert(jarFiles);
		copyIkvmLibraries(log, ikvmHome, path, IkvmInclusionProfile.FULL);
	}

	private void copyIkvmLibraries(Log log, String ikvmHome, String path, IkvmInclusionProfile profile) {
		String ikvmLibs = new PathBuilder(ikvmHome).sub("bin").build();
		File ikvmLibsFile = new File(ikvmLibs);
		if (!ikvmLibsFile.exists() || !ikvmLibsFile.isDirectory()) {
			log.warn("Could not copy IKVM libraries. The compilation might fail.");
			return;
		}
		List<File> toCopy = new ArrayList<>();
		switch (profile) {
		case FULL:
			for (File child : ikvmLibsFile.listFiles())
				if (child.exists() && child.isFile() && child.getName().endsWith(".dll")
						&& child.getName().startsWith("IKVM."))
					toCopy.add(child);
			break;
		case MINIMAL:
			toCopy.add(new File(new PathBuilder(ikvmLibs).sub("IKVM.OpenJDK.Core.dll").build()));
			break;
		}
		log.info("Copying IKVM libraries");
		for (File f : toCopy) {
			if (!f.exists()) {
				log.warn("Could not copy '" + f.getAbsolutePath() + "' because it does not exist");
			} else {
				try {
					Files.copy(f.toPath(), new File(new PathBuilder(path).sub(f.getName()).build()).toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					log.warn("Could not copy '" + f.getAbsolutePath() + "' because an error occured.", e);
				}

			}
		}
	}

	private String buildOutputDir() {
		String path = new PathBuilder(outputDir).sub("generated").sub("libs").build();
		File f = new File(path);
		getLog().debug("Creating output directory: " + path);
		if (!f.exists())
			f.mkdirs();
		return path;
	}

}
