package org.kurento.tutorial.n_one2manycall;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Created by jingbiaowang on 2015/8/27.
 */
public class NUserSession {
    public enum UserType {
        PRESENTER, VIEWER
    }

    private static final Logger log = LoggerFactory.getLogger(NUserSession.class);
    private WebSocketSession session;
    private WebRtcEndpoint webRtcEndpoint;
    private UserType userType;

    public NUserSession(WebSocketSession session) {
        this.session = session;
    }

    public NUserSession(final WebSocketSession session, WebRtcEndpoint webRtcEndpoint) throws Exception {
        this.session = session;
        this.webRtcEndpoint = webRtcEndpoint;
        addIceListener();
    }


    private void addIceListener() throws Exception {

        webRtcEndpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
            @Override
            public void onEvent(OnIceCandidateEvent onIceCandidateEvent) {

                ResponseParameter response = new ResponseParameter();
                response.setId(ResponseParameter.ResponeIdType.iceCandidate.name());

                try {
                    synchronized (session) {
                        if (session.isOpen())
                            log.debug("---session is open on iceCandidateEvent");
                        else
                            log.debug("---session isn't open on iceCandidateEvent");
                        response.setResponse(ResponseParameter.ResponseType.accepted.name());
                        response.setCandidate(onIceCandidateEvent.getCandidate());
                        session.sendMessage(new TextMessage(JsonUtils.toJson(response)));
                        log.info("********** iceCandidate send msg success.");
                    }
                } catch (IOException e) {
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    log.error("********** addOnIceCandidateListener failed : {} ." + e.getMessage());
                    response.setMessage(e.getMessage());
                    try {
                        session.sendMessage(new TextMessage(JsonUtils.toJson(response)));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }


            }
        });


    }

    public String processOffer(String sdpOffer) {

        return webRtcEndpoint.processOffer(sdpOffer);
    }

    public void sendMessage(String response) throws IOException {

        synchronized (session) {
            session.sendMessage(new TextMessage(response));
        }
    }

    public void release() throws IOException {
        if (webRtcEndpoint != null)
            webRtcEndpoint.release();
        if (userType == UserType.PRESENTER && session.isOpen())           //关闭socket链接。
            session.close();
    }

    public void gatherCandidates() {
        webRtcEndpoint.gatherCandidates();
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
    }

    public void addCandidate(IceCandidate candidate) {
        log.info("--candidate msg : index : '{}', mid : {}, Candidate : {}", candidate.getSdpMLineIndex(), candidate.getSdpMid(), candidate.getCandidate());
        log.debug("---add candidate session is open '{}'", session.isOpen());
        webRtcEndpoint.addIceCandidate(candidate);
        log.debug("---after add candidate session is open '{}'", session.isOpen());


    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}
