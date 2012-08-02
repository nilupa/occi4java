package occi.infrastructure.templates;

import java.net.URISyntaxException;
import java.util.Set;

import javax.naming.directory.SchemaViolationException;

import occi.core.Mixin;

public class ResourceTemplate extends Mixin {

	public ResourceTemplate(Set<Mixin> related, String term, String title,
			String scheme, Set<String> attributes)
			throws SchemaViolationException, URISyntaxException {
		super(related, term, title, scheme, attributes);

	}

}
