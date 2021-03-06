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

package occi.http;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import occi.config.OcciConfig;
import occi.core.Kind;
import occi.core.Link;
import occi.core.Mixin;
import occi.http.check.OcciCheck;
import occi.infrastructure.Network;
import occi.infrastructure.Storage;
import occi.infrastructure.Storage.State;
import occi.infrastructure.links.IPNetworkInterface;
import occi.infrastructure.links.NetworkInterface;
import occi.infrastructure.links.StorageLink;
import occi.infrastructure.storage.actions.BackupAction.Backup;
import occi.infrastructure.storage.actions.OfflineAction.Offline;
import occi.infrastructure.storage.actions.OnlineAction.Online;
import occi.infrastructure.storage.actions.ResizeAction.Resize;

import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for Storage resources. Returns information about a specific storage
 * resources.
 * 
 * @author Sebastian Laag
 * @author Sebastian Heckmann
 */
public class OcciRestStorage extends ServerResource {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(OcciRestStorage.class);

	private final OcciCheck occiCheck = new OcciCheck();

	@Get
	public String getOCCIRequest() {
		Form requestHeaders = (Form) getRequest().getAttributes().get(
				"org.restlet.http.headers");
		String acceptCase = OcciCheck.checkCaseSensitivity(
				requestHeaders.toString()).get("accept");
		// put all attributes into a buffer for the response
		StringBuffer buffer = new StringBuffer();
		Representation representation = null;
		if (!getReference().getLastSegment().equals("storage")) {
			OcciCheck.isUUID(getReference().getLastSegment());
			// get the compute instance by the given UUID
			Storage storage = Storage.getStorageList().get(
					UUID.fromString(getReference().getLastSegment()));

			// put all attributes into a buffer for the response
			StringBuffer linkBuffer = new StringBuffer();
			StringBuffer mixinBuffer = new StringBuffer();
			// put all attributes into a buffer for the response
			buffer.append(" Category: ").append(storage.getKind().getTerm())
					.append(" scheme= ").append(storage.getKind().getScheme())
					.append(" class=\"kind\";").append(" size: ")
					.append(storage.getSize()).append(" state: ")
					.append(storage.getState());

			if (!storage.getLinks().isEmpty()) {
				linkBuffer.append(" Link: ");
			}
			for (Link l : storage.getLinks()) {
				if (l instanceof NetworkInterface) {
					NetworkInterface networkInterface = (NetworkInterface) l;
					IPNetworkInterface ipNetworkInterface = null;
					for (Mixin mixin : Mixin.getMixins()) {
						if (mixin instanceof IPNetworkInterface
								&& mixin.getEntities() != null
								&& mixin.getEntities().contains(
										networkInterface)) {
							ipNetworkInterface = (IPNetworkInterface) mixin;
						}
					}
					linkBuffer.append("</");
					linkBuffer.append(l.getLink().getKind().getTerm());
					linkBuffer.append("/");
					linkBuffer.append(l.getId());
					linkBuffer.append(">; ");
					linkBuffer.append("rel=\""
							+ l.getLink().getKind().getScheme());
					linkBuffer.append(l.getLink().getKind().getTerm());
					linkBuffer.append("\"");
					linkBuffer.append(" self=\"/link/");
					linkBuffer.append("networkinterface/");
					linkBuffer.append(networkInterface.getId() + "\";");
					linkBuffer.append(" category=\"");
					linkBuffer.append(l.getLink().getKind().getScheme()
							+ "networkinterface\";");
					if (ipNetworkInterface != null) {
						linkBuffer.append(" category=\"");
						linkBuffer.append(ipNetworkInterface.getScheme()
								+ "ipnetworkinterface\"");
					}
					linkBuffer.append(" occi.core.target=/"
							+ networkInterface.getTarget().getKind().getTerm()
							+ "/" + networkInterface.getTarget().getId());
					linkBuffer.append(" occi.core.source=/"
							+ networkInterface.getLink().getKind().getTerm()
							+ "/" + storage.getId());
					linkBuffer.append(" occi.core.id="
							+ networkInterface.getId());
					linkBuffer.append(" occi.networkinterface.interface="
							+ networkInterface.getNetworkInterface());
					linkBuffer.append(" occi.networkinterface.mac="
							+ networkInterface.getMac());
					linkBuffer.append(" occi.networkinterface.state="
							+ networkInterface.getState());
					if (ipNetworkInterface != null) {
						linkBuffer.append(" occi.networkinterface.address="
								+ ipNetworkInterface.getIp());
						linkBuffer.append(" occi.networkinterface.gateway="
								+ ipNetworkInterface.getGateway());
						linkBuffer.append(" occi.networkinterface.allocation="
								+ ipNetworkInterface.getAllocation());
					}
					if (l instanceof StorageLink) {
						StorageLink storageLink = (StorageLink) l;

						linkBuffer.append("</");
						linkBuffer.append(l.getLink().getKind().getTerm());
						linkBuffer.append("/");
						linkBuffer.append(l.getId());
						linkBuffer.append(">; ");
						linkBuffer.append("rel=\""
								+ l.getLink().getKind().getScheme());
						linkBuffer.append(l.getLink().getKind().getTerm());
						linkBuffer.append("\"");
						linkBuffer.append(" self=\"/link/");
						linkBuffer.append("storage link/");
						linkBuffer.append(storageLink.getId() + "\";");
						linkBuffer.append(" category=\"");
						linkBuffer.append(l.getLink().getKind().getScheme()
								+ "storagelink\";");
						linkBuffer.append(" occi.core.target=/"
								+ storageLink.getTarget().getKind().getTerm()
								+ "/" + storageLink.getTarget().getId());
						linkBuffer.append(" occi.core.source=/"
								+ storageLink.getLink().getKind().getTerm()
								+ "/" + storage.getId());
						linkBuffer.append(" occi.core.id="
								+ storageLink.getId());
						linkBuffer.append(" occi.storagrlink.deviceid= "
								+ storageLink.getDeviceid());
						linkBuffer.append(" occi.storagrlink.mountpoint= "
								+ storageLink.getMountpoint());
						linkBuffer.append(" occi.networkinterface.state="
								+ storageLink.getState());

					}

				}
				buffer.append(linkBuffer);
				LOGGER.debug("Links: " + linkBuffer.toString());
			}
			for (Mixin mixin : Mixin.getMixins()) {
				if (mixin.getEntities() != null) {
					if (mixin.getEntities().contains(storage)) {
						mixinBuffer.append(" ");
						mixinBuffer.append("Category: " + mixin.getTitle()
								+ "; scheme=\"" + mixin.getScheme()
								+ "\"; class=\"mixin\"");
					}
				}
			}
			buffer.append(mixinBuffer);
			LOGGER.debug("Mixin: " + mixinBuffer);
			// access the request headers and get the Accept attribute
			representation = OcciCheck.checkContentType(requestHeaders, buffer,
					getResponse());
			// Check the accept header
			if (requestHeaders.getFirstValue(acceptCase).equals("text/occi")) {
				// generate header rendering
				this.occiCheck.setHeaderRendering(null, storage,
						buffer.toString(), linkBuffer);
				// set right representation and status code
				getResponse().setEntity(representation);
				getResponse().setStatus(Status.SUCCESS_OK);
				return " ";
			}
			// set right representation and status code
			getResponse().setEntity(representation);
			getResponse().setStatus(Status.SUCCESS_OK, buffer.toString());
			return buffer.toString();
		} else {
			// access the request headers and get the X-OCCI-Attribute
			requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			LOGGER.debug("Current request: " + requestHeaders);
			/*
			 * Print all properties of the kind instance
			 */
			int i = 1;
			for (UUID uuid : Network.getNetworkList().keySet()) {
				buffer.append(getRootRef() + "/" + uuid.toString());
				if (i < Network.getNetworkList().size()) {
					buffer.append(",");
				}
				i++;

				representation = OcciCheck.checkContentType(requestHeaders,
						buffer, getResponse());
				if (representation.getMediaType().toString()
						.equals("text/occi")) {
					// Set Location Attribute
					setLocationRef(buffer.toString());
					// return http status code
					getResponse().setStatus(Status.SUCCESS_OK, " ");
					return " ";
				}
			}
			getResponse().setStatus(Status.SUCCESS_OK);
			getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
			return buffer.toString();
		}

	}

	/**
	 * Deletes the resource which applies to the parameters in the header.
	 * 
	 * @return string deleted or not
	 */
	@Delete
	public String deleteOCCIRequest() {
		// set occi version info
		getServerInfo().setAgent(
				OcciConfig.getInstance().config.getString("occi.version"));
		LOGGER.debug("Incoming delete request at storage");
		try {
			// get storage resource that should be deleted
			Storage storage = Storage.getStorageList().get(
					UUID.fromString(getReference().getLastSegment()));
			// remove it from storage resource list
			if (Storage.getStorageList().remove(
					UUID.fromString(storage.getId().toString())) == null) {
				throw new NullPointerException(
						"There is no resorce with the given ID");
			}
			// set storage resource to null
			storage = null;
			getResponse().setStatus(Status.SUCCESS_OK);
			return " ";
		} catch (NullPointerException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					e.getMessage());
			return "UUID not found! " + e.toString()
					+ "\n Storage resource could not be deleted.";
		} catch (Exception e) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					e.getMessage());
			return e.toString();
		}
	}

	/**
	 * Method to create a new storage instance.
	 * 
	 * @param representation
	 * @return string
	 * @throws Exception
	 */
	@Post
	public String postOCCIRequest(Representation representation)
			throws Exception {
		LOGGER.info("Incoming POST request.");
		// set occi version info
		getServerInfo().setAgent(
				OcciConfig.getInstance().config.getString("occi.version"));
		try {
			// access the request headers and get the X-OCCI-Attribute
			Form requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			LOGGER.debug("Current request: " + requestHeaders);
			String attributeCase = OcciCheck.checkCaseSensitivity(
					requestHeaders.toString()).get("x-occi-attribute");
			String xocciattributes = requestHeaders.getValues(attributeCase)
					.replace(",", " ");
			LOGGER.debug("Media-Type: "
					+ requestHeaders.getFirstValue("accept", true));

			OcciCheck.countColons(xocciattributes, 1);

			// split the single occi attributes and put it into a (key,value)
			// map
			LOGGER.debug("Raw X-OCCI Attributes: " + xocciattributes);
			StringTokenizer xoccilist = new StringTokenizer(xocciattributes);
			HashMap<String, Object> xoccimap = new HashMap<String, Object>();
			LOGGER.debug("Tokens in XOCCIList: " + xoccilist.countTokens());
			while (xoccilist.hasMoreTokens()) {
				String[] temp = xoccilist.nextToken().split("\\=");
				if (temp[0] != null && temp[1] != null) {
					String temp1 = temp[1].replace("\"", "");
					LOGGER.debug(temp[0] + " " + temp1 + "\n");
					xoccimap.put(temp[0], temp1);
				}
			}
			// Check if last part of the URI is not action
			if (!getReference().toString().contains("action")) {
				// put occi attributes into a buffer for the response
				StringBuffer buffer = new StringBuffer();
				buffer.append("occi.storage.state=").append(
						xoccimap.get("occi.storage.state"));
				buffer.append(" occi.storage.size=").append(
						xoccimap.get("occi.storage.size"));

				Set<String> set = new HashSet<String>();
				set.add("summary: ");
				set.add(buffer.toString());
				set.add(requestHeaders.getFirstValue("scheme"));

				// create new Compute instance with the given attributes
				Storage storage = new Storage(
						Float.parseFloat((String) xoccimap
								.get("occi.storage.size")), State.offline,
						null, null);
				storage.setKind(new Kind(null, "storage", "storage", null));
				StringBuffer resource = new StringBuffer();
				String path = getRootRef().getPath();
				if (path != null) {
					resource.append(path);
				}
				resource.append("/").append(storage.getKind().getTerm())
						.append("/");
				getRootRef().setPath(resource.toString());

				// check of the category
				if (!"storage".equalsIgnoreCase(xoccimap.get(
						"occi.storage.Category").toString())) {
					throw new IllegalArgumentException(
							"Illegal Category type: "
									+ xoccimap.get("occi.storage.Category"));
				}
				for (Mixin mixin : Mixin.getMixins()) {
					if (mixin.getEntities() != null) {
						if (mixin.getEntities().contains(storage)) {
							buffer.append("Category: " + mixin.getTitle()
									+ "; scheme=\"" + mixin.getScheme()
									+ "\"; class=\"mixin\"");
						}
					}
				}
				// Check accept header
				if (requestHeaders.getFirstValue("accept", true).equals(
						"text/occi")
						|| requestHeaders.getFirstValue("Content-Type", true)
								.equals("text/occi")) {
					// Generate header rendering
					occiCheck.setHeaderRendering(null, storage,
							buffer.toString(), null);
					getResponse().setEntity(representation);
					getResponse().setStatus(Status.SUCCESS_OK);
				}
				storage.getCreate().execute(storage.getId(), null);
				// Location Rendering in HTTP Header, not in body
				setLocationRef((getRootRef().toString() + storage.getId()));
				representation = OcciCheck.checkContentType(requestHeaders,
						buffer, getResponse());
				getResponse().setEntity(representation);
				// set response status
				getResponse().setStatus(Status.SUCCESS_OK, buffer.toString());
				return Response.getCurrent().toString();
			} else {
				String[] splitURI = getReference().toString().split("\\/");
				LOGGER.debug("splitURI length: " + splitURI.length);
				UUID id = null;
				for (String element : splitURI) {
					if (element.contains("?")) {
						element = element.substring(0, element.indexOf("?"));
					}
					if (OcciCheck.isUUID(element)) {
						id = UUID.fromString(element);
					}
				}
				LOGGER.debug("UUID: " + id);
				// Get the storage resource by the given UUID
				Storage storage = Storage.getStorageList().get(id);

				String location = "http:"
						+ getReference().getHierarchicalPart();

				// Extract the action type / name from the last part of the
				// given
				// location URI and split it after the "=" (../?action=backup)
				String[] actionName = getReference()
						.getRemainingPart()
						.subSequence(1,
								getReference().getRemainingPart().length())
						.toString().split("\\=");
				LOGGER.debug("Action Name: " + actionName[1]);
				// Check if actionName[1] is set
				if (actionName.length >= 2) {
					if (actionName[1].equalsIgnoreCase("online")) {
						LOGGER.debug("Online Action called.");
						// Call the online action of the storage resource
						storage.getOnline()
								.execute(
										URI.create(location),
										Online.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.online);
					} else if (actionName[1].equalsIgnoreCase("offline")) {
						LOGGER.debug("Offline Action called.");
						// Call the offline action of the storage resource
						storage.getOffline()
								.execute(
										URI.create(location),
										Offline.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.offline);
					} else if (actionName[1].equalsIgnoreCase("offline")) {
						LOGGER.debug("Offline Action called.");
						// Call the offline action of the storage resource
						storage.getOffline()
								.execute(
										URI.create(location),
										Offline.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.offline);
					} else if (actionName[1].equalsIgnoreCase("backup")) {
						LOGGER.debug("Backup Action called.");
						// Call the backup action of the storage resource
						storage.getBackup()
								.execute(
										URI.create(location),
										Backup.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.online);
					} else if (actionName[1].equalsIgnoreCase("resize")) {
						LOGGER.debug("Resize Action called.");
						// Call the resize action of the storage resource
						storage.getResize()
								.execute(
										URI.create(location),
										Resize.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.online);
					} else if (actionName[1].equalsIgnoreCase("snapshot")) {
						LOGGER.debug("Snapshot Action called.");
						// Call the snapshot action of the storage resource
						storage.getSnapshot()
								.execute(
										URI.create(location),
										occi.infrastructure.storage.actions.SnapshotAction.Snapshot
												.valueOf((String) xoccimap
														.get("method")));
						// Set the current state of the storage resource
						storage.setState(State.online);
					} else {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						return "Invalid action type"
								+ Response.getCurrent().toString();
					}
				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return Response.getCurrent().toString();
				}
				getResponse().setStatus(Status.SUCCESS_OK);
				return Response.getCurrent().toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			return "Exception caught: " + e.toString() + "\n";
		}
	}

	/**
	 * Edit the parameters of a given resource instance.
	 * 
	 * @param representation
	 * @return data of altered instance
	 * @throws Exception
	 */
	@Put
	public String putOCCIRequest(Representation representation) {
		try {
			// set occi version info
			getServerInfo().setAgent(
					OcciConfig.getInstance().config.getString("occi.version"));
			OcciCheck.isUUID(getReference().getLastSegment());
			Storage storage = Storage.getStorageList().get(
					UUID.fromString(getReference().getLastSegment()));
			// access the request headers and get the X-OCCI-Attribute
			Form requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			LOGGER.debug("Raw Request Headers: " + requestHeaders);
			String xocciattributes = "";
			xocciattributes = requestHeaders.getFirstValue("x-occi-attribute",
					true);

			// Check if some attributes are given by the request
			if (xocciattributes != null) {
				// Count the colons in the Request
				OcciCheck.countColons(xocciattributes, 1);
				/*
				 * split the single occi attributes and put it into a
				 * (key,value) map
				 */
				LOGGER.debug("Raw X-OCCI Attributes: " + xocciattributes);
				StringTokenizer xoccilist = new StringTokenizer(xocciattributes);
				HashMap<String, Object> xoccimap = new HashMap<String, Object>();
				while (xoccilist.hasMoreTokens()) {
					String[] temp = xoccilist.nextToken().split("\\=");
					if (temp.length > 1 && temp[0] != null && temp[1] != null) {
						xoccimap.put(temp[0], temp[1]);
					}
				}
				if (!xoccimap.isEmpty()) {
					// Change the storage attribute if it is send by the request
					if (xoccimap.containsKey("occi.storage.size")) {
						storage.setSize(Float.parseFloat(xoccimap.get(
								"occi.storage.size").toString()));
					}
					if (xoccimap.containsKey("occi.storage.state")) {
						storage.setState(State.valueOf((String) xoccimap
								.get("occi.storage.state")));
					}

					// Location Rendering in HTTP Header, not in body
					setLocationRef(getRootRef().toString());

					// set response status
					getResponse().setStatus(Status.SUCCESS_OK);

					return Response.getCurrent().toString();
				} else {
					getResponse().setStatus(Status.SUCCESS_OK);
					return "Nothing changed";
				}
			}
			// Catch possible exceptions
		} catch (Exception e) {
			LOGGER.error("Exception caught: " + e.toString());
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			return "Exception: " + e.getMessage() + "\n";
		}
		return " ";
	}
}