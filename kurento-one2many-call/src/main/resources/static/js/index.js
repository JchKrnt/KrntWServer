/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

var ws = new WebSocket('ws://' + location.host + '/live');
var video;
var webRtcPeer;

var roomDataGlobal;

window.onload = function () {
    console = new Console();
    video = document.getElementById('video');
    getRoomList();
}


window.onbeforeunload = function () {
    ws.close();
}

ws.onclose = function (code, reason) {

    dispose();
}

ws.onmessage = function (message) {
    var parsedMessage = JSON.parse(message.data);
    console.info('Received message: ' + message.data);

    switch (parsedMessage.id) {
        case 'presenterResponse':
            presenterResponse(parsedMessage);
            break;
        case 'viewerResponse':
            viewerResponse(parsedMessage);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'stopCommunication':
            dispose();
            break;

        case 'roomList':
        {
            onRoomList(parsedMessage);
            break;
        }
        case 'register':
        {
            onRegisterRoom(parsedMessage);
            break;
        }
        default:
            console.error('Unrecognized message', parsedMessage);
    }
}

function presenterResponse(message) {
    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error(error);
        });
    }
}

function viewerResponse(message) {
    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error(error);
        });
    }
}

function registerRoom() {

    var nameInput = document.getElementById("room_name_input");
    var roomName = nameInput.value.trim();
    if (roomName == null || roomName == "") {
        alert("请填写房间名称！");
        return;
    }

    var registerMsg = {
        id: "register",
        roomName: roomName
    };
    sendMessage(registerMsg);
}

function onRegisterRoom(message) {

    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        presenter(message.room);
    }

}

function presenter(roomData) {
    roomDataGlobal = roomData;
    if (!webRtcPeer) {
        showSpinner(video);

        var options = {
            localVideo: video,
            onicecandidate: onIceCandidate
        }
        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                webRtcPeer.generateOffer(onOfferPresenter);
            });
    }
}

function onOfferPresenter(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'presenter',
        roomName: roomDataGlobal.name,
        sdpOffer: offerSdp
    }
    sendMessage(message);
}

function viewer() {

}


function onOfferViewer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'viewer',
        roomName: roomDataGlobal.name,
        sdpOffer: offerSdp
    }
    sendMessage(message);
}

function onIceCandidate(candidate) {
    console.log("Local candidate" + JSON.stringify(candidate));

    var message = {
        id: 'onIceCandidate',
        roomName: roomDataGlobal.name,
        candidate: candidate
    };
    sendMessage(message);
}

function stop() {
    var message = {
        id: 'stop'
    }
    sendMessage(message);
    dispose();
}

function dispose() {
    if (webRtcPeer) {
        webRtcPeer.dispose();
        roomDataGlobal = null;
        webRtcPeer = null;
    }
    hideSpinner(video);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    console.log('Senging message: ' + jsonMessage);
    ws.send(jsonMessage);
}

function showSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].poster = './img/transparent-1px.png';
        arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
    }
}

function hideSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].src = '';
        arguments[i].poster = './img/webrtc.png';
        arguments[i].style.background = '';
    }
}
/**
 * get roomList
 */
function getRoomList() {
    var getListMsg = {
        id: "roomList"
    };
    sendMessage(getListMsg);

}

function onRoomList(message) {

    console.log('get list msg : ' + JSON.stringify(message.roomsList));

    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        var roomsList = message.roomsList;
        if (roomsList == null || typeof roomsList == undefined) {
            console.info("there isn't any room.");
            clearRoom();
        } else {
            showRooms(roomsList);
        }
    }
}

function clearRoom() {
    var roomsFrameTbl = document.getElementById("view_tb");
    var tbo = document.getElementById("view_body");
    if (tbo !== null && typeof tbo !== undefined)
        roomsFrameTbl.removeChild(tbo);     //删除table下的tbody元素
    var tBody = document.createElement("tbody");
    tBody.setAttribute("id", "view_body");
    roomsFrameTbl.appendChild(tBody);  //从新添加table body.
    return tBody;
}

function showRooms(roomList) {

    var colum = 4;

    var roomTbody = clearRoom();
    //console.info("room name : " + roomList[i].name);
    //console.info("room sessionId : " + roomList[i].sessionId);


    var tblRow = addRoomRow();

    for (var i = 0; i < roomList.length; i++) {
        var roomTd = addRoom(roomList[i]);
        tblRow.appendChild(roomTd);
    }

    roomTbody.appendChild(tblRow);

}

function addRoomRow() {

    var row = document.createElement("tr");
    row.setAttribute("id", "view_tr");

    return row;
}
/**
 * id="call2" href="#" class="btn btn-success"
 onclick="viewer(); return false;"
 * @param roomData
 * @param row
 * @param i
 */
function addRoom(roomData) {

    var cell = document.createElement("td");
    var linkA = document.createElement("a");
    linkA.setAttribute("href", "#");
    linkA.setAttribute("class", "btn btn-success");
    linkA.onclick = function () {
        viewerClick(roomData);
        return false;
    };
    linkA.innerHTML = "<span class='glyphicon glyphicon-user'></span>" + roomData.name;
    cell.appendChild(linkA);

    return cell;
}

/**
 * 房间观看。
 * @param roomData
 */
function viewerClick(roomData) {
    console.info("onclick room name : " + roomData.name + " sessionId : " + roomData.sessionId)
    roomDataGlobal = roomData;
    if (!webRtcPeer) {
        showSpinner(video);

        var options = {
            remoteVideo: video,
            onicecandidate: onIceCandidate
        }

        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                this.generateOffer(onOfferViewer);
            });
    }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function (event) {
    event.preventDefault();
    $(this).ekkoLightbox();
});
