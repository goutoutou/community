package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.code.kaptcha.Producer;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.config.KaptchaConfig;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @ author : Real
 * @ date : 2021/11/20 22:11
 * @ description :
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    /**
     * 获得修改用户信息界面
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    /**
     * 上传头像
     * @param headerImage
     * @param model
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        // 异常情况处理，Model利用模板携带数据
        if (headerImage == null) {
            model.addAttribute("error", "您还没有添加图片！");
            return "/site/setting";
        }

        // 获取文件名
        String filename = headerImage.getOriginalFilename();
        String filetype = "";
        // 判断文件类型
        if (!StringUtils.isBlank(filename)) {
            // 生成的文件后缀名格式一般为 .jpg / .png / .jpeg 格式
            filetype = filename.substring(filename.lastIndexOf("."));
        }

        // 文件格式正确性判断
        if (StringUtils.isBlank(filetype)) {
            /*if ("jpg".equals(filetype) || "jpeg".equals(filetype) || "png".equals(filetype)) {

            } else {

            }*/
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        // 生成随机文件名
        filename = CommunityUtil.generatorUUID() + filetype;
        // 确定文件存放的路径
        File file = new File(uploadPath + "/" + filename);
        // 将文件存储到目标文件夹中
        try {
            headerImage.transferTo(file);
        } catch (IOException e) {
            logger.error("头像图片存储失败：" + e.getMessage());
            throw new RuntimeException("上传头像失败！服务器异常发生异常", e);
        }

        // 存储成功之后要更新用户的头像路径
        // 更新之后的路径应该为 web 访问路径
        // http://localhost:8080/community/user/header/xxx.jpg
        User user = hostHolder.getUser();
        user = userService.findUserById(user.getId());
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        // 输出测试头像路径
        // System.out.println("更新之后的头像路径： " + headerUrl);
        int updateHeader = userService.updateHeader(user.getId(), headerUrl);
        /*if (updateHeader == 1) {
            model.addAttribute("message", "头像修改成功！");
        }*/
        // 重定向会重新执行 Controller 中的 RequestMapping 映射请求
        return "redirect:/index";
    }

    /**
     * 头像回显
     * @param fileName
     * @param response
     */
    @LoginRequired
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeaderUrl(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 获得 .jpg / .png / .jpeg 类型的字符串，文件后缀名
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
        // 响应文件类型
        response.setContentType("image/" + fileType);

        // 将图片使用输出流写入 response 对象
        try (
                OutputStream outputStream = response.getOutputStream();
                FileInputStream fileInputStream = new FileInputStream(fileName);
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/update/password", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword, Model model) {
        // 判断两次输入的新密码是否相等，是否合法
        if (!StringUtils.isBlank(newPassword) && !StringUtils.isBlank(confirmPassword)) {
            if (!newPassword.equals(confirmPassword)) {
                // 两次密码不相等
                model.addAttribute("confirmPasswordError", "确认密码不一致！请重新输入！");
                return "/site/setting";
            }
        }/* else {
            // 前端界面做了判断，不需要这里的处理
            if (StringUtils.isBlank(newPassword)) {
                model.addAttribute("newPasswordError", "新密码为空！请重新输入！");
            } else {
                model.addAttribute("confirmPasswordError", "确认密码为空！请重新输入！");
            }
            return "/site/setting";
        }*/

        // 获得原始密码，判断初始密码是否正确
        User user = hostHolder.getUser();
        if (oldPassword.length() == 0) {
            // 原始密码为空，应该添加提示信息
            model.addAttribute("initialError", "原始密码为空！请输入原始密码！");
            return "/site/setting";
        }
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!StringUtils.isBlank(oldPassword)) {
            if (!oldPassword.equals(user.getPassword())) {
                model.addAttribute("initialError", "原始密码错误！请重新输入！");
                return "/site/setting";
            }
        }

        // 更新密码
        userService.updatePassword(user.getId(), CommunityUtil.md5(newPassword + user.getSalt()));
        // 修改成功重定向至登录界面，并且设置原有的 LoginTicket 失效
        return "redirect:/logout";
    }

    /**
     * 显示用户主页
     *
     * @param userId 用户 ID
     * @param model  model 对象
     * @return 用户主页界面
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        // 用户相关信息显示
        model.addAttribute("user", user);
        // 用户收到的点赞数量
        int userLikeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("userLikeCount", userLikeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    @RequestMapping(path = "/post/{userId}", method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        // 用户相关信息显示
        model.addAttribute("user", user);

        // 求出个人用户发布的帖子个数
        int rows = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postCount", rows);

        // 设置分页参数
        page.setPath(contextPath + "/user/post/" + userId);
        page.setRows(rows);
        page.setLimit(10);

        // 需要显示的内容包括发布的帖子数量、帖子标题、内容、发布时间、点赞数量
        List<Map<String, Object>> postVoList = new ArrayList<>();
        List<DiscussPost> posts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        if (posts != null) {
            for (DiscussPost post : posts) {
                Map<String, Object> map = new HashMap<>();
                // 获取帖子的点赞数量
                long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, post.getId());
                map.put("id", post.getId());
                map.put("title", post.getTitle());
                map.put("content", post.getContent());
                map.put("createTime", post.getCreateTime());
                map.put("likeCount", likeCount);
                postVoList.add(map);
            }
            model.addAttribute("posts", postVoList);
        }
        return "/site/my-post";
    }

    @RequestMapping(path = "/reply/{userId}", method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        // 用户相关信息显示
        model.addAttribute("user", user);

        // 查询用户发表的评论数量，需要显示
        int commentCount = commentService.findCommentCountByUserId(userId);
        model.addAttribute("commentCount", commentCount);

        // 设置分页参数
        page.setPath(contextPath + "/user/reply/" + userId);
        page.setRows(commentCount);
        page.setLimit(10);

        List<Map<String, Object>> replyVOList = new ArrayList<>();
        List<Comment> comments = commentService.findCommentsByUserId(userId, page.getOffset(), page.getLimit());
        if (comments != null) {
            for (Comment comment : comments) {
                Map<String, Object> map = new HashMap<>();
                // 帖子 ID
                map.put("postId", comment.getEntityId());
                // 帖子标题
                map.put("title", discussPostService.findDiscussPostById(comment.getEntityId()).getTitle());
                // 评论内容
                map.put("content", comment.getContent());
                // 评论时间
                map.put("createTime", comment.getCreateTime());
                replyVOList.add(map);
            }
            model.addAttribute("reply", replyVOList);
        }
        return "/site/my-reply";
    }

}
