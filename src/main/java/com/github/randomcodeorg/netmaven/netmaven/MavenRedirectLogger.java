package com.github.randomcodeorg.netmaven.netmaven;

import org.apache.maven.plugin.logging.Log;

public class MavenRedirectLogger implements InternalLogger {

	private final Log log;
	private final String format;
	
	public MavenRedirectLogger(Log log, String format) {
		this.log = log;
		this.format = format;
	}
	
	public MavenRedirectLogger(Log log){
		this(log, "%s");
	}
	private String build(String format, Object... args){
		return String.format(String.format(this.format, format), args);
	}

	@Override
	public void info(String format, Object... args) {
		log.info(build(format, args));
	}

	@Override
	public void debug(String format, Object... args) {
		log.debug(build(format, args));
	}

	@Override
	public void trace(String format, Object... args) {
		debug(format, args);
	}

	@Override
	public void warn(String format, Object... args) {
		log.warn(build(format, args));
	}

	@Override
	public void error(String format, Object... args) {
		log.error(build(format, args));
	}
	
}
