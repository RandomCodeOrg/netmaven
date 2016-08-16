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

import com.github.randomcodeorg.netmaven.netmaven.InternalLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NugetRepositoryConnector2 implements RepositoryConnector {

	private static final String FORMAT = "package/%s/%s";
	private static final String META_FORMAT = "package-versions/%s";

	private final RemoteRepository repo;
	private final InternalLogger logger;
	private final HttpClient httpClient;
	private final NugetCache cache = new NugetCache();
	private final NugetExpander2 expander;

	public NugetRepositoryConnector2(RemoteRepository repo, InternalLogger logger) {
		this.logger = logger;
		this.repo = repo;
		this.httpClient = HttpClientBuilder.create().build();
		this.expander = new NugetExpander2(logger);
	}

	@Override
	public void get(Collection<? extends ArtifactDownload> artifactDownloads,
			Collection<? extends MetadataDownload> metadataDownloads) {
		if (artifactDownloads != null) {
			for (ArtifactDownload d : artifactDownloads)
				obtainArtifact(d.getArtifact(), d);
		}
		if (metadataDownloads != null) {
			for (MetadataDownload download : metadataDownloads)
				obtainMetadata(download);
		}
	}

	@Override
	public void put(Collection<? extends ArtifactUpload> artifactUploads,
			Collection<? extends MetadataUpload> metadataUploads) {
		throw new RuntimeException("Not supported!");
	}

	@Override
	public void close() {
		cache.close();
	}

	protected void obtainMetadata(MetadataDownload download) {
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
			if (!download.getFile().exists())
				download.getFile().createNewFile();
			OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(download.getFile()),
					StandardCharsets.UTF_8);
			MetadataXpp3Writer writer = new MetadataXpp3Writer();
			writer.write(fileWriter, m);
			fileWriter.close();
		} catch (IOException e) {
			download.setException(new MetadataTransferException(download.getMetadata(), repo, e));
		}
	}

	protected void obtainArtifact(Artifact a, ArtifactDownload download) {
		try {
			String url = buildUrl(a);
			File target = download.getFile();
			if (!cache.contains(url)) {
				downloadNuget(url, cache, a);
			}
			if (target.getName().endsWith(".pom")) {
				expander.expandPom(a, cache.get(url), target);
			} else {
				FileOutputStream fos = new FileOutputStream(target);
				FileInputStream fin = new FileInputStream(cache.get(url));
				IOUtil.copy(fin, fos);
				fin.close();
				fos.close();
			}
		} catch (IOException e) {
			download.setException(new ArtifactTransferException(a, repo, "Could not obtain the artifact.", e));
		} catch (ArtifactTransferException e) {
			download.setException(e);
			return;
		}
	}

	protected void downloadNuget(String url, NugetCache cache, Artifact a) throws ArtifactTransferException {
		try {
			HttpResponse response = httpClient.execute(new HttpGet(url));
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() != 200 || entity == null) {
				logger.warn(
						"Could not fetch the nuget artifact '%s' because the server returned an unexpected result code: %d - %s",
						a, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
				EntityUtils.consume(entity);
				throw new ArtifactNotFoundException(a, repo);
			}
			InputStream in = entity.getContent();
			File tmpFile = File.createTempFile("tmp", ".nuget");
			tmpFile.deleteOnExit();
			if (!tmpFile.exists())
				tmpFile.createNewFile();
			OutputStream out = new FileOutputStream(tmpFile);
			IOUtil.copy(in, out);
			out.close();
			in.close();
			cache.put(url, tmpFile);
		} catch (IOException e) {
			throw new ArtifactTransferException(a, repo, "Could not obtain the artifact.", e);
		}
	}

	protected String buildUrl(Artifact artifact) throws UnsupportedEncodingException {
		String url = repo.getUrl();
		if (!url.endsWith("/"))
			url += "/";
		url += String.format(FORMAT, URLEncoder.encode(artifact.getGroupId(), "UTF-8"),
				URLEncoder.encode(artifact.getVersion(), "UTF-8"));
		return url;
	}

	protected String buildMetaUrl(MetadataDownload download) throws UnsupportedEncodingException {
		String url = repo.getUrl();
		if (!url.endsWith("/"))
			url += "/";
		url += String.format(META_FORMAT, URLEncoder.encode(download.getMetadata().getGroupId(), "UTF-8"));
		return url;
	}

}
