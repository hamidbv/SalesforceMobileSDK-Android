/*
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.hub;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.auth.OAuth2;
import com.salesforce.androidsdk.auth.OAuth2.TokenEndpointResponse;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.AccountInfoNotFoundException;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.ui.LoginActivity;
import com.salesforce.androidsdk.util.CookieHelper;

/**
 * Delegated login activity (invoked by other apps)
 * 
 * If the user has logged in through in the hub app then, the login screen is loaded with the auth token sid 
 * which causes it to go straight into the approval page.
 * 
 */
public class DelegatedLoginActivity extends LoginActivity {

    @Override
    public void onResume() {
    	CookieSyncManager.getInstance().startSync();
    	super.onResume();
    }
    
    @Override
    public void onPause() {
    	CookieSyncManager.getInstance().stopSync();
    	super.onPause();
    }
	
	@Override
	protected void setupWebViewCookies() {
		String accountType = ForceApp.APP.getAccountType();
    	LoginOptions loginOptions = new LoginOptions(
    			ForceApp.APP.getPasscodeHash(),
    			getString(R.string.oauth_callback_url),
    			getString(R.string.oauth_client_id),
    			new String[] {"web"});
		
		try {
			// TODO we should check if the auth token needs to be refreshed
			RestClient client = new ClientManager(this, accountType, loginOptions).peekRestClient();
			if (client != null) {
				Log.i("DelegatedLoginActivity", "Setting cookies on webview: " + client.getClientInfo());
				CookieHelper.setSidCookies(webView, client);
			}
				
		} catch (AccountInfoNotFoundException e) {
			Log.w("DelegatedLoginActivity", e);
		}
	}

	@Override
	protected void onAuthFlowComplete(TokenEndpointResponse tr) {
		FinishDelegatedAuthTask t = new FinishDelegatedAuthTask();
		t.execute(tr);
	}
	
	protected class FinishDelegatedAuthTask extends
			AsyncTask<TokenEndpointResponse, Boolean, Intent> {

		@Override
		protected final Intent doInBackground(
				TokenEndpointResponse... params) {
			try {
				publishProgress(true);

				TokenEndpointResponse tr= params[0];

				String username = OAuth2.getUsernameFromIdentityService(
				HttpAccess.DEFAULT, tr.idUrlWithInstance, tr.authToken);
			
				Intent result = new Intent();
				result.putExtra("clientId", loginOptions.oauthClientId);
				result.putExtra("orgId", tr.orgId);
				result.putExtra("userId", tr.userId);
				result.putExtra("username", username);
				result.putExtra("instanceUrl", tr.instanceUrl);
				result.putExtra("loginUrl", loginOptions.loginUrl);
				result.putExtra("authToken", tr.authToken);
				result.putExtra("refreshToken", tr.refreshToken);
				return result;
			} 
			catch (Exception ex) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Intent result) {
			if (result != null) {
				setResult(RESULT_OK, result);
			}
			else {
				setResult(RESULT_CANCELED);
			}

			finish();
		}

		@Override
		protected void onProgressUpdate(Boolean... values) {
			setProgressBarIndeterminateVisibility(values[0]);
			setProgressBarIndeterminate(values[0]);
		}
	}
}