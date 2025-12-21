package com.ttt.safevault.utils;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.os.Build;
import android.util.Log;
import android.view.autofill.AutofillId;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动填充辅助工具类
 * 用于解析应用结构、识别可填充字段等
 */
public class AutofillHelper {

    private static final String TAG = "AutofillHelper";

    /**
     * 查找可填充的字段
     */
    public static FieldResult findAutofillFields(AssistStructure structure) {
        var result = new FieldResult();

        // 遍历所有视图节点
        var nodes = new ArrayList<ViewNode>();
        traverseStructure(structure, nodes);

        // 查找URL/域名
        result.webDomain = extractWebDomain(nodes);

        // 查找用户名字段
        result.usernameId = findUsernameField(nodes);

        // 查找密码字段
        result.passwordId = findPasswordField(nodes);

        // 查找URL字段
        result.urlId = findUrlField(nodes);

        return result;
    }

    /**
     * 遍历结构获取所有视图节点
     */
    private static void traverseStructure(AssistStructure structure, List<ViewNode> nodes) {
        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            traverseViewNode(windowNode.getRootViewNode(), nodes);
        }
    }

    /**
     * 递归遍历视图节点
     */
    private static void traverseViewNode(ViewNode node, List<ViewNode> nodes) {
        nodes.add(node);

        for (int i = 0; i < node.getChildCount(); i++) {
            traverseViewNode(node.getChildAt(i), nodes);
        }
    }

    /**
     * 查找用户名字段
     */
    private static AutofillId findUsernameField(List<ViewNode> nodes) {
        // 按优先级查找
        // 1. 明确标记为用户名的字段
        // 2. email字段
        // 3. 特定的hint或id
        // 4. 第一个文本输入字段（通常在密码字段前）

        for (ViewNode node : nodes) {
            if (isUsernameField(node)) {
                Log.d(TAG, "Found username field: " + node.getIdEntry());
                return node.getAutofillId();
            }
        }

        return null;
    }

    /**
     * 查找密码字段
     */
    private static AutofillId findPasswordField(List<ViewNode> nodes) {
        for (ViewNode node : nodes) {
            if (isPasswordField(node)) {
                Log.d(TAG, "Found password field: " + node.getIdEntry());
                return node.getAutofillId();
            }
        }

        return null;
    }

    /**
     * 查找URL字段
     */
    private static AutofillId findUrlField(List<ViewNode> nodes) {
        for (ViewNode node : nodes) {
            if (isUrlField(node)) {
                Log.d(TAG, "Found URL field: " + node.getIdEntry());
                return node.getAutofillId();
            }
        }

        return null;
    }

    /**
     * 提取Web域名
     */
    private static String extractWebDomain(List<ViewNode> nodes) {
        // 首先尝试从URL字段提取
        for (ViewNode node : nodes) {
            if (isUrlField(node) && node.getText() != null) {
                String url = node.getText().toString();
                return extractDomainFromUrl(url);
            }
        }

        // 尝试从应用的web域名提取
        for (ViewNode node : nodes) {
            if (node.getWebDomain() != null) {
                return node.getWebDomain();
            }
        }

        return null;
    }

    /**
     * 判断是否为用户名字段
     */
    private static boolean isUsernameField(ViewNode node) {
        // 检查autofill hints
        String[] autofillHints = node.getAutofillHints();
        if (autofillHints != null) {
            for (String hint : autofillHints) {
                if ("username".equals(hint) || "email".equals(hint) ||
                    "emailAddress".equals(hint) || "webEmailAddress".equals(hint)) {
                    return true;
                }
            }
        }

        // 检查hint
        CharSequence hint = node.getHint();
        if (hint != null) {
            String hintStr = hint.toString().toLowerCase();
            if (hintStr.contains("email") || hintStr.contains("用户名") ||
                hintStr.contains("账号") || hintStr.contains("用户")) {
                return true;
            }
        }

        // 检查id
        String id = node.getIdEntry();
        if (id != null) {
            String idStr = id.toLowerCase();
            if (idStr.contains("email") || idStr.contains("username") ||
                idStr.contains("user") || idStr.contains("login")) {
                return true;
            }
        }

        // 检查输入类型
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_CLASS_TEXT) != 0 &&
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为密码字段
     */
    private static boolean isPasswordField(ViewNode node) {
        // 检查autofill hints
        String[] autofillHints = node.getAutofillHints();
        if (autofillHints != null) {
            for (String hint : autofillHints) {
                if ("password".equals(hint) || "newPassword".equals(hint)) {
                    return true;
                }
            }
        }

        // 检查输入类型
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_CLASS_TEXT) != 0 &&
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0) {
            return true;
        }

        // 检查hint
        CharSequence hint = node.getHint();
        if (hint != null) {
            String hintStr = hint.toString().toLowerCase();
            if (hintStr.contains("password") || hintStr.contains("密码") ||
                hintStr.contains("口令")) {
                return true;
            }
        }

        // 检查id
        String id = node.getIdEntry();
        if (id != null) {
            String idStr = id.toLowerCase();
            if (idStr.contains("password") || idStr.contains("pwd")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为URL字段
     */
    private static boolean isUrlField(ViewNode node) {
        // 检查autofill hints
        String[] autofillHints = node.getAutofillHints();
        if (autofillHints != null) {
            for (String hint : autofillHints) {
                if ("webAddress".equals(hint) || "url".equals(hint)) {
                    return true;
                }
            }
        }

        // 检查输入类型
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_CLASS_TEXT) != 0 &&
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_URI) != 0) {
            return true;
        }

        // 检查hint
        CharSequence hint = node.getHint();
        if (hint != null) {
            String hintStr = hint.toString().toLowerCase();
            if (hintStr.contains("url") || hintStr.contains("网址") ||
                hintStr.contains("网站")) {
                return true;
            }
        }

        // 检查id
        String id = node.getIdEntry();
        if (id != null) {
            String idStr = id.toLowerCase();
            if (idStr.contains("url") || idStr.contains("website")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从URL提取域名
     */
    private static String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        // 移除协议
        String domain = url.replace("https://", "")
                         .replace("http://", "")
                         .replace("www.", "");

        // 移除路径
        int slashIndex = domain.indexOf('/');
        if (slashIndex > 0) {
            domain = domain.substring(0, slashIndex);
        }

        // 移除查询参数
        int queryIndex = domain.indexOf('?');
        if (queryIndex > 0) {
            domain = domain.substring(0, queryIndex);
        }

        return domain;
    }

    /**
     * 字段查找结果
     */
    public static class FieldResult {
        public AutofillId usernameId;
        public AutofillId passwordId;
        public AutofillId urlId;
        public String webDomain;

        public boolean hasFields() {
            return usernameId != null || passwordId != null;
        }

        public boolean hasRequiredFields() {
            return usernameId != null && passwordId != null;
        }
    }
}