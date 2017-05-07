package com.shoeboxscientist.hellorainbow;

import fi.iki.elonen.NanoHTTPD;

public class NanoHttpWebServer extends NanoHTTPD {
    private static final String TAG = "HttpSvr";
    private MainActivity mMainActivity;
    public NanoHttpWebServer(MainActivity ctx) {
        super(8080);
        mMainActivity = ctx;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String resp = mMainActivity.onHttpRequest(session.getParms());
        return new NanoHTTPD.Response(resp);
    }
}
