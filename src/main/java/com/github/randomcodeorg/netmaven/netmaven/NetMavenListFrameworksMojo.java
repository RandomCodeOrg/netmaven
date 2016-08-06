package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationConfig;
import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationOutcome;
import com.github.randomcodeorg.netmaven.netmaven.compiler.SelectingNetCompiler;

@Mojo(name = "listFrameworks", requiresDependencyResolution = ResolutionScope.NONE)
public class NetMavenListFrameworksMojo extends AbstractNetMavenMojo {

	public NetMavenListFrameworksMojo() {

	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		CompilationConfig cc = new CompilationConfig(getLog(), "/", "/", new ArrayList<String>(), "/",
				CompilationOutcome.DLL, new ArrayList<File>());
		SelectingNetCompiler compiler = new SelectingNetCompiler(cc);
		String[] frameworks = compiler.getFrameworkVersions();
		log.info("");
		log.info("Avaialable framework versions:");
		for(String framework : frameworks){
			log.info("\t" + framework);
		}
		log.info("");
	}

}
