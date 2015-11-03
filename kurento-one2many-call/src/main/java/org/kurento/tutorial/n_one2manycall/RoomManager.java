package org.kurento.tutorial.n_one2manycall;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jingbiaowang on 2015/8/27.
 */
public class RoomManager {

    private final ConcurrentHashMap<String, Room> roomsByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Room> roomsBySession = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);


    public Room register(String name, NUserSession user) {
        Room room = null;
        synchronized (roomsBySession) {
            room = createRoom(name, user);
            roomsByName.put(name, room);
            roomsBySession.put(user.getSession().getId(), room);
        }
        return room;
    }

    public boolean isExisted(String roomName) {
        return roomsByName.containsKey(roomName);
    }

    public boolean isExistedBySession(WebSocketSession session) {
        return roomsBySession.containsKey(session.getId());
    }


    /**
     * 创建房间。
     *
     * @param name the name of the room.
     * @param user the user create this room.
     * @return
     */
    private Room createRoom(String name, NUserSession user) {
        Room room = new Room(name, user);
        return room;
    }

    public Room getRoomByName(String roomName) {

        return roomsByName.get(roomName);
    }

    public Room getRoomBySession(WebSocketSession webSocketSession) {
        return roomsBySession.get(webSocketSession.getId());
    }

    public RoomBean getRoomBeanByName(String roomName) throws Exception {

        Room room = getRoomByName(roomName);
        if (room != null) {
            RoomBean roomBean = new RoomBean();
            roomBean.setName(room.getName());
            roomBean.setSessionId(room.getPresenter().getSession().getId());
            return roomBean;
        } else {
            throw new NoSuchElementException("There isn't room named of " + roomName);
        }
    }


    public void removeRoom(WebSocketSession session) throws IOException {

        Room room = roomsBySession.remove(session.getId());
        roomsByName.remove(room.getName());
        room.release();
    }

    /**
     * get roomlist by session.
     */
    public ConcurrentHashSet<RoomBean> getRoomList() {
        ConcurrentHashSet<RoomBean> roomList = new ConcurrentHashSet<>();

        synchronized (roomsBySession) {
            log.debug("room size ---- '{}'" + roomList.size());
            for (Room room : roomsBySession.values()
                    ) {
                log.debug("room name ---- {}" + room.getName());
                RoomBean roomBean = new RoomBean();
                roomBean.setSessionId(room.getPresenter().getSession().getId());
                roomBean.setName(room.getName());
                roomList.add(roomBean);
            }
        }

        return roomList;
    }

    public Collection<Room> getRooms() {
        return (Collection<Room>) roomsBySession.values();
    }

    /**
     * if has room.
     *
     * @return
     */
    public boolean hasRoom() {
        synchronized (roomsBySession) {
            if (roomsBySession.isEmpty())
                return false;
            else return true;
        }
    }
}
