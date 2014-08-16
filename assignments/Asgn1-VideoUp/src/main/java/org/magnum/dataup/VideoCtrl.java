package org.magnum.dataup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.magnum.dataup.model.Video;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class VideoCtrl {
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	Map<Long, Video> videos = new HashMap<Long, Video>();
	
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody List<Video> getVideos(){
//		Video video = Video.create().withContentType("video/mpeg")
//	            .withDuration(123).withSubject("Mobile Cloud")
//	            .withTitle("Programming Cloud Services for ...").build();
//			videosMap.put(1L, video);
		return new ArrayList<Video>(videos.values());
	}
	
	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video){
		//Add unique video identifier
		if(video.getId() == 0)
			video.setId(currentId.incrementAndGet());
		//Add video URL to the video
		String url = getUrlBaseForLocalServer()+ "/video/" + video.getId()+ "/data";
		video.setDataUrl(url);
		// Add video to the map holding all the videos
		videos.put(video.getId(), video);
		return video;
	}
	
    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = 
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base = 
           "http://"+request.getServerName() 
           + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
     }
}
