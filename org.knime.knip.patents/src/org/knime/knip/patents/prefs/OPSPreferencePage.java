package org.knime.knip.patents.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.knip.patents.KNIMEOPSPlugin;

public class OPSPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public OPSPreferencePage() {
		super(GRID);
		setPreferenceStore(KNIMEOPSPlugin.getDefault().getPreferenceStore());
		setDescription("Preferences for the KNIME Open Patent Service Plugin.");
	}

	@Override
	protected void createFieldEditors() {

		addField(new BooleanFieldEditor(
				OPSPreferenceConstants.P_ENABLE_THROTTLING,
				"Enable Self-Throttling Control", getFieldEditorParent()));

		addField(new StringFieldEditor(
				OPSPreferenceConstants.P_OAUTH2_CONSUMER_KEY,
				"OAuth 2 Consumer Key for the Open Patent Services",
				getFieldEditorParent()));
	
		addField(new StringFieldEditor(
				OPSPreferenceConstants.P_OAUTH2_CONSUMER_SECRET,
				"OAuth 2 Consumer Secret for the Open Patent Services",
				getFieldEditorParent()));

	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

}
