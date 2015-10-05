package org.knime.knip.patents.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.knip.patents.KNIMEOPSPlugin;

public class OPSPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = KNIMEOPSPlugin.getDefault()
				.getPreferenceStore();

		store.setDefault(OPSPreferenceConstants.P_ENABLE_THROTTLING, true);
		store.setDefault(OPSPreferenceConstants.P_OAUTH2_CONSUMER_KEY, "");
		store.setDefault(OPSPreferenceConstants.P_OAUTH2_CONSUMER_SECRET, "");
	}

}
