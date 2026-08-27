package com.wyy.fm.dto;

import com.wyy.fm.common.NotBlankIfPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求 DTO
 * 
 * 作用：前端调用 PUT /api/user/info 时传入的请求体
 * 
 * 特点：
 * - 所有字段都是可选的（部分更新）
 * - 只更新传入的字段，没传的保持不变
 * 
 * 示例：
 * 只修改昵称：{"nickname": "李四"}
 * 同时修改多个：{"nickname": "李四", "gender": 2}
 *
 * 置空口径（与前端契约，同 UpdateDietRecordRequest）：
 * - null / 缺键 = 不改
 * - 字符串字段传空串 = 参数错误（昵称与头像是展示字段，不允许刷成空）
 *   所以性别可以改回 0（不填），但昵称只能用新值覆盖，不能清空
 */
@Data
public class UpdateUserRequest {

    /**
     * 昵称（可选）
     * - @NotBlankIfPresent：传了就不能是空串或纯空白（不传 = 不改，null 是合法的）
     * - 长度上限对齐 users.nickname（VARCHAR(64)）
     * - 存入前会 trim，避免带尾随空格的名字在列表里对不齐
     */
    @NotBlankIfPresent(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;

    /**
     * 头像 URL（可选）
     * - @NotBlankIfPresent：传了就必须是个非空值；想保持原头像请不传该字段
     * - 长度上限对齐 users.avatar_url（VARCHAR(512)）
     */
    @NotBlankIfPresent(message = "头像 URL 不能为空")
    @Size(max = 512, message = "头像 URL 最长 512 个字符")
    private String avatarUrl;

    /**
     * 性别（可选）
     * - 0：未知
     * - 1：男
     * - 2：女
     */
    @Min(value = 0, message = "性别只能为 0/1/2")
    @Max(value = 2, message = "性别只能为 0/1/2")
    private Integer gender;
}
