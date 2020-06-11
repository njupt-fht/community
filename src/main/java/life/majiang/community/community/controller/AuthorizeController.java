package life.majiang.community.community.controller;

import life.majiang.community.community.dto.AccessTokenDTO;
import life.majiang.community.community.dto.GithubUser;
import life.majiang.community.community.mapper.UserMapper;
import life.majiang.community.community.model.User;
import life.majiang.community.community.provider.GithubProvider;
import life.majiang.community.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

//全屏切换快捷键 ctrl+shift+F12
//快速创建对象快捷键 ctrl+alt+v
//快速换行 shift+enter
@Controller
public class AuthorizeController {
    @Autowired
    private GithubProvider githubProvider;
    @Value("${github.client,id}")
    private String clientId;
    @Value("${github.client.secret}")
    private String clientSecret;
    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Autowired
    private UserService userService;

    //在利用github账号登录时，首先会发送"https://github.com/login/oauth/authorize?client_id=b5b69b390c736df838c4&redirect_uri=http://localhost:8080/callback&scope=user&state=1"
    //然后github调用回调函数（github会返回code和state），这时网页再次利用已知的参数（封装成accessTokenDTO对象了）向github发送请求（post）以获取access_token,
    //在获得access_token后，网页再次利用access_token（uri+access_token）向github发起请求（get），以获取user对象
    //API见https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/
    @GetMapping("/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           //  HttpServletRequest request,//这里暂时没用了
                           HttpServletResponse response) {
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientSecret);
        accessTokenDTO.setCode(code);
        accessTokenDTO.setRedirect_uri(redirectUri);
        accessTokenDTO.setState(state);
        String accessToken = githubProvider.getAccessToken(accessTokenDTO);//
        GithubUser githubUser = githubProvider.getUser(accessToken);
        //判断从GitHub获取的githubUser对象是否存在，若存在，则将其封装成User并存入数据库
        if (githubUser != null && githubUser.getId() != null) {
            User user = new User();
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            user.setName(githubUser.getName());
            user.setAccountId(String.valueOf(githubUser.getId()));

            user.setAvatarUrl(githubUser.getAvatarUrl());
            userService.createOrUpdate(user);
            response.addCookie(new Cookie("token", token));//将token写入cookie，用于自动登录
            //登录成功，写cookie和session
            //request.getSession().setAttribute("user",githubUser);//这里暂时没用了
            return "redirect:/";//重定向
        } else {
            //登录失败，重新登录
            return "redirect:/";
        }

    }
    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response){
        request.getSession().removeAttribute("user");
        Cookie cookie = new Cookie("token",null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";

    }
}
