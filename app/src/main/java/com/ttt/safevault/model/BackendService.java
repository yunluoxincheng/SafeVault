package com.ttt.safevault.model;

import java.util.List;

/**
 * 后端服务接口
 * 定义了前端与后端交互的所有方法
 * 实际实现由后端模块提供，前端只调用接口
 */
public interface BackendService {

    /**
     * 使用主密码解锁应用
     * @param masterPassword 用户输入的主密码
     * @return true表示解锁成功，false表示密码错误
     */
    boolean unlock(String masterPassword);

    /**
     * 锁定应用
     * 清除内存中的敏感数据
     */
    void lock();

    /**
     * 解密并获取单个密码条目
     * @param id 条目ID
     * @return 解密后的PasswordItem对象
     */
    PasswordItem decryptItem(int id);

    /**
     * 搜索密码条目
     * @param query 搜索关键词（标题、用户名、URL等）
     * @return 匹配的条目列表
     */
    List<PasswordItem> search(String query);

    /**
     * 保存或更新密码条目
     * @param item 要保存的条目
     * @return 保存成功后的条目ID（新增时返回新ID，更新时返回原ID）
     */
    int saveItem(PasswordItem item);

    /**
     * 删除密码条目
     * @param id 要删除的条目ID
     * @return true表示删除成功，false表示失败
     */
    boolean deleteItem(int id);

    /**
     * 生成随机密码
     * @param length 密码长度
     * @param symbols 是否包含特殊符号
     * @return 生成的密码字符串
     */
    String generatePassword(int length, boolean symbols);

    /**
     * 生成随机密码（带更多选项）
     * @param length 密码长度
     * @param useUppercase 是否包含大写字母
     * @param useLowercase 是否包含小写字母
     * @param useNumbers 是否包含数字
     * @param useSymbols 是否包含特殊符号
     * @return 生成的密码字符串
     */
    String generatePassword(int length, boolean useUppercase, boolean useLowercase,
                           boolean useNumbers, boolean useSymbols);

    /**
     * 根据域名获取可用的登录凭据（用于自动填充）
     * @param domain 域名或URL
     * @return 该域名下保存的所有账号信息
     */
    List<PasswordItem> getCredentialsForDomain(String domain);

    /**
     * 获取所有密码条目
     * @return 所有条目列表
     */
    List<PasswordItem> getAllItems();

    /**
     * 检查应用是否已初始化（是否已设置主密码）
     * @return true表示已初始化，false表示需要设置主密码
     */
    boolean isInitialized();

    /**
     * 初始化应用，设置主密码
     * @param masterPassword 要设置的主密码
     * @return true表示设置成功
     */
    boolean initialize(String masterPassword);

    /**
     * 更改主密码
     * @param oldPassword 旧的主密码
     * @param newPassword 新的主密码
     * @return true表示更改成功
     */
    boolean changeMasterPassword(String oldPassword, String newPassword);

    /**
     * 导出数据（加密导出）
     * @param exportPath 导出文件路径
     * @return true表示导出成功
     */
    boolean exportData(String exportPath);

    /**
     * 导入数据
     * @param importPath 导入文件路径
     * @return true表示导入成功
     */
    boolean importData(String importPath);

    /**
     * 获取应用统计信息
     * @return 包含条目总数等信息
     */
    AppStats getStats();

    /**
     * 记录进入后台的时间
     */
    void recordBackgroundTime();

    /**
     * 获取后台时间戳
     * @return 进入后台的时间戳
     */
    long getBackgroundTime();

    /**
     * 获取自动锁定超时时间
     * @return 超时时间（分钟）
     */
    int getAutoLockTimeout();

    /**
     * 应用统计信息内部类
     */
    class AppStats {
        public int totalItems;
        public int weakPasswords;
        public int duplicatePasswords;
        public int lastBackupDays;

        public AppStats(int totalItems, int weakPasswords, int duplicatePasswords, int lastBackupDays) {
            this.totalItems = totalItems;
            this.weakPasswords = weakPasswords;
            this.duplicatePasswords = duplicatePasswords;
            this.lastBackupDays = lastBackupDays;
        }
    }
}