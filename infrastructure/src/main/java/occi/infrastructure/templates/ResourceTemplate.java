package occi.infrastructure.templates;

import java.net.URISyntaxException;
import java.util.Set;

import javax.naming.directory.SchemaViolationException;

import occi.core.Action;
import occi.core.Entity;
import occi.core.Mixin;

public class ResourceTemplate extends Mixin{

	public ResourceTemplate(Set<Action> actions, Set<Mixin> related,
			Set<Entity> entities) throws URISyntaxException,
			SchemaViolationException {
		super(actions, related, entities);
	}

}
