package com.bilibili.im.moderation.tool;

import com.bilibili.tool.StringTool;

public final class SensitiveWordTextCleaner {

    private SensitiveWordTextCleaner() {
    }

    /**
     * 对外统一的文本归一化入口。
     * 适合在敏感词匹配前调用，把原始文本清洗成更稳定的匹配文本。
     */
    public static String normalize(String text) {
        return normalizeForMatch(text);
    }

    /**
     * 按既定顺序串联整条清洗流程：
     * 去首尾空白 -> 全角转半角 -> 英文转小写 -> 去不可见字符 -> 去干扰符号 -> 去全部空白。
     */
    public static String normalizeForMatch(String text) {
        String normalized = trimEdgeWhitespace(text);
        normalized = toHalfWidth(normalized);
        normalized = toLowerCaseEnglish(normalized);
        normalized = removeInvisibleChars(normalized);
        normalized = removeInterferenceSymbols(normalized);
        normalized = removeWhitespace(normalized);
        return StringTool.normalizeOptional(normalized);
    }

    /**
     * 去掉文本首尾空白；如果结果为空串则返回 null。
     */
    public static String trimEdgeWhitespace(String text) {
        return StringTool.normalizeOptional(text);
    }

    /**
     * 将全角字符转换为半角字符，便于统一中文输入法下的混合文本。
     */
    public static String toHalfWidth(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == 12288) {
                builder.append(' ');
            } else if (ch >= 65281 && ch <= 65374) {
                builder.append((char) (ch - 65248));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 仅对英文字符做小写化，避免大小写混写影响匹配。
     */
    public static String toLowerCaseEnglish(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                builder.append((char) (ch + ('a' - 'A')));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 去除零宽字符、控制字符等不可见字符，防止通过隐藏字符绕过匹配。
     */
    public static String removeInvisibleChars(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!isInvisibleChar(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 去除常见标点和符号干扰项，适合处理“敏*感-词”这类规避写法。
     */
    public static String removeInterferenceSymbols(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!isInterferenceSymbol(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 删除所有空白字符，适合需要把“敏 感 词”压缩成连续文本的场景。
     */
    public static String removeWhitespace(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isWhitespace(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 将连续空白压缩为单个空格，适合保留基本分词边界的场景。
     */
    public static String collapseWhitespace(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousWhitespace = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (!previousWhitespace) {
                    builder.append(' ');
                    previousWhitespace = true;
                }
                continue;
            }
            builder.append(ch);
            previousWhitespace = false;
        }
        return builder.toString();
    }

    /**
     * 判断字符是否为不可见字符，如格式控制字符、控制字符等。
     */
    public static boolean isInvisibleChar(char ch) {
        return Character.getType(ch) == Character.FORMAT
                || Character.isISOControl(ch)
                || ch == '\u00AD';
    }

    /**
     * 判断字符是否为用于干扰匹配的符号类字符；字母、数字和空白不算干扰符号。
     */
    public static boolean isInterferenceSymbol(char ch) {
        if (Character.isWhitespace(ch) || Character.isLetterOrDigit(ch)) {
            return false;
        }
        int type = Character.getType(ch);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }
}
