package org.kurento.tutorial.n_one2manycall;

import org.kurento.client.IceCandidate;

/**
 * Created by jingbiaowang on 2015/8/28.
 */
public class SignalingParameter {

    public enum SignalingIdType {
        roomList, register, presenter, viewer, onIceCandidate, stop
    }

    private String id;
    private String roomName;
    private String name;
    private String response;
    private String message;
    private String sdpOffer;
    private IceCandidate candidate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public IceCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }
}
