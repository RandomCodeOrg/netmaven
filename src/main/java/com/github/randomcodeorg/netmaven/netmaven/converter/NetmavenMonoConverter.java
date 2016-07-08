package com.github.randomcodeorg.netmaven.netmaven.converter;

import java.io.File;
import java.io.IOException;

import com.github.randomcodeorg.netmaven.netmaven.compiler.CommandBuilder;
import com.github.randomcodeorg.netmaven.netmaven.compiler.CompilationException;
import com.github.randomcodeorg.netmaven.netmaven.compiler.ExecuteableLink;
import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;

public class NetmavenMonoConverter extends NetmavenConverter {

	public NetmavenMonoConverter(ConversionConfig config) {
		super(config);
	}

	@Override
	public void convert(Iterable<File> files) {
		ConversionConfig config = getConfig();
		CommandBuilder cb = new CommandBuilder();
		String resultFile = new PathBuilder(config.getOutputDirectory()).sub("jdependencies.dll").build();
		cb.add("mono", new PathBuilder(config.getIkvmLocation()).sub("bin").sub("ikvmc.exe").build());
		cb.add("-target:library", "-nologo", "-out:" + resultFile);
		for (File f : files) {
			cb.add(f.getAbsolutePath());
		}
		config.getLog().info("Executing converter: mono ikvmc.exe");
		config.getLog().debug(cb.toString());
		ExecuteableLink execLink = new ExecuteableLink(cb.build());
		try {
			if (!execLink.execute(config.getLog())) {
				config.getLog().error("The conversion failed!");
				throw new CompilationException("The conversion failed! Refer to the build output for more details.");
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
