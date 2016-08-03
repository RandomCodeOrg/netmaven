package com.github.randomcodeorg.netmaven.netmaven.compiler;

public class SimpleLogLevelSelector implements LogLevelSelector {

	@Override
	public LogLevel getLevel(boolean isErrOut, String input) {
		if(isErrOut) return LogLevel.ERROR;
		return LogLevel.INFO;
	}

}
