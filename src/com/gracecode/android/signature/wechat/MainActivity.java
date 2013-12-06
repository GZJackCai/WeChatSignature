package com.gracecode.android.signature.wechat;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import android.widget.Toast;
import com.gracecode.android.common.helper.IntentHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "com.gracecode.android.tools.signature.wechat";
    private SharedPreferences mSharedPreferences;
    private ListPreference mPrefPackages;
    private Preference mPrefSignature;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefPackages = (ListPreference) findPreference(getString(R.string.key_packages));
        mPrefSignature = findPreference(getString(R.string.key_signature));
    }

    private void setInstalledPackages() {
        List<PackageInfo> packs = getPackageManager().getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        List<String> name = new ArrayList<>();
        List<String> packageName = new ArrayList<>();
        for (PackageInfo p : packs) {
            // ignore the system apps.
            if (p.versionName == null || (p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            name.add(p.applicationInfo.loadLabel(getPackageManager()).toString());
            packageName.add(p.packageName);
        }

        if (!packageName.isEmpty()) {
            mPrefPackages.setEntries(name.toArray(new String[name.size()]));
            mPrefPackages.setEntryValues(packageName.toArray(new String[packageName.size()]));
            mPrefPackages.setEnabled(true);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        setInstalledPackages();
        updateSelectedPackageSummary();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (getString(R.string.key_get_signature).equals(preference.getKey())) {
            String packageName = mSharedPreferences.getString(getString(R.string.key_packages),
                    getString(android.R.string.unknownName));

            if (packageName.equals(getString(android.R.string.unknownName))) {
                Toast.makeText(this, getString(R.string.choose_an_application), Toast.LENGTH_SHORT).show();
                return true;
            }

            String signature = getSignature(packageName);

            mPrefSignature.setTitle(signature);
            Toast.makeText(this, signature, Toast.LENGTH_SHORT).show();
            Log.i(TAG, signature);
        }

        if (getString(R.string.key_signature).equals(preference.getKey())) {
            String signature = (String) mPrefSignature.getTitle();
            if (signature != null && signature.length() > 0) {
                Toast.makeText(this, signature, Toast.LENGTH_SHORT).show();
                IntentHelper.shareText(this, signature);
            }
        }

        if (getString(R.string.key_feedback).equals(preference.getKey())) {
            IntentHelper.sendMail(this, new String[]{getString(R.string.mail)}, getString(R.string.mail_title), "");
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


    private String getSignature(String packageName) {
        String signature = "";

        try {
            PackageInfo a = getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] sign = a.signatures;
            if (sign != null) {
                for (Signature tmp : sign) {
                    signature += MD5.getMessageDigest(tmp.toByteArray());
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return signature;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getString(R.string.key_packages))) {
            updateSelectedPackageSummary();
            mPrefSignature.setTitle("");
        }
    }

    private void updateSelectedPackageSummary() {
        String packageName = mSharedPreferences.getString(getString(R.string.key_packages),
                getString(android.R.string.unknownName));
        mPrefPackages.setSummary(packageName);
    }
}
