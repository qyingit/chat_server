package com.kingston.logic.user;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kingston.base.ServerManager;
import com.kingston.base.SpringContext;
import com.kingston.data.dao.UserDao;
import com.kingston.data.model.User;
import com.kingston.logic.GlobalConst;
import com.kingston.logic.user.message.ResUserInfoMessage;
import com.kingston.logic.user.message.ResUserRegisterPacket;
import com.kingston.logic.util.IdService;
import com.kingston.net.ChannelUtils;
import com.kingston.net.IoSession;
import com.kingston.net.SessionCloseReason;
import com.kingston.util.ConcurrentHashSet;
import com.kingston.util.LruHashMap;

import io.netty.channel.Channel;

@Component
public class UserService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private IdService idService;

	/** lru缓存最近登录的所有用户 */
	private Map<Long, User> lruUsers = new LruHashMap<>(1000);
	/** 在线用户列表　*/
	private Set<Long> onlneUsers = new ConcurrentHashSet<>();


	public void addUser2Online(long userId) {
		this.onlneUsers.add(userId);
	}

	public void removeFromOnline(long userId) {
		this.onlneUsers.remove(userId);
	}

	public boolean isOnlineUser(long userId) {
		return this.onlneUsers.contains(userId);
	}

	/**
	 * 注册新账号
	 * @param userId qq号码
	 * @param nickName 昵称
	 */
	public void registerNewAccount(Channel channel, byte sex, String nickName, String password) {
		IoSession session = ChannelUtils.getSessionBy(channel);
		User oldUser = userDao.findByName(nickName);
		ResUserRegisterPacket response = new ResUserRegisterPacket();
		if (oldUser != null) {
			response.setResultCode(GlobalConst.FAILED);
			response.setMessage("账号id已存在");
		}else {
			User newUser = createNewUser(sex, nickName, password);
			response.setResultCode(GlobalConst.SUCC);
			response.setMessage(String.valueOf(newUser.getUserId()));
		}
		session.sendPacket(response);
	}

	private User createNewUser(byte sex, String nickName, String password) {
		int newId = idService.getNextId();
		User newUser = new User();
		newUser.setSex(sex);
		newUser.setUserId(newId);
		newUser.setUserName(nickName);
		newUser.setPassword(password);

		userDao.addUser(newUser);

		return newUser;
	}

	public void refreshUserProfile(User user) {
		ResUserInfoMessage response = new ResUserInfoMessage();
		response.setSex(user.getSex());
		response.setUserId(user.getUserId());
		response.setUserName(user.getUserName());
		response.setSignature(user.getSignature());

		ServerManager.INSTANCE.sendPacketTo(user.getUserId(), response);
	}

	public void userLogout(Channel channel, SessionCloseReason reason) {
		IoSession session = ChannelUtils.getSessionBy(channel);
		long userId = session.getUser().getUserId();
		SpringContext.getUserService().removeFromOnline(userId);
		SpringContext.getFriendService().onUserLogout(userId);

		ServerManager.INSTANCE.ungisterUserContext(channel, reason);
	}



}
