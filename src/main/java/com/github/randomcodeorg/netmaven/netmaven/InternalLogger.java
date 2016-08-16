package com.github.randomcodeorg.netmaven.netmaven;

public interface InternalLogger {

	void info(String format, Object... args);
	void debug(String format, Object... args);
	void trace(String format, Object... args);
	void warn(String format, Object... args);
	void error(String format, Object... args);
}
