/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.test;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.net.Uri;

/**
 * If you would like to test sync a single provider with an
 * {@link InstrumentationTestCase}, this provides some of the boiler plate in {@link #setUp} and
 * {@link #tearDown}.
 */
public class SyncBaseInstrumentation extends InstrumentationTestCase {
    private Context mTargetContext;
    ContentResolver mContentResolver;
    private static final int MAX_TIME_FOR_SYNC_IN_MINS = 20;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTargetContext = getInstrumentation().getTargetContext();
        mContentResolver = mTargetContext.getContentResolver();
    }

    /**
     * Syncs the specified provider.
     * @throws Exception
     */
    protected void syncProvider(Uri uri, String account, String authority) throws Exception {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
        extras.putString(ContentResolver.SYNC_EXTRAS_ACCOUNT, account);

        mContentResolver.startSync(uri, extras);
        long startTimeInMillis = SystemClock.elapsedRealtime();
        long endTimeInMillis = startTimeInMillis + MAX_TIME_FOR_SYNC_IN_MINS * 60000;

        int counter = 0;
        // Making sure race condition does not occur when en entry have been removed from pending
        // and active tables and loaded in memory (therefore sync might be still in progress)
        while (counter < 2) {
            // Sleep for 1 second.
            Thread.sleep(1000);
            // Finish test if time to sync has exceeded max time.
            if (SystemClock.elapsedRealtime() > endTimeInMillis) {
                break;
            }

            if (isSyncActive(account, authority)) {
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    protected void cancelSyncsandDisableAutoSync() {
        try {
            ContentResolver.getContentService().setListenForNetworkTickles(false);
        } catch (RemoteException e) {
        }
        mContentResolver.cancelSync(null);
    }

    /**
     * This method tests if any sync is active or not. Sync is considered to be active if the
     * entry is in either the Pending or Active tables.
     * @return
     */
    private boolean isSyncActive(String account, String authority) {
        try {
            return ContentResolver.getContentService().isSyncActive(account,
                    authority);
        } catch (RemoteException e) {
            return false;
        }
    }
}
