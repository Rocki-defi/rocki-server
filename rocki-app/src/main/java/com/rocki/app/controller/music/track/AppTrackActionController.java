package com.rocki.app.controller.music.track;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.app.context.RequestContext;
import com.rocki.app.controller.bo.track.CollaboratorBO;
import com.rocki.app.controller.bo.track.CollaboratorListBO;
import com.rocki.app.controller.vo.track.TrackDetailVO;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppPlaylistService;
import com.rocki.app.service.AppTrackService;
import com.rocki.common.annotation.Login;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.redis.RedisCache;
import com.rocki.common.utils.MessageUtils;
import com.rocki.music.domain.*;
import com.rocki.music.service.CollaboratorService;
import com.rocki.music.service.PlaylistTracksAssociationService;
import com.rocki.music.service.TrackService;
import com.rocki.music.service.TrackTagService;
import com.rocki.music.utils.TrackAndPlayListUtils;
import com.rocki.user.domain.User;
import com.rocki.user.mapper.UserMapper;
import com.rocki.user.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author David Wilde
 * @Package com.rocki.app.controller.music.track
 * @date 2/27/21 4:15 下午
 */
@RestController
@RequestMapping("/api/music/track")
public class AppTrackActionController {
    Logger logger = LoggerFactory.getLogger(AppTrackActionController.class);
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AppTrackService appTrackService;
    @Autowired
    private AppPlaylistService appPlaylistService;
    @Autowired
    private TrackService trackService;
    @Autowired
    private PlaylistTracksAssociationService playlistTracksAssociationService;
    @Autowired
    private TrackTagService trackTagService;
    @Autowired
    private UserService userService;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CollaboratorService collaboratorService;
    @Autowired
    private RedisCache redisCache;

    @Login
    @PostMapping("uploadTrack")
    public AjaxResult uploadTrack(
            @RequestParam(value = "music", required = true) MultipartFile musicFile,
            @RequestParam(value = "image", required = true) MultipartFile imageFile,
            @RequestParam(value = "isPrivate") Integer isPrivate,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "playlistSplit", defaultValue = "0.0") BigDecimal playlistSplit,
            @RequestParam(value = "tags", defaultValue = "") List<String> tags,
            @RequestParam(value = "description", defaultValue = " ", required = false) String description,
            @RequestParam(value = "releaseDate") Date releaseDate,
            @RequestParam(value = "artistName", defaultValue = "") String artistName,
            @RequestParam String collaboratorListBO
    ) {
        CollaboratorListBO bo = JSONObject.parseObject(collaboratorListBO, CollaboratorListBO.class);
        AjaxResult result = uploadMusicTrackS3(musicFile, imageFile, isPrivate, title, playlistSplit, tags, description, releaseDate,
                artistName);
        Track track = JSONObject.parseObject(JSONObject.toJSONString(result.get("data")), Track.class);
        bo.setTrackId(track.getId());
        result = editCollaborator(bo);
        if (!result.get("code").equals(200)) {
            trackService.removeById(track.getId());
            trackTagService.remove(new QueryWrapper<TrackTag>().eq("track_id", track.getId()));
            collaboratorService.remove(new QueryWrapper<Collaborator>().eq("track_id", track.getId()));
            return result;
        }

        String cacheKey = "personalTrackList::" + RequestContext.getUser().getUserId();
        redisCache.deleteObject(cacheKey);

        return result;
    }

    @Login
    @PostMapping("upload")
    public AjaxResult uploadMusicTrackS3(
            @RequestParam(value = "music") MultipartFile musicFile,
            @RequestParam(value = "image") MultipartFile imageFile,
            @RequestParam(value = "isPrivate") Integer isPrivate,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "playlistSplit", defaultValue = "0.0") BigDecimal playlistSplit,
            @RequestParam(value = "tags", defaultValue = "") List<String> tags,
            @RequestParam(value = "description", defaultValue = " ", required = false) String description,
            @RequestParam(value = "releaseDate") Date releaseDate,
            @RequestParam(value = "artistName", defaultValue = "") String artistName) {
        User user = RequestContext.getUser();
        if (StringUtils.isBlank(title)) {
            return AjaxResult.error(MessageUtils.message("not.null", "track title"));
        }
        if (musicFile.isEmpty()) {
            return AjaxResult.error(MessageUtils.message("upload.file.is.empty"));
        }
        if (!TrackAndPlayListUtils.isPrivateOrPublic(isPrivate)) {
            return AjaxResult.error(MessageUtils.message("track.create.type.error"));
        }
        if (playlistSplit.compareTo(BigDecimal.ZERO) < 0 || playlistSplit.compareTo(BigDecimal.ONE) >= 0) {
            return AjaxResult.error(MessageUtils.message("trans.number.playlist.split"));
        }
        if (tags.size() > TrackAndPlayListUtils.TRACK_MAX_TAGS_SIZE) {
            return AjaxResult.error(MessageUtils.message("number_of_tags_exceed_maximum"));
        }
        if (StringUtils.isBlank(artistName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "artist name"));
        }

        Track track = appTrackService.uploadTrackAndSaveRelationInfo(musicFile, imageFile, user, title, playlistSplit, tags,
                description, isPrivate, releaseDate, artistName);
        TrackDetailVO vo = new TrackDetailVO();
        BeanUtils.copyProperties(track, vo);
        return AjaxResult.success(MessageUtils.message("upload.music.success"), vo);
    }

    @GetMapping("play")
    public AjaxResult playTrack(
            @RequestParam("trackId") Long trackId, HttpServletRequest request) {
        FrontLoginUser currentUser = frontTokenService.getLoginUser(request);

        Track trackById = appTrackService.getById(trackId);
        if (trackById == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        if (trackById.getStatus() != 0) {
            return AjaxResult.error(MessageUtils.message("track.status.error"));
        }
        if (TrackAndPlayListUtils.PRIVATE.equals(trackById.getIsPrivate())) {
            if (currentUser == null) {
                return AjaxResult.error(MessageUtils.message("track.not.yours"));
            }
            if (!trackById.getUserId().equals(currentUser.getUser().getUserId())) {
                return AjaxResult.error(MessageUtils.message("track.not.yours"));
            }
        }

        if (trackById.getOssUrl() == null && trackById.getStorageUrl() == null) {
            return AjaxResult.error(MessageUtils.message("storage_disappeared"));
        }

        return AjaxResult.success(trackById.getOssUrl().isEmpty() ? trackById.getStorageUrl() : trackById.getOssUrl());
    }

    @Login
    @PostMapping("addToPlaylist")
    public AjaxResult addToPlaylist(
            @RequestParam(value = "trackId") Long trackId,
            @RequestParam(value = "playlistId") Long playlistId) {
        User user = RequestContext.getUser();
        Track trackById = appTrackService.getById(trackId);
        if (trackById == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        if (!TrackAndPlayListUtils.TRACK_STATUS_NORMAL.equals(trackById.getStatus())) {
            return AjaxResult.error(MessageUtils.message("track.status.error"));
        }
        Playlist playlistById = appPlaylistService.getOne(new QueryWrapper<Playlist>()
                .eq("user_id", user.getUserId())
                .eq("id", playlistId));
        if (playlistById == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        PlaylistTracksAssociation playlistTracksAssociation = playlistTracksAssociationService.getOne(
                new QueryWrapper<PlaylistTracksAssociation>()
                        .eq("playlist_id", playlistId)
                        .eq("track_id", trackId));
        if (playlistTracksAssociation != null) {
            return AjaxResult.error(MessageUtils.message("playlist.already.has.track"));
        }
        appTrackService.addToPlaylist(trackById, playlistById);
        return AjaxResult.success();

    }

    @Login
    @PostMapping("removeFromPlaylist")
    public AjaxResult removeFromPlaylist(
            @RequestParam(value = "trackId") Long trackId,
            @RequestParam(value = "playlistId") Long playlistId) {
        User user = RequestContext.getUser();
        Track trackById = appTrackService.getById(trackId);
        if (trackById == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        Playlist playlistById = appPlaylistService.getOne(new QueryWrapper<Playlist>()
                .eq("user_id", user.getUserId())
                .eq("id", playlistId));
        if (playlistById == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        PlaylistTracksAssociation playlistTracksAssociation = playlistTracksAssociationService.getOne(
                new QueryWrapper<PlaylistTracksAssociation>()
                        .eq("playlist_id", playlistId)
                        .eq("track_id", trackId));
        if (playlistTracksAssociation == null) {
            return AjaxResult.error(MessageUtils.message("playlist.has.not.track"));
        }
        appTrackService.removeFromPlaylist(trackById, playlistById, playlistTracksAssociation);
        return AjaxResult.success();

    }

    @Login
    @PostMapping("editCollaborator")
    public AjaxResult editCollaborator(@RequestBody CollaboratorListBO collaboratorListBO) {
        User user = RequestContext.getUser();
        boolean bool = appTrackService.checkTrackOfUser(collaboratorListBO.getTrackId(), user.getUserId());
        if (!bool) {
            return AjaxResult.error(MessageUtils.message("track.permission.denied"));
        }
        List<CollaboratorBO> collaboratorList = collaboratorListBO.getCollaborators();
        if (collaboratorList.isEmpty()) {
            return AjaxResult.error(MessageUtils.message("not.null", "collaborator"));
        }

        BigDecimal totalSplits = collaboratorList
                .stream()
                .map(CollaboratorBO::getSplit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplits.compareTo(BigDecimal.ONE) != 0) {
            return AjaxResult.error(MessageUtils.message("track.collaborator.split.error"));
        }

        for (CollaboratorBO collaboratorBO : collaboratorList) {
            if (collaboratorBO.getSplit().compareTo(BigDecimal.ONE) > 0 || collaboratorBO.getSplit().compareTo(BigDecimal.ZERO) < 0) {
                return AjaxResult.error(MessageUtils.message("track.collaborator.split.error"));
            }
            int tmp = userMapper.selectCount((new QueryWrapper<User>()
                    .eq("full_name", collaboratorBO.getArtist()).or()
                    .eq("username", collaboratorBO.getArtist())));
            if (tmp == 0) {
                return AjaxResult.error(MessageUtils.message("user.artist.not.exists"));
            }
        }
        appTrackService.updateOrSaveCollaborators(user, collaboratorListBO.getTrackId(), collaboratorList);

        return AjaxResult.success();
    }

    @Login
    @PostMapping("deleteTrack")
    public AjaxResult deleteTrack(@RequestParam(value = "trackId") Long trackId) {
        User user = RequestContext.getUser();

        Track track = trackService.getById(trackId);

        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        if (!track.getUserId().equals(user.getUserId())) {
            return AjaxResult.error(MessageUtils.message("track.not.yours"));
        }

        boolean deleteSuccess = appTrackService.deleteTrack(track, user);
        return deleteSuccess ? AjaxResult.success() : AjaxResult.error(MessageUtils.message("failed.to.delete"));
    }

    @Login
    @PostMapping("setTrackPrivateOrPublic")
    public AjaxResult setTrackPrivateOrPublic(
            @RequestParam(value = "trackId") Long trackId,
            @RequestParam(value = "isPrivate") Integer isPrivate
    ) {
        User user = RequestContext.getUser();

        Track track = trackService.getById(trackId);

        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        if (!user.getUserId().equals(track.getUserId())) {
            return AjaxResult.error(MessageUtils.message("track.not.yours"));
        }
        if (!TrackAndPlayListUtils.isPrivateOrPublic(isPrivate)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        track.setIsPrivate(isPrivate);
        trackService.updateById(track);

        String cacheKey = "personalTrackList::" + user.getUserId();
        redisCache.deleteObject(cacheKey);

        return AjaxResult.success();
    }

    @GetMapping("countStart")
    public AjaxResult countStart(
            @RequestParam("trackId") Long trackId,
            HttpServletRequest request
    ) {
        String cacheIp = request.getRemoteAddr();
        Track track = trackService.getById(trackId);
        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }

        Map<String, String> cacheMap = new HashMap<>(1);
        cacheMap.put("Stream Count: " + trackId.toString(), DF.format(LocalDateTime.now()));

        logger.debug("track id: {}, track duration: {}", track.getId(), track.getLength());
        redisCache.setCacheObject(cacheIp, cacheMap, track.getLength().intValue() * 3, TimeUnit.SECONDS);
        return AjaxResult.success();
    }

    @GetMapping("countEnd")
    public AjaxResult countEnd(
            @RequestParam("trackId") Long trackId,
            HttpServletRequest request) {
        String cacheIp = request.getRemoteAddr();
        Map<String, String> cacheValue = redisCache.getCacheObject(cacheIp);
        if (cacheValue == null) {
            return AjaxResult.error("not same track");
        }
        if (!cacheValue.containsKey("Stream Count: " + trackId.toString())) {
            return AjaxResult.error("not same track");
        }
        LocalDateTime cacheTime = LocalDateTime.parse(cacheValue.get("Stream Count: " + trackId.toString()), DF);

        LocalDateTime current = LocalDateTime.now();
        Track track = trackService.getById(trackId);
        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }

        long duration = track.getLength() * 8 / 10;
        if (current.compareTo(cacheTime.plusSeconds(duration)) < 1) {
            return AjaxResult.error("count faild");
        }

        track.setStreamCount(track.getStreamCount() + 1);
        appTrackService.updateById(track);
        return AjaxResult.success();
    }
}
