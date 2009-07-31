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
import java.util.HashMap;
import java.util.Map;

import com.google.android.collect.Maps;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.Future1;
import android.accounts.Future1Callback;
import android.accounts.OnAccountsUpdatedListener;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.util.Log;

class AccountPreferenceBase extends PreferenceActivity implements OnAccountsUpdatedListener {
    protected static final String TAG = "AccountSettings";
    public static final String AUTHORITIES_FILTER_KEY = "authorities";
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription
            = new HashMap<String, AuthenticatorDescription>();
    protected AuthenticatorDescription[] mAuthDescs;
    private final Handler mHandler = new Handler();
    private Object mStatusChangeListenerHandle;
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    /**
     * Overload to handle account updates.
     */
    public void onAccountsUpdated(Account[] accounts) {

    }

    /**
     * Overload to handle authenticator description updates
     */
    protected void onAuthDescriptionsUpdated() {

    }

    /**
     * Overload to handle sync state updates.
     */
    protected void onSyncStateUpdated() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                | ContentResolver.SYNC_OBSERVER_TYPE_STATUS
                | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS,
                mSyncStatusObserver);
        onSyncStateUpdated();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
    }


    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            mHandler.post(new Runnable() {
                public void run() {
                    onSyncStateUpdated();
                }
            });
        }
    };

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        if (mAccountTypeToAuthorities == null) {
            mAccountTypeToAuthorities = Maps.newHashMap();
            SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
            for (int i = 0, n = syncAdapters.length; i < n; i++) {
                final SyncAdapterType sa = syncAdapters[i];
                ArrayList<String> authorities = mAccountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    mAccountTypeToAuthorities.put(sa.accountType, authorities);
                }
                Log.d(TAG, "added authority " + sa.authority + " to accountType " + sa.accountType);
                authorities.add(sa.authority);
            }
        }
        return mAccountTypeToAuthorities.get(type);
    }

    /**
     * Gets an icon associated with a particular account type. If none found, return null.
     * Note this should only be called after {@link #onAuthDescriptionsUpdated()} has been called
     * since AuthenticatorDescriptions are fetched asynchronously.
     *
     * @param account the type of account
     * @return a drawable for the icon or null if one cannot be found.
     */
    protected Drawable getDrawableForType(final String accountType) {
        Drawable icon = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = (AuthenticatorDescription)
                        mTypeToAuthDescription.get(accountType);
                Context authContext = createPackageContext(desc.packageName, 0);
                icon = authContext.getResources().getDrawable(desc.iconId);
            } catch (PackageManager.NameNotFoundException e) {
                // TODO: place holder icon for missing account icons
                Log.w(TAG, "No icon for account type " + accountType);
            }
        }
        return icon;
    }

    /**
     * Gets the label associated with a particular account type. If none found, return null.
     * Note this should only be called after {@link #onAuthDescriptionsUpdated()} has been called
     * since AuthenticatorDescriptions are fetched asynchronously.
     * @param account the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    protected CharSequence getLabelForType(final String accountType) {
        CharSequence label = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
             try {
                 AuthenticatorDescription desc = (AuthenticatorDescription)
                         mTypeToAuthDescription.get(accountType);
                 Context authContext = createPackageContext(desc.packageName, 0);
                 label = authContext.getResources().getText(desc.labelId);
             } catch (PackageManager.NameNotFoundException e) {
                 // TODO: place holder icon for missing account icons?
                 Log.w(TAG, "No label for account type " + ", type " + accountType);
             }
        }
        return label;
    }

    /**
     * Asynchronously updates provider icons. Subclasses should call this in onCreate()
     * and update any UI that depends on AuthenticatorDescriptions in onAuthDescriptionsUpdated().
     */
    protected void updateAuthDescriptions() {
        AccountManager.get(this).getAuthenticatorTypes(
                new Future1Callback<AuthenticatorDescription[]>() {
            public void run(Future1<AuthenticatorDescription[]> future) {
                if (isFinishing()) return;

                try {
                    mAuthDescs = future.getResult();
                    for (int i = 0; i < mAuthDescs.length; i++) {
                        mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);
                    }
                } catch (OperationCanceledException e) {
                    // the request was canceled
                    Log.w(TAG, "getAuthenticatorTypes(): Operation was canceled");
                }
                onAuthDescriptionsUpdated();
            }
        }, mHandler);
    }
}
