package occi.infrastructure.templates.sla;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.directory.SchemaViolationException;

import occi.core.Mixin;
import occi.infrastructure.templates.ResourceTemplate;

public class SlaTemplate extends ResourceTemplate {

	public SlaTemplate(Set<Mixin> related, String term, String title,
			String scheme, Set<String> attributes)
			throws SchemaViolationException, URISyntaxException {
		super(related, term, title, scheme, attributes);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Static HashSet of all OS template attributes.
	 */
	public static HashSet<String> attributes = new HashSet<String>();
	/**
	 * sla value
	 */
	private String slaName;

	public String getSlaName() {
		return slaName;
	}

	public void setSlaName(String slaName) {
		this.slaName = slaName;
	}

	/**
	 * Generate attribute List.
	 */
	public static void generateAttributeList() {
		if (attributes.isEmpty()) {
			// add all attributes to attribute list
			attributes.add("'occi.resource_tpl.sla'");
		}
	}
}
