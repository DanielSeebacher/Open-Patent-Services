package org.knime.knip.patents.nodes;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public abstract class AbstractPatentDownloadNodeModel extends NodeModel {

	public AbstractPatentDownloadNodeModel(final int in, final int out) {
		super(in, out);
	}

	public static SettingsModelString createConsumerKeyModel() {
		return new SettingsModelString("m_consumerKey", "");
	}

	public static SettingsModelString createConsumerSecretModel() {
		return new SettingsModelString("m_consumerSecret", "");
	}

	protected final SettingsModelString m_consumerKey = createConsumerKeyModel();
	protected final SettingsModelString m_consumerSecret = createConsumerSecretModel();

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_consumerKey.saveSettingsTo(settings);
		m_consumerSecret.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_consumerKey.validateSettings(settings);
		m_consumerSecret.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_consumerKey.loadSettingsFrom(settings);
		m_consumerSecret.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub

	}

}
