package org.knime.knip.patents.features;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.patents.features.binary.ahdh.AHDHNodeFactory;
import org.knime.knip.patents.features.binary.ahdh.SimpleAHDHNodeFactory;
import org.knime.knip.patents.features.binary.ahgc.AHGCNodeFactory;

public class FeaturesNodeSetFactory implements NodeSetFactory {

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
		m_nodeFactories.put(AHGCNodeFactory.class.getCanonicalName(),
				"/community/knip/patent/features/binary");
		m_nodeFactories.put(SimpleAHDHNodeFactory.class.getCanonicalName(),
				"/community/knip/patent/features/binary");
		m_nodeFactories.put(AHDHNodeFactory.class.getCanonicalName(),
				"/community/knip/patent/features/binary");
		return m_nodeFactories.keySet();
	}

}
