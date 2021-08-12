package com.rocki.app.controller.nft.rockinft;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.AmazonS3;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.app.context.RequestContext;
import com.rocki.app.controller.vo.nft.NftUrlsVo;
import com.rocki.app.controller.vo.nft.complex.CreatedTokenVo;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppNftService;
import com.rocki.app.utils.json.RockiJson;
import com.rocki.common.annotation.Login;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.redis.RedisCache;
import com.rocki.common.exception.CustomException;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.eth.nft.BscNftUtils;
import com.rocki.common.utils.file.MimeTypeUtils;
import com.rocki.common.utils.file.awss3.AwsS3Client;
import com.rocki.common.utils.ip.IpUtils;
import com.rocki.common.utils.uuid.IdUtils;
import com.rocki.nft.domain.NftAlbum;
import com.rocki.nft.domain.NftTokenInfo;
import com.rocki.nft.domain.NftTrack;
import com.rocki.nft.service.NftAlbumService;
import com.rocki.nft.service.NftTokenInfoService;
import com.rocki.nft.service.NftTrackService;
import com.rocki.user.domain.User;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.rocki.common.utils.StringUtils.isBlank;

@RequestMapping("/api/v2/nft/user")
public class AppNftUserController {
    Logger logger = LoggerFactory.getLogger(AppNftUserController.class);

    @Autowired
    private AppNftService appNftService;
    @Autowired
    private NftAlbumService nftAlbumService;
    @Autowired
    private NftTrackService nftTrackService;
    @Autowired
    private NftTokenInfoService nftTokenInfoService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;


    @PostMapping("createNftToken")
    @CacheEvict(value = "SingleTrackAlbumsList", key = "#networkId")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult createNftToken(
            @RequestParam(value = "trackTitle", required = true) String trackTitle,
            @RequestParam(value = "trackDescription", required = true) String trackDescription,
            @RequestParam(value = "trackFile", required = true) MultipartFile trackFile,
            @RequestParam(value = "trackImage", required = true) MultipartFile trackImage,

            @RequestParam(value = "albumTitle", required = true) String albumTitle,
            @RequestParam(value = "albumDescription", required = true) String albumDescription,
            @RequestParam(value = "albumImage", required = true) MultipartFile albumImage,

            @RequestParam(value = "artistName", required = true) String artistName,
            @RequestParam(value = "artistDescription", required = true) String artistDescription,
            @RequestParam(value = "artistAvarta", required = true) MultipartFile artistAvarta,

            @RequestParam(value = "userAddr", required = true) String userAddr,
            @RequestParam(value = "initialSupply", required = true) Long initialSupply,
            @ApiParam(name = "networkId", value = "networkId", required = false)
            @RequestParam(value = "networkId", required = false, defaultValue = "56") Long networkId,
            HttpServletRequest request
    ) {
        String userIpAddr = IpUtils.getIpAddr(request);
        logger.info("userAddr: {}, and address is {}, reques {} at {}", userIpAddr, userAddr, request.getRequestURI(), new Date());

//        String userReqPort = String.valueOf(request.getRemotePort());
        String cacheKey = userAddr + ":" + userIpAddr;
        String func = redisCache.getCacheObject(cacheKey);
        if (func != null) {
            return AjaxResult.error(MessageUtils.message("repeated.submit"));
        }
        String cacheObj = "Create NFT Token";
        redisCache.setCacheObject(cacheKey, cacheObj, 60, TimeUnit.SECONDS);

        String erc1155Addr = BscNftUtils.BSC_ERC1155_ADDR;
        String erc20Addr = BscNftUtils.BSC_ERC20_ADDR;
        String nodeUrl = BscNftUtils.BSC_CHAIN_URL;
        if (networkId.equals(BscNftUtils.RINKEBY_CHAIN_ID)) {
            erc1155Addr = BscNftUtils.RINKEBY_ERC1155_ADDR;
            erc20Addr = BscNftUtils.RINKEBY_ERC20_ADDR;
            nodeUrl = BscNftUtils.RINKEBY_CHAIN;
        }

        if (!BscNftUtils.checkBeforeCreating1155(userAddr, erc1155Addr, erc20Addr, nodeUrl)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("fees.not.enough"));
        }

        FrontLoginUser frontLoginUser = frontTokenService.getLoginUser(request);
        Long userId = 0L;
        if (frontLoginUser != null) {
            userId = frontLoginUser.getUser().getUserId();
        }

        if (isBlank(trackTitle)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "track title"));
        }
        if (isBlank(trackDescription)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "track description"));
        }
        if (isBlank(albumTitle)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "album title"));
        }
        if (isBlank(albumDescription)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "album description"));
        }
        if (isBlank(artistName)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "artist name"));
        }
        if (isBlank(artistDescription)) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("not.null", "artist description"));
        }
        if (initialSupply < 1) {
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("illegal.initial.supply"));
        }

        File temp = null;
        long duration = 0L;
        try {
            temp = new File(Objects.requireNonNull(trackFile.getOriginalFilename()));
            FileUtils.copyInputStreamToFile(trackFile.getInputStream(), temp);
            MultimediaObject instance = new MultimediaObject(temp);
            MultimediaInfo result = instance.getInfo();
            duration = result.getDuration() / MimeTypeUtils.DURATION_TO_SECONDS;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("failed-to-parse-audio");
            redisCache.deleteObject(cacheKey);
            return AjaxResult.error(MessageUtils.message("parsing.failed"));
        } finally {
            if (temp != null && temp.exists()) {
                if (!temp.delete()) {
                    temp.deleteOnExit();
                }
            }
        }
        List<String> urls = appNftService.uplodaFiles(trackFile, trackImage, artistAvarta, albumImage, cacheKey);
        String trackUrl = urls.get(0);
        String trackImageUrl = urls.get(1);
        String artistAvartaUrl = urls.get(2);
        String albumImageUrl = urls.get(3);

        Date unifiedTime = new Date();
        NftAlbum nftAlbum = new NftAlbum();
        nftAlbum.setArtistName(artistName);
        nftAlbum.setArtistDescription(artistDescription);
        nftAlbum.setTitle(albumTitle);
        nftAlbum.setDescription(albumDescription);
        nftAlbum.setArtistAvartaUrl(artistAvartaUrl);
        nftAlbum.setCreateTime(unifiedTime);
        nftAlbum.setImageUrl(albumImageUrl);
        nftAlbum.setArtistId(userId);
        nftAlbum.setBouncePoolId(-1L);
        nftAlbum.setBouncePoolType(-1);
        nftAlbumService.save(nftAlbum);
        Long albumId = nftAlbum.getId();

        NftTrack nftTrack = new NftTrack();
        nftTrack.setTitle(albumTitle);
        nftTrack.setDescription(albumDescription);
        nftTrack.setImageUrl(trackImageUrl);
        nftTrack.setTrackUrl(trackUrl);
        nftTrack.setAlbumId(albumId);
        nftTrack.setUploadDate(unifiedTime);
        nftTrack.setReleaseDate(unifiedTime);
        nftTrack.setDuration(duration);
        nftTrackService.save(nftTrack);

        String jsonPath = "nft/json/";
        String jsonName = IdUtils.fastUUID() + ".json";
        File json = new File(jsonName);
        RockiJson.Properties properties = new RockiJson.Properties();
        properties.setArtist(artistName);
        properties.setImage(albumImageUrl);
        properties.setTitle(albumTitle);
        properties.setPlayUrl(trackUrl);
        properties.setDescription(albumDescription);
        properties.setCreateTime(unifiedTime.toString());
        properties.setDuration(duration);
        properties.setReleaseDate(unifiedTime.toString());
        RockiJson rockiJson = new RockiJson();
        rockiJson.setProperties(properties);
        String jsonString = JSON.toJSONString(rockiJson);
        try {
            Writer write = new OutputStreamWriter(new FileOutputStream(json), StandardCharsets.UTF_8);
            write.write(jsonString);
            write.flush();
            write.close();
        } catch (Exception e) {
            e.printStackTrace();
            redisCache.deleteObject(cacheKey);
            throw new CustomException(MessageUtils.message("failed.create.meta"));
        }

        AmazonS3 jsonClient = null;
        String jsonUrl = null;
        try {
            jsonClient = AwsS3Client.getAmazonS3Client();
            jsonName = jsonPath + jsonName;
            jsonClient.putObject(AwsS3Client.BUCKETNAME, jsonName, json);
            /*jsonUrl = "https://rocki-nft.oss-us-west-1.aliyuncs.com/" + jsonName;*/
            jsonUrl = AwsS3Client.CLOUDFLAFRE_CDN + jsonName;
        } catch (Exception e) {
            e.printStackTrace();
            redisCache.deleteObject(cacheKey);
            throw new CustomException(MessageUtils.message("failed.upload"));
        } finally {
            if (json.exists()) {
                if (!json.delete()) {
                    json.deleteOnExit();
                }
            }
        }

        List<String> receipt = BscNftUtils.createTokenForUser(userAddr, jsonUrl, initialSupply, erc1155Addr, nodeUrl);
        if (receipt == null || isBlank(receipt.get(0)) || isBlank(receipt.get(1))) {
            redisCache.deleteObject(cacheKey);
            throw new CustomException(MessageUtils.message("failed.create.token"));
        }

        Long tokenIdOf1155 = Long.valueOf(receipt.get(1));
        NftTokenInfo nftTokenInfo = new NftTokenInfo();
        nftTokenInfo.setTokenId(tokenIdOf1155);
        nftTokenInfo.setUri(jsonUrl);
        nftTokenInfo.setNetworkId(networkId);
        nftTokenInfo.setContractAddress(erc1155Addr);
        nftTokenInfo.setCreaterAddr(userAddr);
        nftTokenInfoService.save(nftTokenInfo);
        Long nftTokenKeyId = nftTokenInfo.getId();

        nftAlbum.setTokenId(nftTokenKeyId);
        nftAlbum.setStatus(1);
        nftAlbumService.updateById(nftAlbum);

        redisCache.deleteObject(cacheKey);
        CreatedTokenVo vo = new CreatedTokenVo(receipt.get(1), receipt.get(0), BscNftUtils.BSC_ERC1155_ADDR, albumId);
        return AjaxResult.success(vo);
    }

    @Login
    @PostMapping("updateAlbumBounceInfo")
    public AjaxResult updateAlbumBounceInfo(
            @RequestParam(name = "albumId", required = true) Long albumId,
            @RequestParam(name = "userAddr", required = true) String userAddr,
            @RequestParam(name = "bounceType") Integer type,
            @RequestParam(name = "poolId", required = true) Long poolId
    ) {
        if (isBlank(userAddr)) {
            AjaxResult.error(MessageUtils.message("illegal.address"));
        }
        NftAlbum nftAlbum = nftAlbumService.getById(albumId);
        if (Objects.isNull(nftAlbum)) {
            return AjaxResult.error(MessageUtils.message("album.not.exist"));
        }

        if (nftAlbum.getTokenId() == null) {
            return AjaxResult.error(MessageUtils.message("no.associated.token.info.with.album"));
        }

        NftTokenInfo nftTokenInfo = nftTokenInfoService.getById(nftAlbum.getTokenId());
        if (!nftTokenInfo.getCreaterAddr().equals(userAddr)) {
            return AjaxResult.error("token.not.belong");
        }

        nftAlbum.setBouncePoolId(poolId);
        nftAlbum.setBouncePoolType(type);
        nftAlbum.setStatus(2);
        nftAlbumService.updateById(nftAlbum);

        return AjaxResult.success();
    }

    @GetMapping("getAllNftAlbumCreatedByUser")
    public AjaxResult getNftAlbumCreatedByUser(
            @RequestParam(name = "userAddr", required = true) String userAddr,
            @RequestParam(name = "networkId", required = false, defaultValue = "56") Long networkId
    ) {
        List<NftAlbum> nftAlbums;
        if (networkId != 0) {
            nftAlbums = nftTokenInfoService.list(new QueryWrapper<NftTokenInfo>()
                    .eq("creater_addr", userAddr).eq("network_id", networkId)).stream()
                    .map(nftTokenInfo -> nftAlbumService.getOne(new QueryWrapper<NftAlbum>()
                            .eq("token_id", nftTokenInfo.getId()))).collect(Collectors.toList());
        } else {
            nftAlbums = nftTokenInfoService.list(new QueryWrapper<NftTokenInfo>()
                    .eq("creater_addr", userAddr)).stream()
                    .map(nftTokenInfo -> nftAlbumService.getOne(new QueryWrapper<NftAlbum>()
                            .eq("token_id", nftTokenInfo.getId()))).collect(Collectors.toList());
        }
        return AjaxResult.success(nftAlbums);
    }

    @Login
    @GetMapping("getAllNftAlbumWithoutCreateToken")
    public AjaxResult getAllNftAlbumWithoutCreateToken() {
        User user = RequestContext.getUser();
        List<NftAlbum> nftAlbums = nftAlbumService.list(new QueryWrapper<NftAlbum>()
                .eq("artist_id", user.getUserId()).eq("status", 0));
        if (nftAlbums.isEmpty()) {
            return AjaxResult.error(MessageUtils.message("no.nft.album.was.created"));
        }
        return AjaxResult.success(nftAlbums);
    }

}
