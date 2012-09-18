package occi.http;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcciRestLink extends ServerResource {
	private static final Logger LOGGER = LoggerFactory
	.getLogger(OcciRestLink.class);
	
	/**
	 * HTTP GET request. Returns Version Info of OCCI.
	 * 
	 * @return OCCI version
	 */
	@Post
	public String postOCCIRequest() {
		getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
		return "Procci server does not support this feature now.";
	}
	
	/**
	 * HTTP GET request. Returns Version Info of OCCI.
	 * 
	 * @return OCCI version
	 */
	@Get
	public String getOCCIRequest() {
		getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
		return "Procci server does not support this feature now.";
	}

}
