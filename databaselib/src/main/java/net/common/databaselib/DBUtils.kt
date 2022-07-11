package net.common.databaselib

import android.content.ContentValues
import java.util.*

object DBUtils {

    var choseongs = arrayOf("\\u1101", "\\u1104", "\\u1108", "\\u110a", "\\u110d")

    fun Map<String, Any>.convertMapToContentValues() : ContentValues {
        var values = ContentValues()
        var keySet = this.keys
        for (key in keySet) {
            var value = get(key).toString()
            if ("null".equals(value, true)) {
                value = ""
            }
            if (key.equals("choseong")) {
                value = getChoseong(value)
            }
            values.put(key, value)
        }
        return values
    }

    fun getChoseong(value: String) : String {
        var unicode = escapeUnicode(value)
        var listData = choseongs.asList()
        if (!listData.contains(unicode))  {
            return value
        }

        if (unicode == listData[0]) {
            return "ㄲ";
        } else if (unicode == listData[1]) {
            return "ㄸ";
        } else if (unicode == listData[2]) {
            return "ㅃ";
        } else if (unicode == listData[3]) {
            return "ㅆ";
        } else if (unicode == listData[4]) {
            return "ㅉ";
        }

        return unicode
    }

    fun escapeUnicode(input: String) : String {
        var sb = StringBuilder(input)
        var f = Formatter(sb)
        for (c in input.toCharArray()) {
            if (c < 128.digitToChar()) sb.append(c) else f.format("\\u%04x", c.digitToInt());
        }
        return sb.toString()
    }

}