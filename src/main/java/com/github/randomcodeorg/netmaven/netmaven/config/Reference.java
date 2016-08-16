package com.github.randomcodeorg.netmaven.netmaven.config;

import java.io.File;
import java.nio.file.Path;

public class Reference {

	private final String referenced;

	private final String hintPath;

	private static final String DOT_DLL = ".dll";

	public Reference(String referenced, String hintPath) {
		this.referenced = referenced;
		this.hintPath = hintPath;
	}

	public Reference(Path projectHome, File referencedFile) {
		if (!referencedFile.exists())
			throw new IllegalArgumentException("The referenced file does not exist.");
		String name = referencedFile.getName();
		if (!name.endsWith(DOT_DLL))
			throw new IllegalArgumentException("The referenced file has an invalid extension.");
		name = name.substring(0, name.length() - DOT_DLL.length());
		this.referenced = name;
		hintPath = projectHome.relativize(referencedFile.getAbsoluteFile().toPath()).toString();
	}

	public String getReferenced() {
		return referenced;
	}

	public String getHintPath() {
		return hintPath;
	}

	public String getHintPath(boolean replaceSeparators) {
		if (!replaceSeparators || hintPath == null)
			return hintPath;
		else
			return hintPath.replace("/", "\\");
	}

	public boolean hasHintPath() {
		return hintPath != null;
	}

}
