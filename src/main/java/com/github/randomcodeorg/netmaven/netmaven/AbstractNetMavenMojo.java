package com.github.randomcodeorg.netmaven.netmaven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractNetMavenMojo extends AbstractMojo {
	
	@Parameter(name = "assemblies")
	private List<String> assemblies;

	public AbstractNetMavenMojo() {
	
	}

	
	protected List<String> getAssemblies(){
		if(assemblies != null){
			if(!assemblies.contains("System")) assemblies.add("System");
		}else{
			assemblies = new ArrayList<>();
			assemblies.add("System");
		}
		return assemblies;
	}
	
}
