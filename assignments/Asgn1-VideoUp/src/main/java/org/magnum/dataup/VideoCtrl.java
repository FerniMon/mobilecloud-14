package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.net.MediaType;

@Controller
public class VideoCtrl {

	private static final AtomicLong currentId = new AtomicLong(0L);
	private final Map<Long, InputStream> videoStore = new HashMap<Long, InputStream>();
	private final Map<Long, Video> videos = new HashMap<Long, Video>();

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody List<Video> getVideos() {
		// Video video = Video.create().withContentType("video/mpeg")
		// .withDuration(123).withSubject("Mobile Cloud")
		// .withTitle("Programming Cloud Services for ...").build();
		// videosMap.put(1L, video);
		return new ArrayList<Video>(videos.values());
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		// Add video only if it is not already being added
		if (video != null && !videos.containsValue(video)) {
			// Add unique video identifier
			if (video.getId() == 0)
				video.setId(currentId.incrementAndGet());
			// Add video URL to the video
			String url = getUrlBaseForLocalServer() + "/video/" + video.getId()
					+ "/data";
			video.setDataUrl(url);
			// Add video to the map holding all the videos
			videos.put(video.getId(), video);
		}

		return video;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) {
		VideoStatus status = new VideoStatus(VideoState.READY);

		try {
			if(!videos.containsKey(id)){
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not existing");
			}else if(videoData.isEmpty()){
				response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to add empty video data");
			}else{
				videoStore.put(id, videoData.getInputStream());
			}
		} catch (IOException io) {
			// DO NOTHING
		}

		return status;
	}
	
//	@RequestMapping(value="/video/{id}/data", method = RequestMethod.GET)
//	public @ResponseBody InputStream getData(@PathVariable("id") long id, HttpServletResponse response){
//		InputStream in = null;
//		try {
//			if(!videos.containsKey(id) || !videoStore.containsKey(id)){
//				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not existing");
//			}else{
//				in = videoStore.get(id);
//			}
//		} catch (IOException io) {
//			System.out.println("Internal error");
//		}
//		
//		return in;
//	}

//	@RequestMapping(value="/video/{id}/data", method = RequestMethod.GET)
//	public @ResponseBody byte[] getData(@PathVariable("id") long id, HttpServletResponse response){
//		try {
//			if(!videos.containsKey(id) || !videoStore.containsKey(id)){
//				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not existing");
//			}else{
//				response.getOutputStream().write(videoStore.get(id));
//			}
//		} catch (IOException io) {
//			System.out.println("Internal error");
//		}
//	}
	
//	@Bean
//	public MultipartConfigElement getMultipartConfig() {
//		MultiPartConfigFactory f = new MultiPartConfigFactory();
//		f.setMaxFileSize("100MB");
//		f.setMaxRequestSize("150MB");
//		return f.createMultipartConfig();
//	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
