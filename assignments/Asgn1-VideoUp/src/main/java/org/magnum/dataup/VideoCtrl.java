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

import org.apache.commons.io.IOUtils;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.core.io.InputStreamResource;
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

@Controller
public class VideoCtrl {

	private static final AtomicLong currentId = new AtomicLong(0L);
	// Map for storing video binary data
	private final Map<Long, InputStream> videoStore = new HashMap<Long, InputStream>();
	// Map for storing video meta data  
	private final Map<Long, Video> videos = new HashMap<Long, Video>();

	/**
	 * @return list with all the video meta data
	 */
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody List<Video> getVideos() {
		return new ArrayList<Video>(videos.values());
	}

	/**
	 * Adds video meta data passed as parameter
	 * @param video
	 * @return video with new assigned unique id
	 */
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

	/**
	 * Adds the video binary data corresponding to the video id passed as parameter
	 * @param id
	 * @param videoData
	 * @param response with error code if any problem arises
	 * @return video status with READY status if all is ok
	 */
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
	
	
	/**
	 * Gets the binary data corresponding to the video id passed as parameter.
	 * @param id
	 * @param response with the data copied from stored video InputStream
	 */
	@RequestMapping(value="/video/{id}/data", method = RequestMethod.GET)
	public @ResponseBody void getData(@PathVariable("id") long id, HttpServletResponse response){
		InputStream in = null;
		
		try {
			if(!videos.containsKey(id) || !videoStore.containsKey(id)){
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not existing");
			}else{
				in = videoStore.get(id);
				IOUtils.copy(in,response.getOutputStream());
			}
		} catch (IOException io) {
			System.out.println("Internal error");
		}		
	}
	
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
