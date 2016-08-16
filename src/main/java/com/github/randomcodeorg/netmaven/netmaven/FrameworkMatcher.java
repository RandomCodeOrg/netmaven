package com.github.randomcodeorg.netmaven.netmaven;

public class FrameworkMatcher<T> {

	public enum Strategy {
		STRICT, ALLOW_LOWER, ALLOW_HIGHER
	}

	private final Strategy strategy;

	private FrameworkVersion bestVersion = null;
	private T bestAssociated = null;

	private FrameworkVersion desiredVersion;
	
	public FrameworkMatcher(Strategy strategy, FrameworkVersion desired) {
		this.strategy = strategy;
		this.desiredVersion = desired;
	}

	public void accept(FrameworkVersion version, T associated) {
		switch (strategy) {
		case STRICT:
			acceptStrict(version, associated);
			return;
		case ALLOW_LOWER:
			acceptLower(version, associated);
			return;
		case ALLOW_HIGHER:
			acceptHigher(version, associated);
		}
	}
	
	public void accept(Iterable<FrameworkVersion> all){
		for(FrameworkVersion v : all) accept(v, null);
	}

	public boolean hasMatch(){
		return bestVersion != null;
	}
	
	public FrameworkVersion getVersion(){
		return bestVersion;
	}
	
	public T getAssociated(){
		return bestAssociated;
	}
	
	public void reset(){
		bestAssociated = null;
		bestVersion = null;
	}
	
	protected void acceptHigher(FrameworkVersion version, T associated) {
		if(version.compareTo(desiredVersion) < 0) return;
		if(bestVersion == null){
			bestVersion = version;
			bestAssociated = associated;
			return;
		}
		if(version.compareTo(bestVersion) < 0){
			bestVersion = version;
			bestAssociated = associated;
		}
	}
	
	protected void acceptLower(FrameworkVersion version, T associated) {
		if(version.compareTo(desiredVersion) > 0) return;
		if(bestVersion == null){
			bestVersion = version;
			bestAssociated = associated;
			return;
		}
		if(version.compareTo(bestVersion) > 0){
			bestVersion = version;
			bestAssociated = associated;
		}
	}
	
	protected void acceptStrict(FrameworkVersion version, T associated) {
		if(bestVersion != null) return;
		if(desiredVersion.equals(version)){
			bestVersion = version;
			bestAssociated = associated;
		}
	}

}
