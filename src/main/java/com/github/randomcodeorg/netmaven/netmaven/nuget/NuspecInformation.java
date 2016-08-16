package com.github.randomcodeorg.netmaven.netmaven.nuget;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.codehaus.plexus.logging.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import com.github.randomcodeorg.netmaven.netmaven.InternalLogger;

public class NuspecInformation {

	// private final Document doc;
	private final Element metadata;
	private final Element dependencies;
	private static final String METADATA_ELEMENT = "metadata";
	private static final String LICENSE_URL_ELEMENT = "licenseUrl";
	private static final String DESCRIPTION_ELEMENT = "description";
	private static final String TITLE_ELEMENT = "title";
	private static final String PROJECT_URL_ELEMENT = "projectUrl";
	private static final String DEPENDENCIES_ELEMENT = "dependencies";
	private static final String DEPENDENCY_ELEMENT = "dependency";
	private static final String DEPENDENCY_ID_ATTR = "id";
	private static final String DEPENDENCY_VERSION_ATTR = "version";

	private final Document doc;
	private final Namespace ns;

	public NuspecInformation(Document doc) {
		this.doc = doc;
		ns = doc.getRootElement().getNamespace();
		this.metadata = doc.getRootElement().getChild(METADATA_ELEMENT, ns);
		if (metadata != null) {
			dependencies = metadata.getChild(DEPENDENCIES_ELEMENT, ns);
		} else {
			dependencies = null;
		}
	}

	public void apply(Model model, InternalLogger logger) {
		Element e;
		logger.debug("Applying nuget specification to maven model");
		if (metadata != null) {
			if ((e = metadata.getChild(LICENSE_URL_ELEMENT, ns)) != null) {
				License license = new License();
				license.setUrl(e.getText());
				model.getLicenses().add(license);
			}
			if ((e = metadata.getChild(DESCRIPTION_ELEMENT, ns)) != null) {
				model.setDescription(e.getText());
			}
			if ((e = metadata.getChild(TITLE_ELEMENT, ns)) != null) {
				model.setName(e.getText());
			}
			if ((e = metadata.getChild(PROJECT_URL_ELEMENT, ns)) != null) {
				model.setUrl(e.getText());
			}
			if (dependencies != null) {
				for (Object depO : dependencies.getChildren(DEPENDENCY_ELEMENT, ns)) {
					if (depO instanceof Element) {
						Element dependency = (Element) depO;
						Dependency depModel = new Dependency();
						depModel.setGroupId(dependency.getAttributeValue(DEPENDENCY_ID_ATTR));
						depModel.setArtifactId(depModel.getGroupId());
						depModel.setVersion(dependency.getAttributeValue(DEPENDENCY_VERSION_ATTR));
						depModel.setType("nuget");
						logger.debug("Adding dependency obtaiend from nuget specification: %s", dependency);
						model.getDependencies().add(depModel);
					}
				}
			} else {
				logger.debug("No dependencies section found in nuget specification.");
			}
		} else {
			logger.debug("Could not apply nuget specifications because the <metadata> element could not be found.");
			logger.debug("XML is:\n%s", new XMLOutputter().outputString(doc));
		}
	}

	@Deprecated
	public void apply(Model model, Logger logger) {
		Element e;
		logger.debug("Applying nuget specification to maven model");
		if (metadata != null) {
			if ((e = metadata.getChild(LICENSE_URL_ELEMENT, ns)) != null) {
				License license = new License();
				license.setUrl(e.getText());
				model.getLicenses().add(license);
			}
			if ((e = metadata.getChild(DESCRIPTION_ELEMENT, ns)) != null) {
				model.setDescription(e.getText());
			}
			if ((e = metadata.getChild(TITLE_ELEMENT, ns)) != null) {
				model.setName(e.getText());
			}
			if ((e = metadata.getChild(PROJECT_URL_ELEMENT, ns)) != null) {
				model.setUrl(e.getText());
			}
			if (dependencies != null) {
				for (Object depO : dependencies.getChildren(DEPENDENCY_ELEMENT, ns)) {
					if (depO instanceof Element) {
						Element dependency = (Element) depO;
						Dependency depModel = new Dependency();
						depModel.setGroupId(dependency.getAttributeValue(DEPENDENCY_ID_ATTR));
						depModel.setArtifactId(depModel.getGroupId());
						depModel.setVersion(dependency.getAttributeValue(DEPENDENCY_VERSION_ATTR));
						depModel.setType("dll");
						logger.debug(
								String.format("Adding dependency obtaiend from nuget specification: %s", dependency));
						model.getDependencies().add(depModel);
					}
				}
			} else {
				logger.debug("No dependencies section found in nuget specification.");
			}
		} else {
			logger.debug("Could not apply nuget specifications because the <metadata> element could not be found.");
			logger.debug(String.format("XML is:\n%s", new XMLOutputter().outputString(doc)));
		}
	}

}
