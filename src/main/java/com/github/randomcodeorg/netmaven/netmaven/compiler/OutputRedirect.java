package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.io.InputStream;
import java.util.Scanner;

import org.apache.maven.plugin.logging.Log;

public class OutputRedirect {

	private boolean isError;
	private Process process;
	private final Log log;

	private final Object lockObject = new Object();
	private boolean completed = false;
	private final LogLevelSelector levelSelector;

	public OutputRedirect(Process p, Log log, boolean isError, LogLevelSelector selector) {
		this.isError = isError;
		this.process = p;
		this.log = log;
		this.levelSelector = selector;
	}

	public OutputRedirect start() {
		new Thread(new Runnable() {

			public void run() {
				if (isError) {
					executeError();
				} else {
					executeOutput();
				}
				complete();
			}
		}).start();
		return this;
	}

	private void complete() {
		synchronized (lockObject) {
			completed = true;
			lockObject.notifyAll();
		}
	}

	private void executeError() {
		executeOutput();
	}

	private void executeOutput() {
		InputStream in;
		if (isError)
			in = process.getErrorStream();
		else
			in = process.getInputStream();
		Scanner sc = new Scanner(in);
		String line;
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			switch (levelSelector.getLevel(isError, line)) {
			case ERROR:
				log.error(line);
				break;
			case INFO:
				log.info(line);
				break;
			case WARN:
				log.warn(line);
				break;
			}
		}
		sc.close();
	}

	public void waitFor() {
		synchronized (lockObject) {
			if (completed == true)
				return;
			try {
				lockObject.wait();
			} catch (InterruptedException e) {
			}
		}
	}

}
