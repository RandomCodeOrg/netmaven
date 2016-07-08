package com.github.randomcodeorg.netmaven.netmaven.compiler;

public class CompilationException extends RuntimeException {

	private static final long serialVersionUID = 1768659311046174931L;

	public CompilationException() {
	}

	public CompilationException(String message) {
		super(message);
	}

	public CompilationException(Throwable cause) {
		super(cause);
	}

	public CompilationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CompilationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
