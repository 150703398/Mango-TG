#!/usr/bin/env python3
"""Exercise channel subtitle/profile production branches and retired title overrides."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ.get("JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home")) / "bin"
BASE = ROOT / "TMessagesProj/src/main/java/org/telegram"
shared = (BASE / "messenger/SharedConfig.java").read_text()
avatar = (BASE / "ui/Components/ChatAvatarContainer.java").read_text()
profile = (BASE / "ui/ProfileActivity.java").read_text()


def method(source, signature):
    start = source.index(signature)
    end = source.index("{", start) + 1
    depth = 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


shared_methods = "\n".join(method(shared, signature) for signature in (
    "    private static String getLocalChannelDisplayMembersKey(",
    "    public static String getLocalChannelDisplayMembers(int account, long chatId)",
    "    public static String getLocalChannelDisplayMembers(int account, TLRPC.Chat chat)",
    "    public static void setLocalChannelDisplayMembers(",
    "    public static void clearLocalChannelDisplayMembers(",
    "    public static String getDisplayChatTitle(",
))
avatar_methods = "\n".join(method(avatar, signature) for signature in (
    "    public static CharSequence getChatSubtitle(int account,",
    "    public static CharSequence getChatSubtitle(TLRPC.Chat",
))
start = profile.index("            CharSequence statusString;", profile.index("String localMembers") - 200)
end = profile.index("            if (copyFromChatActivity)", start)
profile_status = profile[start:end]
start = profile.index("                if (a == 0 && onlineTextOverride != null)", end)
end = profile.index("                if (a == 1 && isTopic)", start)
profile_labels = profile[start:end]

fixture = r'''
import java.util.*;
public class LocalDisplayRegression {
 static class TextUtils { static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; } }
 static class TLRPC {
  static class Chat { long id; String title; boolean channel, megagroup, broadcast, has_geo, admin, publicChat, mono, kicked, left; int participants_count; }
  static class ChatFull { int participants_count; boolean can_view_participants; Participants participants; }
  static class Participants { ArrayList<Object> participants = new ArrayList<>(); }
  static class Topic { int totalMessagesCount; }
 }
 static class ChatObject {
  static boolean isChannel(TLRPC.Chat c) { return c != null && c.channel; }
  static boolean isChannelAndNotMegaGroup(TLRPC.Chat c) { return isChannel(c) && !c.megagroup; }
  static boolean isMonoForum(TLRPC.Chat c) { return c != null && c.mono; }
  static boolean isPublic(TLRPC.Chat c) { return c.publicChat; }
  static boolean hasAdminRights(TLRPC.Chat c) { return c.admin; }
  static boolean isKickedFromChat(TLRPC.Chat c) { return c.kicked; }
  static boolean isLeftFromChat(TLRPC.Chat c) { return c.left; }
 }
 static class R {
  static class string { static final int Loading=1, ChannelPublic=2, ChannelPrivate=3, MegaLocation=4, MegaPublic=5, MegaPrivate=6, YouWereKicked=7, YouLeft=8, TopicProfileStatus=9; }
  static class drawable { static final int arrow_newchat=1; }
 }
 static class LocaleController {
  static String getString(int id) { return "label" + id; }
  static String formatPluralString(String key, int value, Object... args) { return key + ":" + value; }
  static String formatPluralStringComma(String key, int value) { return key + ":" + value; }
 }
 static String formatString(String key, int id, Object... args) { return key; }
 static class ColoredImageSpan { ColoredImageSpan(int id) {} }
 static class Spanned { static final int SPAN_EXCLUSIVE_EXCLUSIVE=33; }
 static class SpannableString implements CharSequence {
  String value;
  SpannableString(String v) { value=v; }
  void setSpan(Object o,int s,int e,int f) {}
  public int length() { return value.length(); }
  public char charAt(int i) { return value.charAt(i); }
  public CharSequence subSequence(int a,int b) { return value.substring(a,b); }
  public String toString() { return value; }
 }
 static class SpannableStringBuilder extends SpannableString {
  SpannableStringBuilder(String v) { super(v); }
  SpannableStringBuilder append(char c) { value+=c; return this; }
  SpannableStringBuilder append(CharSequence c) { value+=c; return this; }
 }
 static class Label { CharSequence text; void setText(CharSequence s) { text=s; } }
 static class SharedConfig {
  static final String PREF_LOCAL_CHANNEL_DISPLAY_MEMBERS_PREFIX="local_channel_display_members_";
  static final Map<String,String> prefs=new HashMap<>();
  static String getLocalDisplayValue(String key) { return prefs.getOrDefault(key, ""); }
  static void setLocalDisplayValue(String key, String value) {
   if (value==null || value.trim().isEmpty()) prefs.remove(key); else prefs.put(key,value.trim());
  }
''' + shared_methods + r'''
 }
''' + avatar_methods + r'''
 static String[] profile(TLRPC.Chat chat, TLRPC.ChatFull chatInfo, int currentAccount, boolean isTopic) {
  TLRPC.Chat currentChat=chat;
  int onlineCount=3;
  TLRPC.Topic topic = new TLRPC.Topic(); topic.totalMessagesCount=9;
''' + profile_status + r'''
  boolean copyFromChatActivity=false;
  CharSequence onlineTextOverride=null;
  Label[] onlineTextView={new Label(),new Label()};
  for(int a=0;a<2;a++) {
''' + profile_labels + r'''
  }
  return new String[]{onlineTextView[0].text.toString(),onlineTextView[1].text.toString()};
 }
 static int checks;
 static void check(boolean value,String detail) { checks++; if (!value) throw new AssertionError(detail); }
 static void eq(Object actual,Object expected,String detail) { check(Objects.equals(actual,expected),detail+": "+actual+" != "+expected); }
 static TLRPC.Chat channel() {
  TLRPC.Chat c=new TLRPC.Chat(); c.id=11; c.title="Actual title"; c.channel=true; c.broadcast=true; c.participants_count=23; return c;
 }
 static TLRPC.ChatFull full(int count) { TLRPC.ChatFull f=new TLRPC.ChatFull(); f.participants_count=count; return f; }
 public static void main(String[] args) {
  TLRPC.Chat c=channel();
  SharedConfig.prefs.put("local_channel_remark_0_11","Old local title");
  eq(SharedConfig.getDisplayChatTitle(0,c),"Actual title","old title override ignored");
  c.title="New server title";
  eq(SharedConfig.getDisplayChatTitle(0,c),"New server title","server title changes remain visible");
  eq(SharedConfig.getDisplayChatTitle(0,null),"","null chat title");
  c.title=null; eq(SharedConfig.getDisplayChatTitle(0,c),"","null server title");
  c=channel();
  SharedConfig.setLocalChannelDisplayMembers(0,c.id,"  88,888 subscribers  ");
  SharedConfig.setLocalChannelDisplayMembers(1,c.id,"99 subscribers");
  eq(getChatSubtitle(0,c,full(23),0),"88,888 subscribers","chat header custom count");
  eq(getChatSubtitle(1,c,full(23),0),"99 subscribers","account isolation");
  eq(getChatSubtitle(2,c,full(23),0),"Subscribers:23","other account server fallback");
  eq(getChatSubtitle(0,c,null,0),"88,888 subscribers","before full info arrives");
  eq(getChatSubtitle(0,c,full(0),0),"88,888 subscribers","server zero cannot override local count");
  for(boolean admin:new boolean[]{false,true}) {
   c.admin=admin;
   for(TLRPC.ChatFull info:new TLRPC.ChatFull[]{null,full(0),full(23)}) {
    if(info!=null) info.can_view_participants=true;
    String[] labels=profile(c,info,0,false);
    eq(labels[0],"88,888 subscribers","profile compact header admin="+admin);
    eq(labels[1],"88,888 subscribers","profile expanded header admin="+admin);
    if(info!=null) eq(info.participants_count,info.participants_count==0?0:23,"server count remains unchanged");
   }
  }
  eq(c.participants_count,23,"basic chat count unchanged");
  eq(SharedConfig.getLocalChannelDisplayMembers(0,(TLRPC.Chat)null),"","null local count");
  c.megagroup=true;
  eq(getChatSubtitle(0,c,full(23),2),"Members:23, OnlineCount:2","group never uses channel override");
  eq(profile(c,full(23),0,true)[0],"messages:8","topic message count unchanged");
  c.megagroup=false;c.mono=true;
  eq(SharedConfig.getLocalChannelDisplayMembers(0,c),"","direct messages channel excluded");
  c.mono=false;
  SharedConfig.clearLocalChannelDisplayMembers(0,c.id);
  eq(getChatSubtitle(0,c,full(24),0),"Subscribers:24","clear immediately restores live count");
  eq(getChatSubtitle(1,c,full(24),0),"99 subscribers","clear does not affect other account");
  SharedConfig.setLocalChannelDisplayMembers(0,c.id," ");
  eq(getChatSubtitle(0,c,full(25),0),"Subscribers:25","blank resets override");
  c.channel=false;c.megagroup=false;
  eq(getChatSubtitle(0,c,null,0),"Members:23","ordinary groups unchanged");
  System.out.println("PASS: "+checks+" local display production assertions");
 }
}
'''
with tempfile.TemporaryDirectory(prefix="v50-local-display-") as directory:
    source = Path(directory) / "LocalDisplayRegression.java"
    source.write_text(fixture)
    subprocess.run([str(JAVA / "javac"), "-d", directory, str(source)], check=True)
    subprocess.run([str(JAVA / "java"), "-cp", directory, "LocalDisplayRegression"], check=True)

# Integration checks cover which account is passed, refresh masks and retired UI routes.
assert "newSubtitle = getChatSubtitle(currentAccount, chat, info, onlineCount);" in avatar
assert "UPDATE_MASK_CHAT_NAME | MessagesController.UPDATE_MASK_CHAT_MEMBERS" in method(profile, "    private void refreshLocalChannelDisplay()")
assert "SharedConfig.getLocalChannelRemark" not in profile
assert "SharedConfig.setLocalChannelRemark" not in profile
assert "LocalProfileDisplayEditChatName" not in profile
assert "ChannelRemarkEdit" not in profile
assert "chatId != 0 || !canSetLocalUserRemark(user)" in method(profile, "    private boolean showPeerRemarkOptions(")
assert "TextUtils.isEmpty(localMembers) && chatActivity.avatarContainer.getSubtitleTextView()" in profile
print("PASS: 8 channel refresh / retired UI integration assertions")
