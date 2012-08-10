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

package occi.infrastructure.storage.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.naming.directory.SchemaViolationException;

import occi.core.Action;
import occi.core.Category;
import occi.core.Method;
import occi.infrastructure.Compute;
import occi.infrastructure.Storage;
import occi.infrastructure.injection.Injection;
import occi.infrastructure.interfaces.StorageInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfflineAction extends Action {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(OfflineAction.class);

	/**
	 * Enum for the Start Actions.
	 */
	public enum Offline implements Method {
		offline
	}

	private Offline offline;

	private static StorageInterface storageInterface = Injection
			.getStorageInterface();

	public OfflineAction() throws SchemaViolationException, URISyntaxException {
		Category category = new Category("offline",
				"http://schemas.ogf.org/occi/infrastructure/storage/action#",
				"Action");

	}

	@Override
	public void execute(URI uri, Method method) {
		Storage storage = null;
		String uriString = uri.toString();
		uriString = uriString.substring(0, uri.toString().length());
		uriString = uriString.substring(uriString.length() - 36);

		LOGGER.debug("URI " + uriString);
		UUID storageUuid = UUID.fromString(uriString);
		LOGGER.debug("UUID " + storageUuid.toString());
		for (UUID uuid : Compute.getComputeList().keySet()) {
			if (uuid.equals(storageUuid)) {
				storage = Storage.getStorageList().get(storageUuid);
			}
		}
		storageInterface.offlineStorage(storage);

	}

	/**
	 * @param offline the offline to set
	 */
	public void setOffline(Offline offline) {
		this.offline = offline;
	}

	/**
	 * @return the offline
	 */
	public Offline getOffline() {
		return offline;
	}
}