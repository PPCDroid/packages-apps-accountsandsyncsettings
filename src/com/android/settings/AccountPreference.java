/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import java.util.ArrayList;

import com.android.providers.subscribedfeeds.R;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * AccountPreference is used to display a username, status and provider icon for an account on
 * the device.
 */
public class AccountPreference extends Preference {
    private static final String TAG = "AccountPreference";
    public static final int SYNC_ALL_OK = 0; // all know sync adapters are enabled and OK
    public static final int SYNC_SOME_OK = 1;  // some sync adapters enabled and OK
    public static final int SYNC_NONE = 2; // no sync adapters are enabled
    public static final int SYNC_ERROR = 3; // one or more sync adapters have a problem
    private int mStatus;
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private Drawable mProviderIcon;
    private ImageView mSyncStatusIcon;
    private ImageView mProviderIconView;

    public AccountPreference(Context context, Account account, Drawable icon,
            ArrayList<String> authorities) {
        super(context);
        mAccount = account;
        mAuthorities = authorities;
        mProviderIcon = icon;
        setLayoutResource(R.layout.account_preference);
        setTitle(mAccount.mName);
        setSummary(R.string.signed_in_sync_disabled);
        // Add account info to the intent for AccountSyncSettings
        Intent intent = new Intent("android.settings.ACCOUNT_SYNC_SETTINGS");
        intent.putExtra("account", mAccount);
        setIntent(intent);
        setPersistent(false);
        setSyncStatus(SYNC_NONE);
    }

    public Account getAccount() {
        return mAccount;
    }

    public ArrayList<String> getAuthorities() {
        return mAuthorities;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        setSummary(getSyncStatusMessage(mStatus));
        mProviderIconView = (ImageView) view.findViewById(R.id.providerIcon);
        mProviderIconView.setImageDrawable(mProviderIcon);
        mSyncStatusIcon = (ImageView) view.findViewById(R.id.syncStatusIcon);
        mSyncStatusIcon.setImageResource(getSyncStatusIcon(mStatus));
    }

    public void setProviderIcon(Drawable icon) {
        mProviderIcon = icon;
        if (mProviderIconView != null) {
            mProviderIconView.setImageDrawable(icon);
        }
    }

    public void setSyncStatus(int status) {
        mStatus = status;
        if (mSyncStatusIcon != null) {
            mSyncStatusIcon.setImageResource(getSyncStatusIcon(status));
        }
        setSummary(getSyncStatusMessage(status));
    }

    private int getSyncStatusMessage(int status) {
        int res;
        switch (status) {
            case SYNC_ALL_OK:
                res = R.string.signed_in_synced;
                break;
            case SYNC_SOME_OK:
                res = R.string.singed_in_synced_partial;
                break;
            case SYNC_NONE:
                res = R.string.signed_in_sync_disabled;
                break;
            case SYNC_ERROR:
                res = R.string.signed_in_sync_error;
                break;
            default:
                res = R.string.signed_in_sync_error;
                Log.e(TAG, "Unknown sync status: " + status);
        }
        return res;
    }

    private int getSyncStatusIcon(int status) {
        int res;
        switch (status) {
            case SYNC_ALL_OK:
                res = R.drawable.ic_signed_in_synced;
                break;
            case SYNC_SOME_OK:
                res = R.drawable.ic_signed_in_synced_partial;
                break;
            case SYNC_NONE:
                res = R.drawable.ic_signed_in_sync_disabled;
                break;
            case SYNC_ERROR:
                res = R.drawable.ic_signed_in_sync_error;
                break;
            default:
                res = R.drawable.ic_signed_in_sync_error;
                Log.e(TAG, "Unknown sync status: " + status);
        }
        return res;
    }

    @Override
    public int compareTo(Preference other) {
        if (!(other instanceof AccountPreference)) {
            // Put other preference types above us
            return 1;
        }
        return mAccount.mName.compareTo(((AccountPreference) other).mAccount.mName);
    }
}
