package life.majiang.community.community.dto;

import lombok.Data;

//自动生成get，set方法快捷键 Fn+alt+Insert
@Data
public class AccessTokenDTO {
    private String client_id;
    private String client_secret;
    private String code;
    private String redirect_uri;
    private String state;
}
