package com.simplechat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.http.*;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

@SuppressWarnings("serial")
public class ChatServlet extends HttpServlet {
    private static String readFileAsString(String filePath)
    throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
    
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if(req.getParameter("userId") != null) {
			String userId = req.getParameter("userId");
			String html = readFileAsString("chat.html");
			
		    ChannelService channelService = ChannelServiceFactory.getChannelService();
	
		    // The 'Game' object exposes a method which creates a unique string based on the game's key
		    // and the user's id.
		    String token = channelService.createChannel(userId);
		    
		    // Index is the contents of our index.html resource, details omitted for brevity.
		    html = html.replaceAll("\\{\\{ token \\}\\}", token).replaceAll("\\{\\{ userId \\}\\}", req.getParameter("userId"));
	
		    resp.setContentType("text/html");
		    resp.getWriter().write(html);
		} else {
			resp.setContentType("text/html");
			String html = readFileAsString("index.html");
		    resp.getWriter().write(html);
		}
	}
}

