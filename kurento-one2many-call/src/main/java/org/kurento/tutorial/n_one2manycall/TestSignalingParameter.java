package org.kurento.tutorial.n_one2manycall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jchsohu on 15-9-6.
 */
public class TestSignalingParameter {

    private static final Logger log = LoggerFactory.getLogger(NCallHandler.class);
    private static final Gson gson = new GsonBuilder().create();

    public static void testMsg(String msg) {

        String myMsg = "{\"id\":\"onIceCandidate\",\"candidate\":\"{\\\"candidate\\\":\\\"candidate:3561384661 1 udp 2122260223 10.2.153.228 54233 typ host generation 0\\\",\\\"sdpMid\\\":\\\"audio\\\",\\\"sdpMLineIndex\\\":0}\"}\n";

    }
}
