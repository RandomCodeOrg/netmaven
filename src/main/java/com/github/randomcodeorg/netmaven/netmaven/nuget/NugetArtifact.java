package com.github.randomcodeorg.netmaven.netmaven.nuget;

import com.google.common.base.Objects;

public class NugetArtifact {

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String frameworkVersion;

	
	public NugetArtifact(String groupId, String artifactId, String version, String frameworkVersion) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.frameworkVersion = frameworkVersion;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getFrameworkVersion() {
		return frameworkVersion;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof NugetArtifact) {
			NugetArtifact other = (NugetArtifact) obj;
			return Objects.equal(groupId, other.groupId) && Objects.equal(artifactId, other.artifactId)
					&& Objects.equal(version, other.version) && Objects.equal(frameworkVersion, other.frameworkVersion);
		}
		return super.equals(obj);
	}
	
	public boolean equalsPartial(NugetArtifact a){
		return Objects.equal(groupId, a.groupId) && Objects.equal(artifactId, a.artifactId) && Objects.equal(version, a.version);
	}

	@Override
	public String toString() {
		return String.format("%s:%s-%s:%s", groupId, frameworkVersion, artifactId, version);
	}

	public String toShortString(){
		return String.format("%s:%s:%s", groupId, artifactId, version);
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
