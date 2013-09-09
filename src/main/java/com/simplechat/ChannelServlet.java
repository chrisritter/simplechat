package com.simplechat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.gson.Gson;

public class ChannelServlet extends HttpServlet {
	private static final long serialVersionUID = -6871081045210796007L;

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if(req.getRequestURI().equals("/_ah/channel/connected/")) {
			MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
			IdentifiableValue idVal = memcache.getIdentifiable("users");
			
			ChannelService channelService = ChannelServiceFactory.getChannelService();
			ChannelPresence presence = channelService.parsePresence(req);
			String userId = presence.clientId();
			List<String> signedInUsers;
			if(idVal == null) {
				signedInUsers = new ArrayList<String>();
				signedInUsers.add(userId);
				memcache.put("users", signedInUsers);
			} else {
				while(true) {
					signedInUsers = (List<String>) idVal.getValue();
					signedInUsers.add(userId);
					if(memcache.putIfUntouched("users", idVal, signedInUsers)) {
						break;
					} else {
						idVal = memcache.getIdentifiable("users");
					}
				}
			}
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("type", "signin");
			map.put("userId", userId);
			broadcastMessage(signedInUsers, map);
		} else if(req.getRequestURI().equals("/_ah/channel/disconnected/")) {
			MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
			IdentifiableValue idVal = memcache.getIdentifiable("users");
			
			ChannelService channelService = ChannelServiceFactory.getChannelService();
			ChannelPresence presence = channelService.parsePresence(req);
			String userId = presence.clientId();
			List<String> signedInUsers = null;
			if(idVal != null) {
				while(true) {
					signedInUsers = (List<String>) idVal.getValue();
					signedInUsers.remove(userId);
					if(memcache.putIfUntouched("users", idVal, signedInUsers)) {
						break;
					} else {
						idVal = memcache.getIdentifiable("users");
					}
				}
			}
			Map<String, String> map = new HashMap<String, String>();
			map.put("type", "signout");
			map.put("userId", userId);
			broadcastMessage(signedInUsers, map);
		} else {
			Map<String, String> map = new HashMap<String, String>();
			map.put("type", "message");
			map.put("userId", req.getParameter("userId"));
			map.put("message", req.getParameter("message"));
			broadcastMessage((List<String>) MemcacheServiceFactory.getMemcacheService().get("users"), map);
		}
	}
	
	public static void broadcastMessage(List<String> signedInUsers, Map<String, String> map) {
		String message = new Gson().toJson(map);
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		if(signedInUsers == null) {
			throw new IllegalArgumentException("Channel presence to /_ah/channel/connected|disconnected is set up wrong");
		}
		for(String userId : signedInUsers) {
			channelService.sendMessage(new ChannelMessage(userId,message));
		}
	}
}
