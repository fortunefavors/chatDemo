package com.xiaoyu.rtc.wrapper;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Debug功能 -- 待邀请成员列表
 */
public class Member implements Serializable {
    public long id;
    public String nick;
    public String uri;
    public String no;
    public boolean custom;

    public String getDisplayNo(boolean br) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(nick)) {
            sb.append(nick);
        }
        if (!TextUtils.isEmpty(uri) && !uri.equals(nick)) {
            if (br && sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append(uri);
        }

        if (!TextUtils.isEmpty(no) && !no.equals(uri) && !no.equals(nick)) {
            if (br && sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append(no);
        }
        return sb.toString();
    }

    public void copy(Member member) {
        id = member.id;
        nick = member.nick;
        uri = member.uri;
        no = member.no;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(uri, member.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", nick='" + nick + '\'' +
                ", uri='" + uri + '\'' +
                ", no='" + no + '\'' +
                ", custom=" + custom +
                '}';
    }
}