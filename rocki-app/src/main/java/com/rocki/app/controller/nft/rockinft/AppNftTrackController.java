package com.rocki.app.controller.nft.rockinft;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.app.controller.vo.nft.BouncePoolVo;
import com.rocki.app.controller.vo.nft.NftAlbumVo;
import com.rocki.app.controller.vo.nft.NftTokenInfoVo;
import com.rocki.app.controller.vo.nft.NftTrackVo;
import com.rocki.app.controller.vo.nft.complex.NftSigleVo;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.utils.MessageUtils;
import com.rocki.nft.domain.NftAlbum;
import com.rocki.nft.domain.NftTokenInfo;
import com.rocki.nft.domain.NftTrack;
import com.rocki.nft.mapper.NftAlbumMapper;
import com.rocki.nft.service.NftAlbumService;
import com.rocki.nft.service.NftTokenInfoService;
import com.rocki.nft.service.NftTrackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NFT -- version 1.0
 *
 * @author David Wilde
 * @Package com.rocki.app.controller.music.track
 * @date 2/27/21 4:21 下午
 */
@RestController
@RequestMapping("/api/v2/nft/track")
@Api("Rocki NFT Track接口")
public class AppNftTrackController extends BaseController {
    Logger appNftTrackControllerLogger = LoggerFactory.getLogger(AppNftTrackController.class);

    @Autowired
    private NftAlbumService nftAlbumService;
    @Autowired
    private NftAlbumMapper nftAlbumMapper;
    @Autowired
    private NftTrackService nftTrackService;
    @Autowired
    private NftTokenInfoService nftTokenInfoService;

    @GetMapping("listSingleTrackAlbums")
    @Cacheable(value = "SingleTrackAlbumsList", key = "#networkId", cacheManager = "fiveMin")
    public AjaxResult listSingleTrackAlbums(
            @RequestParam(name = "networkId", required = false, defaultValue = "56") Long networkId
    ) {
        Page<NftAlbum> page = getPage();
        if (networkId == 56L) {
            page = nftAlbumService.page(page, new QueryWrapper<NftAlbum>().ge("status", 0));
        } else {
            page = nftAlbumMapper.listAllAlbumByNetworkId(page, networkId);
        }
        List<NftSigleVo> vos = page.getRecords().stream().filter(nftAlbum -> {
            int integer = nftTrackService.count(new QueryWrapper<NftTrack>().eq("album_id", nftAlbum.getId()));
            return integer == 1;
        }).map(nftAlbum -> {
            NftTrack nftTrack = nftTrackService.getOne(
                    new QueryWrapper<NftTrack>().eq("album_id", nftAlbum.getId()));
            NftTokenInfo nftTokenInfo = nftTokenInfoService.getById(nftAlbum.getTokenId());

            NftSigleVo vo = new NftSigleVo();

            NftAlbumVo albumVo = new NftAlbumVo();
            BeanUtils.copyProperties(nftAlbum, albumVo);
            vo.setAlbumVo(albumVo);

            NftTrackVo trackVo = new NftTrackVo();
            BeanUtils.copyProperties(nftTrack, trackVo);
            vo.setTrackVo(trackVo);

            NftTokenInfoVo tokenInfoVo = new NftTokenInfoVo();
            if (nftTokenInfo != null) {
                BeanUtils.copyProperties(nftTokenInfo, tokenInfoVo);
                vo.setTokenInfoVo(tokenInfoVo);
            }else {
                vo.setTokenInfoVo(null);
            }

            BouncePoolVo bouncePoolVo = new BouncePoolVo();
            BeanUtils.copyProperties(nftAlbum, bouncePoolVo);
            vo.setBouncePoolVo(bouncePoolVo);

//            Integer i = ServletUtils.getParameterToInt("pageSize");
            return vo;
        }).collect(Collectors.toList());
        TableDataInfo tableDataInfo = getDataTable(vos, page.getTotal());
//        redisCache.setCacheObject("API:listSingleTrackAlbums", tableDataInfo, 60, TimeUnit.MINUTES);
        return AjaxResult.success(tableDataInfo);
    }


    @GetMapping("listDetailByAlbumId")
    public AjaxResult listDetailByAlbumId(
            @RequestParam(name = "albumId", required = true) Long albumId
    ) {
        NftAlbum nftAlbum = nftAlbumService.getById(albumId);
        if (nftAlbum == null || nftAlbum.getStatus() == -1) {
            return AjaxResult.error(MessageUtils.message("album.not.found"));
        }
        NftTrack nftTrack = nftTrackService.getOne(
                new QueryWrapper<NftTrack>().eq("album_id", nftAlbum.getId()));
        NftTokenInfo nftTokenInfo = nftTokenInfoService.getById(nftAlbum.getTokenId());

        NftSigleVo vo = new NftSigleVo();

        NftAlbumVo albumVo = new NftAlbumVo();
        BeanUtils.copyProperties(nftAlbum, albumVo);
        vo.setAlbumVo(albumVo);

        NftTrackVo trackVo = new NftTrackVo();
        BeanUtils.copyProperties(nftTrack, trackVo);
        vo.setTrackVo(trackVo);

        NftTokenInfoVo tokenInfoVo = new NftTokenInfoVo();
        if (nftTokenInfo != null) {
            BeanUtils.copyProperties(nftTokenInfo, tokenInfoVo);
            vo.setTokenInfoVo(tokenInfoVo);
        }else {
            vo.setTokenInfoVo(null);
        }

        BouncePoolVo bouncePoolVo = new BouncePoolVo();
        BeanUtils.copyProperties(nftAlbum, bouncePoolVo);
        vo.setBouncePoolVo(bouncePoolVo);

        return AjaxResult.success(vo);
    }

    @GetMapping("listAlbumByTitle")
    public AjaxResult listAlbumByTitle(
            @RequestParam(value = "title", required = true) String trackTitle
    ) {
        if (StringUtils.isBlank(trackTitle)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Track Title"));
        }
        Page<NftAlbum> page = getPage();
        page = nftAlbumService.page(page, new QueryWrapper<NftAlbum>().like("title", trackTitle)
                .ge("status", 0));
        TableDataInfo dataTable = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(dataTable);
    }

}
