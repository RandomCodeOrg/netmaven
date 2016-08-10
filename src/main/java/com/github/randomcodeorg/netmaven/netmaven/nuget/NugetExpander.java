package com.github.randomcodeorg.netmaven.netmaven.nuget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class NugetExpander {

	private final Pattern executablePattern = Pattern.compile("lib(/net[0-9]+)?/[^/]+\\.(dll|exe)$"); // Pattern.compile("lib/net[0-9]+/[^/]+\\.(dll|exe)$");
	private final Pattern frameworkPattern = Pattern.compile("^(net[0-9]+\\-)([A-Za-z0-9_\\-.]+)$");
	private final Pattern nuspecPattern = Pattern.compile("[^/]+\\.nuspec$");
	private final Map<NugetArtifact, File> executableArtifacts = new HashMap<>();
	private final Map<NugetArtifact, File> pomArtifacts = new HashMap<>();
	private final RemoteRepository repo;
	private final Logger logger;

	public NugetExpander(RemoteRepository rep, Logger logger) {
		this.repo = rep;
		this.logger = logger;
	}

	public void close() {
		for (File f : executableArtifacts.values())
			f.delete();
		for (File f : pomArtifacts.values())
			f.delete();
		executableArtifacts.clear();
		pomArtifacts.clear();
	}

	public void expand(File tmpFile, ArtifactDownload download) throws ZipException, IOException {
		ZipFile zipFile = new ZipFile(tmpFile);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ZipEntry entry;
		Document doc = null;
		Set<ZipEntry> executables = new HashSet<>();
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();
			logger.debug(String.format("Inspectring entry: %s", entry.getName()));
			doc = handleEntry(zipFile, entry, executables, doc);
		}
		if (doc == null) {
			download.setException(new ArtifactNotFoundException(download.getArtifact(), repo,
					"Could not find the required NuGet specifications (*.nuspec) within the package."));
			return;
		}
		NugetArtifact artifact;
		NuspecInformation nuspecInfo = new NuspecInformation(doc);
		for (ZipEntry e : executables) {
			artifact = buildArtifact(download.getArtifact().getGroupId(), download.getArtifact().getVersion(), e);
			executableArtifacts.put(artifact, writeTemp(zipFile, e));
			logger.debug(String.format("Temporary file for artifact '%s' is: %s", artifact,
					executableArtifacts.get(artifact).getAbsolutePath()));
			Model model = new Model();
			if (e.getName().endsWith(".dll")) {
				model.setPackaging("dll");
			} else if (e.getName().endsWith(".exe")) {
				model.setPackaging("exe");
			}
			model.setModelVersion("4.0.0");
			model.setGroupId(artifact.getGroupId());
			model.setArtifactId(artifact.getArtifactId());
			model.setVersion(artifact.getVersion());
			nuspecInfo.apply(model, logger);
			pomArtifacts.put(artifact, writeTemp(model));
			logger.debug(String.format("Temporary pom for artifact '%s' is: %s", artifact,
					pomArtifacts.get(artifact).getAbsolutePath()));
		}
		zipFile.close();
	}

	public Collection<NugetArtifact> findAlternatives(ArtifactDownload download){
		Map<String, NugetArtifact> result = new HashMap<>();
		for(NugetArtifact a : executableArtifacts.keySet()){
			if(a.getGroupId().equals(download.getArtifact().getGroupId()) && a.getVersion().equals(download.getArtifact().getVersion()))
				if(!result.containsKey(a.toShortString())) result.put(a.toShortString(), a);
		}
		return result.values();
	}
	
	public File getFile(ArtifactDownload download, boolean useStrictMode) {
		NugetArtifact nugetArt;
		Artifact a = download.getArtifact();
		if (frameworkPattern.matcher(a.getArtifactId()).matches()) {
			String[] args = a.getArtifactId().split("\\-");
			nugetArt = new NugetArtifact(a.getGroupId(), args[1], a.getVersion(), args[0]);
		} else {
			nugetArt = new NugetArtifact(download.getArtifact().getGroupId(), download.getArtifact().getArtifactId(),
					download.getArtifact().getVersion(), null);
		}

		if (download.getFile().getName().endsWith(".pom")) {
			logger.debug(String.format("Requesting temporary pom.xml for: %s (%s)", nugetArt,
					download.getFile().getAbsolutePath()));
			return findFile(pomArtifacts, nugetArt, useStrictMode);
		} else {
			logger.debug(String.format("Requesting temporary artifact for: %s (%s)", nugetArt,
					download.getFile().getAbsolutePath()));
			return findFile(executableArtifacts, nugetArt, useStrictMode);
		}
	}

	private File findFile(Map<NugetArtifact, File> map, NugetArtifact a, boolean useStrictMode) {
		if (useStrictMode) {
			if (map.containsKey(a))
				return map.get(a);
			else
				return null;
		} else {
			File result = findFile(map, a, true);
			if (result != null)
				return result;
			logger.debug(String.format("Using yealding search for: %s", a));
			NugetArtifact resultA = null;
			for (NugetArtifact child : map.keySet()) {
				logger.debug(String.format("\tYealding search: Checking '%s'", child));
				if (child.equalsPartial(a)) {
					if (resultA == null) {
						resultA = child;
						result = map.get(child);
					} else {
						if (child.getFrameworkVersion().compareTo(resultA.getFrameworkVersion()) > 0) {
							resultA = child;
							result = map.get(child);
						}
					}
				}
			}
			if (result != null) {
				logger.warn(String.format(
						"Selected framework version '%s' for artifact '%s'. Define the artifactId using the following format to specify the desired framework version: <artifactId>%s-%s</artifactId>",
						resultA.getFrameworkVersion(), resultA.toShortString(), resultA.getFrameworkVersion(),
						resultA.getArtifactId()));

			} else {
				logger.debug(String.format("Temporary artifact not found for: %s", a));
			}
			return result;
		}
	}

	private File writeTemp(Model model) throws IOException {
		File tmpFile = File.createTempFile("tmp", ".pom");
		tmpFile.deleteOnExit();
		ModelWriter writer = new DefaultModelWriter();
		writer.write(tmpFile, null, model);
		return tmpFile;
	}

	private NugetArtifact buildArtifact(String groupId, String version, ZipEntry entry) {
		String name = entry.getName();
		String artifactId = name.substring(name.lastIndexOf("lib/") + 4, name.lastIndexOf("."));
		if (artifactId.contains("/")) {
			String[] args = artifactId.split("/");
			return new NugetArtifact(groupId, args[1], version, args[0]);
		} else {
			return new NugetArtifact(groupId, artifactId, version, null);
		}

	}

	private File writeTemp(ZipFile zip, ZipEntry entry) throws IOException {
		File tmpFile = File.createTempFile("tmp", ".tmp");
		tmpFile.deleteOnExit();
		InputStream in = zip.getInputStream(entry);
		FileOutputStream fos = new FileOutputStream(tmpFile);
		IOUtil.copy(in, fos);
		fos.close();
		in.close();
		return tmpFile;
	}

	private Document handleEntry(ZipFile file, ZipEntry entry, Set<ZipEntry> executables, Document nuget)
			throws IOException {
		String name = entry.getName();
		if (executablePattern.matcher(name).matches()) {
			logger.debug(String.format("'%s' is an executable", entry.getName()));
			executables.add(entry);
		} else if (nuspecPattern.matcher(name).matches()) {
			logger.debug(String.format("'%s' is a nuget specifiaction file", entry.getName()));
			return handleNuspecEntry(file, entry);
		}
		return nuget;
	}

	private Document handleNuspecEntry(ZipFile file, ZipEntry entry) throws IOException {
		InputStream in = file.getInputStream(entry);
		Document doc;
		SAXBuilder builder = new SAXBuilder();
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		try {
			doc = (Document) builder.build(in);
		} catch (JDOMException e) {
			throw new IOException(e);
		}
		return doc;
	}

}
