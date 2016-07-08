package com.github.randomcodeorg.netmaven.netmaven.compiler;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

	private final List<String> args = new ArrayList<>();
	
	public CommandBuilder() {
		
	}
	
	public void add(String... args){
		for(String a : args) this.args.add(a);
	}
	
	public void add(Iterable<String> args){
		for(String a : args) this.args.add(a);
	}
	
	public String[] build(){
		return args.toArray(new String[args.size()]);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<args.size(); i++){
			if(i > 0) sb.append(" ");
			sb.append(args.get(i));
		}
		return sb.toString();
	}
	
}
