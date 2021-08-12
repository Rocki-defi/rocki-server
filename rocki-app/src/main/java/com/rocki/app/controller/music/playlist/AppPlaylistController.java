package com.rocki.app.controller.music.playlist;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.app.context.RequestContext;
import com.rocki.app.controller.vo.playlist.PlaylistDetailVo;
import com.rocki.app.controller.vo.playlist.PlaylistForTopVo;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppPlaylistService;
import com.rocki.common.annotation.Login;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.ServletUtils;
import com.rocki.music.domain.Playlist;
import com.rocki.music.domain.PlaylistFollows;
import com.rocki.music.domain.PlaylistTracksAssociation;
import com.rocki.music.domain.TrackFollows;
import com.rocki.music.mapper.PlaylistFollowsMapper;
import com.rocki.music.mapper.TrackFollowsMapper;
import com.rocki.music.service.PlaylistService;
import com.rocki.music.service.PlaylistTagService;
import com.rocki.music.service.PlaylistTracksAssociationService;
import com.rocki.music.utils.TrackAndPlayListUtils;
import com.rocki.user.domain.User;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Playlist controller for user
 *
 * @author GZC
 * @date 2020-11-07
 */
@RestController
@RequestMapping("/api/music/playlist")
public class AppPlaylistController extends BaseController {
    @Autowired
    private AppPlaylistService appPlaylistService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private PlaylistTracksAssociationService playlistTracksAssociationService;
    @Autowired
    private PlaylistTagService playlistTagService;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;
    @Autowired
    private PlaylistFollowsMapper playlistFollowsMapper;
    @Autowired
    private TrackFollowsMapper trackFollowsMapper;

    @GetMapping("listAllPlaylist")
    public AjaxResult listAllPlaylist() {
        Page<Playlist> page = getPage();
        List<PlaylistDetailVo> voList = appPlaylistService.pageAllPlaylist(page, page.getSize(), page.getCurrent());
        TableDataInfo tableDataInfo = getDataTable(voList, page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping("search")
    public AjaxResult searchPlaylist(
            @RequestParam(value = "title") String title) {
        if (StringUtils.isBlank(title)) {
            return AjaxResult.error(MessageUtils.message("not.null", "title"));
        }
        Page<Playlist> page = getPage();
        page = playlistService.page(page, new QueryWrapper<Playlist>()
                .eq("is_private", TrackAndPlayListUtils.PUBLIC).like("title", title));
        TableDataInfo dataTable = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @Login
    @PostMapping("create")
    public AjaxResult create(
            @RequestParam(value = "image") MultipartFile imageFile,
            @RequestParam(value = "playlistTitle") String playlistTitle,
            @RequestParam(value = "isPrivate", defaultValue = "2") Integer isPrivate,
            @RequestParam(value = "description") String description) {
        User user = RequestContext.getUser();
        if (!TrackAndPlayListUtils.PUBLIC.equals(isPrivate)
                && !TrackAndPlayListUtils.PRIVATE.equals(isPrivate)) {
            return AjaxResult.error(MessageUtils.message("playlist.create.type.error"));
        }

        Playlist playlist = appPlaylistService.uploadAndSave(imageFile, playlistTitle, isPrivate, description, user);
        return AjaxResult.success(playlist);
    }

    @GetMapping("top")
    public AjaxResult top(
            @RequestParam(value = "topFlag") Integer topFlag) {

        List<PlaylistForTopVo> playlistForTopVos = appPlaylistService.topPlaylist(topFlag).stream()
                .map(playlist -> {
                    PlaylistForTopVo playlistForTopVo = new PlaylistForTopVo();
                    BeanUtils.copyProperties(playlist, playlistForTopVo);
                    return playlistForTopVo;
                }).collect(Collectors.toList());

        return AjaxResult.success(playlistForTopVos);
    }

    @GetMapping("detail")
    public AjaxResult detail(
            @RequestParam(value = "playlistId") Long playlistId) {
        FrontLoginUser loginUser = frontTokenService.getLoginUser(ServletUtils.getRequest());
        boolean flag = false;
        if (loginUser != null) {
            User user = loginUser.getUser();
            PlaylistFollows follows = playlistFollowsMapper.selectOne(new QueryWrapper<PlaylistFollows>()
                    .eq("user_id", user.getUserId())
                    .eq("playlist_id", playlistId));
            if (follows != null) {
                flag = true;
            }
        }
        Playlist playlist = appPlaylistService.playlistDetail(playlistId);
        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        PlaylistDetailVo playlistDetailVo = new PlaylistDetailVo();
        BeanUtils.copyProperties(playlist, playlistDetailVo);
        List<String> nameList = playlistTagService.listForTagByPlaylistId(playlistId);
        Map<String, Object> data = new HashMap<>(3);
        data.put("playlistDetail", playlistDetailVo);
        data.put("tags", nameList);
        data.put("flag", flag);
        return AjaxResult.success(data);
    }

    @GetMapping("listForPlaylist")
    public AjaxResult listForPlaylist(
            @ApiParam(name = "userId", value = "用户Id", required = true)
            @RequestParam(value = "userId") Long userId,
            HttpServletRequest httpServletRequest) {
        Page<Playlist> page = getPage();

        List<PlaylistForTopVo> listVOList = playlistService.page(page, new QueryWrapper<Playlist>()
                .eq("user_id", userId)
                .eq("is_private", TrackAndPlayListUtils.PUBLIC)
                .orderByDesc("id"))
                .getRecords()
                .stream().map(playlist -> {
                    PlaylistForTopVo playlistForTopVo = new PlaylistForTopVo();
                    BeanUtils.copyProperties(playlist, playlistForTopVo);
                    return playlistForTopVo;
                }).collect(Collectors.toList());
        page.setTotal(listVOList.size());
        TableDataInfo dataTable = getDataTable(listVOList, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @GetMapping("listForTrackFromPlaylist")
    public AjaxResult listForTrackFromPlaylist(
            @ApiParam(name = "playlistId", value = "歌单Id", required = true)
            @RequestParam(value = "playlistId") Long playlistId, HttpServletRequest httpServletRequest) {
        Playlist playlist = appPlaylistService.playlistDetail(playlistId);
        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }

        FrontLoginUser currentUser = frontTokenService.getLoginUser(httpServletRequest);
        Integer isPrivate = playlist.getIsPrivate();
        if (TrackAndPlayListUtils.PUBLIC.equals(isPrivate)) {
            return returnPlayList(playlistId, currentUser);
        }

        if (TrackAndPlayListUtils.PRIVATE.equals(isPrivate) && currentUser == null) {
            return AjaxResult.error(MessageUtils.message("playlist.is.private"));
        }
        if (TrackAndPlayListUtils.PRIVATE.equals(isPrivate) &&
                !playlist.getUserId().equals(currentUser.getUser().getUserId())) {
            return AjaxResult.error(MessageUtils.message("playlist.is.private"));
        }
        return playlist.getUserId().equals(currentUser.getUser().getUserId()) ?
                returnPlayList(playlistId, currentUser) :
                AjaxResult.error(MessageUtils.message("playlist.is.private"));
    }

    private AjaxResult returnPlayList(Long playlistId, FrontLoginUser currentUser) {
        Page<PlaylistTracksAssociation> page = getPage();
        page = playlistTracksAssociationService.page(page, new QueryWrapper<PlaylistTracksAssociation>()
                .eq("playlist_id", playlistId));
        List<PlaylistTracksAssociation> list = page.getRecords();
        List<Map<String, Object>> mapList = list.stream().map(playlistTracksAssociation -> {
            Map<String, Object> map = new HashMap<>(2);
            map.put("trackDetail", playlistTracksAssociation);
            Integer integer = 0;
            if (currentUser != null) {
                integer = trackFollowsMapper.selectCount(new QueryWrapper<TrackFollows>()
                        .eq("track_id", playlistTracksAssociation.getTrackId())
                        .eq("follows_id", currentUser.getUser().getUserId()));
            }
            map.put("flag", integer);
            return map;
        }).collect(Collectors.toList());

        TableDataInfo dataTable = getDataTable(mapList, page.getTotal());
        return AjaxResult.success(dataTable);
    }


    @Login
    @PostMapping("setPlaylistPrivateOrPublic")
    @ApiOperation("更改歌单成私人或者公有")
    public AjaxResult setPlaylistPrivateOrPublic(
            @ApiParam(name = "playlistId", value = "playlist ID", required = true)
            @RequestParam(value = "playlistId") Long playlistId,
            @ApiParam(name = "isPrivate", value = "private or public", required = true)
            @RequestParam(value = "isPrivate") Integer isPrivate
    ) {
        User user = RequestContext.getUser();

        Playlist playlist = playlistService.getById(playlistId);
        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }

        if (!user.getUserId().equals(playlist.getUserId())) {
            return AjaxResult.error(MessageUtils.message("playlist.not.yours"));
        }

        if (!TrackAndPlayListUtils.isPrivateOrPublic(isPrivate)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        playlist.setIsPrivate(isPrivate);
        playlistService.updateById(playlist);

        return AjaxResult.success();
    }

    @Login
    @PostMapping("deleteMyPlaylist")
    public AjaxResult deleteMyPlaylist(
            @RequestParam(value = "playlistId") Long playlistId
    ) {
        User user = RequestContext.getUser();
        Playlist playlist = playlistService.getById(playlistId);

        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        if (!user.getUserId().equals(playlist.getUserId())) {
            return AjaxResult.error(MessageUtils.message("playlist.not.yours"));
        }

        appPlaylistService.deleteMyPlaylist(playlist, user);
        return AjaxResult.success();
    }

    @Login
    @PostMapping("editPlaylistDetail")
    @ApiOperation("Edit/update Playlist detail")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult editPlaylistDetail(
            @ApiParam(name = "playlistId", value = "Playlist Id", required = true)
            @RequestParam(value = "playlistId", required = true) Long playlistId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "playlistTitle", required = false) String playlistTitle,
            @ApiParam(name = "description", value = "歌单描述", required = false)
            @RequestParam(value = "description", required = false) String description
    ) {
        User user = RequestContext.getUser();
        Playlist playlist = playlistService.getById(playlistId);

        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        if (!user.getUserId().equals(playlist.getUserId())) {
            return AjaxResult.error(MessageUtils.message("playlist.not.yours"));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            playlist.setImage(appPlaylistService.uploadNewImage(imageFile));
        }
        if (StringUtils.isNotBlank(playlistTitle)) {
            playlist.setTitle(playlistTitle);
        }
        if (StringUtils.isNotBlank(description)) {
            playlist.setDescription(description);
        }

        playlistService.updateById(playlist);

        return AjaxResult.success(playlist);
    }
}
