package com.github.randomcodeorg.netmaven.netmaven.nuget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.MetadataTransferException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NugetRepositoryConnector implements RepositoryConnector {

	private static final String FORMAT = "package/%s/%s";
	private static final String META_FORMAT = "package-versions/%s";

	private final String repoUrl;
	private final RemoteRepository repo;
	private final HttpClient httpClient;
	private final Logger logger;
	private final NugetExpander expander;

	public NugetRepositoryConnector(RemoteRepository repo, Logger logger) {
		this.repoUrl = repo.getUrl();
		this.repo = repo;
		this.httpClient = HttpClientBuilder.create().build();
		this.logger = logger;
		this.expander = new NugetExpander(repo, logger);
	}

	@Override
	public void get(Collection<? extends ArtifactDownload> artifactDownloads,
			Collection<? extends MetadataDownload> metadataDownloads) {
		if (artifactDownloads != null) {
			for (ArtifactDownload download : artifactDownloads) {
				logger.debug(String.format("Requesting download for: %s:%s (%s)", download.getArtifact().getGroupId(),
						download.getArtifact().getArtifactId(), download.getFile().getAbsolutePath()));
				downloadArtifact(download);
			}
		}
		if (metadataDownloads != null) {
			for (MetadataDownload download : metadataDownloads) {
				logger.debug(String.format("Requesting metadata for: %s:%s (%s)", download.getMetadata().getGroupId(),
						download.getMetadata().getArtifactId(), download.getFile().getAbsolutePath()));
				downloadMetadata(download);
			}
		}
	}

	@Override
	public void put(Collection<? extends ArtifactUpload> artifactUploads,
			Collection<? extends MetadataUpload> metadataUploads) {
		throw new RuntimeException("Not supported!");
	}

	protected void downloadArtifact(ArtifactDownload download) {
		File f = expander.getFile(download, true);
		if (f != null) {
			logger.debug(String.format("Found temporary artifact for: %s", download.getArtifact()));
			try {
				assertExists(download.getFile());
				OutputStream out = new FileOutputStream(download.getFile());
				InputStream in = new FileInputStream(f);
				logger.debug(String.format("Copying from '%s' to '%s'...", f.getAbsolutePath(),
						download.getFile().getAbsolutePath()));
				IOUtil.copy(in, out);
				in.close();
				out.close();
				return;
			} catch (IOException e) {
				download.setException(new ArtifactTransferException(download.getArtifact(), repo, e));
				return;
			}
		} else {

			doFetchArtifact(download);
		}
	}

	protected void downloadMetadata(MetadataDownload download) {
		try {
			String url = buildMetaUrl(download);
			HttpResponse response = httpClient.execute(new HttpGet(url));
			HttpEntity entitiy = response.getEntity();
			if (response.getStatusLine().getStatusCode() != 200) {
				download.setException(new MetadataNotFoundException(download.getMetadata(), repo));
				EntityUtils.consume(entitiy);
				return;
			}
			Gson gson = new GsonBuilder().create();
			InputStreamReader reader = new InputStreamReader(entitiy.getContent());
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[1024];
			int read;
			while ((read = reader.read(buffer)) != -1) {
				sb.append(buffer, 0, read);
			}
			reader.close();
			String[] versions = gson.fromJson(sb.toString(), String[].class);
			Metadata m = new Metadata();
			m.setGroupId(download.getMetadata().getGroupId());
			m.setArtifactId(download.getMetadata().getArtifactId());
			if (m.getVersioning() == null)
				m.setVersioning(new Versioning());
			m.getVersioning().setLastUpdatedTimestamp(new Date());
			if (m.getVersioning().getVersions() == null)
				m.getVersioning().setVersions(new ArrayList<String>());
			for (String v : versions)
				m.getVersioning().getVersions().add(v);
			assertExists(download.getFile());
			OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(download.getFile()),
					StandardCharsets.UTF_8);
			MetadataXpp3Writer writer = new MetadataXpp3Writer();
			writer.write(fileWriter, m);
			fileWriter.close();
		} catch (IOException e) {
			download.setException(new MetadataTransferException(download.getMetadata(), repo, e));
		}
	}

	private void doFetchArtifact(ArtifactDownload download) {
		try {
			Artifact a = download.getArtifact();
			String url = buildUrl(a);
			logger.debug(String.format("Fetchig artifact '%s' from: %s", a, url));
			HttpResponse response = httpClient.execute(new HttpGet(url));
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() != 200) {
				download.setException(new ArtifactNotFoundException(a, repo));
				logger.warn(String.format(
						"Could not fetch the nuget artifact '%s' because the server returned an unexpected result code: %d - %s",
						a, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
				EntityUtils.consume(entity);
				return;
			}
			if (entity != null) {
				InputStream in = entity.getContent();
				File tmpFile = File.createTempFile("tmp", ".nupkg");
				logger.debug(String.format("Writing temporary file: %s", tmpFile.getAbsolutePath()));
				tmpFile.deleteOnExit();
				OutputStream out = new FileOutputStream(tmpFile);
				IOUtil.copy(in, out);
				out.close();
				in.close();
				expander.expand(tmpFile, download);
				tmpFile.delete();
				tmpFile = expander.getFile(download, false);
				if (tmpFile == null) {
					Collection<NugetArtifact> alternatives = expander.findAlternatives(download);
					if (!alternatives.isEmpty()) {
						logger.error("");
						logger.error(String.format(
								"The nuget dependency '%s' could not be located. Did you mean one of the following dependencies?",
								a.toString()));
						for (NugetArtifact alt : alternatives) {
							logger.error(String.format("\t%s", alt.toShortString()));
						}
						logger.error("");
						download.setException(new ArtifactNotFoundException(a, repo,
								"Could not find the dependency for the given artifactId. Please refer to the build output to get details about available alternatives."));
					} else {
						download.setException(new ArtifactNotFoundException(a, repo));
					}
					return;
				}
				in = new FileInputStream(tmpFile);
				assertExists(download.getFile());
				out = new FileOutputStream(download.getFile());
				IOUtil.copy(in, out);
				in.close();
				out.close();
			}
		} catch (IOException e) {
			download.setException(
					new ArtifactTransferException(download.getArtifact(), repo, "Could not obtain the artifact.", e));
		}
	}

	private void assertExists(File f) throws IOException {
		if (f.exists())
			return;
		File parent = f.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		f.createNewFile();
	}

	private String buildMetaUrl(MetadataDownload download) throws UnsupportedEncodingException {
		String url = repoUrl;
		if (!repoUrl.endsWith("/"))
			url += "/";
		url += String.format(META_FORMAT, URLEncoder.encode(download.getMetadata().getGroupId(), "UTF-8"));
		return url;
	}

	private String buildUrl(Artifact artifact) throws UnsupportedEncodingException {
		String url = repoUrl;
		if (!repoUrl.endsWith("/"))
			url += "/";
		url += String.format(FORMAT, URLEncoder.encode(artifact.getGroupId(), "UTF-8"),
				URLEncoder.encode(artifact.getVersion(), "UTF-8"));
		return url;
	}

	@Override
	public void close() {
		logger.debug("Closing the NugetRepositoryConnector");
		expander.close();
	}

}
