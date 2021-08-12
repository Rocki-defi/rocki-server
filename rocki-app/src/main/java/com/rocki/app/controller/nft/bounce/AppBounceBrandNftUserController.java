package com.rocki.app.controller.nft.bounce;

import com.rocki.app.controller.bo.nft.ReceiptBo;
import com.rocki.app.controller.vo.bounce.RockiMetadata;
import com.rocki.app.controller.vo.nft.NftUrlsVo;
import com.rocki.app.service.AppBounceBrandNftService;
import com.rocki.app.service.AppNftService;
import com.rocki.app.service.FileHandleService;
import com.rocki.common.annotation.Login;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.redis.RedisCache;
import com.rocki.common.exception.CustomException;
import com.rocki.common.utils.MessageCollection;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.RegexUtils;
import com.rocki.common.utils.eth.nft.BscUtils;
import com.rocki.common.utils.ip.IpUtils;
import com.rocki.common.utils.security.RsaUtils;
import com.rocki.message.aws.SendEmail;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.rocki.common.utils.StringUtils.isBlank;
import static com.rocki.common.utils.StringUtils.isEmpty;

@RestController
@RequestMapping("/api/v2/nft/user/brand")
public class AppBounceBrandNftUserController {
    Logger logger = LoggerFactory.getLogger(AppBounceBrandNftUserController.class);

    private final AppNftService appNftService;

    private final AppBounceBrandNftService appBounceBrandNftService;
    private final FileHandleService fileHandleService;

    private final RedisCache redisCache;

    @Value("${rsa.public}")
    private String publickey;
    @Value("${rsa.private}")
    private String privatekey;

    @Autowired
    public AppBounceBrandNftUserController(
            AppNftService appNftService, AppBounceBrandNftService appBounceBrandNftService,
            FileHandleService fileHandleService, RedisCache redisCache) {
        this.appNftService = appNftService;
        this.appBounceBrandNftService = appBounceBrandNftService;
        this.fileHandleService = fileHandleService;
        this.redisCache = redisCache;
    }


    @PostMapping("sendReceipt")
    public AjaxResult sendReceipt(
            @ApiParam(name = "contractAddr", value = "contract address", required = true)
            @RequestParam(name = "contractAddr", required = true) String contractAddr,
            @ApiParam(name = "tokenId", value = "token id", required = true)
            @RequestParam(name = "tokenId", required = true) Long tokenId,

            @ApiParam(name = "email", value = "email", required = true)
            @RequestParam(name = "email", required = true) String email,

            @ApiParam(name = "hash", value = "hash", required = true)
            @RequestParam(name = "hash", required = true) String hash,
            @ApiParam(name = "userAddr", value = "user's address", required = true)
            @RequestParam(name = "userAddr", required = true) String userAddr,
            @ApiParam(name = "blockNum", value = "block number", required = true)
            @RequestParam(name = "blockNum", required = true) Long blockNum,

            @ApiParam(name = "count", value = "count of nft user bought", required = true)
            @RequestParam(name = "count", required = true) String count,
            @ApiParam(name = "total", value = "total of NFT", required = true)
            @RequestParam(name = "total", required = true) String total,
            @ApiParam(name = "price", value = "price of nft", required = true)
            @RequestParam(name = "price", required = true) String price,
            HttpServletRequest request) {
        AjaxResult result;
        long currentBlockNum = BscUtils.getCurrentBlockNumber();
        String userIpAddr = IpUtils.getIpAddr(request);
        logger.info("userAddr: {} : {} hash {} at {}", userIpAddr, request.getRequestURI(), hash, new Date());
        String cacheKey = userIpAddr + ":" + request.getRequestURI();
        String cacheHash = redisCache.getCacheObject(cacheKey);

        if (cacheHash != null) {
            result = AjaxResult.error(MessageUtils.message(MessageCollection.REPEATED_SUBMIT));
        } else if (!RegexUtils.isEmail(email)) {
            result = AjaxResult.error(MessageUtils.message(MessageCollection.USER_EMAIL_NOT_VALID));
        } else if (isEmpty(hash) || isEmpty(userAddr) || isEmpty(contractAddr)
                || !appBounceBrandNftService.verifyTransaction(
                currentBlockNum, blockNum, hash, contractAddr)) {
            redisCache.setCacheObject(cacheKey, hash, 1, TimeUnit.SECONDS);
            result = AjaxResult.error();
        } else {
            RockiMetadata rockiMetadata = appBounceBrandNftService.getRockiMetadataObj(
                    tokenId, contractAddr);
            String key = rockiMetadata.getKey() == null ? rockiMetadata.getEmail() : rockiMetadata.getKey();
            logger.info("userAddr: {} : {} hash {} at {}, get metadata success",
                    userIpAddr, request.getRequestURI(), hash, new Date());
            String sellerEmail;
            try {
                if (key == null) {
                    sellerEmail = "null";
                } else {
                    sellerEmail = new String(RsaUtils.decryptByPrivateKey(Base64.decodeBase64(key),
                            Base64.decodeBase64(privatekey)));
                }
            } catch (Exception e) {
                throw new CustomException(MessageUtils.message(""));
            }
            String title = rockiMetadata.getAlbumTitle();
            String artist = rockiMetadata.getArtistName();
            logger.info("userAddr: {} : {} hash {} at {}, get artist info success", userIpAddr, request.getRequestURI(),
                    hash, new Date());
            if (RegexUtils.isEmail(sellerEmail)) {
                SendEmail.sendEmailToSeller(sellerEmail, artist, title, price, email, count);
            }
            logger.info("userAddr: {} : {} hash {} at {}, send email to seller success", userIpAddr,
                    request.getRequestURI(), hash, new Date());
            if (RegexUtils.isEmail(email)) {
                SendEmail.sendEmailToBuyer(email, userAddr, title, count,
                        artist, price, total);
            }
            logger.info("userAddr: {} : {} hash {} at {}, send to buyer success",
                    userIpAddr, request.getRequestURI(),hash, new Date());
            redisCache.setCacheObject(hash, cacheKey, 5, TimeUnit.SECONDS);
            result = AjaxResult.success();
        }

        return result;
    }


    @PostMapping("uploadNftFiles")
    public AjaxResult uploadNftFiles(
            @ApiParam(name = "trackFile", value = "歌曲文件", required = true)
            @RequestParam(name = "trackFile") MultipartFile trackFile,
            @ApiParam(name = "trackImage", value = "歌曲封面", required = true)
            @RequestParam(name = "trackImage") MultipartFile trackImage,

            @ApiParam(name = "albumImage", value = "专辑封面", required = true)
            @RequestParam(name = "albumImage") MultipartFile albumImage,

            @ApiParam(name = "artistAvarta", value = "艺人头像", required = true)
            @RequestParam(name = "artistAvarta") MultipartFile artistAvarta,

            @ApiParam(name = "email", value = "email")
            @RequestParam(name = "email", required = false, defaultValue = "null") String email,
            HttpServletRequest request
    ) {
        // Limit the upload times for each IP.
        logger.info("userAddr: {} : {}, at {}", IpUtils.getIpAddr(request), request.getRequestURI(), new Date());

        String cacheKey = String.format("%s:%s", IpUtils.getIpAddr(request), request.getRequestURI());
        String func = redisCache.getCacheObject(cacheKey);
        if (func != null) {
            return AjaxResult.error(MessageUtils.message(MessageCollection.REPEATED_SUBMIT));
        }
        redisCache.setCacheObject(cacheKey, email, 5, TimeUnit.SECONDS);

        Long duration = fileHandleService.getTrackDuration(trackFile);

        // upload files to S3 (track file, cover of track, artist avarta, cover of album)
        List<String> urls = appNftService.uplodaFiles(trackFile, trackImage, artistAvarta, albumImage, cacheKey);

        try {
            if (RegexUtils.isEmail(email)) {
                email = Base64.encodeBase64String(RsaUtils.encryptByPublicKey(email.getBytes(),
                        Base64.decodeBase64(publickey)));
            }
        } catch (Exception e) {
            logger.debug("encrpted addr false {}", email);
        }

        NftUrlsVo nftUrlsVo = new NftUrlsVo(urls.get(0), urls.get(1), urls.get(2), urls.get(3), duration, email);

        return AjaxResult.success(nftUrlsVo);
    }

    /**
     * To re-encrpte user's email address, if <a href="AppBounceBrandNftUserController.html#sendReceipt">sendRecept</a>
     * faild.
     *
     * @param email email address
     * @param request HttpServletRequest
     * @return encrypted email address
     */
    @Login
    @PostMapping("redoea")
    public AjaxResult reEncryptedAddr(
            @ApiParam(name = "email", value = "email", required = true)
            @RequestParam(name = "email") String email,
            HttpServletRequest request
    ) {
        String cacheKey = String.format("%s:%s", IpUtils.getIpAddr(request), request.getRequestURI());
        String func = redisCache.getCacheObject(cacheKey);
        if (func != null) {
            logger.info("userIpAddr: {}:{} at {}", IpUtils.getIpAddr(request), request.getRequestURI(), new Date());
            return AjaxResult.error(MessageUtils.message(MessageCollection.REPEATED_SUBMIT));
        }
        if (isBlank(email) || !RegexUtils.isEmail(email)) {
            logger.info("userIpAddr: {}:{} at {}", IpUtils.getIpAddr(request), request.getRequestURI(), new Date());
            redisCache.setCacheObject(cacheKey, email, 5, TimeUnit.SECONDS);
            return AjaxResult.error(MessageUtils.message(MessageCollection.USER_EMAIL_NOT_VALID));
        }

        try {
            email = Base64.encodeBase64String(RsaUtils.encryptByPublicKey(email.getBytes(),
                    Base64.decodeBase64(publickey)));
        } catch (Exception e) {
            logger.debug("encrpted addr false {}", email);
        }

        return AjaxResult.success(email);

    }

}
