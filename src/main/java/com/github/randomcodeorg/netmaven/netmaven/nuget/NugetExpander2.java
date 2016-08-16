package com.github.randomcodeorg.netmaven.netmaven.nuget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.artifact.Artifact;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.github.randomcodeorg.netmaven.netmaven.FrameworkMatcher;
import com.github.randomcodeorg.netmaven.netmaven.FrameworkVersion;
import com.github.randomcodeorg.netmaven.netmaven.InternalLogger;

public class NugetExpander2 {

	private final InternalLogger logger;
	private static final Pattern NUSPEC_PATTERN = Pattern.compile("[^/]+\\.nuspec$");
	private static final Pattern DLL_PATTERN = Pattern.compile("^lib(\\/net[1-9](0|5))?\\/[^/]+\\.dll$");
	private static final Pattern INDEPENDENT_PATTERN = Pattern.compile("^lib\\/[^/]+\\.dll$");

	private static final String DEPENDENT_PATTERN_FORMAT = "^lib\\/%s\\/[^/]+\\.dll$";

	public NugetExpander2(InternalLogger logger) {
		this.logger = logger;
	}

	public void expandDlls(FrameworkVersion version, File src, File targetDir, Collection<File> files)
			throws IOException {
		if (!src.exists() || !src.isFile())
			return;
		if (targetDir.exists() && !targetDir.isDirectory())
			throw new IllegalArgumentException("The given target path does not point to a directory.");
		if (!targetDir.exists())
			targetDir.mkdirs();
		Set<ZipEntry> entries = new HashSet<>();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(src);
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			ZipEntry entry;
			while (zipEntries.hasMoreElements()) {
				entry = zipEntries.nextElement();
				if (isDll(zipFile, entry))
					entries.add(entry);
			}
			Set<ZipEntry> independent = getIndependent(entries);
			Set<FrameworkVersion> frameworkVersions = getFrameworkVersions(entries);
			FrameworkMatcher<Void> matcher = new FrameworkMatcher<>(FrameworkMatcher.Strategy.ALLOW_LOWER, version);
			matcher.accept(frameworkVersions);
			Set<ZipEntry> dependent = new HashSet<>();
			if (matcher.hasMatch()) {
				getDependent(entries, matcher.getVersion(), dependent);
			}
			expand(zipFile, independent, targetDir, files);
			expand(zipFile, dependent, targetDir, files);
		} finally {
			zipFile.close();
		}
	}

	protected void expand(ZipFile file, Iterable<ZipEntry> entries, File targetDir, Collection<File> files)
			throws IOException {
		for (ZipEntry entry : entries) {
			expand(file, entry, targetDir, files);
		}
	}

	protected void expand(ZipFile file, ZipEntry entry, File targetDir, Collection<File> files) throws IOException {
		String name = entry.getName();
		name = name.substring(name.lastIndexOf("/") + 1);
		File destFile = new File(targetDir, name);
		if (destFile.exists())
			destFile.delete();
		destFile.createNewFile();
		FileOutputStream fos = null;
		InputStream in = null;
		try {
			fos = new FileOutputStream(destFile);
			in = file.getInputStream(entry);
			IOUtil.copy(in, fos);
			files.add(destFile);
		} finally {
			if (fos != null)
				fos.close();
			if (in != null)
				in.close();
		}
	}

	public void getDependent(Set<ZipEntry> all, FrameworkVersion selectedVersion, Set<ZipEntry> result) {
		Pattern pattern = Pattern.compile(String.format(DEPENDENT_PATTERN_FORMAT, selectedVersion.getLazyFormat()));
		for (ZipEntry e : all) {
			if (pattern.matcher(e.getName()).matches())
				result.add(e);
		}
	}

	protected Set<FrameworkVersion> getFrameworkVersions(Set<ZipEntry> all) {
		Set<String> versions = new HashSet<>();
		String tmp;
		for (ZipEntry entry : all) {
			tmp = entry.getName();
			if (INDEPENDENT_PATTERN.matcher(tmp).matches())
				continue;
			tmp = tmp.substring("lib/".length(), "lib/".length() + "netXX".length());
			versions.add(tmp);
		}
		Set<FrameworkVersion> result = new HashSet<>();
		FrameworkVersion v;
		for (String versionString : versions) {
			try {
				v = FrameworkVersion.parse(versionString);
				result.add(v);
			} catch (ParseException e) {
				logger.debug("Unknown framework version: %s", versionString);
			}
		}
		return result;
	}

	protected Set<ZipEntry> getIndependent(Set<ZipEntry> all) {
		Set<ZipEntry> result = new HashSet<>();
		for (ZipEntry entry : all) {
			if (INDEPENDENT_PATTERN.matcher(entry.getName()).matches())
				result.add(entry);
		}
		return result;
	}

	protected boolean isDll(ZipFile file, ZipEntry entry) {
		return DLL_PATTERN.matcher(entry.getName()).matches();
	}

	public void expandPom(Artifact a, File src, File target) throws IOException {
		NuspecInformation info = readNuspec(src);
		Model model = new Model();
		model.setModelVersion("4.0.0");
		model.setGroupId(a.getGroupId());
		model.setArtifactId(a.getArtifactId());
		model.setVersion(a.getVersion());
		info.apply(model, logger);
		model.setPackaging("nuget");
		ModelWriter writer = new DefaultModelWriter();
		writer.write(target, null, model);
		return;
	}

	protected NuspecInformation readNuspec(File src) throws IOException {
		ZipFile zipFile = new ZipFile(src);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ZipEntry entry;
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();
			if (NUSPEC_PATTERN.matcher(entry.getName()).matches()) {
				InputStream in = zipFile.getInputStream(entry);
				try {
					Document doc;
					SAXBuilder builder = new SAXBuilder();
					XMLOutputter xmlOutput = new XMLOutputter();
					xmlOutput.setFormat(Format.getPrettyFormat());
					doc = (Document) builder.build(in);
					return new NuspecInformation(doc);
				} catch (JDOMException e) {
					throw new IOException(e);
				} finally {
					in.close();
					zipFile.close();
				}
			}
		}
		zipFile.close();
		throw new IOException("Could not locate the nuget specification (.nuspec) entry.");
	}

}
