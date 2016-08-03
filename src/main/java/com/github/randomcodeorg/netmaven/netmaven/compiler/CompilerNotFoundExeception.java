package com.github.randomcodeorg.netmaven.netmaven.compiler;

public class CompilerNotFoundExeception extends CompilationException {

	private static final long serialVersionUID = -2255965868994522180L;

	public CompilerNotFoundExeception() {
		super();
	}

	public CompilerNotFoundExeception(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CompilerNotFoundExeception(String message, Throwable cause) {
		super(message, cause);
	}

	public CompilerNotFoundExeception(String message) {
		super(message);
	}

	public CompilerNotFoundExeception(Throwable cause) {
		super(cause);
	}
	
	

}
