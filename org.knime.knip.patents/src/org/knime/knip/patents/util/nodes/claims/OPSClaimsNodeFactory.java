package org.knime.knip.patents.util.nodes.claims;

import java.util.Arrays;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.base.node.ValueToCellsNodeFactory;
import org.knime.knip.base.node.ValueToCellsNodeModel;

public class OPSClaimsNodeFactory extends ValueToCellsNodeFactory<StringValue> {

	@Override
	protected ValueToCellsNodeDialog<StringValue> createNodeDialog() {
		return new ValueToCellsNodeDialog<StringValue>() {
			@Override
			public void addDialogComponents() {
				addDialogComponent("Options", "Claims Language", new DialogComponentStringSelection(
						OPSClaimsNodeModel.createLanguageModel(), "Claims Language", Arrays.asList("EN", "DE", "FR")));
			}
		};
	}

	@Override
	public ValueToCellsNodeModel<StringValue> createNodeModel() {
		return new OPSClaimsNodeModel();
	}

}
