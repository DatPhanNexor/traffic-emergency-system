package com.example.suco.service.suco.baocao.system.realtime;

import com.example.suco.dto.SuCoMapDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BaoCaoRealtimeService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastReport(SuCoMapDto dto) {

        messagingTemplate.convertAndSend(
                "/topic/su-co",
                dto
        );
    }

    public void broadcastDelete(Long reportId) {

        messagingTemplate.convertAndSend(
                "/topic/su-co-delete",
                reportId
        );
    }

    public void broadcastTruSo(Long truSoId, SuCoMapDto dto) {

        messagingTemplate.convertAndSend(
                "/topic/tru-so/" + truSoId + "/su-co",
                dto
        );
    }

    public void refreshUserHistory(String uid) {

        messagingTemplate.convertAndSend(
                "/topic/user/" + uid + "/history",
                "REFRESH"
        );
    }

    public void broadcastAdminNotification(String message) {

        messagingTemplate.convertAndSend(
                "/topic/admin-notifications",
                message
        );
    }

    public void broadcastUserStats(String uid, Object user) {

        messagingTemplate.convertAndSend(
                "/topic/user-stats/" + uid,
                user
        );
    }
}