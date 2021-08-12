package com.rocki.app.controller.dapp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.app.controller.vo.staking.ArtistVrfyVo;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.StringUtils;
import com.rocki.dapp.domain.BurnRecord;
import com.rocki.dapp.domain.StakeRecord;
import com.rocki.dapp.service.BurnRecordService;
import com.rocki.dapp.service.StakeRecordService;
import com.rocki.user.domain.ArtistCertifiedId;
import com.rocki.user.mapper.ArtistCertifiedIdMapper;
import com.rocki.user.service.ArtistCertifiedIdService;
import com.rocki.user.utils.UserUtils;
import com.rocki.user.vo.VerifiedArtistVo;
import com.rocki.user.vo.VerifiedArtistWithRewardVo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David Wilde
 * @Package com.rocki.app.controller.staking
 * @ClassName AppUserActionController
 * @date 7/28/21 10:30 上午
 */
@RestController
@RequestMapping(path = "api/v2/stk/vrfy/")
@ApiOperation("userAction")
public class AppUserActionController extends BaseController {
    Logger logger = LoggerFactory.getLogger(AppUserActionController.class);

    @Value("${vrfy.prvt}")
    private String vrfyAdminPrvtKey;
    @Value("${vrfy.expiration}")
    private Long timeExpiration;

    @Autowired
    private ArtistCertifiedIdService artistCertifiedIdService;
    @Autowired
    private StakeRecordService stakeRecordService;
    @Autowired
    private BurnRecordService burnRecordService;

    @Autowired
    private ArtistCertifiedIdMapper artistCertifiedIdMapper;

    @GetMapping(path = "verifyArtist")
    public AjaxResult verifyArtist(
            @RequestParam("userId") Long userId
    ) {
        logger.info("Artist ({}) has been verified, verified stated ================", userId);
        ArtistCertifiedId artistCertifiedId = artistCertifiedIdService.getOne(
                new QueryWrapper<ArtistCertifiedId>().eq("user_id", userId));
        if (artistCertifiedId == null || !UserUtils.ARTIST_CERTIFIED.equals(artistCertifiedId.getStatus())) {
            return AjaxResult.error(MessageUtils.message("not.verified.artist"));
        }

        long timestamp = LocalDateTime.now()
                .plusMinutes(timeExpiration).toEpochSecond(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        String encoded = TypeEncoder.encode(new Address(artistCertifiedId.getUserAddress()))
                + TypeEncoder.encode(new org.web3j.abi.datatypes.generated.Uint32(timestamp));
        logger.info("timestamp: {}", timestamp);
        logger.info("resultEncoded: {}", encoded);

        Credentials credentials = Credentials.create(vrfyAdminPrvtKey);
        ECKeyPair ecKeyPair = new ECKeyPair(
                credentials.getEcKeyPair().getPrivateKey(), credentials.getEcKeyPair().getPublicKey());

        byte[] hashMsg3 = Hash.sha3(Numeric.hexStringToByteArray(encoded));
        logger.info("signedMessage: {}", Numeric.toHexStringNoPrefix(hashMsg3));

        Sign.SignatureData signatureData =
                Sign.signPrefixedMessage(hashMsg3, ecKeyPair);

        logger.info("v: " + Numeric.toHexStringNoPrefix(signatureData.getV()));
        logger.info("r: " + Numeric.toHexStringNoPrefix(signatureData.getR()));
        logger.info("s: " + Numeric.toHexStringNoPrefix(signatureData.getS()));

        String result = Numeric.toHexStringNoPrefix(signatureData.getR());
        result = result + Numeric.toHexStringNoPrefix(signatureData.getS());
        result = result + Numeric.toHexStringNoPrefix(signatureData.getV());

        logger.info("verified ended. ================================");

        return AjaxResult.success(new ArtistVrfyVo(result, timestamp, artistCertifiedId.getUserAddress()));
    }


    @GetMapping(path = "stakeArtistInfo")
    private AjaxResult stakeArtistInfo(
            @ApiParam(name = "userAddr", value = "user address", required = true)
            @RequestParam(name = "userAddr") String userAddr,
            @ApiParam(name = "artistAddr", value = "artist address", required = true)
            @RequestParam(name = "artistAddr") String artistAddr,
            @ApiParam(name = "activityId", value = "activity id", required = true)
            @RequestParam(name = "activityId") Long activityId
    ) {
        if (StringUtils.isBlank(userAddr) || StringUtils.isBlank(artistAddr)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        List<StakeRecord> recordList = stakeRecordService.list(new QueryWrapper<StakeRecord>().eq("user_addr", userAddr)
                .eq("artist_addr", artistAddr).eq("activity_id", activityId));

        if (recordList.isEmpty()) {
            return AjaxResult.success("empty", new VerifiedArtistWithRewardVo());
        }

        BigDecimal amount = BigDecimal.ZERO;
        for (StakeRecord stakeRecord : recordList) {
            if (stakeRecord.getOperationType() == 1) {
                amount = amount.add(stakeRecord.getAmount());
            } else {
                amount = amount.subtract(stakeRecord.getAmount());
            }
        }

        VerifiedArtistVo verifiedArtistVo = artistCertifiedIdMapper.selectArtistInfo(artistAddr);

        VerifiedArtistWithRewardVo result = new VerifiedArtistWithRewardVo();
        result.setArtistAddr(artistAddr);
        result.setAvatarUrl(verifiedArtistVo.getAvatarUrl());
        result.setFollowers(verifiedArtistVo.getFollowers());
        result.setArtistname(verifiedArtistVo.getArtistname());
        result.setUserId(verifiedArtistVo.getUserId());
        result.setUserStakeOrBurnAmount(amount);

        return AjaxResult.success(result);
    }

    @GetMapping(path = "stakedArtist")
    public AjaxResult stakedArtist(
            @ApiParam(name = "addr", value = "addr", required = true)
            @RequestParam(name = "addr") String userAddr,
            @ApiParam(name = "activityId", value = "activity id", required = true)
            @RequestParam(name = "activityId") Long activityId
    ) {
        if (StringUtils.isBlank(userAddr)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        List<StakeRecord> recordList = stakeRecordService.list(
                new QueryWrapper<StakeRecord>().eq("user_addr", userAddr).eq("activity_id", activityId).orderByAsc("id"));

        Map<String, BigDecimal> map = new HashMap<>();
        if (recordList.isEmpty()) {
            return AjaxResult.success("empty", new ArrayList<VerifiedArtistWithRewardVo>());
        } else {
            recordList.forEach(record -> {
                if (map.containsKey(record.getArtistAddr())) {
                    BigDecimal amount = map.get(record.getArtistAddr());
                    if (record.getOperationType() == 1) {
                        amount = amount.add(record.getAmount());
                    } else {
                        amount = amount.subtract(record.getAmount());
                    }
                    map.put(record.getArtistAddr(), amount);
                } else {
                    map.put(record.getArtistAddr(), record.getAmount());
                }
            });

            List<VerifiedArtistWithRewardVo> vos = new ArrayList<>();
            map.forEach((k, v) -> {
                VerifiedArtistVo vo = artistCertifiedIdMapper.selectArtistInfo(k);
                if (vo != null && v.compareTo(BigDecimal.ZERO) > 0) {
                    VerifiedArtistWithRewardVo voWithReward = new VerifiedArtistWithRewardVo();
                    BeanUtils.copyProperties(vo, voWithReward);
                    voWithReward.setUserStakeOrBurnAmount(v);
                    vos.add(voWithReward);
                }
            });
            return AjaxResult.success(vos);
        }
    }

    @GetMapping(path = "burnedArtistInfo")
    private AjaxResult burnedArtistInfo(
            @ApiParam(name = "userAddr", value = "user address", required = true)
            @RequestParam(name = "userAddr") String userAddr,
            @ApiParam(name = "artistAddr", value = "artist address", required = true)
            @RequestParam(name = "artistAddr") String artistAddr,
            @ApiParam(name = "activityId", value = "activity id", required = true)
            @RequestParam(name = "activityId") Long activityId
    ) {
        if (StringUtils.isBlank(userAddr) || StringUtils.isBlank(artistAddr)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        List<BurnRecord> recordList = burnRecordService.list(new QueryWrapper<BurnRecord>().eq("user_addr", userAddr)
                .eq("artist_addr", artistAddr).eq("activity_id", activityId));

        if (recordList.isEmpty()) {
            return AjaxResult.success("empty", new VerifiedArtistWithRewardVo());
        }

        BigDecimal amount = BigDecimal.ZERO;
        for (BurnRecord burnRecord : recordList) {
            amount = amount.add(burnRecord.getAmount());
        }

        VerifiedArtistVo verifiedArtistVo = artistCertifiedIdMapper.selectArtistInfo(artistAddr);

        VerifiedArtistWithRewardVo result = new VerifiedArtistWithRewardVo();
        result.setArtistAddr(artistAddr);
        result.setAvatarUrl(verifiedArtistVo.getAvatarUrl());
        result.setFollowers(verifiedArtistVo.getFollowers());
        result.setArtistname(verifiedArtistVo.getArtistname());
        result.setUserId(verifiedArtistVo.getUserId());
        result.setUserStakeOrBurnAmount(amount);

        return AjaxResult.success(result);
    }


    @GetMapping(path = "burnedArtist")
    public AjaxResult burnedArtist(
            @ApiParam(name = "addr", value = "addr", required = true)
            @RequestParam(name = "addr") String userAddr,
            @ApiParam(name = "activityId", value = "activity id", required = true)
            @RequestParam(name = "activityId") Long activityId
    ) {
        if (StringUtils.isBlank(userAddr)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        List<BurnRecord> recordList = burnRecordService.list(
                new QueryWrapper<BurnRecord>().eq("user_addr", userAddr).eq("activity_id", activityId).orderByAsc("id"));

        Map<String, BigDecimal> map = new HashMap<>();
        if (recordList.isEmpty()) {
            return AjaxResult.success("empty", new ArrayList<VerifiedArtistWithRewardVo>());
        } else {
            for (BurnRecord burnRecord : recordList) {
                if (map.containsKey(burnRecord.getArtistAddr())) {
                    BigDecimal amount = map.get(burnRecord.getArtistAddr());
                    amount = amount.add(burnRecord.getAmount());
                    map.put(burnRecord.getArtistAddr(), amount);
                } else {
                    map.put(burnRecord.getArtistAddr(), burnRecord.getAmount());
                }
            }

            List<VerifiedArtistWithRewardVo> vos = new ArrayList<>();
            map.forEach((k, v) -> {
                VerifiedArtistVo vo = artistCertifiedIdMapper.selectArtistInfo(k);
                if (vo != null) {
                    VerifiedArtistWithRewardVo voWithReward = new VerifiedArtistWithRewardVo();
                    BeanUtils.copyProperties(vo, voWithReward);
                    voWithReward.setUserStakeOrBurnAmount(v);
                    vos.add(voWithReward);
                }
            });

            return AjaxResult.success(vos);
        }
    }
}
