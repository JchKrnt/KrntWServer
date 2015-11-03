package org.kurento.tutorial.n_one2manycall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.catalina.Pipeline;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

/* Added by Hank */
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.MediaProfileSpecType;
/* END */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by jingbiaowang on 2015/8/27.
 */
public class NCallHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NCallHandler.class);
    private static final Gson gson = new GsonBuilder().create();

    //    @Autowired
    private RoomManager roomManager = new RoomManager();
    @Autowired
    private KurentoClient kurento;

    /* Added by Hank */
    //RecorderEndpoint recorderEndpoint;

    /* END */


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        log.debug("-----webSocket closed by sessionId : '{}'", session.getId());

        stop(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        log.debug("----receive msg from client.---");

        log.debug("Incoming message from session id -'{}' msg payload-{}", session.getId(), message.getPayload());

        SignalingParameter signaling = gson.fromJson(message.getPayload(), SignalingParameter.class);
        log.debug("Incoming message from session id -'{}' msg payload-{}: signal id {}", session.getId(), message.getPayload(), signaling.getId());


        SignalingParameter.SignalingIdType signalingIdType = SignalingParameter.SignalingIdType.valueOf(signaling.getId());
        log.debug("----receive msg from client. signalIdType-:'{}'", signalingIdType.name());
        switch (signalingIdType) {

            case register: {
                log.debug("----receive msg from client----:register case.");
                String roomName = signaling.getRoomName();
                ResponseParameter response = new ResponseParameter();
                response.setId(ResponseParameter.ResponeIdType.register.name());
                if (!roomManager.isExisted(roomName)) {
                    RoomBean room = register(roomName, session);

                    if (room != null) {
                        response.setResponse(ResponseParameter.ResponseType.accepted.name());
                        response.setRoom(room);
                    } else {
                        response.setResponse(ResponseParameter.ResponseType.rejected.name());
                        response.setMessage("resiger room failure.");
                    }
                } else {
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    response.setMessage("The room has existed, modify another name.");
                }

                log.debug("---register result to client : {}", gson.toJson(response));
                session.sendMessage(new TextMessage(gson.toJson(response)));

                break;
            }

            case roomList: {
                log.debug("----receive msg from client---: roomList case.");

                try {
                    ResponseParameter response = new ResponseParameter();

                    if (roomManager.hasRoom()) {
                        response.setId(ResponseParameter.ResponeIdType.roomList.name());
                        response.setRoomsList(roomManager.getRoomList());
                        response.setResponse(ResponseParameter.ResponseType.accepted.name());
                        session.sendMessage(new TextMessage(gson.toJson(response)));

                    } else {
                        response.setId(ResponseParameter.ResponeIdType.roomList.name());
                        response.setResponse(ResponseParameter.ResponseType.accepted.name());
//                        response.setRoomsList(debugRoomData());
                        response.setMessage("no room");
                        session.sendMessage(new TextMessage(gson.toJson(response)));
                    }
                    log.debug("----send msg to client roomlist---:'{}'", gson.toJson(response));
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                    ResponseParameter response = new ResponseParameter();
                    response.setId(ResponseParameter.ResponeIdType.presenterResponse.name());
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    response.setMessage(t.getMessage());
                    session.sendMessage(new TextMessage(gson.toJson(response)));
                }
                break;
            }
            case presenter: {
                log.debug("----receive msg from client---: presenter case.");
                try {
                    present(signaling, session);
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                    stop(session);

                    ResponseParameter response = new ResponseParameter();
                    response.setId(ResponseParameter.ResponeIdType.presenterResponse.name());
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    response.setMessage(t.getMessage());
                    session.sendMessage(new TextMessage(gson.toJson(response)));
                }

                break;
            }
            case viewer: {
                log.debug("----receive msg from client---:viewer case.");
                try {
                    viewer(session, signaling);
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);

                    ResponseParameter response = new ResponseParameter();
                    response.setId(ResponseParameter.ResponeIdType.presenterResponse.name());
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    response.setMessage(t.getMessage());
                    session.sendMessage(new TextMessage(gson.toJson(response)));
                    stop(session);

                }
                break;
            }
            case onIceCandidate: {
                log.debug("----receive msg from client---:onIceCandidate case roomName : {}.", signaling.getRoomName());
                try {

                    Room room = roomManager.getRoomByName(signaling.getRoomName());
                    log.debug("----onIceCandidate case get room-- roomName : {}.", room.getName());

                    NUserSession user = null;
                    if (room.getPresenter().getSession().getId() == session.getId())    //presenter.
                        user = room.getPresenter();
                    else
                        user = room.getViewerBySession().get(session.getId());      //viewer.

                    log.info("----addCandidate user : '{}'", user.getSession().getId());

                    if (user != null) {
                        log.debug("----go to addCandidate user : '{}'", user.getSession().getId());
                        user.addCandidate(signaling.getCandidate());
                    } else {

                        ResponseParameter response = new ResponseParameter();
                        response.setId(ResponseParameter.ResponeIdType.iceCandidate.name());
                        response.setResponse(ResponseParameter.ResponseType.rejected.name());
                        log.error("----The user is null for this candidate in the room named {}" + signaling.getRoomName());
                        response.setMessage("There isn't any user for this candidate in the room {} ." + signaling.getRoomName());
                    }
                } catch (Throwable t) {
                    log.error("onIncCandidate erro : " + t.getMessage(), t);

                    ResponseParameter response = new ResponseParameter();
                    response.setId(ResponseParameter.ResponeIdType.iceCandidate.name());
                    response.setResponse(ResponseParameter.ResponseType.rejected.name());
                    response.setMessage(t.getMessage());
                    session.sendMessage(new TextMessage(gson.toJson(response)));
                    stop(session);

                }
                break;
            }
            case stop: {

                stop(session);
                break;
            }
            default:
                break;
        }

    }

    private ConcurrentHashSet<RoomBean> debugRoomData() {
        ConcurrentHashSet<RoomBean> roomsList = new ConcurrentHashSet<>();
        for (int i = 0; i < 3; i++) {
            RoomBean roomBean = new RoomBean();
            roomBean.setName("room" + i);
            roomBean.setSessionId("sessionId" + i);
            roomsList.add(roomBean);
        }

        return roomsList;
    }

    public RoomBean register(String roomName, WebSocketSession session) throws Exception {
        log.debug("----receive msg from client----:register method.");
        MediaPipeline pipeline = kurento.createMediaPipeline();
        WebRtcEndpoint endpoint = new WebRtcEndpoint.Builder(pipeline).build();

        NUserSession presenter = new NUserSession(session, endpoint);
        Room room = roomManager.register(roomName, presenter);
        RoomBean roomBean = new RoomBean();
        roomBean.setSessionId(session.getId());
        roomBean.setName(room.getName());
        return roomBean;
    }

    /**
     * 广播提供者sdp交互。
     *
     * @param signaling
     * @param session
     */
    public synchronized void present(SignalingParameter signaling, WebSocketSession session) throws Exception {
        log.debug("----receive msg from client----:presenter method.");
        String roomName = signaling.getRoomName();
        Room room = roomManager.getRoomBySession(session);
        if (room != null) {
            NUserSession presenter = room.getPresenter();

			/* Added by Hank */
            WebRtcEndpoint webrtcEndpoint = presenter.getWebRtcEndpoint();
            MediaPipeline mediaPipeline = webrtcEndpoint.getMediaPipeline();

            String filename = "file:////opt/kurento/kurento-6.1.0/kurento-tutorial-java/kurento-one2many-call/recsamples/recording-" + session.getId() + ".mp4";
            log.debug("HANK>>>>>>>>>>> rec filename = '{}'", filename);
            RecorderEndpoint recorderEndpoint = new RecorderEndpoint.Builder(mediaPipeline,
                    filename).withMediaProfile(MediaProfileSpecType.MP4).build();
            webrtcEndpoint.connect(recorderEndpoint);
            recorderEndpoint.record();
            /* END */


            String sdpAnswer = presenter.processOffer(signaling.getSdpOffer());

            ResponseParameter response = new ResponseParameter();
            response.setRoom(roomManager.getRoomBeanByName(roomName));
            response.setId(ResponseParameter.ResponeIdType.presenterResponse.name());
            response.setResponse(ResponseParameter.ResponseType.accepted.name());
            response.setSdpAnswer(sdpAnswer);

            log.debug("****sdp answer***** : " + sdpAnswer);

            presenter.sendMessage(gson.toJson(response));
            presenter.gatherCandidates();

        } else {
            stop(session);
            ResponseParameter response = new ResponseParameter();
            response.setId(ResponseParameter.ResponeIdType.presenterResponse.name());
            response.setResponse(ResponseParameter.ResponseType.rejected.name());
            response.setMessage("The room is null when interact sdp for presenter.");
            session.sendMessage(new TextMessage(gson.toJson(response)));
        }
    }

    public synchronized void viewer(WebSocketSession session, SignalingParameter signaling) throws Exception {
        log.debug("----receive msg from client----:viewer methoed.");
        String roomName = signaling.getRoomName();
        Room room = roomManager.getRoomByName(roomName);
        if (room != null) {
            NUserSession viewer = new NUserSession(session, new WebRtcEndpoint.Builder(room.getPipeline()).build());
            room.join(viewer);
            String sdpAnswer = viewer.processOffer(signaling.getSdpOffer());

            ResponseParameter response = new ResponseParameter();
            response.setRoom(roomManager.getRoomBeanByName(roomName));
            response.setId(ResponseParameter.ResponeIdType.viewerResponse.name());
            response.setResponse(ResponseParameter.ResponseType.accepted.name());
            response.setSdpAnswer(sdpAnswer);

            log.debug("****sdp answer***** : " + sdpAnswer);

            viewer.sendMessage(gson.toJson(response));
            viewer.gatherCandidates();
        } else {
            ResponseParameter response = new ResponseParameter();
            response.setId(ResponseParameter.ResponeIdType.viewerResponse.name());
            response.setResponse(ResponseParameter.ResponseType.rejected.name());
            response.setMessage("the room named " + roomName + " isn't available");
            session.sendMessage(new TextMessage(gson.toJson(response)));
        }

    }

    public synchronized void stop(WebSocketSession session) throws IOException {
        log.debug("----receive msg from client----:stop method");
        if (roomManager.isExistedBySession(session)) {      //is presenter, and remove it's viewers.

            roomManager.removeRoom(session);
        } else {     //remove viewer.

            Collection<Room> rooms = roomManager.getRooms();
            for (Room room : rooms) {
                if (room.getViewerBySession().containsKey(session.getId())) {
                    room.removeViewer(session);
                    break;
                }
            }
        }
    }


}
