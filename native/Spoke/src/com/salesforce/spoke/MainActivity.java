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
package com.salesforce.spoke;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.ClientInfo;

/**
 * Main activity for Spoke
 * 
 * Spoke calls out to the hub to get its access token
 * - hub does a full login if it doesn't have an access token already
 * - hub only does the approval step otherwise
 * 
 */
public class MainActivity extends Activity {

	private static final int DELEGATED_LOGIN = 10;
	private RestClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup view
		setContentView(R.layout.main);

		// Login through hub
		LoginOptions loginOptions = new LoginOptions(
				getString(R.string.app_name), 
				null, 
				null, 
				getString(R.string.oauth_callback_url),
				getString(R.string.oauth_client_id),
				new String[] { "api" });

		Intent intent = new Intent("androidsdk.delegatedLogin");
		intent.putExtras(loginOptions.asBundle());
	    startActivityForResult(intent, DELEGATED_LOGIN);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DELEGATED_LOGIN) { 
			if (resultCode == Activity.RESULT_OK) {
				try {
					String clientId = data.getStringExtra("clientId");
					String instanceUrl = data.getStringExtra("instanceUrl");
					String loginUrl = data.getStringExtra("loginUrl");
					String accountName = data.getStringExtra("accountName");
					String username = data.getStringExtra("username");
					String userId = data.getStringExtra("userId");
					String orgId = data.getStringExtra("orgId");

					ClientInfo clientInfo = new ClientInfo(clientId, new URI(instanceUrl), new URI(loginUrl), accountName, username, userId, orgId);
					String authToken = data.getStringExtra("authToken");
					client = new RestClient(clientInfo, authToken, HttpAccess.DEFAULT, null);

					// Show everything
					findViewById(R.id.root).setVisibility(View.VISIBLE);
		
					// Show welcome
					((TextView) findViewById(R.id.welcome_text)).setText(getString(R.string.welcome, client.getClientInfo().username));
				
				} 
				catch (URISyntaxException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
			else {
				Toast.makeText(this, "Failed to call Hub", Toast.LENGTH_LONG).show();
			}
		}
		else {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}
}