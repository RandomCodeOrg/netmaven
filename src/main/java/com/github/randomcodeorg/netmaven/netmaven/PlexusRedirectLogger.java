package com.github.randomcodeorg.netmaven.netmaven;

import org.codehaus.plexus.logging.Logger;

public class PlexusRedirectLogger implements InternalLogger {

	private final Logger logger;
	
	private final String format;
	
	public PlexusRedirectLogger(Logger logger, String logFormat) {
		this.logger = logger;
		this.format = logFormat;
	}
	
	public PlexusRedirectLogger(Logger logger){
		this(logger, "%s");
	}

	@Override
	public void info(String format, Object... args) {
		logger.info(build(format, args));
	}

	@Override
	public void debug(String format, Object... args) {
		logger.debug(build(format, args));
	}

	@Override
	public void trace(String format, Object... args) {
		debug(format, args);
	}

	@Override
	public void warn(String format, Object... args) {
		logger.warn(build(format, args));
	}

	@Override
	public void error(String format, Object... args) {
		logger.error(build(format, args));
	}
	
	private String build(String format, Object... args){
		return String.format(String.format(this.format, format), args);
	}
	
	public static InternalLogger named(Logger logger, String name){
		return new PlexusRedirectLogger(logger, String.format("%s: %%s", name));
	}

}
