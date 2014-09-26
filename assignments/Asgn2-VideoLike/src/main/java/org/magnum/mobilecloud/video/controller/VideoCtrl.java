package org.magnum.mobilecloud.video.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class VideoCtrl {

	@Autowired
	private VideoRepo videoRepo;

	// Processes POST requests to /video path and returns the added video
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		// Set 0 likes as its a new video
		v.setLikes(0);
		return videoRepo.save(v);
	}

	// Processes GET requests to /video path and returns a list of videos
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return Lists.newArrayList(videoRepo.findAll());
	}

	// Processes GET requests to /video/{id} and returns the added video updated
	// with persisted object id
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH +"/{id}", method = RequestMethod.GET)
	public @ResponseBody Video getVideo(@PathVariable("id") long id,
			HttpServletResponse response) {
		Video video = null;
		try {
			if (videoRepo.exists(id)) {
				video = videoRepo.findOne(id);
			}

			if (video == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Video not found");
			}
		} catch (IOException e) {
			// Do nothing
		}

		return video;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}" + "/like", method = RequestMethod.POST)
	public void likeVideo(@PathVariable("id") long id,
			HttpServletResponse response, Authentication authentication) {
		try {
			if (!videoRepo.exists(id)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Video not found");
			}else{
				Video video = videoRepo.findOne(id);
				UserDetails user = (UserDetails)authentication.getPrincipal();
				if (video.getLikers().contains(user.getUsername())){
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Video already liked by the user");
				}else{
					video.addLiker(user.getUsername());
					video.incrLikes();
					videoRepo.save(video);
				}
			}
		} catch (IOException e) {
			//Do nothing
		}
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}" + "/unlike", method = RequestMethod.POST)
	public void unlikeVideo(@PathVariable("id") long id, HttpServletResponse response, Authentication authentication) {
		try {
			if (!videoRepo.exists(id)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Video not found");
			}else{
				Video video = videoRepo.findOne(id);
				UserDetails user = (UserDetails)authentication.getPrincipal();
				if (!video.getLikers().contains(user.getUsername())){
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Video not previously liked by the user");
				}else{
					video.removeLiker(user.getUsername());
					videoRepo.save(video);
				}
			}
		} catch (IOException e) {
			//Do nothing
		}
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}" + "/likedby", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getLikedBy(@PathVariable("id") long id, HttpServletResponse response){
		Set<String> likers = null;
		try {
			if (!videoRepo.exists(id)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Video not found");
			}else{
				Video video = videoRepo.findOne(id);
				likers = video.getLikers();
			}
		} catch (IOException e) {
			//Do nothing
		}
		
		return likers;
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH +"/search/findByName", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByName(@RequestParam("title") String title){
		return videoRepo.findByName(title);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH +"/search/findByDurationLessThan", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam("duration") String duration){
		return videoRepo.findByDurationLessThan(Long.parseLong(duration));
	}
}
