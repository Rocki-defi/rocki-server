package com.rocki.app.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.app.context.RequestContext;
import com.rocki.app.controller.bo.user.AppSocialLinkBo;
import com.rocki.app.controller.bo.user.AppUpdatePwdBO;
import com.rocki.app.controller.bo.user.AppUserBO;
import com.rocki.app.controller.bo.user.UserRegisterBO;
import com.rocki.app.controller.vo.user.InvitedUserVo;
import com.rocki.app.controller.vo.user.UserAssetsVO;
import com.rocki.app.controller.vo.user.UserInfoVO;
import com.rocki.app.controller.vo.user.UserLoginVO;
import com.rocki.app.controller.vo.verification.BadgeFlagVo;
import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.app.interceptor.FrontTokenServiceImpl;
import com.rocki.app.service.AppUserService;
import com.rocki.common.annotation.Login;
import com.rocki.common.constant.Constants;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.redis.RedisCache;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.RegexUtils;
import com.rocki.common.utils.ServletUtils;
import com.rocki.common.utils.file.MimeTypeUtils;
import com.rocki.common.utils.file.awss3.AWSFileUpload;
import com.rocki.common.utils.ip.IpUtils;
import com.rocki.common.utils.sign.Md5Utils;
import com.rocki.common.utils.uuid.IdUtils;
import com.rocki.message.aws.SendEmail;
import com.rocki.music.service.TrackService;
import com.rocki.user.domain.ArtistCertifiedId;
import com.rocki.user.domain.User;
import com.rocki.user.domain.UserKycInfo;
import com.rocki.user.domain.UserSocialLink;
import com.rocki.user.service.ArtistCertifiedIdService;
import com.rocki.user.service.UserAssetsService;
import com.rocki.user.service.UserKycInfoService;
import com.rocki.user.service.UserService;
import com.rocki.user.service.UserSocialLinkService;
import com.rocki.user.utils.UserUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author David Wilde
 * @Package com.rocki.app.controller.user.account
 * @date 2/27/21 11:05 上午
 */
@RestController
@RequestMapping("/api/user")
@Validated
@ResponseBody
public class AppUserAccountController {
    Logger logger = LoggerFactory.getLogger(AppUserAccountController.class);

    @Value("${token.expire-front}")
    private int expireFront;

    @Autowired
    private UserService userService;
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private FrontTokenServiceImpl frontTokenService;
    @Autowired
    private UserSocialLinkService userSocialLinkService;
    @Autowired
    private ArtistCertifiedIdService artistCertifiedIdService;
    @Autowired
    private UserKycInfoService userKycInfoService;

    @PostMapping("registerByEmail")
    public AjaxResult registerUserByEmail(@Valid @RequestBody UserRegisterBO form) {

        if (StringUtils.isBlank(form.getEmail()) || !RegexUtils.isEmail(form.getEmail())) {
            return AjaxResult.error(MessageUtils.message("user.email.not.valid"));
        }
        if (StringUtils.isBlank(form.getUsername())) {
            return AjaxResult.error(MessageUtils.message("not.null", "username"));
        }
        if (form.getArtist() == null) {
            form.setArtist(false);
        }

        Long parentId = 0L;
        if (StringUtils.isNotBlank(form.getInviteCode())) {
            User parent = userService.getOne(new QueryWrapper<User>()
                    .eq("invitation_code", form.getInviteCode()));
            if (parent == null) {
                return AjaxResult.error(MessageUtils.message("parent.user.not.exists"));
            }
            parentId = parent.getUserId();
        }

        User userEntity = userService.getOne(new QueryWrapper<User>().eq("email", form.getEmail()));
        if (userEntity != null) {
            return AjaxResult.error(MessageUtils.message("user.registered"));
        }

        appUserService.saveUserFront(form, parentId);
        return AjaxResult.success(MessageUtils.message("send.register.email.success"));

    }

    @Login
    @GetMapping("setFullName")
    public AjaxResult setFullName(
            @ApiParam(name = "name", value = "", required = true)
            @RequestParam("name") String name
    ) {
        FrontLoginUser frontLoginUser = RequestContext.get();
        User user = frontLoginUser.getUser();
        user.setFullName(name);
        userService.updateById(user);
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        redisCache.setCacheObject(Constants.LOGIN_TOKEN_KEY + frontLoginUser.getToken(),
                frontLoginUser, expireFront, TimeUnit.SECONDS);
        return AjaxResult.success(vo);
    }

    @PostMapping("sendMail")
    public AjaxResult sendMailAgain(
            @ApiParam(name = "email", value = "", example = "xxx@11.com", required = true)
            @RequestParam("email") String email,
            @RequestParam("codeType") Integer codeType) {
        if (StringUtils.isBlank(email) || !RegexUtils.isEmail(email)) {
            return AjaxResult.error(MessageUtils.message("user.email.not.valid"));
        }

        User userEntity = userService.getOne(new QueryWrapper<User>().eq("email", email));
        String result = "";
        if (userEntity == null) {
            return AjaxResult.error(MessageUtils.message("user.not.exists"));
        }
        if (UserUtils.USER_STATUS_DELETE.equals(userEntity.getStatus())) {
            return AjaxResult.error(MessageUtils.message("user.deleted"));
        }
        if (codeType == 1) {
            String codeRedisInstance = redisCache.getCacheObject("register:" + email);
            if (codeRedisInstance != null) {
                return AjaxResult.error(MessageUtils.message("sent.email.too.frequently"));
            }
            if (userEntity.getStatus() != -1) {
                return AjaxResult.error(MessageUtils.message("user.registered"));
            }
            String code = IdUtils.returnRandomNumberSix();
            if (!SendEmail.sendEmail(userEntity.getEmail(), userEntity.getUsername(), code, "1")) {
                return AjaxResult.error(MessageUtils.message("send.email.fail"));
            }
            redisCache.setCacheObject("register:" + userEntity.getEmail(),
                    code, 15 * 60, TimeUnit.SECONDS);
        } else if (codeType == 2) {
            String codeRedisInstance = redisCache.getCacheObject("modifyPwd:" + email);
            if (codeRedisInstance != null) {
                return AjaxResult.error(MessageUtils.message("sent.email.too.frequently"));
            }
            if (UserUtils.USER_STATUS_INACTIVATED.equals(userEntity.getStatus())) {
                return AjaxResult.error(MessageUtils.message("user.not.register"));
            }

            if (UserUtils.USER_STATUS_FROZEN.equals(userEntity.getStatus())
                    || UserUtils.USER_STATUS_BLOCK.equals(userEntity.getStatus())) {
                return AjaxResult.error(MessageUtils.message("user.blocked"));
            }
            String code = IdUtils.returnRandomNumberSix();
            if (!SendEmail.sendEmail(userEntity.getEmail(), userEntity.getUsername(), code, "2")) {
                return AjaxResult.error(MessageUtils.message("send.email.fail"));
            }
            logger.info("Email sent to " + email + " at " + new Date().toString() + "code type and verification code is:" + code);
            redisCache.setCacheObject("modifyPwd:" + userEntity.getEmail(),
                    code, 15 * 60, TimeUnit.SECONDS);
        } else {
            return AjaxResult.error(MessageUtils.message("user.jcaptcha.type.error"));
        }

        return AjaxResult.success(MessageUtils.message("send.email.success"));
    }

    @PostMapping("confirmRegister")
    public AjaxResult confirmRegister(
            @ApiParam(name = "email", value = "", example = "xxx@11.com", required = true)
            @RequestParam("email") String email,
            @ApiParam(name = "code", value = "", example = "123456", required = true)
            @RequestParam("code") String code) {
        if (StringUtils.isBlank(email) || !RegexUtils.isEmail(email)) {
            return AjaxResult.error(MessageUtils.message("user.email.not.valid"));
        }
        if (StringUtils.isBlank(code)) {
            return AjaxResult.error(MessageUtils.message("not.null", "code"));
        }
        String codeRedisInstance = redisCache.getCacheObject("register:" + email);
        if (codeRedisInstance == null) {
            logger.info("email: {}, {} at {}", email, MessageUtils.message("user.jcaptcha.expire"), new Date());
            return AjaxResult.error(MessageUtils.message("user.jcaptcha.expire"));
        }
        if (!codeRedisInstance.equals(code)) {
            return AjaxResult.error(MessageUtils.message("user.jcaptcha.error"));
        }
        User userEntity = userService.getOne(new QueryWrapper<User>().eq("email", email));
        if (userEntity == null) {
            return AjaxResult.error(MessageUtils.message("user.not.exists"));
        }
        if (userEntity.getStatus() != -1) {
            return AjaxResult.error(MessageUtils.message("user.registered"));
        }
        User user = appUserService.registerUserService(userEntity);
        String token = frontTokenService.createToken(new FrontLoginUser(user));
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);
        userLoginVO.setToken(token);

        redisCache.deleteObject("register:" + email);
        return AjaxResult.success(userLoginVO);
    }

    @Login
    @PostMapping("setArtistBio")
    public AjaxResult setArtistBio(
            @ApiParam(name = "bio", value = "bio", required = false)
            @RequestParam(value = "bio") String bio

    ) {
        FrontLoginUser frontLoginUser = RequestContext.get();

        User user = userService.getById(frontLoginUser.getUser().getUserId());

        if (user == null) {
            return AjaxResult.error(MessageUtils.message("user.not.complete.first.step.or.not.login"));
        }
        if (!UserUtils.USER_ROLE_ARTIST.equals(user.getArtistsType())) {
            return AjaxResult.error(MessageUtils.message("not.an.artist"));
        }

        if (StringUtils.isBlank(bio)) {
            return AjaxResult.error(MessageUtils.message("illegal.param"));
        }

        if (StringUtils.isNotBlank(bio)) {
            user.setBio(bio);
            user.setDescription(bio);

            userService.updateById(user);
            frontLoginUser.setUser(user);
            redisCache.setCacheObject(Constants.LOGIN_TOKEN_KEY + frontLoginUser.getToken(),
                    frontLoginUser, expireFront, TimeUnit.SECONDS);

            return AjaxResult.success(MessageUtils.message("set.bio.success"), user);
        }
        return AjaxResult.error();
    }

    @Login
    @PostMapping("addSocialLink")
    public AjaxResult addSocialLinks(
            @Validated
            @RequestBody AppSocialLinkBo socialLinkBo
    ) {
        User user = RequestContext.getUser();
        if (StringUtils.isBlank(socialLinkBo.getSocialLink())) {
            return AjaxResult.error(MessageUtils.message("not.null", "social link"));
        }
        if (StringUtils.isBlank(socialLinkBo.getSocialPlatform())) {
            socialLinkBo.setSocialPlatform("Other");
        }
        UserSocialLink link = appUserService.addSocialLinks(user, socialLinkBo);
        return AjaxResult.success(link);
    }

    @Login
    @PostMapping("modifySocialLink")
    public AjaxResult modifySocialLink(
            @ApiParam(name = "id", value = "id", required = true)
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "newLink") String newSocialLink
    ) {
        if (StringUtils.isBlank(newSocialLink)) {
            return AjaxResult.error(MessageUtils.message("not.null", "social link"));
        }

        UserSocialLink link = appUserService.modifySocialLink(id, newSocialLink);
        return AjaxResult.success(link);
    }

    @Login
    @PostMapping("deleteSocialLink")
    public AjaxResult deleteSocialLink(
            @RequestParam(value = "userLinkId") Long id) {
        User user = RequestContext.getUser();
        UserSocialLink link = userSocialLinkService.getById(id);
        if (!link.getUserId().equals(user.getUserId())) {
            return AjaxResult.error();
        }
        userSocialLinkService.removeById(id);
        return AjaxResult.success();
    }

    @Login
    @GetMapping("mySocialLinks")
    public AjaxResult showMySocialLinks() {
        Long userId = RequestContext.getUser().getUserId();
        List<UserSocialLink> links = userSocialLinkService.list(new QueryWrapper<UserSocialLink>()
                .eq("user_id", userId));
        return AjaxResult.success(links);
    }


    @Login
    @GetMapping("getFullName")
    public AjaxResult getFullName() {
        FrontLoginUser loginUser = RequestContext.get();
        User user = userService.getById(loginUser.getUser().getUserId());
        return AjaxResult.success(user.getFullName());
    }


    @Login
    @GetMapping("findUserInfo")
    @ApiOperation("Find user information")
    public AjaxResult findUserInfo() {
        FrontLoginUser frontLoginUser = RequestContext.get();
        User user = frontLoginUser.getUser();
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        /*userInfoVO.setBannerUrl(StringUtils.isBlank(user.getHeadImageUrl())
                ? user.getAvatarUrl() : null);*/
        ArtistCertifiedId certifiedId = artistCertifiedIdService.getOne(
                new QueryWrapper<ArtistCertifiedId>().eq("user_id", user.getUserId()));
        String did = null;
        if (certifiedId != null) {
            UserKycInfo kycInfo = userKycInfoService.getOne(new QueryWrapper<UserKycInfo>()
                    .eq("user_id", user.getUserId()));
            did = kycInfo.getOwnerDid();
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("info", userInfoVO);
        data.put("badgeFlag", certifiedId == null ? new BadgeFlagVo(UserUtils.CERTIFIED_NONE, null, null)
                : new BadgeFlagVo(certifiedId.getStatus(), certifiedId.getUserAddress(), did));
        return AjaxResult.success(data);
    }

    @Login
    @GetMapping("getInvitationCode")
    public AjaxResult getInvitationCode() {
        FrontLoginUser frontLoginUser = RequestContext.get();

        User user = userService.getById(frontLoginUser.getUser().getUserId());
        if (StringUtils.isBlank(user.getInvitationCode())) {
            User user1;
            String randomNumber = null;
            do {
                randomNumber = IdUtils.fastSimpleUUID().substring(0, 4) + IdUtils.returnRandomNumberSix();
                user1 = userService.getOne(new QueryWrapper<User>().eq("invitation_code", randomNumber));
            } while (user1 != null);
            user.setInvitationCode(randomNumber);
            userService.updateById(user);
            frontLoginUser.setUser(user);
            redisCache.setCacheObject(Constants.LOGIN_TOKEN_KEY + frontLoginUser.getToken(),
                    frontLoginUser, expireFront, TimeUnit.SECONDS);
            return AjaxResult.success(randomNumber);
        }
        if (StringUtils.isNotBlank(user.getInvitationCode())) {
            return AjaxResult.success(frontLoginUser.getUser().getInvitationCode());
        }
        return AjaxResult.error();
    }

    @PostMapping("modifyPwdByEmailCode")
    @ApiOperation("")
    public AjaxResult modifyPwd(@RequestBody AppUpdatePwdBO appUpdatePwdBO) {
        logger.info("email: {}, want to modify his password at {} with {}", appUpdatePwdBO.getEmail(), new Date(),
                appUpdatePwdBO.getCode());
        if (StringUtils.isBlank(appUpdatePwdBO.getEmail()) && !RegexUtils.isEmail(appUpdatePwdBO.getEmail())) {
            return AjaxResult.error(MessageUtils.message("not.null", "email"));
        }

        if (StringUtils.isBlank(appUpdatePwdBO.getNewPassword())) {
            return AjaxResult.error(MessageUtils.message("not.null", "new password"));
        }
        User user = userService.getOne(new QueryWrapper<User>().eq("email", appUpdatePwdBO.getEmail()));
        if (user == null) {
            return AjaxResult.error(MessageUtils.message("user.not.exists"));
        }

        String emailCode = redisCache.getCacheObject("modifyPwd:" + appUpdatePwdBO.getEmail());
        if (emailCode == null) {
            return AjaxResult.error(MessageUtils.message("user.jcaptcha.expire"));
        } else if (!emailCode.equals(appUpdatePwdBO.getCode())) {
            return AjaxResult.error(MessageUtils.message("user.jcaptcha.error"));
        } else {
            user.setPassword(Md5Utils.hash(appUpdatePwdBO.getNewPassword()));
            user.setStatus(UserUtils.USER_STATUS_NORMAL);
            userService.updateById(user);
            redisCache.deleteObject("modifyPwd:" + appUpdatePwdBO.getEmail());
            return AjaxResult.success();
        }
    }

    @Login
    @PostMapping("uploadAvatar")
    public AjaxResult uploadAvatar(@RequestParam(value = "file") MultipartFile file) {
        FrontLoginUser frontLoginUser = RequestContext.get();
        User user = frontLoginUser.getUser();
        if (file.isEmpty()) {
            return AjaxResult.error(MessageUtils.message("upload.file.is.empty"));
        }
        String basePath = "avatar/";
        try {

            String url = AWSFileUpload.upload(basePath, file, MimeTypeUtils.IMAGE_EXTENSION);
            user.setAvatarUrl(url);
            user.setUpdateTime(new Date());
            userService.updateById(user);

            frontLoginUser.setUser(user);
            redisCache.setCacheObject(Constants.LOGIN_TOKEN_KEY + frontLoginUser.getToken(),
                    frontLoginUser, expireFront, TimeUnit.SECONDS);
            return AjaxResult.success("success", url);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(MessageUtils.message("upload.file.error"));
        }
    }

    @SuppressWarnings("AlibabaRemoveCommentedCode")
    @Login
    @GetMapping("getUserAssets")
    public AjaxResult getUserAssets(
            @ApiParam(name = "currencyName", value = "", required = false)
            @RequestParam(value = "currencyName", defaultValue = "ROCKI") String currencyName
    ) {
        Long userId = RequestContext.getUser().getUserId();

        if (StringUtils.isBlank(currencyName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "title"));
        }

        User user = userService.getById(userId);
        UserAssetsVO userAssetsVo = new UserAssetsVO();
        userAssetsVo.setBalance(user.getNotesbalance());
        userAssetsVo.setUserId(userId);
        userAssetsVo.setAddress(user.getAddress());
        return AjaxResult.success(userAssetsVo);
    }

    @Login
    @DeleteMapping("fullyDeleteMyAccount")
    @ApiOperation("")
    public AjaxResult fullyDeleteMyAccount() {
        FrontLoginUser frontLoginUser = RequestContext.get();
        logger.info("User {} want to delete his account, his user id is {} and his email is {}",
                frontLoginUser.getUser().getUsername(), frontLoginUser.getUser().getUserId(),
                frontLoginUser.getUser().getEmail());
        userService.fullyDeleteAccount(frontLoginUser.getUser());
        frontTokenService.delLoginUser(frontLoginUser.getToken());
        return AjaxResult.success();
    }

     */
    @Login
    @PostMapping("deleteMyAccount")
    @ApiOperation("")
    public AjaxResult deleteMyAccount() {
        frontTokenService.delLoginUser(RequestContext.get().getToken());
        FrontLoginUser frontLoginUser = RequestContext.get();
        appUserService.deleteAccount(frontLoginUser);

        return AjaxResult.success();
    }

    @PostMapping("login")
    @ApiOperation("")
    public AjaxResult userLogin(@Valid @RequestBody AppUserBO form) {

        if (StringUtils.isBlank(form.getEmail())) {
            return AjaxResult.error(MessageUtils.message("not.null", "email"));
        }

        if (StringUtils.isBlank(form.getPassword())) {
            return AjaxResult.error(MessageUtils.message("not.null", "password"));
        }

        //
        User user = appUserService.checkUserInfo(form.getEmail(), form.getPassword());
        if (user != null) {
            if (UserUtils.USER_STATUS_DELETE.equals(user.getStatus())) {
                return AjaxResult.error(MessageUtils.message("user.deleted"));
            }
            if (!UserUtils.USER_STATUS_NORMAL.equals(user.getStatus())
                    && !UserUtils.USER_STATUS_INACTIVATED.equals(user.getStatus())) {
                return AjaxResult.error(MessageUtils.message("user.blocked"));
            }
            if (!user.getPassword().equals(Md5Utils.hash(form.getPassword()))) {
                return AjaxResult.error(MessageUtils.message("user.password.not.match"));
            }
            user.setLastLoginTime(new Date());
            user.setLastLoginIp(IpUtils.getIpAddr(ServletUtils.getRequest()));
            userService.updateById(user);
            String token = frontTokenService.createToken(new FrontLoginUser(user));
            UserLoginVO userLoginVO = new UserLoginVO();
            BeanUtils.copyProperties(user, userLoginVO);
            userLoginVO.setToken(token);
            return AjaxResult.success(MessageUtils.message("user.login.success"), userLoginVO);
        } else {
            return AjaxResult.error(MessageUtils.message("user.not.exists"));
        }

    }

    @GetMapping("logout")
    public AjaxResult logout() {
        FrontLoginUser loginUser = frontTokenService.getLoginUser(ServletUtils.getRequest());
        if (loginUser != null && loginUser.getUser() != null) {
            frontTokenService.delLoginUser(loginUser.getToken());
        }
        return AjaxResult.success();

    }

    @Login
    @GetMapping("listInvitedUsers")
    public AjaxResult listInvitedUsers() {
        User user = RequestContext.getUser();
        List<User> invitedUsers = userService.list(new QueryWrapper<User>().eq("parent_id", user.getUserId()));
        List<InvitedUserVo> voList = invitedUsers.stream().map(invitedUser -> {
            InvitedUserVo vo = new InvitedUserVo();
            BeanUtils.copyProperties(invitedUser, vo);
            vo.setStreamRewards("N/A");
            return vo;
        }).collect(Collectors.toList());

        return AjaxResult.success(voList);
    }

    @Login
    @PostMapping("banner")
    public AjaxResult setBanner(
            @ApiParam(name = "bannerImage", value = "", required = true)
            @RequestParam(value = "bannerImage") MultipartFile bannerImage
    ) {
        FrontLoginUser frontLoginUser = RequestContext.get();
        User user = RequestContext.getUser();
        if (bannerImage.isEmpty()) {
            return AjaxResult.error(MessageUtils.message("upload.file.is.empty"));
        }
        String url = appUserService.uploadBanner(user, bannerImage);
        frontLoginUser.setUser(user);
        redisCache.setCacheObject(Constants.LOGIN_TOKEN_KEY + frontLoginUser.getToken(),
                frontLoginUser, expireFront, TimeUnit.SECONDS);

        return AjaxResult.success(url);
    }
}
