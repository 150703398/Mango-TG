#!/usr/bin/env python3
"""Run the complete production keyword filter and sponsored-message method offline.

Android storage, messages and network are fixtures; Android SDK org.json and all
filtering/persistence/request-completion logic are the production implementations.
No network queue is executed. Request callbacks are delivered explicitly by tests.
"""
from pathlib import Path
import os
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'TMessagesProj/src/main/java/org/telegram/messenger'
JAVA = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin'
SDK = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
controller = (SOURCE / 'MessagesController.java').read_text()
shared_config = (SOURCE / 'SharedConfig.java').read_text()


def extract(text, marker):
    start = text.index(marker)
    end = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]


fixtures = {
'android/content/SharedPreferences.java': r'''
package android.content;
public interface SharedPreferences {
 boolean getBoolean(String key, boolean fallback); String getString(String key, String fallback);
 long getLong(String key, long fallback); Editor edit();
 interface Editor { Editor putBoolean(String key,boolean value); Editor putString(String key,String value); Editor putLong(String key,long value); void apply(); }
}''',
'android/text/TextUtils.java': r'''package android.text; public class TextUtils {
 public static boolean isEmpty(CharSequence text){return text==null||text.length()==0;}
 public static boolean equals(CharSequence a,CharSequence b){return a==b||a!=null&&b!=null&&a.toString().equals(b.toString());}
}''',
'android/os/SystemClock.java': r'''package android.os; public class SystemClock {public static long elapsedRealtime(){return 1000000;}}''',
'androidx/collection/LongSparseArray.java': r'''package androidx.collection; public class LongSparseArray<T> extends java.util.HashMap<Long,T> {
 public T get(long key){return super.get(key);}public void put(long key,T value){super.put(key,value);}public void remove(long key){super.remove(key);}
}''',
'org/telegram/messenger/Prefs.java': r'''package org.telegram.messenger;
import android.content.SharedPreferences; import java.util.*;
public class Prefs implements SharedPreferences {
 final Map<String,Object> values=new HashMap<>();
 int writes;
 public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}
 public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
 public long getLong(String k,long d){return (Long)values.getOrDefault(k,d);}
 public Editor edit(){return new Editor(){final Map<String,Object> pending=new HashMap<>();
 public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
 public Editor putString(String k,String v){pending.put(k,v);return this;}
 public Editor putLong(String k,long v){pending.put(k,v);return this;}
 public void apply(){values.putAll(pending);writes++;}};}
}''',
'org/telegram/messenger/UserConfig.java': r'''package org.telegram.messenger; public class UserConfig {
 public static final int MAX_ACCOUNT_COUNT=4;public static int[] getActivatedAccounts(){return new int[]{0,1,3};}
}''',
'org/telegram/messenger/FileLog.java': r'''package org.telegram.messenger;public class FileLog {public static void e(Exception error){throw new AssertionError(error);}}''',
'org/telegram/messenger/Utilities.java': r'''package org.telegram.messenger;public class Utilities {
 public static final Queue globalQueue=new Queue(); public static class Queue {public int queued;public void postRunnable(Runnable task){queued++; /* Deliberately never execute network tasks. */}}
}''',
'org/telegram/messenger/AndroidUtilities.java': r'''package org.telegram.messenger;import java.util.*;public class AndroidUtilities {
 static final ArrayList<Runnable> queue=new ArrayList<>();public static void runOnUIThread(Runnable action){queue.add(action);}
 static void flush(){while(!queue.isEmpty())queue.remove(0).run();}
}''',
'org/telegram/messenger/NotificationCenter.java': r'''package org.telegram.messenger;import java.util.*;public class NotificationCenter {
 public static final int keywordFilterSettingsUpdated=1,didLoadSponsoredMessages=2;
 static final NotificationCenter[] instances={new NotificationCenter(),new NotificationCenter(),new NotificationCenter(),new NotificationCenter()};
 final int[] counts=new int[3];public static NotificationCenter getInstance(int a){return instances[a];}
 public void postNotificationName(int id,Object...args){counts[id]++;}
}''',
'org/telegram/messenger/SharedConfig.java': r'''package org.telegram.messenger;public class SharedConfig {
 //PRODUCTION_SETTING
}''',
'org/telegram/messenger/UserObject.java': r'''package org.telegram.messenger;import org.telegram.tgnet.TLRPC;public class UserObject {
 public static String getUserName(TLRPC.User user){return user.username;}public static String getPublicUsername(TLRPC.User user){return user.username;}public static boolean isBot(TLRPC.User user){return user!=null&&user.bot;}
}''',
'org/telegram/messenger/ChatObject.java': r'''package org.telegram.messenger;import org.telegram.tgnet.TLRPC;public class ChatObject {
 public static String getPublicUsername(TLRPC.Chat chat){return chat.username;}public static boolean isChannel(TLRPC.Chat chat){return chat!=null&&chat.channel;}
}''',
'org/telegram/tgnet/tl/TL_stories.java': r'''package org.telegram.tgnet.tl;import java.util.*;import org.telegram.tgnet.TLRPC;public class TL_stories {
 public static class StoryItem {public String caption;public ArrayList<TLRPC.MessageEntity> entities;}
}''',
'org/telegram/tgnet/TLRPC.java': r'''
package org.telegram.tgnet;import java.util.*;import org.telegram.tgnet.tl.TL_stories;
public class TLRPC {
 public static class Peer {public long user_id,chat_id,channel_id;}
 public static class TL_peerUser extends Peer{} public static class TL_peerUser_layer131 extends Peer{}
 public static class TL_peerChannel extends Peer{} public static class TL_peerChannel_layer131 extends Peer{}
 public static class User {public long id;public String username;public boolean bot;public ArrayList<TL_username> usernames;}
 public static class Chat {public long id;public String username,title;public boolean channel;public ArrayList<TL_username> usernames;}
 public static class TL_username {public boolean active;public String username;}
 public static class Message {public int id,date,flags;public String message;public Peer from_id,peer_id;public MessageMedia media;
 public ArrayList<MessageEntity> entities;public TL_textWithEntities originalText,translatedText,translatedVoiceTranscription,summaryText,translatedSummaryText;
 public Fwd fwd_from;public ReplyMarkup reply_markup;}
 public static class TL_message extends Message{}
 public static class Fwd {public String from_name,post_author,saved_from_name;public Peer from_id,saved_from_id;}
 public static class MessageEntity {public int offset,length;public String url;}
 public static class TL_textWithEntities {public String text;public ArrayList<MessageEntity> entities;}
 public static class MessageMedia {public String title,description,captionLegacy,address,provider,venue_id,venue_type,phone_number,first_name,last_name,vcard,currency,start_param;
 public WebPage webpage;public TL_game game;public TL_stories.StoryItem storyItem;public Peer peer;public Document document;public ArrayList<Document> alt_documents;}
 public static class TL_messageMediaWebPage extends MessageMedia{}
 public static class TL_messageMediaPoll extends MessageMedia{public Poll poll;}
 public static class TL_messageMediaGame extends MessageMedia{}
 public static class TL_messageMediaStory extends MessageMedia{} public static class TL_messageMediaStory_layer162 extends MessageMedia{}
 public static class TL_messageMediaGiveaway extends MessageMedia{public String prize_description;}
 public static class TL_messageMediaGiveawayResults extends MessageMedia{public String prize_description;}
 public static class TL_messageMediaToDo extends MessageMedia{public TodoList todo;}
 public static class WebPage {public String url,display_url,type,site_name,title,description,embed_url,embed_type,author,displayedText,archivePath;public Document document;}
 public static class Poll {public TL_textWithEntities question;public ArrayList<PollAnswer> answers;}
 public static class PollAnswer {public TL_textWithEntities text;}
 public static class TodoList {public TL_textWithEntities title;public ArrayList<TodoItem> list;}
 public static class TodoItem {public TL_textWithEntities title;}
 public static class TL_game {public String short_name,title,description;public Document document;}
 public static class Document {public String file_name,file_name_fixed,mime_type,localPath,localThumbPath;public ArrayList<DocumentAttribute> attributes;}
 public static class DocumentAttribute {public String file_name,title,performer,alt;}
 public static class ReplyMarkup {public ArrayList<TL_keyboardButtonRow> rows;}
 public static class TL_keyboardButtonRow {public ArrayList<KeyboardButton> buttons;}
 public static class KeyboardButton {public String text,url,query,fwd_text;}
 public static class TL_keyboardButtonCopy_layer223 extends KeyboardButton{public String copy_text;}
 public static class TL_keyboardButtonCopy extends KeyboardButton{public String copy_text;}
 public static class TL_messages_getSponsoredMessages {public Object peer;}
 public static class messages_SponsoredMessages {public int flags,posts_between;public ArrayList<TL_sponsoredMessage> messages=new ArrayList<>();public ArrayList<User> users=new ArrayList<>();public ArrayList<Chat> chats=new ArrayList<>();}
 public static class TL_messages_sponsoredMessages extends messages_SponsoredMessages{}
 public static class TL_sponsoredMessage {public String title,url,sponsor_info,additional_info,button_text;public byte[] random_id;public boolean recommended,can_report;public Object photo,color;public MessageMedia media;}
}''',
'org/telegram/messenger/MessageObject.java': r'''
package org.telegram.messenger;import java.util.*;import org.telegram.tgnet.TLRPC;import androidx.collection.LongSparseArray;
public class MessageObject {
 public int currentAccount;public TLRPC.Message messageOwner;public CharSequence caption,messageText;public long did;public boolean sponsored,out;
 public byte[] sponsoredId;public String sponsoredTitle,sponsoredUrl,sponsoredInfo,sponsoredAdditionalInfo,sponsoredButtonText;
 public boolean sponsoredRecommended,sponsoredCanReport;public Object sponsoredPhoto,sponsoredColor;public TLRPC.MessageMedia sponsoredMedia;public ArrayList<Object> textLayoutBlocks;
 public MessageObject(int account,long dialog,int id,String text){currentAccount=account;did=dialog;messageOwner=new TLRPC.Message();messageOwner.id=id;messageOwner.date=(int)(System.currentTimeMillis()/1000);messageOwner.message=text;}
 public MessageObject(int account,TLRPC.Message message,LongSparseArray<TLRPC.User> users,LongSparseArray<TLRPC.Chat> chats,boolean a,boolean b){currentAccount=account;messageOwner=message;}
 public int getId(){return messageOwner.id;}public long getDialogId(){return did;}public boolean isSponsored(){return sponsored;}public boolean isOutOwner(){return out;}public String getFileName(){return "";}
 public void setType(){}public void generateThumbs(boolean a){}
}''',
'org/telegram/messenger/MessagesController.java': r'''
package org.telegram.messenger;import java.util.*;import android.content.SharedPreferences;import android.os.SystemClock;import androidx.collection.LongSparseArray;import org.telegram.tgnet.TLRPC;
public class MessagesController {
 static final MessagesController[] instances={new MessagesController(0),new MessagesController(1),new MessagesController(2),new MessagesController(3)};
 static final Prefs[] preferences={new Prefs(),new Prefs(),new Prefs(),new Prefs()};
 static final Prefs globalPreferences=new Prefs();
 final int currentAccount;final Map<Long,TLRPC.User> users=new HashMap<>();final Map<Long,TLRPC.Chat> chats=new HashMap<>();
 final LongSparseArray<SponsoredMessagesInfo> sponsoredMessages=new LongSparseArray<>();final Connection connection=new Connection();
 MessagesController(int account){currentAccount=account;}public static MessagesController getInstance(int a){return instances[a];}
 public static SharedPreferences getMainSettings(int a){return preferences[a];}public TLRPC.User getUser(long id){return users.get(id);}public TLRPC.Chat getChat(long id){return chats.get(id);}
 public static SharedPreferences getGlobalMainSettings(){return globalPreferences;}
 Object getInputPeer(long did){return did;}TLRPC.Peer getPeer(long did){return new TLRPC.Peer();}Connection getConnectionsManager(){return connection;}
 void putUsers(ArrayList<TLRPC.User> values,boolean cache){for(TLRPC.User user:values)users.put(user.id,user);}void putChats(ArrayList<TLRPC.Chat> values,boolean cache){for(TLRPC.Chat chat:values)chats.put(chat.id,chat);}
 NotificationCenter getNotificationCenter(){return NotificationCenter.getInstance(currentAccount);}
 interface Callback {void run(Object response,Object error);}static class Connection {final ArrayList<Callback> requests=new ArrayList<>();void sendRequest(Object request,Callback callback){requests.add(callback);}int getCurrentTime(){return 1000;}}
 //PRODUCTION_METHODS
}''',
'org/telegram/messenger/AdWhitelistTest.java': r'''
package org.telegram.messenger;import java.util.*;import java.lang.reflect.*;import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.KeywordMessageFilterManager.WhitelistDialog;
public class AdWhitelistTest {
 static int checks,id=1;static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
 static KeywordMessageFilterManager fresh(int account)throws Exception{Field field=KeywordMessageFilterManager.class.getDeclaredField("instances");field.setAccessible(true);((KeywordMessageFilterManager[])field.get(null))[account]=null;return KeywordMessageFilterManager.getInstance(account);}
 static ArrayList<WhitelistDialog> entries(long...ids){ArrayList<WhitelistDialog> result=new ArrayList<>();for(long did:ids)result.add(new WhitelistDialog(did,"Chat "+did,did==-30,did>0));return result;}
 static MessageObject message(int account,long did,String text,long sender){MessageObject m=new MessageObject(account,did,id++,text);if(sender>0){m.messageOwner.from_id=new TLRPC.TL_peerUser();m.messageOwner.from_id.user_id=sender;}else if(sender<0){m.messageOwner.from_id=new TLRPC.TL_peerChannel();m.messageOwner.from_id.channel_id=-sender;}return m;}
 static void seed(int account){
  MessagesController.getMainSettings(account).edit().putString("keyword_filter_remote_keywords","[\"remote-offer\"]").putString("keyword_filter_remote_blocked_usernames","[\"remote_spammer\",\"channel_spammer\"]").apply();
  MessagesController c=MessagesController.getInstance(account);
  for(long uid:new long[]{10,11,12}){TLRPC.User u=new TLRPC.User();u.id=uid;u.username=uid==11?"remote_spammer":uid==12?"custom_spammer":"clean_user";c.users.put(uid,u);}
  for(long cid:new long[]{20,30,40}){TLRPC.Chat chat=new TLRPC.Chat();chat.id=cid;chat.channel=cid!=20;chat.title="Chat "+cid;chat.username=cid==40?"channel_spammer":"clean_channel";c.chats.put(cid,chat);}
 }
 static void clearNotice(int account){NotificationCenter.getInstance(account).counts[1]=0;NotificationCenter.getInstance(account).counts[2]=0;}
 static void rawGlobal(boolean enabled){MessagesController.getGlobalMainSettings().edit().putBoolean("block_sponsored_messages_full",enabled).apply();}
 static TLRPC.TL_messages_sponsoredMessages sponsored(){TLRPC.TL_messages_sponsoredMessages response=new TLRPC.TL_messages_sponsoredMessages();response.flags=1;response.posts_between=3;TLRPC.TL_sponsoredMessage ad=new TLRPC.TL_sponsoredMessage();ad.title="Ad";response.messages.add(ad);return response;}
 public static void main(String[] args)throws Exception{
  check(!SharedConfig.isBlockSponsoredMessagesFullEnabled(),"production sponsored setting defaults to false");
  seed(0);seed(1);KeywordMessageFilterManager m=KeywordMessageFilterManager.getInstance(0);m.addKeyword("local-offer");m.setCustomBlockedUsersFromText("custom_spammer");AndroidUtilities.flush();
  check(!m.isDialogWhitelisted(0),"zero dialog is never whitelisted");
  check(!m.addWhitelistDialogs(entries(0)),"zero ID is not added");
  for(long did:new long[]{10,-20,-30}){
   check("local-offer".equals(m.getMatchingKeyword(message(0,did,"LOCAL-OFFER",0),false)),"local keyword matches before whitelist "+did);
   check("remote-offer".equals(m.getMatchingKeyword(message(0,did,"REMOTE-OFFER",0),false)),"remote keyword matches before whitelist "+did);
   for(long sender:new long[]{11,12,-40}){
    MessageObject msg=message(0,did,"hello",sender);
    check(m.getMatchingKeyword(msg,false)!=null,"blocked sender matches before whitelist "+did+"/"+sender);
    check(m.shouldHideMessageByBlockedUsername(msg),"blocked sender fast path before whitelist "+did+"/"+sender);
   }
  }
  MessagesController c=MessagesController.getInstance(0);MessagesController other=MessagesController.getInstance(1);
  c.sponsoredMessages.put(-30,c.new SponsoredMessagesInfo());other.sponsoredMessages.put(-30,other.new SponsoredMessagesInfo());clearNotice(0);clearNotice(1);
  check(m.addWhitelistDialogs(entries(10,-20,-30)),"add private, group and channel whitelist");AndroidUtilities.flush();
  check(c.sponsoredMessages.isEmpty(),"whitelist change clears sponsored cache");check(!other.sponsoredMessages.isEmpty(),"cache invalidation stays in current account");
  check(NotificationCenter.getInstance(0).counts[1]==1&&NotificationCenter.getInstance(1).counts[1]==0,"whitelist change notifies only current account once");
  check(m.getWhitelistCount()==3,"all three dialog kinds persisted");
  check(!m.addWhitelistDialogs(entries(10,-20,-30)),"duplicate whitelist add is unchanged");AndroidUtilities.flush();check(NotificationCenter.getInstance(0).counts[1]==1,"duplicate add does not notify");
  for(long did:new long[]{10,-20,-30}){
   check(m.isDialogWhitelisted(did),"public whitelist lookup "+did);
   check(m.getMatchingKeyword(message(0,did,"local-offer remote-offer",0),true)==null,"whitelist overrides local and remote terms "+did);
   for(long sender:new long[]{11,12,-40}){
    MessageObject msg=message(0,did,"local-offer remote-offer",sender);
    check(m.getMatchingKeyword(msg,true)==null,"whitelist wins over blocked sender in match path "+did+"/"+sender);
    check(!m.shouldHideMessageByBlockedUsername(msg),"whitelist wins over blocked sender fast path "+did+"/"+sender);
   }
  }
  check(m.getLogs().isEmpty(),"whitelisted messages never add filtered logs");
  KeywordMessageFilterManager account1=KeywordMessageFilterManager.getInstance(1);check(!account1.isDialogWhitelisted(-30),"whitelist does not cross accounts");
  check(account1.shouldHideMessageByBlockedUsername(message(1,-30,"hello",11)),"other account still filters its sender");
  m=fresh(0);check(m.getWhitelistCount()==3&&m.isDialogWhitelisted(10)&&m.isDialogWhitelisted(-20)&&m.isDialogWhitelisted(-30),"fresh production manager reloads persisted whitelist");
  check(m.getMatchingKeyword(message(0,-30,"local-offer remote-offer",11),false)==null,"reloaded whitelist still overrides filters");
  m.getWhitelistDialogs().clear();check(m.getWhitelistCount()==3,"whitelist getter returns a defensive copy");
  ArrayList<WhitelistDialog> renamed=new ArrayList<>();renamed.add(new WhitelistDialog(-30," Renamed ",true,false));check(m.addWhitelistDialogs(renamed),"existing whitelist entry updates title");m=fresh(0);check(m.getWhitelistDialogs().get(2).title.equals("Renamed"),"updated metadata reloads");AndroidUtilities.flush();
  check(m.removeWhitelistDialogs(Arrays.asList(-20L)),"remove group from whitelist");AndroidUtilities.flush();
  check(!m.isDialogWhitelisted(-20)&&m.isDialogWhitelisted(-30)&&m.isDialogWhitelisted(10),"remove only requested entry");
  check("local-offer".equals(m.getMatchingKeyword(message(0,-20,"local-offer",0),false)),"removed group resumes local filtering");
  check(m.shouldHideMessageByBlockedUsername(message(0,-20,"hello",11)),"removed group resumes sender filtering");
  check(!m.removeWhitelistDialogs(Arrays.asList(-20L)),"removing absent entry is unchanged");m=fresh(0);check(!m.isDialogWhitelisted(-20)&&m.isDialogWhitelisted(-30),"removal survives reload");
  m.setCustomBlockedUsersFromText("12");AndroidUtilities.flush();check(!m.shouldHideMessageByBlockedUsername(message(0,-30,"hello",12)),"whitelist also overrides numeric blocked user IDs");
  m.setEnabled(false);AndroidUtilities.flush();check(!m.shouldHideMessageByBlockedUsername(message(0,-30,"hello",11)),"whitelist stays effective with keyword toggle disabled");
  check(m.shouldHideMessageByBlockedUsername(message(0,-20,"hello",11)),"keyword toggle does not disable separate sender block list");
  // Execute the complete production sponsored request method with controllable asynchronous callbacks.
  clearNotice(0);c.sponsoredMessages.clear();rawGlobal(true);
  check(c.getSponsoredMessages(-30)==null&&c.connection.requests.size()==1,"whitelisted channel may request sponsored messages");
  MessagesController.SponsoredMessagesInfo pending=c.sponsoredMessages.get(-30);
  check(pending!=null&&pending.loading,"allowed sponsored request stores pending info");
  check(c.getSponsoredMessages(-30)==pending&&c.connection.requests.size()==1,"loading sponsored request deduplicates");
  c.connection.requests.get(0).run(sponsored(),null);AndroidUtilities.flush();
  check(c.sponsoredMessages.get(-30)==pending&&!pending.loading&&pending.messages.size()==1&&pending.posts_between==3,"allowed sponsored response populates cache");
  check(NotificationCenter.getInstance(0).counts[2]==1,"allowed sponsored response notifies once");
  check(c.getSponsoredMessages(-30)==pending&&c.connection.requests.size()==1,"allowed fresh sponsored cache is reused");
  check(c.getSponsoredMessages(-40)==null&&c.connection.requests.size()==1,"non-whitelisted channel cannot request sponsored ads");
  // A request that was allowed before a whitelist removal must not publish afterwards.
  c.clearSponsoredMessagesCache();c.getSponsoredMessages(-30);MessagesController.Callback removed=c.connection.requests.get(1);
  check(m.removeWhitelistDialogs(Arrays.asList(-30L)),"remove channel with in-flight sponsored request");AndroidUtilities.flush();removed.run(sponsored(),null);AndroidUtilities.flush();
  check(!c.sponsoredMessages.containsKey(-30L)&&NotificationCenter.getInstance(0).counts[2]==1,"removed whitelist discards in-flight sponsored completion");
  // Re-add and launch another request; the prior request must not overwrite its identity.
  m.addWhitelistDialogs(entries(-30));AndroidUtilities.flush();c.getSponsoredMessages(-30);MessagesController.SponsoredMessagesInfo replacement=c.sponsoredMessages.get(-30);
  removed.run(sponsored(),null);AndroidUtilities.flush();check(c.sponsoredMessages.get(-30)==replacement&&replacement.loading,"old completion cannot replace a newer request");
  c.connection.requests.get(2).run(sponsored(),null);AndroidUtilities.flush();check(!replacement.loading&&NotificationCenter.getInstance(0).counts[2]==2,"replacement request completes normally");
  // Flip the global switch without clearing cache to exercise the callback's current-settings guard.
  rawGlobal(false);c.clearSponsoredMessagesCache();c.getSponsoredMessages(-40);MessagesController.Callback switched=c.connection.requests.get(3);
  rawGlobal(true);switched.run(sponsored(),null);AndroidUtilities.flush();check(!c.sponsoredMessages.containsKey(-40L)&&NotificationCenter.getInstance(0).counts[2]==2,"response rechecks global block setting");
  rawGlobal(false);c.getSponsoredMessages(-40);c.connection.requests.get(4).run(sponsored(),null);AndroidUtilities.flush();check(c.sponsoredMessages.get(-40)!=null,"global block disabled restores normal sponsored behavior");
  rawGlobal(true);check(c.getSponsoredMessages(-40)==null&&!c.sponsoredMessages.containsKey(-40L),"blocked cached sponsored messages are evicted before returning");
  // The shared setter is also used by history restoration and reset, independently of the settings UI.
  for(int account=0;account<4;account++){
   MessagesController cached=MessagesController.getInstance(account);
   cached.sponsoredMessages.put(-30,cached.new SponsoredMessagesInfo());clearNotice(account);
  }
  int writes=MessagesController.globalPreferences.writes;
  SharedConfig.setBlockSponsoredMessagesFullEnabled(false);
  check(!SharedConfig.isBlockSponsoredMessagesFullEnabled(),"direct setter immediately updates production getter");
  check(!MessagesController.globalPreferences.getBoolean("block_sponsored_messages_full",true),"direct setter persists global setting");
  check(MessagesController.globalPreferences.writes==writes+1,"changed global setting persists exactly once");
  check(AndroidUtilities.queue.size()==1,"changed global setting schedules one UI invalidation");
  AndroidUtilities.flush();
  for(int account:new int[]{0,1,3}){
   check(MessagesController.getInstance(account).sponsoredMessages.isEmpty(),"shared setter clears activated account cache "+account);
   check(NotificationCenter.getInstance(account).counts[1]==1,"shared setter notifies activated account "+account);
  }
  check(!MessagesController.getInstance(2).sponsoredMessages.isEmpty()&&NotificationCenter.getInstance(2).counts[1]==0,"shared setter leaves inactive account alone");
  c.getSponsoredMessages(-40);MessagesController.SponsoredMessagesInfo setterPending=c.sponsoredMessages.get(-40);
  MessagesController.Callback setterResponse=c.connection.requests.get(5);
  SharedConfig.setBlockSponsoredMessagesFullEnabled(false);
  check(MessagesController.globalPreferences.writes==writes+1&&AndroidUtilities.queue.isEmpty(),"same-value setter does not write or schedule invalidation");
  check(c.sponsoredMessages.get(-40)==setterPending,"same-value setter preserves a pending request");
  for(int account:new int[]{0,1,3})check(NotificationCenter.getInstance(account).counts[1]==1,"same-value setter does not notify account "+account);
  SharedConfig.setBlockSponsoredMessagesFullEnabled(true);AndroidUtilities.flush();
  check(SharedConfig.isBlockSponsoredMessagesFullEnabled()&&MessagesController.globalPreferences.getBoolean("block_sponsored_messages_full",false),"opposite setter value is readable and persisted");
  for(int account:new int[]{0,1,3})check(NotificationCenter.getInstance(account).counts[1]==2,"opposite setter value notifies account "+account);
  setterResponse.run(sponsored(),null);AndroidUtilities.flush();
  check(!c.sponsoredMessages.containsKey(-40L)&&NotificationCenter.getInstance(0).counts[2]==0,"shared setter invalidates old request before delayed completion");
  check(NotificationCenter.getInstance(2).counts[1]==0,"subsequent global changes still skip inactive account");
  check(Utilities.globalQueue.queued>0,"remote refresh queued but never executed");
  System.out.println("PASS: "+checks+" production ad whitelist, persistence, account and sponsored callback behavior checks");
 }
}'''
}
fixtures['org/telegram/messenger/MessagesController.java'] = fixtures['org/telegram/messenger/MessagesController.java'].replace('//PRODUCTION_METHODS', '\n'.join(extract(controller, marker) for marker in (
    '    public class SponsoredMessagesInfo {',
    '    public SponsoredMessagesInfo getSponsoredMessages(',
    '    public void clearSponsoredMessagesCache()')))
fixtures['org/telegram/messenger/SharedConfig.java'] = fixtures['org/telegram/messenger/SharedConfig.java'].replace('//PRODUCTION_SETTING', '\n'.join([
    re.search(r'private static final String PREF_BLOCK_SPONSORED_MESSAGES_FULL\s*=\s*"[^"]+";', shared_config).group(0),
    extract(shared_config, '    public static boolean isBlockSponsoredMessagesFullEnabled()'),
    extract(shared_config, '    public static void setBlockSponsoredMessagesFullEnabled('),
]))

with tempfile.TemporaryDirectory(prefix='mango-v50-ad-whitelist-') as directory:
    base = Path(directory)
    sources = []
    for name, content in fixtures.items():
        path = base / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        sources.append(path)
    json_dir = sorted(SDK.glob('sources/android-*/org/json/JSONObject.java'))[-1].parent
    for original in json_dir.glob('*.java'):
        text = original.read_text()
        text = re.sub(r'^import (?:static )?(?:android\.|libcore\.).*;\n', '', text, flags=re.M)
        text = re.sub(r'@(?:UnsupportedAppUsage|SystemApi|NonNull|Nullable)(?:\([^)]*\))?', '', text)
        path = base / 'org/json' / original.name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        sources.append(path)
    sources.append(SOURCE / 'KeywordMessageFilterManager.java')
    output = base / 'classes'
    subprocess.run([str(JAVA / 'javac'), '-d', str(output)] + [str(p) for p in sources], check=True)
    subprocess.run([str(JAVA / 'java'), '-cp', str(output), 'org.telegram.messenger.AdWhitelistTest'], check=True)
