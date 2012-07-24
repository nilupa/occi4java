/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 * modify it under the terms of the GNU Affero General Public License
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */

package occi.infrastructure.templates;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import javax.naming.directory.SchemaViolationException;

import occi.core.Action;
import occi.core.Entity;
import occi.core.Mixin;

public class OSTemplate extends Mixin {

	public OSTemplate(Set<Action> actions, Set<Mixin> related,
			Set<Entity> entities) throws URISyntaxException,
			SchemaViolationException {
		super(actions, related, entities);
		metaDataMap = new HashMap<String, String>();
		generateAttributeList();
	}
	
	/**
	 * Constructor for the query interface. Necessary.
	 * 
	 * @param term
	 * @param title
	 * @param scheme
	 * @throws SchemaViolationException
	 * @throws URISyntaxException
	 */
	public OSTemplate(Set<Mixin> related, String term, String title,
			String scheme, Set<String> attributes)
			throws SchemaViolationException, URISyntaxException {
		super(related, term, title, scheme, attributes);
		generateAttributeList();
	}

	/**
	 * Map containing meta data values
	 */
	private HashMap<String, String> metaDataMap;	
	
	/**
	 * Returns Hash map which contains meta data
	 */
	public HashMap<String, String> getMetaDataMap() {
		return metaDataMap;
	}

	/**
	 * Static HashSet of all OS template attributes.
	 */
	public static HashSet<String> attributes = new HashSet<String>();
	
	private String term;
	
	/**
	 * Generate attribute List.
	 */
	public static void generateAttributeList() {
		if (attributes.isEmpty()) {
			// add all attributes to attribute list
			attributes.add("procci.os_tpl.metadata.map");
			attributes.add("procci.os_tpl.term");
		}
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}
	
	
}
