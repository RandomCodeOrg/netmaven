package com.github.randomcodeorg.netmaven.netmaven.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;

public class ProjectConfig {

	private final Document document;
	private static final String ITEM_GROUP_TAG = "ItemGroup";
	private static final String REFERENCE_TAG = "Reference";
	private static final String HINT_PATH_TAG = "HintPath";
	private static final String INCLUDE_ATTR = "Include";
	//private static final String PROJECT_TAG = "Project";
	private boolean replaceSeparators = true;
	private final Element project;

	private final Map<String, Reference> newReferences = new HashMap<>();
	private final Map<String, Reference> toEdit = new HashMap<>();

	public ProjectConfig(Document doc) {
		this.document = doc;
		this.project = doc.getRootElement();
	}

	public Document getDocument(){
		return document;
	}
	
	public void setReplaceSeparators(boolean value) {
		this.replaceSeparators = value;
	}

	protected Set<Element> getItemGroups() {
		Set<Element> nodes = new HashSet<>();
		Element child;
		for (Object childO : project.getChildren()) {
			if (!(childO instanceof Element))
				continue;
			child = (Element) childO;
			if (ITEM_GROUP_TAG.equalsIgnoreCase(child.getName())) {
				nodes.add(child);
			}
		}
		return nodes;
	}

	protected Map<Element, Set<Element>> getReferences() {
		Map<Element, Set<Element>> result = new HashMap<>();
		Set<Element> references;
		for (Element itemGroup : getItemGroups()) {
			references = new HashSet<>();
			Element child;
			for (Object childO : itemGroup.getChildren()) {
				if (!(childO instanceof Element))
					continue;
				child = (Element) childO;
				if (REFERENCE_TAG.equalsIgnoreCase(child.getName()))
					references.add(child);
			}
			if (!references.isEmpty())
				result.put(itemGroup, references);
		}
		return result;
	}

	protected Element getReferenceElement(String reference) {
		Map<Element, Set<Element>> references = getReferences();
		for (Element itemGroup : references.keySet()) {
			if (isConditional(itemGroup))
				continue;
			for (Element ref : references.get(itemGroup)) {
				if (reference.equalsIgnoreCase(ref.getAttributeValue(INCLUDE_ATTR)))
					return ref;
			}
		}
		return null;
	}

	protected Reference getExistingReference(String reference) {
		Element referenceElement = getReferenceElement(reference);
		if (referenceElement == null)
			return null;
		return toReference(referenceElement);
	}

	protected Reference toReference(Element referenceElement) {
		String referenced = referenceElement.getAttributeValue(INCLUDE_ATTR);
		String hintPath = null;
		Element child;
		for (Object childO : referenceElement.getChildren()) {
			if (!(childO instanceof Element))
				continue;
			child = (Element) childO;
			if (HINT_PATH_TAG.equalsIgnoreCase(child.getName())) {
				hintPath = child.getText();
				break;
			}
		}
		return new Reference(referenced, hintPath);
	}

	protected boolean isConditional(Element e) {
		return e.getAttribute("Condition") != null;
	}

	public Reference getReference(String reference) {
		if (newReferences.containsKey(reference))
			return newReferences.get(reference);
		if (toEdit.containsKey(reference))
			return toEdit.get(reference);
		return getExistingReference(reference);
	}

	public boolean hasReference(String reference) {
		return getReference(reference) != null;
	}

	public boolean hasReference(String reference, String hintPath) {
		Reference ref = getReference(reference);
		if (ref == null)
			return false;
		if (!ref.hasHintPath())
			return false;
		return ref.getHintPath().equalsIgnoreCase(hintPath);
	}

	public void setReference(Reference reference) {
		Reference existing = getExistingReference(reference.getReferenced());
		if (existing == null) {
			newReferences.put(reference.getReferenced(), reference);
		} else {
			toEdit.put(reference.getReferenced(), reference);
		}
	}

	protected Element getReferencesItemGroup() {
		Map<Element, Set<Element>> references = getReferences();
		for (Element itemGroup : references.keySet()) {
			if (!isConditional(itemGroup))
				return itemGroup;
		}
		Element itemGroup = new Element(ITEM_GROUP_TAG);
		itemGroup.setNamespace(project.getNamespace());
		project.addContent(itemGroup);
		return itemGroup;
	}

	public void save() {
		Element refElement;
		Element hintElement;
		for (Reference ref : toEdit.values()) {
			refElement = getReferenceElement(ref.getReferenced());
			hintElement = null;
			Element child;
			for (Object childO : refElement.getChildren()) {
				if (!(childO instanceof Element))
					continue;
				child = (Element) childO;
				if (HINT_PATH_TAG.equals(child.getName())) {
					hintElement = child;
					break;
				}
			}
			if (ref.hasHintPath()) {
				if (hintElement != null)
					hintElement.setText(ref.getHintPath(replaceSeparators));
				else {
					hintElement = new Element(HINT_PATH_TAG, project.getNamespace());
					hintElement.setText(ref.getHintPath(replaceSeparators));
					refElement.addContent(hintElement);
				}
			} else {
				if (hintElement != null)
					hintElement.getParent().removeContent(hintElement);
			}
		}
		toEdit.clear();
		Element itemGroup = getReferencesItemGroup();
		for (Reference ref : newReferences.values()) {
			refElement = new Element(REFERENCE_TAG, project.getNamespace());
			refElement.setAttribute(INCLUDE_ATTR, ref.getReferenced());
			if (ref.hasHintPath()) {
				hintElement = new Element(HINT_PATH_TAG, project.getNamespace());
				hintElement.setText(ref.getHintPath(replaceSeparators));
				refElement.addContent(hintElement);
			}
			itemGroup.addContent(refElement);
		}
		newReferences.clear();
	}

}
