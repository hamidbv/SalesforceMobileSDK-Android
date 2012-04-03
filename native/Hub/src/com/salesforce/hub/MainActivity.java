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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient;

/**
 * Main activity for hub
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup view
		setContentView(R.layout.main);
	}
	
	@Override 
	public void onResume() {
		super.onResume();
		
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);
		
		/*
		 * Un-comment this block to have the passcode screen
		
		// Bring up passcode screen if needed
		ForceApp.APP.getPasscodeManager().lockIfNeeded(this, true);
		
		// Do nothing - when the app gets unlocked we will be back here
		if (ForceApp.APP.getPasscodeManager().isLocked()) {
			return;
		}
		
		*/
		
		// Login options
		String accountType = ForceApp.APP.getAccountType();
    	LoginOptions loginOptions = new LoginOptions(
    			ForceApp.APP.getPasscodeHash(),
    			getString(R.string.oauth_callback_url),
    			getString(R.string.oauth_client_id),
    			new String[] {"full"});
		
		// Get a rest client
		new ClientManager(this, accountType, loginOptions).getRestClient(this, new RestClientCallback() {
			@Override
			public void authenticatedRestClient(RestClient client) {
				if (client == null) {
					ForceApp.APP.logout(MainActivity.this);
					return;
				}
				
				// Show everything
				findViewById(R.id.root).setVisibility(View.VISIBLE);

				// Show welcome
				((TextView) findViewById(R.id.welcome_text)).setText(getString(R.string.welcome, client.getClientInfo().username));
				
			}
		});
	}
	
	@Override
	public void onUserInteraction() {
		/*
		 * Un-comment this block if you usethe passcode screen

		ForceApp.APP.getPasscodeManager().recordUserInteraction();
		
		*/
	}

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		ForceApp.APP.logout(this);
	}
}