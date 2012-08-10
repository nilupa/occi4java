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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.naming.directory.SchemaViolationException;

import occi.config.OcciConfig;
import occi.core.Kind;
import occi.core.Link;
import occi.core.Mixin;
import occi.http.check.OcciCheck;
import occi.infrastructure.Compute;
import occi.infrastructure.Compute.Architecture;
import occi.infrastructure.Compute.State;
import occi.infrastructure.Network;
import occi.infrastructure.Storage;
import occi.infrastructure.compute.actions.CloneAction.Clone;
import occi.infrastructure.compute.actions.CollateAction.Collate;
import occi.infrastructure.compute.actions.CreateAction;
import occi.infrastructure.compute.actions.DecollateAction.DeCollate;
import occi.infrastructure.compute.actions.DeleteAction;
import occi.infrastructure.compute.actions.MigrateAction.Migrate;
import occi.infrastructure.compute.actions.RestartAction.Restart;
import occi.infrastructure.compute.actions.SLAAction.Sla;
import occi.infrastructure.compute.actions.StartAction.Start;
import occi.infrastructure.compute.actions.StopAction.Stop;
import occi.infrastructure.compute.actions.SuspendAction.Suspend;
import occi.infrastructure.links.IPNetworkInterface;
import occi.infrastructure.links.NetworkInterface;
import occi.infrastructure.links.StorageLink;
import occi.infrastructure.templates.OSTemplate;
import occi.infrastructure.templates.sla.SlaTemplate;

import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcciRestCompute extends ServerResource {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(OcciRestCompute.class);

	private final OcciCheck occiCheck = new OcciCheck();
	private IPNetworkInterface ipNetworkInterface = null;

	private void createLinks(Form requestHeaders, Compute compute,
			StringBuffer buffer) {
		String linkCase = OcciCheck.checkCaseSensitivity(
				requestHeaders.toString()).get("Link");
		for (String valueString : requestHeaders.getValuesArray(linkCase)) {
			String[] spiltString = valueString.split(";");
			Map<String, String> valueMap = new HashMap<String, String>();
			for (String str : spiltString) {
				String[] spiltArrayOfEqualSign = str.split("=");
				if (spiltArrayOfEqualSign.length == 2) {
					String strWithoutDoubleQuote = spiltArrayOfEqualSign[1]
							.replace("\"", "");
					valueMap.put(spiltArrayOfEqualSign[0].trim(),
							strWithoutDoubleQuote.trim());
				} else {
					String[] linkUUID = str.replaceAll("</[\\w]+/", "").split(
							">");
					if (str.contains("network")) {
						valueMap.put("network.id", linkUUID[0]);

					} else if (str.contains("storage")) {
						valueMap.put("storage.id", linkUUID[0]);
					}
				}
			}
			if (valueMap.get("rel").equals(
					"http://schemas.ogf.org/occi/infrastructure#network")
					&& valueMap
							.get("category")
							.equals("http://schemas.ogf.org/occi/infrastructure#networkinterface")) {
				try {
					if (valueMap.get("occi.networkinterface.interface") == null) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new RuntimeException(
								"'occi.networkinterface.interface' value not found");
					}
					if (valueMap.get("occi.networkinterface.mac") == null) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new RuntimeException(
								"'occi.networkinterface.mac' value not found");
					}
					NetworkInterface networkInterface = new NetworkInterface(
							valueMap.get("occi.networkinterface.interface")
									.toString(),
							valueMap.get("occi.networkinterface.mac")
									.toString(),
							occi.infrastructure.links.NetworkInterface.State.inactive,
							Network.getNetworkList()
									.get(UUID.fromString(valueMap
											.get("network.id"))), compute);
					networkInterface.getLink().setTitle("network link");
					compute.getLinks().add(networkInterface);
					LOGGER.debug("NetworkInterface UUID: "
							+ networkInterface.getId().toString()
							+ networkInterface.getNetworkInterface());
					buffer.append("occi.networkinterface.interface=").append(
							valueMap.get("occi.networkinterface.interface"));
					buffer.append("occi.networkinterface.mac=").append(
							valueMap.get("occi.networkinterface.mac"));
					if (ipNetworkInterface != null) {
						ipNetworkInterface.getEntities().add(networkInterface);
						networkInterface.getMixins().add(
								new Kind(null, "ipnetworkinterface",
										"ipnetworkinterface", null));
						buffer.append("occi.network.address=").append(
								ipNetworkInterface.getIp());
						buffer.append("occi.network.gateway=").append(
								ipNetworkInterface.getGateway());
						buffer.append("occi.network.allocation=").append(
								ipNetworkInterface.getAllocation());
					}
				} catch (SchemaViolationException e) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					throw new IllegalArgumentException(
							"Schema violation Exception");
				} catch (URISyntaxException e) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					throw new IllegalArgumentException("URI Syntax Error");
				}

			} else if (valueMap.get("rel").equals(
					"http://schemas.ogf.org/occi/infrastructure#storage")
					&& valueMap
							.get("category")
							.equals("http://schemas.ogf.org/occi/infrastructure#storagelink")) {
				if (valueMap.get("occi.storagelink.deviceid") == null) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					throw new RuntimeException(
							" 'occi.storagelink.deviceid' value not found");
				} else if (valueMap.get("occi.storagelink.mountpoint") == null) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					throw new RuntimeException(
							"'occi.storagelink.mountpoint' value not found");
				}
				try {
					StorageLink storageLink = new StorageLink(
							valueMap.get("occi.storagelink.deviceid")
									.toString(),
							occi.infrastructure.links.StorageLink.State.inactive,
							valueMap.get("occi.storagelink.mountpoint"),
							Storage.getStorageList().get(
									UUID.fromString("storage.id")), compute);
					storageLink.getLink().setTitle("storage link");
					compute.getLinks().add(storageLink);
					LOGGER.debug("StorageLink UUID: "
							+ storageLink.getId().toString());
				} catch (URISyntaxException e) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					throw new IllegalArgumentException("URI Syntax Error");
				}

			}
		}
	}

	/**
	 * Adds the applicable mixins which is coming with the HTTP request.
	 * Applicable mixins are defined in the OcciRestQuery.java
	 * 
	 * @param representation
	 * @param compute
	 */
	private void createMixinsForOSTempletesAndSLA(Form requestHeaders,
			Compute compute, HashMap<String, Object> xoccimap,
			StringBuffer buffer) {
		// access the request headers and get the Category
		for (String categoryString : requestHeaders.getValuesArray("Category")) {
			String[] splitArray = categoryString.split(";");
			if (!splitArray[0].equals("compute")) {
				String category = null;
				String scheme = null;
				String title = null;
				Map<String, String> valueMap = new HashMap<String, String>();
				String className = "";
				for (String str : splitArray) {
					String[] splitByEqualArray = str.split("=");
					if (splitByEqualArray.length == 2) {
						String stringWithoutDoubleQuote = splitByEqualArray[1]
								.replace("\"", "");
						valueMap.put(splitByEqualArray[0].trim(),
								stringWithoutDoubleQuote.trim());
					} else {
						valueMap.put("category", splitByEqualArray[0].trim());
					}

				}
				for (Map.Entry<String, String> entry : valueMap.entrySet()) {

					if (entry.getKey().equals("category")) {
						category = entry.getValue().trim();
					} else if (entry.getKey().equals("scheme")) {
						scheme = entry.getValue().trim();
					} else if (entry.getKey().equals("class")) {
						className = entry.getValue().trim();
					} else if (entry.getKey().equals("title")) {
						title = entry.getValue().trim();
					}

				}
				if (title == null) {
					title = category;
				}
				if (className.equals("mixin")
						&& category.equals("os_tpl")
						&& scheme
								.equals("http://schemas.ogf.org/occi/infrastucture#")) {
					try {
						Kind kindOSTemplate = new Kind(null, null, null, null,
								"os_tpl", title, scheme, null);
						compute.getMixins().add(kindOSTemplate);
						OSTemplate osTemplate = new OSTemplate(null, category,
								title, scheme, null);
						if (xoccimap.get("occi.os_tpl.image") == null) {
							getResponse().setStatus(
									Status.CLIENT_ERROR_BAD_REQUEST);
							throw new RuntimeException(
									"'occi.os_tpl.image' not found");
						}
						osTemplate.setOsTerm(xoccimap.get("occi.os_tpl.image")
								.toString());
						buffer.append(" occi.os_tpl.image=").append(
								xoccimap.get("occi.os_tpl.image").toString());
						osTemplate.getEntities().add(compute);
					} catch (SchemaViolationException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException(
								"Schema violation Exception");
					} catch (URISyntaxException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException("URI Sysntax Error");
					}
				} else if (className.equals("mixin")
						&& category.equals("sla")
						&& scheme
								.equals("http://proactive/occi/procci.resource_tpl.sla/infrastructure#")) {

					try {
						Kind slaTemplate = new Kind(null, null, null, null,
								"sla", title, scheme, null);
						compute.getMixins().add(slaTemplate);
						SlaTemplate template = new SlaTemplate(null, category,
								title, scheme, null);
						if (xoccimap.get("occi.resource_tpl.sla") == null) {
							getResponse().setStatus(
									Status.CLIENT_ERROR_BAD_REQUEST);
							throw new RuntimeException(
									"'occi.resource_tpl.sla' value not found");
						}
						template.setSlaName(xoccimap.get(
								"occi.resource_tpl.sla").toString());
						buffer.append(" occi.resource_tpl.sla=").append(
								xoccimap.get("occi.resource_tpl.sla")
										.toString());
						template.getEntities().add(compute);
					} catch (SchemaViolationException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException(
								"Schema violation Exception");
					} catch (URISyntaxException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException("URI Syntax Error");
					}

				} else if (className.equals("mixin")
						&& category.equals("ipnetworkinterface")
						&& scheme
								.equals("http://schemas.ogf.org/occi/infrastructure/networkinterface#")) {
					try {
						IPNetworkInterface ipNetworkInterface = new IPNetworkInterface(
								null, category, title, scheme,
								IPNetworkInterface.attributes);
						ipNetworkInterface.setIp(xoccimap.get(
								"occi.networkinterface.address").toString());
						ipNetworkInterface.setGateway(xoccimap.get(
								"occi.networkinterface.gateway").toString());
						ipNetworkInterface
								.setAllocation(IPNetworkInterface.Allocation.DYNAMIC);
						this.ipNetworkInterface = ipNetworkInterface;
					} catch (SchemaViolationException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException("URI Syntax Error");
					} catch (URISyntaxException e) {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						throw new IllegalArgumentException("URI Syntax Error");
					}

				}
			}
		}
	}

	/**
	 * Method to create a new compute instance.
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
		HashMap<String, Object> xoccimap = new HashMap<String, Object>();
		try {
			// access the request headers and get the X-OCCI-Attribute
			Form requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			LOGGER.debug("Current request: " + requestHeaders);
			String attributeCase = OcciCheck.checkCaseSensitivity(
					requestHeaders.toString()).get("x-occi-attribute");
			String xocciattributes = requestHeaders.getValues(attributeCase)
					.replace(",", " ");
			String acceptCase = OcciCheck.checkCaseSensitivity(
					requestHeaders.toString()).get("accept");
			LOGGER.debug("Media-Type: "
					+ requestHeaders.getFirstValue(acceptCase));
			LOGGER.debug("getref getlastseg: "
					+ getReference().getLastSegment());

			OcciCheck.countColons(xocciattributes, 1);

			// split the single occi attributes and put it into a
			// (key,value)
			// map
			LOGGER.debug("Raw X-OCCI Attributes: " + xocciattributes);
			StringTokenizer xoccilist = new StringTokenizer(xocciattributes);
			// HashMap<String, Object> xoccimap = new HashMap<String,
			// Object>();
			LOGGER.debug("Tokens in XOCCIList: " + xoccilist.countTokens());
			while (xoccilist.hasMoreTokens()) {
				String[] temp = xoccilist.nextToken().split("\\=");
				if (temp[0] != null && temp[1] != null) {
					LOGGER.debug(temp[0] + " " + temp[1] + "\n");
					xoccimap.put(temp[0], temp[1]);
				}
			}
			// Check if last part of the URI is not action
			if (!getReference().toString().contains("action")) {
				// put occi attributes into a buffer for the response
				StringBuffer buffer = new StringBuffer();
				buffer.append("occi.compute.architecture=").append(
						xoccimap.get("occi.compute.architecture"));
				buffer.append(" occi.compute.cores=").append(
						xoccimap.get("occi.compute.cores"));
				buffer.append(" occi.compute.hostname=").append(
						xoccimap.get("occi.compute.hostname"));
				buffer.append(" occi.compute.speed=").append(
						xoccimap.get("occi.compute.speed"));
				buffer.append(" occi.compute.memory=").append(
						xoccimap.get("occi.compute.memory"));
				buffer.append(" occi.compute.state=").append("inactive");

				Set<String> set = new HashSet<String>();
				set.add("summary: ");
				set.add(requestHeaders.getFirstValue("scheme"));
				Set<String> keySet = xoccimap.keySet();
				for (String key : keySet) {
					set.add(key + "=" + xoccimap.get(key));
				}
				LOGGER.debug("Attribute set: " + set.toString());

				// create new Compute instance with the given attributes
				Compute compute = new Compute(
						Architecture.valueOf((String) xoccimap
								.get("occi.compute.architecture")),
						Integer.parseInt((String) xoccimap
								.get("occi.compute.cores")),
						(String) xoccimap.get("occi.compute.hostname"),
						Float.parseFloat((String) xoccimap
								.get("occi.compute.speed")),
						Float.parseFloat((String) xoccimap
								.get("occi.compute.memory")), State.inactive,
						set);
				createLinks(requestHeaders, compute, buffer);
				createMixinsForOSTempletesAndSLA(requestHeaders, compute,
						xoccimap, buffer);
				URI uri = new URI(compute.getId().toString());
				// Create libvirt domain
				CreateAction createAction = new CreateAction();
				createAction.execute(uri, null);

				StringBuffer resource = new StringBuffer();
				String path = getRootRef().getPath();
				if (path != null) {
					resource.append(path);
				}
				resource.append("/").append(compute.getKind().getTerm())
						.append("/");
				getRootRef().setPath(resource.toString());

				// check of the category
				if (!"compute".equalsIgnoreCase(xoccimap.get(
						"occi.compute.Category").toString())) {
					throw new IllegalArgumentException(
							"Illegal Category type: "
									+ xoccimap.get("occi.compute.Category"));
				}
				for (Mixin mixin : Mixin.getMixins()) {
					if (mixin.getEntities() != null) {
						if (mixin.getEntities().contains(compute)) {
							buffer.append(" ");
							buffer.append("Category: " + mixin.getTitle()
									+ "; scheme=\"" + mixin.getScheme()
									+ "\"; class=\"mixin\"");
						}
					}
				}
				LOGGER.debug("Compute Uuid: " + compute.getUuid());
				LOGGER.debug("Compute Kind scheme: "
						+ compute.getKind().getScheme());
				// Check accept header
				if (requestHeaders.getFirstValue(acceptCase)
						.equals("text/occi")
						|| requestHeaders.getFirstValue("content-type", true)
								.equals("text/occi")) {
					// Generate header rendering
					this.occiCheck.setHeaderRendering(null, compute,
							buffer.toString(), null);
					getResponse().setEntity(representation);
					getResponse().setStatus(Status.SUCCESS_OK);
				}
				// Location Rendering in HTTP Header, not in body
				setLocationRef((getRootRef().toString() + compute.getId()));
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
				// Get the compute resource by the given UUID
				Compute compute = Compute.getComputeList().get(id);

				String location = "http:"
						+ getReference().getHierarchicalPart();

				// Extract the action type / name from the last part of the
				// given
				// location URI and split it after the "=" (../?action=stop)
				String[] actionName = getReference()
						.getRemainingPart()
						.subSequence(1,
								getReference().getRemainingPart().length())
						.toString().split("\\=");
				LOGGER.debug("Action Name: " + actionName[1]);

				// Check if actionName[1] is set
				if (actionName.length >= 2) {
					// Call the Start action of the compute resource
					if (actionName[1].equalsIgnoreCase("start")) {
						LOGGER.debug("Start Action called.");
						LOGGER.debug(xoccimap.toString());
						compute.getStart().execute(URI.create(location),
								Start.valueOf((String) xoccimap.get("method")));
						// ///////////////////////////////////////////////////////////////////////////
						// Set the current state of the compute resource.
						// This modification was done due polling
						// requirement of
						// the OpenStack server which is used in proactive
						// scheduler.
						if (compute.getHostname().equalsIgnoreCase("VIRTUAL")) {
							compute.setState(State.inactive);

						} else {
							compute.setState(State.active);
						}
						// ////////////////////////////////////////////////////////////////////////////
					}

					else if (actionName[1].equalsIgnoreCase("stop")) {
						LOGGER.debug("Stop Action called.");
						// Call the Stop action of the compute resource
						compute.getStop().execute(URI.create(location),
								Stop.valueOf((String) xoccimap.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.inactive);
					}

					else if (actionName[1].equalsIgnoreCase("restart")) {
						LOGGER.debug("Restart Action called.");
						// Call the Restart action of the compute resource
						compute.getRestart()
								.execute(
										URI.create(location),
										Restart.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					}

					else if (actionName[1].equalsIgnoreCase("suspend")) {
						LOGGER.debug("Suspend Action called.");
						// Call the Suspend action of the compute resource
						compute.getSuspend()
								.execute(
										URI.create(location),
										Suspend.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.suspended);
					} else if (actionName[1].equalsIgnoreCase("clone")) {
						LOGGER.debug("Clone Action called.");
						// Call the Clone action of the compute resource
						compute.getClone().execute(URI.create(location),
								Clone.valueOf((String) xoccimap.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else if (actionName[1].equalsIgnoreCase("collate")) {
						LOGGER.debug("Collate Action called.");
						// Call the Collate action of the compute resource
						compute.getCollate()
								.execute(
										URI.create(location),
										Collate.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else if (actionName[1].equalsIgnoreCase("decollate")) {
						LOGGER.debug("De collate Action called.");
						// Call the Collate action of the compute resource
						compute.getDeCollate().execute(
								URI.create(location),
								DeCollate.valueOf((String) xoccimap
										.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else if (actionName[1].equalsIgnoreCase("snapshot")) {
						LOGGER.debug("Snapshot Action called.");
						// Call the snapshot action of the compute resource
						compute.getComputeSnapshot()
								.execute(
										URI.create(location),
										occi.infrastructure.compute.actions.SnapshotAction.Snapshot
												.valueOf((String) xoccimap
														.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else if (actionName[1].equalsIgnoreCase("migrate")) {
						LOGGER.debug("Migrate Action called.");
						// Call the migrate action of the compute resource
						compute.getMigrate()
								.execute(
										URI.create(location),
										Migrate.valueOf((String) xoccimap
												.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else if (actionName[1].equalsIgnoreCase("sla")) {
						LOGGER.debug("Sla Action called.");
						// Call the Collate action of the compute resource
						compute.getSla().execute(URI.create(location),
								Sla.valueOf((String) xoccimap.get("method")));
						// Set the current state of the compute resource
						compute.setState(State.active);
					} else {
						getResponse()
								.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
						return "Invalid action type"+Response.getCurrent().toString();
					}
				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return Response.getCurrent().toString();

				}
				getResponse().setStatus(Status.SUCCESS_OK);
				return Response.getCurrent().toString();
			}

		} catch (Exception e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			e.printStackTrace();
			return "Exception caught: " + e.toString() + "\n";
		}
	}

	@Get
	public String getOCCIRequest() {
		// set occi version info
		getServerInfo().setAgent(
				OcciConfig.getInstance().config.getString("occi.version"));
		LOGGER.debug("getReference().getLastSegment(): "
				+ getReference().getLastSegment().toString());
		try {
			Form requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			String acceptCase = OcciCheck.checkCaseSensitivity(
					requestHeaders.toString()).get("accept");
			// put all attributes into a buffer for the response
			StringBuffer buffer = new StringBuffer();
			Representation representation = null;
			if (!getReference().getLastSegment().equals("compute")) {
				OcciCheck.isUUID(getReference().getLastSegment());
				// get the compute instance by the given UUID
				Compute compute = Compute.getComputeList().get(
						UUID.fromString(getReference().getLastSegment()));

				// put all attributes into a buffer for the response
				StringBuffer linkBuffer = new StringBuffer();
				StringBuffer mixinBuffer = new StringBuffer();
				// put all attributes into a buffer for the response
				buffer.append(" Category: ")
						.append(compute.getKind().getTerm())
						.append(" scheme= ")
						.append(compute.getKind().getScheme())
						.append(" class=\"kind\";").append(" Architecture: ")
						.append(compute.getArchitecture()).append(" Cores: ")
						.append(compute.getCores()).append(" Hostname: ")
						.append(compute.getHostname()).append(" Memory: ")
						.append(compute.getMemory()).append(" Speed: ")
						.append(compute.getSpeed()).append(" State: ")
						.append(compute.getState());

				if (!compute.getLinks().isEmpty()) {
					linkBuffer.append(" Link: ");
				}
				for (Link l : compute.getLinks()) {
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
								+ networkInterface.getTarget().getKind()
										.getTerm() + "/"
								+ networkInterface.getTarget().getId());
						linkBuffer.append(" occi.core.source=/"
								+ networkInterface.getLink().getKind()
										.getTerm() + "/" + compute.getId());
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
							linkBuffer
									.append(" occi.networkinterface.allocation="
											+ ipNetworkInterface
													.getAllocation());
						}

					}
					buffer.append(linkBuffer);
					LOGGER.debug("Links: " + linkBuffer.toString());
				}
				for (Mixin mixin : Mixin.getMixins()) {
					if (mixin.getEntities() != null) {
						if (mixin.getEntities().contains(compute)) {
							mixinBuffer.append(" ");
							mixinBuffer.append("Category: " + mixin.getTitle()
									+ "; scheme=\"" + mixin.getScheme()
									+ "\"; class=\"mixin\"");
							if (mixin instanceof OSTemplate) {
								OSTemplate osTemplate = (OSTemplate) mixin;
								mixinBuffer.append("occi.os_tpl.term: "
										+ osTemplate.getOsTerm());
							} else if (mixin instanceof SlaTemplate) {
								SlaTemplate slaTemplate = (SlaTemplate) mixin;
								mixinBuffer.append("occi.resource_tpl.sla: "
										+ slaTemplate.getSlaName());
							}
						}
					}
				}
				buffer.append(mixinBuffer);
				LOGGER.debug("Mixin: " + mixinBuffer);
				// access the request headers and get the Accept attribute
				representation = OcciCheck.checkContentType(requestHeaders,
						buffer, getResponse());
				// Check the accept header
				if (requestHeaders.getFirstValue(acceptCase)
						.equals("text/occi")) {
					// generate header rendering
					this.occiCheck.setHeaderRendering(null, compute,
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
				// initialize compute list
				Map<UUID, Compute> computeList = Compute.getComputeList();
				Compute compute = null;
				// iterate through all available compute resources
				int i = 1;
				for (UUID id : computeList.keySet()) {
					compute = computeList.get(id);
					buffer.append(getReference());
					buffer.append(compute.getId());
					if (i < computeList.size()) {
						buffer.append(",");
					}
					i++;
				}
				representation = OcciCheck.checkContentType(requestHeaders,
						buffer, getResponse());
				getResponse().setEntity(representation);
				if (computeList.size() <= 0) {
					// return http status code
					getResponse().setStatus(Status.SUCCESS_NO_CONTENT,
							buffer.toString());
					return "There are no compute resources";
				} else if (representation.getMediaType().toString()
						.equals("text/occi")) {
					// Set Location Attribute
					setLocationRef(buffer.toString());
					// return http status code
					getResponse().setStatus(Status.SUCCESS_OK, " ");
				} else {
					// return http status code
					getResponse().setStatus(Status.SUCCESS_OK);
				}
				return " ";
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return "UUID(" + UUID.fromString(getReference().getLastSegment())
					+ ") not found! " + e.toString() + "\n";
		} catch (Exception e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			return e.toString();
		}
	}

	private void deleteComputeResourceById(String uuid) {

		try {
			DeleteAction deleteAction = new DeleteAction();
			deleteAction.execute(new URI(uuid), null);
		} catch (ResourceException re) {
			throw re;
		} catch (URISyntaxException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					e.getMessage());
			throw new RuntimeException("URI Syntax error.");
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
		LOGGER.debug("Incoming delete request");
		try {
			if (getReference().getLastSegment().equals("compute")) {
				Map<UUID, Compute> computeList = Compute.getComputeList();
				Set<UUID> keySet = computeList.keySet();
				Iterator<UUID> iterator = keySet.iterator();
				while (iterator.hasNext()) {
					UUID next = iterator.next();
					deleteComputeResourceById(next.toString());
					iterator.remove();
				}
			} else {
				OcciCheck.isUUID(getReference().getLastSegment());
				Compute compute = Compute.getComputeList().get(
						UUID.fromString(getReference().getLastSegment()));
				deleteComputeResourceById(compute.getId().toString());
				// remove it from compute resource list
				if (Compute.getComputeList().remove(
						UUID.fromString(compute.getId().toString())) == null) {
					getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					throw new RuntimeException("Compute resource : "
							+ compute.getId().toString()
							+ " cannot be deleted.");
				}
			}
			getResponse().setStatus(Status.SUCCESS_OK);
			return " ";
		} catch (ResourceException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			return "Exception caught :" + e.toString();
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
	public String putOCCIRequest(Representation representation)
			throws Exception {
		try {
			// set occi version info
			getServerInfo().setAgent(
					OcciConfig.getInstance().config.getString("occi.version"));
			OcciCheck.isUUID(getReference().getLastSegment());
			Compute compute = Compute.getComputeList().get(
					UUID.fromString(getReference().getLastSegment()));
			// access the request headers and get the X-OCCI-Attribute
			Form requestHeaders = (Form) getRequest().getAttributes().get(
					"org.restlet.http.headers");
			LOGGER.debug("Raw Request Headers: " + requestHeaders);
			String caseAttributes = OcciCheck.checkCaseSensitivity(
					requestHeaders.toString()).get("x-occi-attribute");
			String xocciattributes = "";
			xocciattributes = requestHeaders.getFirstValue(caseAttributes);

			LOGGER.debug("X-OCCI-Attributes != null?: "
					+ xocciattributes.isEmpty());
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
				LOGGER.debug("Tokens in XOCCIList: " + xoccilist.countTokens());
				while (xoccilist.hasMoreTokens()) {
					String[] temp = xoccilist.nextToken().split("\\=");
					if (temp.length > 1 && temp[0] != null && temp[1] != null) {
						xoccimap.put(temp[0], temp[1]);
					}
				}
				LOGGER.debug("X-OCCI-Map empty?: " + xoccimap.isEmpty());

				if (xoccimap.isEmpty()) {
					getResponse().setStatus(Status.SUCCESS_OK);
					return "Nothing changed";
				} else {
					// Change the compute attribute if it is send by the request
					if (xoccimap.containsKey("occi.compute.architecture")) {
						LOGGER.info((String) xoccimap
								.get("occi.compute.architecture"));
						compute.setArchitecture(Architecture
								.valueOf((String) xoccimap
										.get("occi.compute.architecture")));
					}
					if (xoccimap.containsKey("occi.compute.cores")) {
						LOGGER.info((String) xoccimap.get("occi.compute.cores"));
						compute.setCores(Integer.parseInt(xoccimap.get(
								"occi.compute.cores").toString()));
					}
					if (xoccimap.containsKey("occi.compute.hostname")) {
						LOGGER.info((String) xoccimap
								.get("occi.compute.hostname"));
						compute.setHostname((String) xoccimap
								.get("occi.compute.hostname"));
					}
					if (xoccimap.containsKey("occi.compute.memory")) {
						LOGGER.info((String) xoccimap
								.get("occi.compute.memory"));
						compute.setMemory(Float.parseFloat(xoccimap.get(
								"occi.compute.memory").toString()));
					}
					if (xoccimap.containsKey("occi.compute.speed")) {
						LOGGER.info((String) xoccimap.get("occi.compute.speed"));
						compute.setSpeed(Float.parseFloat(xoccimap.get(
								"occi.compute.speed").toString()));
					}
					if (xoccimap.containsKey("occi.compute.state")) {
						LOGGER.info((String) xoccimap.get("occi.compute.state"));
						compute.setState(State.valueOf((String) xoccimap
								.get("occi.compute.state")));
					}

					// Location Rendering in HTTP Header, not in body
					setLocationRef(getRootRef().toString());

					// set response status
					getResponse().setStatus(Status.SUCCESS_OK);
					return Response.getCurrent().toString();
				}
			}
			// Catch possible exceptions
		} catch (Exception e) {

			LOGGER.error("Exception caught: " + e.toString());
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					e.toString());
			return "Exception: " + e.getMessage() + "\n";
		}
		return null;
	}
}