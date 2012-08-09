package occi.infrastructure.compute.actions;

import java.net.URI;
import java.util.UUID;

import occi.core.Action;
import occi.core.Method;
import occi.infrastructure.Compute;
import occi.infrastructure.injection.Injection;
import occi.infrastructure.interfaces.ComputeInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneAction extends Action {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CloneAction.class);

	private static ComputeInterface computeInterface = Injection
			.getComputeInterface();

	/**
	 * Enum for the Start Actions.
	 */
	public enum Clone implements Method {
		clone
	}

	@Override
	public void execute(URI uri, Method method) {
		LOGGER.debug("libvirt: clone");

		Compute compute = null;
		String uriString = uri.toString();

		uriString = uriString.substring(0, uri.toString().length());
		uriString = uriString.substring(uriString.length() - 36);

		LOGGER.debug("URI " + uriString);
		UUID computeUuid = UUID.fromString(uriString);
		LOGGER.debug("UUID " + computeUuid.toString());
		for (UUID uuid : Compute.getComputeList().keySet()) {
			if (uuid.equals(computeUuid)) {
				compute = Compute.getComputeList().get(computeUuid);
			}
		}
		computeInterface.makeCloneCompute(compute);

	}

}
