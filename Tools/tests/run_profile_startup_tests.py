#!/usr/bin/env python3
"""Run the real shared-media startup code offline, including an old-order mutation.

Only Android/controller dependencies are stubbed. Initial tab selection and the
ProfileActivity overrides are extracted from production Java on every run.
"""
from pathlib import Path
import os
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "TMessagesProj/src/main/java/org/telegram/ui"
JAVA = Path(os.environ.get(
    "JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
)) / "bin"


def method(text, marker):
    start = text.index(marker)
    end = text.index("{", start) + 1
    depth = 1
    while depth:
        if text[end] == "{":
            depth += 1
        elif text[end] == "}":
            depth -= 1
        end += 1
    return text[start:end]


layout = (SOURCE / "Components/SharedMediaLayout.java").read_text()
profile = (SOURCE / "ProfileActivity.java").read_text()
start = layout.index("        this.viewType = viewType;")
finish = layout.index("        onTabProgress(this.initialTab);", start)
finish += len("        onTabProgress(this.initialTab);")
startup = layout[start:finish]
assignment = "        this.profileActivity = parent;"
if startup.count(assignment) != 1:
    raise AssertionError("Expected exactly one profile assignment in startup code")
# Recreate the old order: its profile assignment was after tab selection and
# onTabProgress. This mutation must actually throw when selecting stories.
old_startup = startup.replace(assignment, "", 1) + "\n" + assignment
constants = layout[layout.index("    public static final int TAB_PHOTOVIDEO ="):
                   layout.index("    public static boolean isAnyStoryPageType(")]
story_helpers = "\n".join(method(layout, signature) for signature in (
    "    public static boolean isAnyStoryPageType(int type)",
    "    public static boolean isStoryAlbumPageType(int type)",
    "    private static int getStoryAlbumType(int index)",
))
profile_overrides = "\n".join(method(profile, signature) for signature in (
    "            protected boolean isStoriesView()",
    "            protected int getInitialTab()",
))


def constructor(name, body):
    return """
 static class %s extends Harness {
  %s(Case fixture) {
   myProfile = fixture.self;
   Context context = new Context();
   long did = fixture.id;
   SharedMediaPreloader preloader = new SharedMediaPreloader(fixture.media);
   int commonGroupsCount = 0;
   TLRPC.ChatFull chatInfo = fixture.chat;
   TLRPC.UserFull userInfo = fixture.full;
   int initialTab = fixture.requestedTab;
   BaseFragment parent = fixture.parent;
   Delegate delegate = new Delegate();
   int viewType = 1;
   Theme.ResourcesProvider resourcesProvider = new Theme.ResourcesProvider();
%s
  }
 }
""" % (name, name, body)


fixture = """
public class ProfileStartupRegression {
 static class Context {}
 static class Delegate {}
 static class Theme { static class ResourcesProvider {} }
 static class FlickerLoadingView {
  FlickerLoadingView(Context context) {}
  void setIsSingleCell(boolean single) {}
 }
 static class TLRPC {
  static class ProfileTab {}
  static class TL_profileTabPosts extends ProfileTab {}
  static class TL_profileTabGifts extends ProfileTab {}
  static class TL_profileTabFiles extends ProfileTab {}
  static class TL_profileTabGifs extends ProfileTab {}
  static class TL_profileTabLinks extends ProfileTab {}
  static class TL_profileTabMusic extends ProfileTab {}
  static class TL_profileTabVoice extends ProfileTab {}
  static class User { boolean bot, bot_has_main_app, bot_can_edit; }
  static class BotInfo { boolean has_preview_medias; }
  static class UserFull {
   ProfileTab main_tab;
   boolean stories_pinned_available;
   int stargifts_count;
   BotInfo bot_info;
  }
  static class ChatFull {
   ProfileTab main_tab;
   boolean stories_pinned_available;
   int stargifts_count;
  }
 }
 static class StoriesController {
  boolean hidden;
  int queries;
  long lastDialog;
  boolean isStoriesHidden(long id) { queries++; lastDialog = id; return hidden; }
 }
 static class MessagesController {
  final StoriesController stories = new StoriesController();
  TLRPC.User user;
  TLRPC.User getUser(long id) { return id > 0 ? user : null; }
  StoriesController getStoriesController() { return stories; }
 }
 static class BaseFragment {
  final MessagesController messages = new MessagesController();
  MessagesController getMessagesController() { return messages; }
 }
 static class SharedMediaPreloader {
  int topicId;
  final int[] counts;
  SharedMediaPreloader(int[] counts) { this.counts = counts; }
  int[] getLastMediaCount() { return counts; }
 }
""" + constants + story_helpers + """
 static class Harness {
  int viewType, topicId, initialTab, progressTab = -1, progressCalls;
  int[] hasMedia;
  boolean myProfile;
  Theme.ResourcesProvider resourcesProvider;
  BaseFragment profileActivity, progressParent;
  SharedMediaPreloader sharedMediaPreloader;
  Delegate delegate;
  FlickerLoadingView globalGradientView;
  void onTabProgress(int tab) {
   progressCalls++;
   progressTab = tab;
   progressParent = profileActivity;
  }
""" + profile_overrides + """
 }
 static class Case {
  final String name;
  final BaseFragment parent = new BaseFragment();
  final int[] media = new int[6];
  long id = 1234;
  boolean self;
  TLRPC.UserFull full;
  TLRPC.ChatFull chat;
  int requestedTab = -1, expectedTab, expectedQueries;
  Case(String name, int tab, int queries) {
   this.name = name; expectedTab = tab; expectedQueries = queries;
   parent.messages.user = new TLRPC.User();
  }
  Case full(boolean stories) {
   full = new TLRPC.UserFull(); full.stories_pinned_available = stories; return this;
  }
  Case hidden() { parent.messages.stories.hidden = true; return this; }
  Case self() { self = true; id = 42; return this; }
  Case chat() {
   id = -99; chat = new TLRPC.ChatFull(); chat.stories_pinned_available = true;
   return this;
  }
  Case requested(int tab) { requestedTab = tab; return this; }
  Case previews() {
   full(false); full.bot_info = new TLRPC.BotInfo();
   full.bot_info.has_preview_medias = true; parent.messages.user.bot = true;
   return this;
  }
 }
""" + constructor("FixedStartup", startup) + constructor("OldOrderStartup", old_startup) + """
 static int checks, regressions;
 static void check(boolean ok, String message) {
  checks++; if (!ok) throw new AssertionError(message);
 }
 static void verify(Case c) {
  FixedStartup current = new FixedStartup(c);
  check(current.initialTab == c.expectedTab, c.name + ": selected tab");
  check(current.profileActivity == c.parent, c.name + ": retained parent");
  check(current.progressCalls == 1 && current.progressTab == c.expectedTab,
        c.name + ": initial progress callback");
  check(current.progressParent == c.parent, c.name + ": parent ready before callback");
  StoriesController stories = c.parent.messages.stories;
  check(stories.queries == c.expectedQueries, c.name + ": visibility query count");
  if (c.expectedQueries > 0) {
   check(stories.lastDialog == c.id, c.name + ": queries correct dialog");
  }
  try {
   OldOrderStartup old = new OldOrderStartup(c);
   check(c.expectedQueries == 0, c.name + ": old order must reproduce story crash");
   check(old.initialTab == c.expectedTab, c.name + ": non-story control tab");
  } catch (NullPointerException expected) {
   check(c.expectedQueries > 0, c.name + ": only story paths crash with old order");
   check(expected.getStackTrace()[0].getClassName().endsWith("$OldOrderStartup"),
         c.name + ": NPE originates in production constructor fragment");
   regressions++;
  }
 }
 public static void main(String[] args) {
  Case[] cases = {
   new Case("user without loaded full info", TAB_PHOTOVIDEO, 0),
   new Case("user without stories", TAB_PHOTOVIDEO, 0).full(false),
   new Case("user with stories", TAB_STORIES, 1).full(true),
   new Case("user with hidden stories", TAB_PHOTOVIDEO, 1).full(true).hidden(),
   new Case("own profile without loaded full info", TAB_STORIES, 1).self(),
   new Case("own profile without stories", TAB_STORIES, 1).self().full(false),
   new Case("own profile with stories", TAB_STORIES, 1).self().full(true),
   new Case("chat with stories", TAB_STORIES, 1).chat(),
   new Case("chat with hidden stories", TAB_PHOTOVIDEO, 1).chat().hidden(),
   new Case("bot preview media", TAB_STORIES, 1).previews(),
   new Case("bot preview media bypass hiding", TAB_STORIES, 1).previews().hidden(),
   new Case("explicit archived stories", TAB_ARCHIVED_STORIES, 1).requested(TAB_ARCHIVED_STORIES),
   new Case("hidden archived stories", TAB_PHOTOVIDEO, 1).requested(TAB_ARCHIVED_STORIES).hidden(),
   new Case("explicit story album", getStoryAlbumType(7), 1).requested(getStoryAlbumType(7)),
   new Case("hidden story album", TAB_PHOTOVIDEO, 1).requested(getStoryAlbumType(7)).hidden(),
   new Case("explicit gifts", TAB_GIFTS, 0).full(true).requested(TAB_GIFTS),
   new Case("explicit saved dialogs", TAB_SAVED_DIALOGS, 0).self().requested(TAB_SAVED_DIALOGS),
   new Case("explicit common groups", TAB_COMMON_GROUPS, 0).full(true).requested(TAB_COMMON_GROUPS),
   new Case("explicit recommendations", TAB_RECOMMENDED_CHANNELS, 0).chat().requested(TAB_RECOMMENDED_CHANNELS)
  };
  for (Case c : cases) verify(c);
  Case editableBot = new Case("editable bot main app", TAB_BOT_PREVIEWS, 0).previews();
  editableBot.parent.messages.user.bot_has_main_app = true;
  editableBot.parent.messages.user.bot_can_edit = true;
  verify(editableBot);
  Case posts = new Case("posts primary tab", TAB_STORIES, 1).full(true);
  posts.full.main_tab = new TLRPC.TL_profileTabPosts();
  verify(posts);
  Case files = new Case("files primary tab", TAB_FILES, 0).full(true);
  files.full.main_tab = new TLRPC.TL_profileTabFiles(); files.media[1] = 3;
  verify(files);
  Case audio = new Case("first available media", TAB_AUDIO, 0);
  audio.media[4] = 2;
  verify(audio);
  Case uncached = new Case("uncached user with stories", TAB_STORIES, 1).full(true);
  uncached.parent.messages.user = null;
  verify(uncached);
  check(regressions > 0, "Mutation must reproduce the original NPE");
  System.out.println("PASS: " + (cases.length + 5) + " profile startup scenarios, "
      + checks + " checks; old constructor order reproduced " + regressions + " NPEs");
 }
}
"""

with tempfile.TemporaryDirectory(prefix="profile-startup-tests-") as directory:
    java = Path(directory) / "ProfileStartupRegression.java"
    java.write_text(fixture)
    subprocess.run([str(JAVA / "javac"), "-d", directory, str(java)], check=True)
    subprocess.run([str(JAVA / "java"), "-cp", directory, "ProfileStartupRegression"], check=True)
