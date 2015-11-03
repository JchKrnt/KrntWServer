package org.kurento.tutorial.n_one2manycall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jingbiaowang on 2015/8/27.
 */
public class Room {

    private static final Logger log = LoggerFactory.getLogger(NCallHandler.class);
    private static final Gson gson = new GsonBuilder().create();

    private String name;
    private MediaPipeline pipeline;
    private NUserSession presenter;
    private final ConcurrentHashMap<String, NUserSession> viewerBySession = new ConcurrentHashMap<>();


    public Room(String roomName, NUserSession presenter) {
        this.name = roomName;
        this.presenter = presenter;
        this.pipeline = presenter.getWebRtcEndpoint().getMediaPipeline();
    }

    public void join(NUserSession viewer) {

        viewerBySession.put(viewer.getSession().getId(), viewer);

        //link with presenter.
        presenter.getWebRtcEndpoint().connect(viewer.getWebRtcEndpoint());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(MediaPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public NUserSession getPresenter() {
        return presenter;
    }

    public void setPresenter(NUserSession presenter) {
        this.presenter = presenter;
    }

    public NUserSession getViewBySession(WebSocketSession session) {

        return viewerBySession.get(session.getId());
    }

    public void release() throws IOException {

        for (NUserSession viewer : viewerBySession.values()) {
            ResponseParameter response = new ResponseParameter();
            response.setId(ResponseParameter.ResponeIdType.stopCommunication.name());
            viewer.sendMessage(gson.toJson(response));
        }
        presenter.release();

        log.info("Releasing media pipeline.");
        if (pipeline != null)
            pipeline.release();
        pipeline = null;

    }

    public ConcurrentHashMap<String, NUserSession> getViewerBySession() {
        return viewerBySession;
    }

    public void removeViewer(WebSocketSession session) throws IOException {
        NUserSession viewer = viewerBySession.remove(session.getId());
        viewer.release();
    }

}
