package org.knime.knip.patents.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.patents.util.nodes.claims.OPSClaimsNodeFactory;
import org.knime.knip.patents.util.nodes.description.OPSDescriptionNodeFactory;
import org.knime.knip.patents.util.nodes.drawings.OPSImagesNodeFactory;
import org.knime.knip.patents.util.nodes.fullcycle.FullCycleNodeFactory;
import org.knime.knip.patents.util.nodes.search.OPSSearchNodeFactory;

/**
 * NodeSetFactory for Nodes which are used to calculate features.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 */
public class PatentNodeSetFactory implements NodeSetFactory {

	private final Map<String, String> m_nodeFactories = new HashMap<String, String>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfigRO getAdditionalSettings(final String id) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAfterID(final String id) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCategoryPath(final String id) {
		return m_nodeFactories.get(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(
			final String id) {
		try {
			return (Class<? extends NodeFactory<? extends NodeModel>>) Class
					.forName(id);
		} catch (final ClassNotFoundException e) {
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getNodeFactoryIds() {
		m_nodeFactories.put(OPSImagesNodeFactory.class.getCanonicalName(),
				"/community/knip/patents/Utilities/");
		m_nodeFactories.put(OPSSearchNodeFactory.class.getCanonicalName(),
				"/community/knip/patents/Utilities/");
		m_nodeFactories.put(OPSClaimsNodeFactory.class.getCanonicalName(),
				"/community/knip/patents/Utilities/");
		m_nodeFactories.put(OPSDescriptionNodeFactory.class.getCanonicalName(),
				"/community/knip/patents/Utilities/");
		m_nodeFactories.put(FullCycleNodeFactory.class.getCanonicalName(),
				"/community/knip/patents/Utilities/");

		return m_nodeFactories.keySet();
	}

}
