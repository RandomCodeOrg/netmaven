package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.util.regex.Pattern;

public class RegexLogLevelSelector implements LogLevelSelector {

	private final Pattern warningPattern;
	private final Pattern errorPattern;
	
	
	public RegexLogLevelSelector(String errorPattern, String warningPattern) {
		this.warningPattern = Pattern.compile(warningPattern);
		this.errorPattern = Pattern.compile(errorPattern);
	}
	
	@Override
	public LogLevel getLevel(boolean isErrOut, String input) {
		if(errorPattern.matcher(input).matches()) return LogLevel.ERROR;
		if(warningPattern.matcher(input).matches()) return LogLevel.WARN;
		return LogLevel.INFO;
	}
	
	

}
