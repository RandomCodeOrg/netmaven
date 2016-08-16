package com.github.randomcodeorg.netmaven.netmaven;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FrameworkVersion implements Comparable<FrameworkVersion> {

	/**
	 * Matches all version definitions like: 4.4, 3.2.1, 3
	 */
	private static final Pattern FULL_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)*$");

	/**
	 * Matches all version definitions like: net40, net35
	 */
	private static final Pattern LAZY_PATTERN = Pattern.compile("^net[1-9](0|5)");

	private final List<Integer> components = new ArrayList<>();

	public FrameworkVersion(int... components) {
		for (int i : components) {
			if (i < 0)
				throw new IllegalArgumentException("An version component must not be negative. Actual value: " + i);
			this.components.add(i);
		}
	}

	public int countComponents() {
		return components.size();
	}

	public int get(int componentIndex){
		if(componentIndex < 0) throw new IndexOutOfBoundsException();
		if(componentIndex >= components.size()) return 0;
		return components.get(componentIndex);
	}
	
	public static FrameworkVersion parse(String value) throws ParseException {
		if (value == null)
			throw new IllegalArgumentException("The value to be parsed must not be null.");
		if (FULL_PATTERN.matcher(value).matches()) {
			return parseFullPattern(value);
		} else if (LAZY_PATTERN.matcher(value).matches()) {
			return parseLazyPattern(value);
		}
		throw new ParseException("The given version string has an unknown format.", 0);
	}

	private static FrameworkVersion parseFullPattern(String value) throws ParseException {
		String parts[] = value.split("\\.");
		int[] components = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
			components[i] = Integer.parseInt(parts[i]);
		return new FrameworkVersion(components);
	}

	private static FrameworkVersion parseLazyPattern(String value) throws ParseException {
		int major = Integer.parseInt(value.charAt(3) + ""); 
		int minor = Integer.parseInt(value.charAt(4) + ""); 
		return new FrameworkVersion(major, minor);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<components.size(); i++){
			if(i>0) sb.append(".");
			sb.append(components.get(i));
		}
		return sb.toString();
	}
	
	public int countMatches(FrameworkVersion other){
		int maxComponents = countComponents();
		if(other.countComponents() > maxComponents) maxComponents = other.countComponents();
		int result = 0;
		for(int i=0; i<maxComponents; i++){
			if(get(i)==other.get(i)) result++; else break;
		}
		return result;
	}

	@Override
	public int compareTo(FrameworkVersion other) {
		if(other == null) throw new IllegalArgumentException("Cannot compare to null.");
		int maxComponents = countComponents();
		if(other.countComponents() > maxComponents) maxComponents = other.countComponents();
		int tmp;
		Integer a;
		Integer b;
		for(int i=0; i<maxComponents; i++){
			a = get(i);
			b = other.get(i);
			tmp = a.compareTo(b);
			if(tmp != 0) return tmp;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(obj == this) return true;
		if(!(obj instanceof FrameworkVersion)) return false;
		return equals((FrameworkVersion) obj);
	}
	
	public boolean equals(FrameworkVersion v) {
		if(v == null) return false;
		return compareTo(v) == 0;
	}
	
	public String getLazyFormat(){
		return String.format("net%d%d", get(0), get(1));
	}
	
	
}
