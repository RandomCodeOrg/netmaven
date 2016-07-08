package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.IOException;

import org.apache.maven.plugin.logging.Log;

public class ExecuteableLink {

	private final String[] cmd;

	public ExecuteableLink(String... cmd) {
		this.cmd = cmd;
	}

	public boolean execute(Log log) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Process p = pb.start();
		new OutputRedirect(p, log, true).start();
		new OutputRedirect(p, log, false);
		return p.waitFor() == 0;
	}

}
