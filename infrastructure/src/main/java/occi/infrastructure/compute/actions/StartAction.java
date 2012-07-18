/**
 * Copyright (C) 2010-2011 Sebastian Heckmann, Sebastian Laag
 *
 * Contact Email: <sebastian.heckmann@udo.edu>, <sebastian.laag@udo.edu>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package occi.infrastructure.compute.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.UUID;

import javax.naming.directory.SchemaViolationException;

import occi.core.Action;
import occi.core.Category;
import occi.core.Method;
import occi.infrastructure.Compute;
import occi.infrastructure.injection.Injection;
import occi.infrastructure.interfaces.ComputeInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Action Interface. In this case the StartAction starts
 * the current VM identified by the UUID.
 * 
 * @see Action
 * @author Sebastian Heckmann
 * @author Sebastian Laag
 */
public class StartAction extends Action {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(StartAction.class);

	private static ComputeInterface computeInterface = Injection.getComputeInterface();

	/**
	 * Enum for the Start Actions.
	 */
	public enum Start implements Method {
		start
	}

	private Start start;
	private HashSet<String> attributes = new HashSet<String>();

	public StartAction() throws SchemaViolationException, URISyntaxException {
		attributes.add("start");
		Category category = new Category("start",
				"http://schemas.ogf.org/occi/infrastructure/compute/action#",
				"Action", attributes);
	}

	@Override
	public void execute(URI uri, Method start) {
		LOGGER.debug("libvirt: Start");
		
		Compute compute = null;
		String uriString = uri.toString();
		
//		if (uri.toString().endsWith("/")) {
//			uriString = uriString.substring(0, uri.toString().length() - 1);
//			uriString = uriString.substring(uriString.length() - 36);
//		}
		uriString = uriString.substring(0, uri.toString().length());
		System.out.println(uriString.length());
		uriString = uriString.substring(uriString.length() - 36);
		
		LOGGER.debug("URI " + uriString);
		UUID computeUuid = UUID.fromString(uriString);
		LOGGER.debug("UUID " + computeUuid.toString());
		for (UUID uuid : Compute.getComputeList().keySet()) {
			if (uuid.equals(computeUuid)) {
				compute = Compute.getComputeList().get(computeUuid);
			}
		}
		computeInterface.startCompute(compute);
	}

	public HashSet<String> getAttributes() {
		if (attributes.isEmpty()) {
			for (Start start : Start.values()) {
				attributes.add(start.toString());
			}
		}
		return attributes;
	}

	/**
	 * Returns the possible Start Attributes of the Enum
	 * 
	 * @return String
	 */
	public static String getStartAttributesAsString() {
		StringBuffer startAttributes = new StringBuffer();
		for (Start start : Start.values()) {
			startAttributes.append(start.toString() + ", ");
		}
		return startAttributes.toString();
	}
}