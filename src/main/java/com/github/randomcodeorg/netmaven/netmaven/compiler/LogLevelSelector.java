package com.github.randomcodeorg.netmaven.netmaven.compiler;

public interface LogLevelSelector {
	
	LogLevel getLevel(boolean isErrOut, String input);

}
