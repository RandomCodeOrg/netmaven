package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.IOException;

import org.apache.maven.plugin.logging.Log;

public class ExecuteableLink {

	private final String[] cmd;
	private final LogLevelSelector logLevelSelector;

	public ExecuteableLink(LogLevelSelector levelSelector, String... cmd) {
		this.cmd = cmd;
		this.logLevelSelector = levelSelector;
	}

	public boolean execute(Log log) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Process p = pb.start();
		OutputRedirect r1 = new OutputRedirect(p, log, true, logLevelSelector).start();
		OutputRedirect r2 = new OutputRedirect(p, log, false, logLevelSelector).start();
		boolean result = p.waitFor() == 0;
		r1.waitFor();
		r2.waitFor();
		return result;
	}

}
