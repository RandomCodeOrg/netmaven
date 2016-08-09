package com.github.randomcodeorg.netmaven.netmaven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.github.randomcodeorg.netmaven.netmaven.compiler.PathBuilder;

@Mojo(name = "setup", defaultPhase = LifecyclePhase.VALIDATE, inheritByDefault = false)
public class NetMavenSetupMojo extends AbstractNetMavenMojo {

	private static final String IKVM_DOWNLOAD_DIR = "https://github.com/RandomCodeOrg/netmaven/blob/master/bin/ikvm.zip?raw=true";

	public NetMavenSetupMojo() {
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		setupIKVMLibrary(getLog());
	}

	protected void setupIKVMLibrary(Log log) throws MojoExecutionException {
		String ikvmHome = getIKVMLibraryPath();
		if (ikvmHome != null) {
			log.info("The IKVM library is already present at '" + ikvmHome + "'.");
			return;
		}
		String ikvmPath = getDefaultIKVMPath();
		File ikvmFile = new File(ikvmPath);
		if (!ikvmFile.exists()) {
			log.info("Creating IKVM home directory at '" + ikvmPath + "'");
			ikvmFile.mkdirs();
		}
		File tmpFile = dowanloadIKVM(log);
		extractIKVM(log, tmpFile, ikvmFile);
		log.info("Deleting the temporary file");
		tmpFile.delete();

		log.info("");
		printLicenseAndDisclaimerInfo(log, ikvmFile.getAbsolutePath());
		log.info("");

		log.info("");
		log.info("Setup completed!");
		log.info("");
	}

	protected void printLicenseAndDisclaimerInfo(Log log, String path) {
		log.warn(
				"\nThe IKVM library is an external tool that can be used to access Java dependencies from the .NET-Framework."
						+ String.format(
								"Please refer to the license file at '%s' to get more information about the terms and conditions",
								PathBuilder.create(path).sub("LICENSE").build())
						+ " regarding the IKVM library and its dependencies."
						+ " The transformations which are applied by IKVM, might be a forbidden manipulation of third party property."
						+ " Please check the license descriptions of all (Java) dependencies that will or might be transformed by IKVM.\n\n"
						+ "DO NOT USE IKVM OR ANY JAVA DEPENDENCY IF YOU DON'T AGREE WITH THE REFERED LICENSES OR RESTRICTIONS.");
	}

	protected void extractIKVM(Log log, File tmpFile, File targetDir) throws MojoExecutionException {
		try {
			log.info("Extracting the IKVM library...");
			ZipFile zipFile = new ZipFile(tmpFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			ZipEntry entry;
			String[] parts;
			PathBuilder currentTarget;
			while (entries.hasMoreElements()) {
				entry = entries.nextElement();
				parts = entry.getName().split("/");
				if (parts.length < 2 || parts[1].isEmpty())
					continue;
				currentTarget = PathBuilder.create(targetDir.getAbsolutePath());
				for (int i = 1; i < parts.length; i++) {
					currentTarget.sub(parts[i]);
				}
				if (entry.isDirectory()) {
					if (!new File(currentTarget.build()).exists())
						new File(currentTarget.build()).mkdirs();
				} else {
					extract(log, zipFile, entry, currentTarget);
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not extract the IKVM zip file because an I/O error occured.", e);
		}

	}

	protected void extract(Log log, ZipFile zipFile, ZipEntry entry, PathBuilder target) throws IOException {
		log.debug(String.format("Extracting '%s' to '%s'", entry.getName(), target.build()));
		File targetFile = new File(target.build());
		if (!targetFile.getParentFile().exists())
			targetFile.getParentFile().mkdirs();
		InputStream in = zipFile.getInputStream(entry);
		ReadableByteChannel rbc = Channels.newChannel(in);
		FileOutputStream fos = new FileOutputStream(targetFile);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.flush();
		fos.close();
		rbc.close();
		in.close();
		log.debug(String.format("Completed extracting '%s'", entry.getName()));

	}

	protected File dowanloadIKVM(Log log) throws MojoExecutionException {
		try {
			log.info("Downloading the IKVM executables...");
			File tmpFile = File.createTempFile("mvn", ".zip");
			log.debug("Temporary file will be at '" + tmpFile.getAbsolutePath() + "'");
			tmpFile.deleteOnExit();
			URL resource = new URL(IKVM_DOWNLOAD_DIR);
			ReadableByteChannel rbc = Channels.newChannel(resource.openStream());
			FileOutputStream fos = new FileOutputStream(tmpFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.flush();
			fos.close();
			rbc.close();
			log.info("Download completed!");
			return tmpFile;
		} catch (IOException e) {
			log.error("An I/O error occured during the download of the IKVM library.", e);
			throw new MojoExecutionException("AnI/O error occured during the download of the IKVM library.", e);
		}
	}

}
