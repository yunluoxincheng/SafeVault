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
        
        Log.d(TAG, "Total nodes found: " + nodes.size());

        // 查找URL/域名
        result.webDomain = extractWebDomain(nodes);

        // 查找密码字段（优先）
        result.passwordId = findPasswordField(nodes);
        
        // 查找用户名字段
        result.usernameId = findUsernameField(nodes);
        
        // 如果没找到用户名但找到了密码，尝试查找密码字段前的文本输入框
        if (result.usernameId == null && result.passwordId != null) {
            result.usernameId = findTextFieldBeforePassword(nodes, result.passwordId);
        }
        
        // 如果仍然没有找到任何字段，尝试查找任何可编辑的文本输入框
        if (result.usernameId == null && result.passwordId == null) {
            result.usernameId = findAnyEditableTextField(nodes);
        }

        // 查找URL字段
        result.urlId = findUrlField(nodes);
        
        Log.d(TAG, "Fields result - username: " + (result.usernameId != null) + 
                   ", password: " + (result.passwordId != null));

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
        // 检查是否可编辑
        if (!isEditableTextField(node)) {
            return false;
        }
        
        // 检查autofill hints
        String[] autofillHints = node.getAutofillHints();
        if (autofillHints != null) {
            for (String hint : autofillHints) {
                String h = hint.toLowerCase();
                if (h.contains("username") || h.contains("email") || 
                    h.contains("phone") || h.contains("account")) {
                    Log.d(TAG, "Username field found by autofillHint: " + hint);
                    return true;
                }
            }
        }

        // 检查hint
        CharSequence hint = node.getHint();
        if (hint != null) {
            String hintStr = hint.toString().toLowerCase();
            if (hintStr.contains("email") || hintStr.contains("用户名") ||
                hintStr.contains("账号") || hintStr.contains("用户") ||
                hintStr.contains("手机") || hintStr.contains("phone") ||
                hintStr.contains("username") || hintStr.contains("account") ||
                hintStr.contains("邮箱") || hintStr.contains("登录")) {
                Log.d(TAG, "Username field found by hint: " + hintStr);
                return true;
            }
        }

        // 检查id
        String id = node.getIdEntry();
        if (id != null) {
            String idStr = id.toLowerCase();
            if (idStr.contains("email") || idStr.contains("username") ||
                idStr.contains("user") || idStr.contains("login") ||
                idStr.contains("account") || idStr.contains("phone")) {
                Log.d(TAG, "Username field found by id: " + idStr);
                return true;
            }
        }

        // 检查输入类型
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
            (inputType & android.text.InputType.TYPE_CLASS_PHONE) != 0) {
            Log.d(TAG, "Username field found by inputType: " + inputType);
            return true;
        }

        return false;
    }
    
    /**
     * 检查是否是可编辑的文本输入框
     */
    private static boolean isEditableTextField(ViewNode node) {
        // 检查是否有 autofillId
        if (node.getAutofillId() == null) {
            return false;
        }
        
        // 检查是否可编辑
        int inputType = node.getInputType();
        if (inputType == 0) {
            return false;
        }
        
        // 必须是文本类型
        if ((inputType & android.text.InputType.TYPE_CLASS_TEXT) == 0 &&
            (inputType & android.text.InputType.TYPE_CLASS_PHONE) == 0 &&
            (inputType & android.text.InputType.TYPE_CLASS_NUMBER) == 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 查找密码字段前的文本输入框（作为用户名）
     */
    private static AutofillId findTextFieldBeforePassword(List<ViewNode> nodes, AutofillId passwordId) {
        AutofillId lastTextField = null;
        
        for (ViewNode node : nodes) {
            if (node.getAutofillId() != null && node.getAutofillId().equals(passwordId)) {
                // 找到密码字段，返回之前的文本输入框
                if (lastTextField != null) {
                    Log.d(TAG, "Found text field before password");
                }
                return lastTextField;
            }
            
            // 记录可编辑的文本输入框（非密码）
            if (isEditableTextField(node) && !isPasswordField(node)) {
                lastTextField = node.getAutofillId();
            }
        }
        
        return null;
    }
    
    /**
     * 查找任何可编辑的文本输入框
     */
    private static AutofillId findAnyEditableTextField(List<ViewNode> nodes) {
        for (ViewNode node : nodes) {
            if (isEditableTextField(node) && !isPasswordField(node)) {
                Log.d(TAG, "Found any editable text field");
                return node.getAutofillId();
            }
        }
        return null;
    }

    /**
     * 判断是否为密码字段
     */
    private static boolean isPasswordField(ViewNode node) {
        // 检查是否可编辑
        if (node.getAutofillId() == null) {
            return false;
        }
        
        // 检查autofill hints
        String[] autofillHints = node.getAutofillHints();
        if (autofillHints != null) {
            for (String hint : autofillHints) {
                String h = hint.toLowerCase();
                if (h.contains("password")) {
                    Log.d(TAG, "Password field found by autofillHint: " + hint);
                    return true;
                }
            }
        }

        // 检查输入类型 - 这是最可靠的方式
        int inputType = node.getInputType();
        int variation = inputType & android.text.InputType.TYPE_MASK_VARIATION;
        if (variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            Log.d(TAG, "Password field found by inputType: " + inputType);
            return true;
        }

        // 检查hint
        CharSequence hint = node.getHint();
        if (hint != null) {
            String hintStr = hint.toString().toLowerCase();
            if (hintStr.contains("password") || hintStr.contains("密码") ||
                hintStr.contains("口令")) {
                Log.d(TAG, "Password field found by hint: " + hintStr);
                return true;
            }
        }

        // 检查id
        String id = node.getIdEntry();
        if (id != null) {
            String idStr = id.toLowerCase();
            if (idStr.contains("password") || idStr.contains("pwd") || idStr.contains("passwd")) {
                Log.d(TAG, "Password field found by id: " + idStr);
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