package org.knime.knip.patents.nodes;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

public abstract class AbstractPatentDownloadNodeDialog extends
		DefaultNodeSettingsPane {

	public AbstractPatentDownloadNodeDialog() {
		createNewGroup("OAuth2 Settings");

		addDialogComponent(new DialogComponentString(
				AbstractPatentDownloadNodeModel.createConsumerKeyModel(),
				"Consumer Key"));

		addDialogComponent(new DialogComponentString(
				AbstractPatentDownloadNodeModel.createConsumerSecretModel(),
				"Consumer Secret"));
	}

}
