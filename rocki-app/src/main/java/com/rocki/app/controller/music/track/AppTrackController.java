package com.rocki.app.controller.music.track;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.app.controller.vo.track.CollaboratorVo;
import com.rocki.app.controller.vo.track.TrackDetailVO;
import com.rocki.app.controller.vo.track.TrackForListVO;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppTrackService;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.core.redis.RedisCache;
import com.rocki.common.utils.MessageUtils;
import com.rocki.music.domain.Collaborator;
import com.rocki.music.domain.Tag;
import com.rocki.music.domain.Track;
import com.rocki.music.domain.TrackFollows;
import com.rocki.music.mapper.TrackFollowsMapper;
import com.rocki.music.mapper.TrackMapper;
import com.rocki.music.service.TagService;
import com.rocki.music.service.TrackService;
import com.rocki.music.service.TrackTagService;
import com.rocki.music.utils.TrackAndPlayListUtils;
import com.rocki.user.domain.ArtistCertifiedId;
import com.rocki.user.domain.User;
import com.rocki.user.mapper.UserMapper;
import com.rocki.user.service.ArtistCertifiedIdService;
import com.rocki.user.utils.UserUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Track Controller
 *
 * @author GZC
 * @date 2020-11-07
 */
@RestController
@RequestMapping("/api/music/track")
public class AppTrackController extends BaseController {
    Logger trackControllerLogger = LoggerFactory.getLogger(AppTrackController.class);

    @Autowired
    private AppTrackService appTrackService;
    @Autowired
    private TrackService trackService;
    @Autowired
    private TrackTagService trackTagService;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;
    @Autowired
    private TrackFollowsMapper trackFollowsMapper;
    @Autowired
    private TagService tagService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TrackMapper trackMapper;
    @Autowired
    private ArtistCertifiedIdService artistCertifiedIdService;

    @GetMapping("listTracksByOneTag")
    public AjaxResult listTracksByTag(
            @ApiParam(name = "tag", value = "tag", readOnly = true)
            @RequestParam(value = "tag") String tagName,
            HttpServletRequest request
    ) {
        if (StringUtils.isBlank(tagName)) {
            return AjaxResult.error("not.null", "tag");
        }
        Tag tag = tagService.getOne(new QueryWrapper<Tag>().eq("name", tagName));
        if (tag == null) {
            return AjaxResult.error(MessageUtils.message("tag.not.exist"));
        }

        Page<Track> page = getPage();
        page = trackMapper.listTracksByTag(page, tag.getId());
        List<Map<String, Object>> collect = page.getRecords().stream()
                .map(track -> {
                    Map<String, Object> map = new HashMap<>(2);
                    TrackForListVO trackForListVO = new TrackForListVO();
                    BeanUtils.copyProperties(track, trackForListVO);
                    map.put("trackDetail", trackForListVO);
                    FrontLoginUser user = frontTokenService.getLoginUser(request);
                    Integer integer = 0;
                    if (user != null) {
                        integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                                .eq("track_id", track.getId())
                                .eq("follows_id", user.getUser().getUserId()));
                    }
                    map.put("trackDetail", trackForListVO);
                    map.put("flag", integer);
                    return map;
                }).collect(Collectors.toList());
        TableDataInfo tableDataInfo = getDataTable(collect, page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping("listAllTrack")
    public AjaxResult listAllTrack() {
        Page<Track> page = getPage();
        page = trackService.page(page, new QueryWrapper<Track>()
                .eq("status", TrackAndPlayListUtils.TRACK_STATUS_NORMAL)
                .le("release_date", new Date()).orderByDesc("release_date"));
        List<TrackDetailVO> voList = page.getRecords().stream()
                .map(track -> {
                    TrackDetailVO vo = new TrackDetailVO();
                    BeanUtils.copyProperties(track, vo);
                    return vo;
                }).collect(Collectors.toList());
        TableDataInfo tableDataInfo = getDataTable(voList, page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping("search")
    public AjaxResult searchTrack(
            @RequestParam(value = "trackTitle") String trackTitle, HttpServletRequest request) {
        if (StringUtils.isBlank(trackTitle)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Track Title"));
        }
        Page<Track> page = getPage();
        page = trackService.page(page, new QueryWrapper<Track>()
                .eq("status", TrackAndPlayListUtils.TRACK_STATUS_NORMAL)
                .eq("is_private", TrackAndPlayListUtils.PUBLIC)
                .like("title", trackTitle)
                .le("release_date", new Date())
                .orderByDesc("release_date"));
        List<Map<String, Object>> collect = page.getRecords().stream()
                .map(track -> {
                    Map<String, Object> map = new HashMap<>(2);
                    TrackForListVO trackForListVO = new TrackForListVO();
                    BeanUtils.copyProperties(track, trackForListVO);
                    map.put("trackDetail", trackForListVO);
                    FrontLoginUser user = frontTokenService.getLoginUser(request);
                    Integer integer = 0;
                    if (user != null) {
                        integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                                .eq("track_id", track.getId())
                                .eq("follows_id", user.getUser().getUserId()));
                    }
                    map.put("trackDetail", trackForListVO);
                    map.put("flag", integer);
                    return map;
                }).collect(Collectors.toList());
        TableDataInfo dataTable = getDataTable(collect, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @GetMapping("top")
    @ApiOperation("Top playing track list")
    public AjaxResult topTracks(
            @RequestParam(value = "topFlag") Integer topFlag, HttpServletRequest request) {
        List<Track> tracks = appTrackService.topTracksService(topFlag);
        FrontLoginUser user = frontTokenService.getLoginUser(request);
        List<Map<String, Object>> listVOList;
        if (user != null) {
            listVOList = tracks.stream()
                    .map(track -> {
                        Map<String, Object> map = new HashMap<>(2);
                        TrackForListVO trackForListVO = new TrackForListVO();
                        BeanUtils.copyProperties(track, trackForListVO);
                        Integer integer = 0;
                        integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                                .eq("track_id", track.getId())
                                .eq("follows_id", user.getUser().getUserId()));
                        map.put("trackDetail", trackForListVO);
                        map.put("flag", integer);
                        return map;
                    }).collect(Collectors.toList());
        } else {
            listVOList = tracks.stream()
                    .map(track -> {
                        Map<String, Object> map = new HashMap<>(2);
                        TrackForListVO trackForListVO = new TrackForListVO();
                        BeanUtils.copyProperties(track, trackForListVO);
                        Integer integer = 0;
                        map.put("trackDetail", trackForListVO);
                        map.put("flag", integer);
                        return map;
                    }).collect(Collectors.toList());
        }
        return AjaxResult.success(listVOList);
    }

    @GetMapping("listForNewTrack")
    @ApiOperation("Get new track list")
    public AjaxResult listForNewTrack(
            HttpServletRequest request
    ) {
        Page<Track> page = getPage();
        List<Track> list = appTrackService.getNewTrackList(page, page.getSize());
        FrontLoginUser user = frontTokenService.getLoginUser(request);
        List<Map<String, Object>> collections;
        if (user != null) {
            collections = list.stream()
                    .map(track -> {
                        HashMap<String, Object> map = new HashMap<>(2);
                        TrackForListVO trackForListVO = new TrackForListVO();
                        BeanUtils.copyProperties(track, trackForListVO);
                        Integer integer = 0;
                        integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                                .eq("track_id", track.getId()).eq("follows_id", user.getUser().getUserId()));
                        map.put("trackDetail", trackForListVO);
                        map.put("flag", integer);
                        return map;
                    }).collect(Collectors.toList());
        } else {
            collections = list.stream()
                    .map(track -> {
                        HashMap<String, Object> map = new HashMap<>(2);
                        TrackForListVO trackForListVO = new TrackForListVO();
                        BeanUtils.copyProperties(track, trackForListVO);
                        map.put("trackDetail", trackForListVO);
                        map.put("flag", 0);
                        return map;
                    }).collect(Collectors.toList());
        }
        TableDataInfo dataTable = getDataTable(collections, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @GetMapping("detail")
    public AjaxResult detail(
            @RequestParam("trackId") Long trackId, HttpServletRequest request) {

        Track trackById = appTrackService.getOne(new QueryWrapper<Track>()
                .eq("id", trackId).eq("status", TrackAndPlayListUtils.TRACK_STATUS_NORMAL));
        if (trackById == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        if (trackById.getStatus() != 0) {
            return AjaxResult.error(MessageUtils.message("track.status.error"));
        }
        TrackDetailVO trackDetailVO = new TrackDetailVO();
        BeanUtils.copyProperties(trackById, trackDetailVO);
        List<String> tags = trackTagService.listForTagByTrackId(trackId);
        FrontLoginUser user = frontTokenService.getLoginUser(request);
        Integer integer = 0;
        ArtistCertifiedId certifiedId = null;
        if (user != null) {
            integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                    .eq("track_id", trackId).eq("follows_id", user.getUser().getUserId()));
        }

        certifiedId = artistCertifiedIdService.getOne(
                new QueryWrapper<ArtistCertifiedId>().eq("user_id", trackById.getUserId()));
        Map<String, Object> data = new HashMap<>(4);
        data.put("trackDetail", trackDetailVO);
        data.put("tags", tags);
        data.put("flag", integer);
        data.put("badgeFlag", certifiedId == null ? UserUtils.CERTIFIED_NONE : certifiedId.getStatus());
        return AjaxResult.success(data);
    }

    @GetMapping("collaborator")
    public AjaxResult collaborator(
            @RequestParam("trackId") Long trackId) {

        List<Collaborator> collaboratorList = appTrackService.getCollaborator(trackId);
        List<CollaboratorVo> voList = collaboratorList.stream().map(collaborator -> {
            CollaboratorVo vo = new CollaboratorVo();
            BeanUtils.copyProperties(collaborator, vo);
            vo.setUserId(null);
            try {
                User user = userMapper.selectOne(new QueryWrapper<User>().eq("full_name", collaborator.getArtist())
                        .or().eq("username", collaborator.getArtist()).last("limit 1"));

                if (user != null) {
                    vo.setUserId(user.getUserId());
                }
            } catch (Exception e) {
                trackControllerLogger.info("controller - track id is: {} and controller - artist name is: {}", trackId, collaborator.getArtist());
            }
            return vo;
        }).collect(Collectors.toList());
        return AjaxResult.success(voList);
    }

    @GetMapping("listUserTracks")
    public AjaxResult listUserTracks(
            @RequestParam(value = "userId") Long userId, HttpServletRequest httpServletRequest) {
        Page<Track> page = getPage();
        FrontLoginUser frontLoginUser = frontTokenService.getLoginUser(httpServletRequest);

        List<Track> trackList = trackService.page(page, new QueryWrapper<Track>()
                .eq("user_id", userId).eq("status", 0)
                .le("release_date", new Date()).eq("is_private", TrackAndPlayListUtils.PUBLIC)
                .orderByDesc("release_date")).getRecords();
        List<Map<String, Object>> listMap;

        listMap = trackList.stream()
                .map(track -> {
                    List<String> nameList = trackTagService.listForTagByTrackId(track.getId());
                    TrackForListVO trackForListVO = new TrackForListVO();
                    Map<String, Object> data = new HashMap<>(3);
                    Integer integer = 0;
                    if (frontLoginUser != null) {
                        integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                                .eq("track_id", track.getId())
                                .eq("follows_id", frontLoginUser.getUser().getUserId()));
                    }
                    BeanUtils.copyProperties(track, trackForListVO);
                    data.put("trackDetail", trackForListVO);
                    data.put("tags", nameList);
                    data.put("flag", integer);
                    return data;
                }).collect(Collectors.toList());
        TableDataInfo dataTable = getDataTable(listMap, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @GetMapping("listTags")
    @ApiOperation("list tags")
    public AjaxResult listTags(
            @ApiParam(name = "tagName", value = "tag", required = true)
            @RequestParam(value = "tagName") String tagName
    ) {
        List<Tag> tags = tagService.list(new QueryWrapper<Tag>()
                .like("name", tagName).last("limit 25"));
        return AjaxResult.success(tags);
    }

}
