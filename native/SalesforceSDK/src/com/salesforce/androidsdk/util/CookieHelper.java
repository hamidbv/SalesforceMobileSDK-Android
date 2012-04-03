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
package com.salesforce.androidsdk.util;

import android.os.SystemClock;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.salesforce.androidsdk.rest.RestClient;

/**
 * Helper class for managing cookies
 */
public class CookieHelper {
    /**
     * Set cookies on cookie manager
     * @param client
     */
	public static void setSidCookies(WebView webView, RestClient client) {
    	Log.i("CookieHelper.setSidCookies", "setting cookies");
    	CookieSyncManager cookieSyncMgr = CookieSyncManager.getInstance();    	
    	CookieManager cookieMgr = CookieManager.getInstance();
    	cookieMgr.setAcceptCookie(true);  // Required to set additional cookies that the auth process will return.
    	cookieMgr.removeSessionCookie();
    	
    	SystemClock.sleep(250); // removeSessionCookies kicks out a thread - let it finish

    	String accessToken = client.getAuthToken();
    	
    	// Android 3.0+ clients want to use the standard .[domain] format. Earlier clients will only work
    	// with the [domain] format.  Set them both; each platform will leverage its respective format.
    	addSidCookieForDomain(cookieMgr,"salesforce.com", accessToken);
    	addSidCookieForDomain(cookieMgr,".salesforce.com", accessToken);

	    cookieSyncMgr.sync();
    }

    private static void addSidCookieForDomain(CookieManager cookieMgr, String domain, String sid) {
        String cookieStr = "sid=" + sid;
    	cookieMgr.setCookie(domain, cookieStr);
    }
}
