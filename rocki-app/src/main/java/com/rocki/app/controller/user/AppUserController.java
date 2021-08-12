package com.rocki.app.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.app.context.RequestContext;
import com.rocki.app.controller.vo.currency.CurrencyRecordVo;
import com.rocki.app.controller.vo.playlist.PlaylistDetailVo;
import com.rocki.app.controller.vo.playlist.PlaylistForTopVo;
import com.rocki.app.controller.vo.user.ArtistInfoVO;
import com.rocki.user.domain.ArtistCertifiedId;
import com.rocki.user.service.ArtistCertifiedIdService;
import com.rocki.user.vo.ArtistSearchResultVo;
import com.rocki.app.controller.vo.user.UserInfoVO;
import com.rocki.app.controller.vo.user.UserTopVO;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppCurrencyService;
import com.rocki.app.service.AppPlaylistService;
import com.rocki.app.service.AppTrackService;
import com.rocki.app.service.AppUserService;
import com.rocki.common.annotation.Login;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.ServletUtils;
import com.rocki.currency.domain.CurrencyRecord;
import com.rocki.currency.service.CurrencyRecordService;
import com.rocki.music.domain.Playlist;
import com.rocki.music.domain.Track;
import com.rocki.music.mapper.PlaylistMapper;
import com.rocki.music.mapper.TrackMapper;
import com.rocki.music.service.TrackService;
import com.rocki.user.domain.IncomeRecord;
import com.rocki.user.domain.User;
import com.rocki.user.domain.UserFeedback;
import com.rocki.user.domain.UserFollows;
import com.rocki.user.domain.UserSocialLink;
import com.rocki.user.mapper.UserFollowsMapper;
import com.rocki.user.mapper.UserMapper;
import com.rocki.user.service.IncomeRecordService;
import com.rocki.user.service.UserFeedbackService;
import com.rocki.user.service.UserService;
import com.rocki.user.service.UserSocialLinkService;
import com.rocki.user.utils.UserUtils;
import com.rocki.user.vo.VerificationArtistSearchResultVo;
import com.rocki.user.vo.VerifiedArtistVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author gzc
 * @date 2018-09-23 15:31
 */
@RestController
@RequestMapping("/api/user")
@Validated
@ResponseBody
public class AppUserController extends BaseController {
    Logger ppUserControllerLogger = LoggerFactory.getLogger(AppUserController.class);

    @Value("${token.expire-front}")
    private int expireFront;

    @Autowired
    private AppUserService appUserService;
    @Autowired
    private UserService userService;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;
    @Autowired
    private UserFeedbackService userFeedbackService;
    @Autowired
    private AppPlaylistService appPlaylistService;
    @Autowired
    private TrackService trackService;
    @Autowired
    private IncomeRecordService incomeRecordService;
    @Autowired
    private UserFollowsMapper userFollowsMapper;
    @Autowired
    private AppTrackService appTrackService;
    @Autowired
    private PlaylistMapper playlistMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TrackMapper trackMapper;
    @Autowired
    private CurrencyRecordService currencyRecordService;
    @Autowired
    private AppCurrencyService appCurrencyService;
    @Autowired
    private UserSocialLinkService userSocialLinkService;
    @Autowired
    private ArtistCertifiedIdService artistCertifiedIdService;

    @GetMapping("showUserSocialLinks")
    public AjaxResult showUserSocialLinks(
            @RequestParam(value = "userId") Long userId) {
        List<UserSocialLink> links = userSocialLinkService.list(new QueryWrapper<UserSocialLink>()
                .eq("user_id", userId));
        return AjaxResult.success(links);
    }

    @PostMapping("reportTrack")
    public AjaxResult reportTrack(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("trackId") Long trackId,
            @RequestParam("msg") String reportMsg) {
        Track track = trackService.getById(trackId);

        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }
        String link = "https://rocki.app/track/" + trackId;
        String msg = "I believe this track violates your Terms of Services: " + link + "\n" + reportMsg;

        helpAndFeedback(msg, name, email);
        return AjaxResult.success();
    }

    @PostMapping("contactUs")
    public AjaxResult helpAndFeedback(
            @RequestParam("msg") String msg,
            @RequestParam("name") String name,

            @RequestParam("email") String email) {

        UserFeedback entity = new UserFeedback();
        entity.setCreateTime(new Date());
        entity.setMsg(msg);
        entity.setEmail(email);
        entity.setName(name);
        userFeedbackService.save(entity);

        return AjaxResult.success();
    }


    @Login
    @PostMapping("followsPlaylist")
    public AjaxResult followsPlaylist(
            @RequestParam(value = "playlistId") Long playlistId) {
        User user = RequestContext.getUser();
        Playlist playlist = appPlaylistService.playlistDetail(playlistId);
        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        if (playlist.getUserId().equals(user.getUserId())) {
            return AjaxResult.error(MessageUtils.message("playlist.is.yours"));
        }
        Playlist followsPlaylist = appPlaylistService.followsPlaylist(playlist, user);
        PlaylistDetailVo playlistDetailVo = new PlaylistDetailVo();
        BeanUtils.copyProperties(followsPlaylist, playlistDetailVo);
        return AjaxResult.success(playlistDetailVo);
    }

    @Login
    @PostMapping("unsubscribePlaylist")
    public AjaxResult unsubscribePlaylist(
            @RequestParam(value = "playlistId") Long playlistId) {
        User user = RequestContext.getUser();
        Playlist playlist = appPlaylistService.playlistDetail(playlistId);
        if (playlist == null) {
            return AjaxResult.error(MessageUtils.message("playlist.not.exist"));
        }
        Playlist unsubscribePlaylist = appPlaylistService.unsubscribePlaylist(playlist, user);
        PlaylistDetailVo playlistDetailVo = new PlaylistDetailVo();
        BeanUtils.copyProperties(unsubscribePlaylist, playlistDetailVo);
        return AjaxResult.success(playlistDetailVo);
    }


    @GetMapping("top")
    public AjaxResult topArtist(
            @RequestParam(value = "topFlag") Integer topFlag) {
        List<UserTopVO> userTopVOList = appUserService.topArtist(topFlag)
                .stream()
                .map(user -> {
                    UserTopVO userTopVO = new UserTopVO();
                    BeanUtils.copyProperties(user, userTopVO);
                    return userTopVO;
                }).collect(Collectors.toList());
        return AjaxResult.success(userTopVOList);

    }

    @GetMapping("listALlArtist")
    public AjaxResult lsitAllArtist() {
        Page<User> page = getPage();
        page = userService.page(page, new QueryWrapper<User>()
                .eq("artists_type", UserUtils.USER_ROLE_ARTIST).orderByDesc("full_name"));
        List<ArtistInfoVO> voList = page.getRecords().stream()
                .filter(user -> user.getAvatarUrl() != null)
                .map(user -> {
                    ArtistInfoVO vo = new ArtistInfoVO();
                    BeanUtils.copyProperties(user, vo);
                    return vo;
                }).collect(Collectors.toList());
        TableDataInfo tableDataInfo = getDataTable(voList, voList.size());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping("listAllArtistByName")
    @Cacheable(value = "artistListByFirstLetter", key = "#letter", cacheManager = "fiveMin",
            condition = "#letter != null ")
    public AjaxResult listAllArtistByName(
            @RequestParam(value = "letter", required = false) Character letter) {
        List<User> artists = null;
        if (letter != null && letter - 'A' >= 0 && letter - 'Z' <= 0) {
            artists = userService.list(new QueryWrapper<User>()
                    .eq("", UserUtils.USER_ROLE_ARTIST)
                    .gt("", 0)
                    .and(wrapper -> wrapper.likeRight("username", letter)
                            .or().likeRight("full_name", letter)));
            List<ArtistInfoVO> voList = artists.stream()
                    .map(sourceVo -> {
                        ArtistInfoVO target = new ArtistInfoVO();
                        BeanUtils.copyProperties(sourceVo, target);
                        ArtistCertifiedId certifiedId = artistCertifiedIdService.getOne(
                                new QueryWrapper<ArtistCertifiedId>().eq("user_id", sourceVo.getUserId()));
                        target.setBadgeFlag(certifiedId == null ? UserUtils.CERTIFIED_NONE : certifiedId.getStatus());
                        return target;
                    }).collect(Collectors.toList());
            return AjaxResult.success(voList);
        } else if (letter != null && letter == '#') {
            artists = userService.list(new QueryWrapper<User>()
                    .eq("", UserUtils.USER_ROLE_ARTIST)
                    .gt("", 0).last("'"));
            List<ArtistInfoVO> voList = artists.stream()
                    .map(user -> {
                        ArtistInfoVO vo = new ArtistInfoVO();
                        BeanUtils.copyProperties(user, vo);
                        return vo;
                    }).collect(Collectors.toList());
            return AjaxResult.success(voList);
        } else {
            artists = userService.list(new QueryWrapper<User>()
                    .eq("artists_type", UserUtils.USER_ROLE_ARTIST).gt("track_count", 10));
            Map<Object, List<ArtistInfoVO>> voMap = artists.parallelStream()
                    .map(user -> {
                        ArtistInfoVO vo = new ArtistInfoVO();
                        BeanUtils.copyProperties(user, vo);
                        return vo;
                    }).collect(Collectors.toList()).parallelStream().collect(Collectors.groupingBy(
                            artistInfoVO -> {
                                char indexCh = artistInfoVO.getUsername().toUpperCase().charAt(0);
                                if (indexCh - 'A' >= 0 && indexCh - 'Z' <= 0) {
                                    return indexCh;
                                } else {
                                    return '#';
                                }
                            }, Collectors.toList()));
            return AjaxResult.success(voMap);
        }
    }

    @Login
    @GetMapping("listFollowingArtist")
    public AjaxResult listFollowingArtist() {
        User user = RequestContext.getUser();

        Page<User> page = getPage();
        page = userMapper.getFavouriteListsPage(page, user.getUserId());

        List<User> list = page.getRecords();
        TableDataInfo tableDataInfo = getDataTable(list, page.getTotal());

        return AjaxResult.success(tableDataInfo);
    }

    @Login
    @GetMapping("listFollowingList")
    public AjaxResult listFollowingList() {
        User user = RequestContext.getUser();

        Page<Playlist> page = getPage();
        page = playlistMapper.getFavouriteListsPage(page, user.getUserId());

        List<Playlist> list = page.getRecords();
        TableDataInfo tableDataInfo = getDataTable(list, page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @Login
    @GetMapping("listFavouriteTrack")
    public AjaxResult listFavouriteTrack() {
        User user = RequestContext.getUser();

        Page<Track> page = getPage();
        page = trackMapper.getFavouriteListsPage(page, user.getUserId());

        List<Track> list = page.getRecords();
        TableDataInfo tableDataInfo = getDataTable(list, page.getTotal());

        return AjaxResult.success(tableDataInfo);
    }

    @Login
    @PostMapping("addTrackAsFavourite")
    public AjaxResult addTrackAsFavourite(
            @RequestParam(value = "trackId") Long trackId) {
        User user = RequestContext.getUser();

        Track track = trackService.getById(trackId);

        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }

        appTrackService.followTrack(track, user);
        return AjaxResult.success();
    }

    @Login
    @PostMapping("unsubscribeTrack")
    public AjaxResult unsubscribeTrack(
            @RequestParam(value = "trackId") Long trackId
    ) {
        User user = RequestContext.getUser();

        Track track = trackService.getById(trackId);

        if (track == null) {
            return AjaxResult.error(MessageUtils.message("track.not.exist"));
        }

        return appTrackService.unsubscribeTrack(track, user) ?
                AjaxResult.success() : AjaxResult.error();
    }

    @Login
    @PostMapping("followsArtist")
    public AjaxResult followsArtist(
            @RequestParam(value = "artistId") Long artistId) {
        User user = RequestContext.getUser();
        User artist = userService.getOne(new QueryWrapper<User>()
                .eq("status", UserUtils.USER_STATUS_NORMAL)
                .eq("user_id", artistId)
                .eq("artists_type", UserUtils.USER_ROLE_ARTIST));
        if (artist == null) {
            return AjaxResult.error(MessageUtils.message("artist.not.exists"));
        }
        if (user.getUserId().longValue() == artist.getUserId().longValue()) {
            return AjaxResult.error(MessageUtils.message("can.not.follow.self"));
        }
        User followsArtist = appUserService.followsArtist(artist, user);
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(followsArtist, userInfoVO);
        return AjaxResult.success(userInfoVO);
    }

    @Login
    @PostMapping("unsubscribeArtist")
    public AjaxResult unsubscribeArtist(
            @RequestParam(value = "artistId") Long artistId) {
        User user = RequestContext.getUser();
        User artist = userService.getOne(new QueryWrapper<User>()
                .eq("user_id", artistId)
                .eq("artists_type", UserUtils.USER_ROLE_ARTIST));
        if (artist == null) {
            return AjaxResult.error(MessageUtils.message("artist.not.exists"));
        }
        if (artist.getUserId().longValue() == user.getUserId().longValue()) {
            return AjaxResult.error(MessageUtils.message("can.not.unfollow.self"));
        }
        User followsArtist = appUserService.unsubscribeArtist(artist, user);
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(followsArtist, userInfoVO);
        return AjaxResult.success(userInfoVO);
    }

    @GetMapping("searchArtistBaseByName")
    public AjaxResult searchArtistBaseByName(
            @RequestParam(value = "artistName") String artistName) {
        if (StringUtils.isBlank(artistName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Artist Name"));
        }
        Page<VerificationArtistSearchResultVo> page = getPage();
        page = userMapper.searchArtistBaseByName(page, artistName);
        List<VerificationArtistSearchResultVo> vos = page.getRecords().stream()
                .map(sourceVo -> {
                    VerificationArtistSearchResultVo target = new VerificationArtistSearchResultVo();
                    BeanUtils.copyProperties(sourceVo, target);
                    ArtistCertifiedId certifiedId = artistCertifiedIdService.getOne(
                            new QueryWrapper<ArtistCertifiedId>().eq("user_id", sourceVo.getUserId()));
                    target.setBadgeFlag(certifiedId == null ? UserUtils.CERTIFIED_NONE : certifiedId.getStatus());
                    return target;
                }).collect(Collectors.toList());
        TableDataInfo tableDataInfo = getDataTable(vos, page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping("searchArtist")
    public AjaxResult searchArtist(
            @RequestParam(value = "artistName") String artistName) {
        if (StringUtils.isBlank(artistName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Artist Name"));
        }
        Page<User> page = getPage();
        page = userService.page(page, new QueryWrapper<User>()
                .eq("artists_type", UserUtils.USER_ROLE_ARTIST)
                .and(wrapper -> wrapper.like("full_name", artistName).or()
                        .like("username", artistName))
                .orderByDesc("create_time"));
        List<UserTopVO> userTopVOList = page.getRecords()
                .stream()
                .map(user -> {
                    UserTopVO userTopVO = new UserTopVO();
                    BeanUtils.copyProperties(user, userTopVO);
                    return userTopVO;
                }).collect(Collectors.toList());
        TableDataInfo dataTable = getDataTable(userTopVOList, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @Login
    @GetMapping("listForTracks")
    @ApiOperation("Pagination to query the user’s song list")
    public AjaxResult listForTracks() {
        User user = RequestContext.getUser();
        Page<Track> page = getPage();

        List<Map<String, Object>> listVOList = appUserService.pageUserTracks(page, user);
        TableDataInfo dataTable = getDataTable(listVOList, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @Login
    @GetMapping("listForPlaylist")
    public AjaxResult listForPlaylist() {
        User user = RequestContext.getUser();
        Page<Playlist> page = getPage();
        List<Playlist> list = appUserService.listPersonalPlaylist(page, user, page.getSize(), page.getCurrent());
        List<PlaylistForTopVo> listVOList = list.stream()
                .map(playlist -> {
                    PlaylistForTopVo playlistForTopVo = new PlaylistForTopVo();
                    BeanUtils.copyProperties(, playlistForTopVo);
                    return playlistForTopVo;
                }).collect(Collectors.toList());
        TableDataInfo dataTable = getDataTable(listVOList, page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @Login
    @GetMapping("listForEarnings")
    public AjaxResult listForEarnings() {
        User user = RequestContext.getUser();
        Page<IncomeRecord> page = getPage();
        page = incomeRecordService.page(page, new QueryWrapper<IncomeRecord>()
                .eq("", user.getUserId()));
        TableDataInfo dataTable = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(dataTable);
    }

    @GetMapping("artistInfo")
    public AjaxResult artistInfo(
            @RequestParam("artistId") Long artistId) {
        User byId = userService.getById(artistId);
        if (byId == null) {
            return AjaxResult.error(MessageUtils.message("user.artist.not.exists"));
        }
        FrontLoginUser loginUser = frontTokenService.getLoginUser(ServletUtils.getRequest());
        boolean flag = false;
        if (loginUser != null) {
            User user = loginUser.getUser();
            UserFollows follows = userFollowsMapper.selectOne(new QueryWrapper<UserFollows>()
                    .eq("", artistId)
                    .eq("", user.getUserId()));
            if (follows != null) {
                flag = true;
            }
        }
        ArtistInfoVO vo = new ArtistInfoVO();
        BeanUtils.copyProperties(byId, vo);
        vo.setBannerUrl(StringUtils.isBlank(byId.getHeadImageUrl()) ? byId.getAvatarUrl() : byId.getHeadImageUrl());

        ArtistCertifiedId certifiedId = artistCertifiedIdService.getOne(
                new QueryWrapper<ArtistCertifiedId>().eq("user_id", artistId));

        Map<String, Object> data = new HashMap<>(3);
        data.put("artistInfo", vo);
        data.put("flag", flag);
        data.put("badgeFlag", certifiedId == null ? UserUtils.CERTIFIED_NONE : certifiedId.getStatus());
        return AjaxResult.success(data);
    }


    @Login
    @GetMapping("getSelfCurrencyRecord")
    public AjaxResult getSelfCurrencyRecord() {
        User user = RequestContext.getUser();
        String userAddress = user.getAddress();
        Page<CurrencyRecord> page = getPage();

        page = currencyRecordService.page(page, new QueryWrapper<CurrencyRecord>()
                .eq("", userAddress).or().eq("", userAddress));

        List<CurrencyRecordVo> currencyRecordVos = page.getRecords().stream().map(currencyRecord -> {
            CurrencyRecordVo currencyRecordVo = new CurrencyRecordVo();
            BeanUtils.copyProperties(currencyRecord, currencyRecordVo);
            appCurrencyService.setCurrencyRecordActionInfo(currencyRecordVo);
            return currencyRecordVo;
        }).collect(Collectors.toList());
        TableDataInfo dataTable = getDataTable(currencyRecordVos, page.getTotal());
        return AjaxResult.success(dataTable);
    }
}


