package com.kingston.logic.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kingston.base.ServerManager;
import com.kingston.logic.chat.message.resp.ResChatToUserPacket;
import com.kingston.net.IoSession;

@Component
public class ChatService {
	
	@Autowired
	private IChatInspector chatInspector;
	
	public void chat(IoSession fromUser, long toUserId, String content) {
		IoSession toUser = ServerManager.INSTANCE.getSessionBy(toUserId);
		if (fromUser == null || toUser == null) {
			return;
		}
		if (!checkDirtyWords(content)) {
			return;
		}
		
		//双方都推送消息
		ResChatToUserPacket response = new ResChatToUserPacket();
		response.setContent(content);
		response.setFromUserId(fromUser.getUser().getUserId());
		toUser.sendPacket(response);
		
		fromUser.sendPacket(response);
	}
	
	private boolean checkDirtyWords(String content) {
		return true;
	}

}
