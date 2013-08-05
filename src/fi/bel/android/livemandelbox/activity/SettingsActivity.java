package fi.bel.android.livemandelbox.activity;

import java.util.Locale;

import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import fi.bel.android.livemandelbox.R;
import fi.bel.android.livemandelbox.service.ViewUpdateService;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
        	.replace(android.R.id.content, new SettingsFragment())
        	.commit();
	}

	public static class SettingsFragment extends PreferenceFragment {
		private final Handler handler = new Handler();
		protected boolean update;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs);
		}

		@Override
		public void onResume() {
			super.onResume();
			update = true;
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (update) {
						updateStatus();
						handler.postDelayed(this, 1000);
					}
				}
			}, 0);
		}

		@Override
		public void onPause() {
			super.onPause();
			update = false;
		}

		/* FIXME: query actual state from ViewUpdateService. */
		public void updateStatus() {
			Preference p = getPreferenceScreen().findPreference("status");
			long now = System.currentTimeMillis();
			long viewTime = ViewUpdateService.getViewTime(getActivity());
			int updateFreq = ViewUpdateService.getUpdateFrequency(getActivity());

			final String time;
			if (viewTime == 0) {
				time = getString(R.string.next_update_unseen);
			} else {
				long timeLeft = viewTime + updateFreq * 1000 - now;
				if (timeLeft < 0) {
					time = getString(R.string.next_update_processing);
				} else {
					timeLeft /= 1000;
					time = String.format(Locale.ROOT, "%02d:%02d:%02d",
							timeLeft / 3600, (timeLeft / 60) % 60, timeLeft % 60);
				}
			}
			p.setTitle(getString(R.string.next_update) + ": " + time);
		}
	}
}
