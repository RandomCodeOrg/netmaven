package com.github.randomcodeorg.netmaven.netmaven.nuget;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NugetCache {
	
	private final Map<String, File> store = new HashMap<>();
	
	public NugetCache() {
		
	}
	
	public void put(String origin, File f){
		store.put(origin, f);
	}
	
	public boolean contains(String origin){
		return store.containsKey(origin);
	}
	
	public File get(String origin){
		return store.get(origin);
	}
	
	public void close(){
		for(File f : store.values()){
			if(f.exists()) f.delete();
		}
	}

}
