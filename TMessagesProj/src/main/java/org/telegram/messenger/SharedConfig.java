/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.WebView;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutManagerCompat;

import org.telegram.PhoneFormat.PhoneFormat;
import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SharedConfig {
    private static final String PREF_LOCAL_DISPLAY_PHONE_PREFIX = "local_display_phone_";
    private static final String PREF_LOCAL_DISPLAY_USER_ID_PREFIX = "local_display_user_id_";
    private static final String PREF_LOCAL_DISPLAY_USERNAME_PREFIX = "local_display_username_";
    private static final String PREF_LOCAL_USER_DISPLAY_PHONE_PREFIX = "local_user_display_phone_";
    private static final String PREF_LOCAL_USER_DISPLAY_USER_ID_PREFIX = "local_user_display_user_id_";
    private static final String PREF_LOCAL_USER_DISPLAY_USERNAME_PREFIX = "local_user_display_username_";
    private static final String PREF_LOCAL_USER_REMARK_PREFIX = "local_user_remark_";
    private static final String PREF_LOCAL_USER_NAME_COLOR_PREFIX = "local_user_name_color_";
    private static final String PREF_LOCAL_USER_PREMIUM_STATUS_PREFIX = "local_user_premium_status_";
    private static final String PREF_LOCAL_USER_EMOJI_STATUS_PREFIX = "local_user_emoji_status_";
    private static final String PREF_LOCAL_USER_BIO_PREFIX = "local_user_bio_";
    private static final String PREF_LOCAL_CHAT_BIO_PREFIX = "local_chat_bio_";
    private static final String PREF_LOCAL_CHANNEL_DISPLAY_USERNAME_PREFIX = "local_channel_display_username_";
    private static final String PREF_LOCAL_CHANNEL_DISPLAY_MEMBERS_PREFIX = "local_channel_display_members_";
    private static final String PREF_VPN_PROXY_AUTO_SWITCH_ENABLED = "vpn_proxy_auto_switch_enabled";
    private static final String PREF_DOWNLOAD_SPEED_MULTIPLIER = "downloadSpeedMultiplier";
    private static final String PREF_UPLOAD_SPEED_MULTIPLIER = "uploadSpeedMultiplier";
    private static final String PREF_CUSTOM_DOWNLOAD_PATH = "customDownloadPath";
    private static final String PREF_CUSTOM_DOWNLOAD_TREE_URI = "customDownloadTreeUri";
    private static final String PREF_HIDE_CHANNEL_SEND_AS = "hideChannelSendAs";
    private static final String PREF_DEFAULT_HIGH_QUALITY_MEDIA = "defaultHighQualityMedia";
    private static final String PREF_STICKER_MESSAGE_SIZE_PERCENT = "stickerMessageSizePercent";
    private static final String PREF_TABLET_FULL_WIDTH = "tabletFullWidth";
    private static final String PREF_TABLET_WIDTH_RATIO_PERCENT = "tabletWidthRatioPercent";
    private static final String PREF_AUTO_TRANSLATE_MESSAGES = "autoTranslateMessagesEnabled";
    private static final String PREF_DIALOG_ROW_HEIGHT_PERCENT = "dialogRowHeightPercent";
    private static final String PREF_TRANSLATION_DISPLAY_MODE = "translationDisplayMode";
    private static final String PREF_AUTO_REACTION_ENABLED = "autoReactionEnabled";
    private static final String PREF_AUTO_REACTION_EMOJI = "autoReactionEmoji";
    private static final String PREF_AUTO_REACTION_TARGET = "autoReactionTarget";
    private static final String PREF_AUTO_REACTION_USERS_PREFIX = "autoReactionUsers_";
    private static final String PREF_CHANNEL_FEED_ENABLED = "channelFeedEnabled";
    private static final String PREF_CHANNEL_FEED_CUSTOM_ENTRIES = "channelFeedCustomEntries";
    private static final String PREF_UNLIMITED_CHANNEL_JOIN_ENABLED = "unlimitedChannelJoinEnabled";
    private static final String PREF_UNLIMITED_DIALOG_FILTERS_ENABLED = "unlimitedDialogFiltersEnabled";
    private static final String PREF_LOCAL_HOLD_VERIFICATION_ENABLED = "localHoldVerificationEnabled";
    private static final String PREF_CUSTOM_STAR_BALANCE = "customStarBalance";
    private static final String PREF_INPUT_BAR_TEXT = "inputBarText";
    private static final String PREF_LOCAL_VIRTUAL_GIFT_BUY_ENABLED = "localVirtualGiftBuyEnabled";
    private static final String PREF_LOCAL_VIRTUAL_GIFTS_PREFIX = "localVirtualGifts_";
    private static final String PREF_LOCAL_VIRTUAL_WORN_GIFT_PREFIX = "localVirtualWornGift_";
    private static final String PREF_SEND_MP3_AS_VOICE_ENABLED = "sendMp3AsVoiceEnabled";
    private static final String PREF_CUSTOM_VIDEO_WALLPAPER_PATH = "customVideoWallpaperPath";
    private static final String PREF_CUSTOM_VIDEO_WALLPAPER_URI = "customVideoWallpaperUri";
    private static final String PREF_BLOCK_SPONSORED_MESSAGES_FULL = "block_sponsored_messages_full";

    public static final int[] DOWNLOAD_SPEED_MULTIPLIERS = {1, 5, 10, 20, 50};
    public static final int[] UPLOAD_SPEED_MULTIPLIERS = DOWNLOAD_SPEED_MULTIPLIERS;
    public static final int[] STICKER_MESSAGE_SIZE_PERCENTS = {60, 80, 100, 120, 140, 160};
    public static final int[] TABLET_WIDTH_RATIO_PERCENTS = {25, 30, 32, 35, 40, 45, 50, 55};
    public static final int[] DIALOG_ROW_HEIGHT_PERCENTS = {80, 90, 100, 110, 120};
    public static final int[] MESSAGE_TEXT_SIZES = {12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
    public static final int TRANSLATION_DISPLAY_TRANSLATED_ONLY = 0;
    public static final int TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED = 1;
    public static final int AUTO_REACTION_TARGET_INCOMING = 0;
    public static final int AUTO_REACTION_TARGET_OUTGOING = 1;
    public static final int AUTO_REACTION_TARGET_BOTH = 2;
    public static final String CHANNEL_FEED_URL = "https://1812652903.v.123pan.cn/1812652903/GY/PDL.txt";

    /**
     * V2: Ping and check time serialized
     * V3: Proxy display name serialized
     * V4: Built-in proxy ownership serialized
     */
    private final static int PROXY_SCHEMA_V2 = 2;
    private final static int PROXY_SCHEMA_V3 = 3;
    private final static int PROXY_SCHEMA_V4 = 4;
    private final static int PROXY_CURRENT_SCHEMA_VERSION = PROXY_SCHEMA_V4;

    public final static int PASSCODE_TYPE_PIN = 0,
            PASSCODE_TYPE_PASSWORD = 1;
    private static int legacyDevicePerformanceClass = -1;

    public static boolean loopStickers() {
        return LiteMode.isEnabled(LiteMode.FLAG_ANIMATED_STICKERS_CHAT);
    }

    private static String getLocalDisplayPhoneKey(int account) {
        return PREF_LOCAL_DISPLAY_PHONE_PREFIX + account;
    }

    private static String getLocalDisplayUserIdKey(int account) {
        return PREF_LOCAL_DISPLAY_USER_ID_PREFIX + account;
    }

    private static String getLocalDisplayUsernameKey(int account) {
        return PREF_LOCAL_DISPLAY_USERNAME_PREFIX + account;
    }

    private static String getLocalUserDisplayPhoneKey(int account, long userId) {
        return PREF_LOCAL_USER_DISPLAY_PHONE_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserDisplayUserIdKey(int account, long userId) {
        return PREF_LOCAL_USER_DISPLAY_USER_ID_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserDisplayUsernameKey(int account, long userId) {
        return PREF_LOCAL_USER_DISPLAY_USERNAME_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserRemarkKey(int account, long userId) {
        return PREF_LOCAL_USER_REMARK_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserNameColorKey(int account, long userId) {
        return PREF_LOCAL_USER_NAME_COLOR_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserPremiumStatusKey(int account, long userId) {
        return PREF_LOCAL_USER_PREMIUM_STATUS_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserEmojiStatusKey(int account, long userId) {
        return PREF_LOCAL_USER_EMOJI_STATUS_PREFIX + account + "_" + userId;
    }

    private static String getLocalUserBioKey(int account, long userId) {
        return PREF_LOCAL_USER_BIO_PREFIX + account + "_" + userId;
    }

    private static String getLocalChatBioKey(int account, long chatId) {
        return PREF_LOCAL_CHAT_BIO_PREFIX + account + "_" + chatId;
    }

    private static String getLocalChannelDisplayUsernameKey(int account, long chatId) {
        return PREF_LOCAL_CHANNEL_DISPLAY_USERNAME_PREFIX + account + "_" + chatId;
    }

    private static String getLocalChannelDisplayMembersKey(int account, long chatId) {
        return PREF_LOCAL_CHANNEL_DISPLAY_MEMBERS_PREFIX + account + "_" + chatId;
    }

    private static String getLocalDisplayValue(String key) {
        return MessagesController.getGlobalMainSettings().getString(key, "");
    }

    private static void setLocalDisplayValue(String key, String value) {
        String displayValue = value == null ? "" : value.trim();
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        if (TextUtils.isEmpty(displayValue)) {
            editor.remove(key);
        } else {
            editor.putString(key, displayValue);
        }
        editor.apply();
    }

    public static String getLocalDisplayPhone(int account) {
        return getLocalDisplayValue(getLocalDisplayPhoneKey(account));
    }

    public static void setLocalDisplayPhone(int account, String phone) {
        setLocalDisplayValue(getLocalDisplayPhoneKey(account), phone);
    }

    public static void clearLocalDisplayPhone(int account) {
        setLocalDisplayPhone(account, null);
    }

    public static String getDisplayPhone(int account, TLRPC.User user) {
        String localPhone = user != null && !UserObject.isUserSelf(user) ? getLocalUserDisplayPhone(account, user.id) : getLocalDisplayPhone(account);
        if (!TextUtils.isEmpty(localPhone)) {
            return localPhone;
        }
        if (user != null && !TextUtils.isEmpty(user.phone)) {
            return PhoneFormat.getInstance().format("+" + user.phone);
        }
        return "";
    }

    public static String getLocalUserDisplayPhone(int account, long userId) {
        if (userId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalUserDisplayPhoneKey(account, userId));
    }

    public static void setLocalUserDisplayPhone(int account, long userId, String phone) {
        if (userId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalUserDisplayPhoneKey(account, userId), phone);
    }

    public static void clearLocalUserDisplayPhone(int account, long userId) {
        setLocalUserDisplayPhone(account, userId, null);
    }

    public static String getLocalDisplayUserId(int account) {
        return getLocalDisplayValue(getLocalDisplayUserIdKey(account));
    }

    public static void setLocalDisplayUserId(int account, String userId) {
        setLocalDisplayValue(getLocalDisplayUserIdKey(account), userId);
    }

    public static void clearLocalDisplayUserId(int account) {
        setLocalDisplayUserId(account, null);
    }

    public static String getDisplayUserId(int account, TLRPC.User user) {
        String localUserId = user != null && !UserObject.isUserSelf(user) ? getLocalUserDisplayUserId(account, user.id) : getLocalDisplayUserId(account);
        if (!TextUtils.isEmpty(localUserId)) {
            return localUserId;
        }
        return user != null && user.id != 0 ? String.valueOf(user.id) : "";
    }

    public static String getLocalUserDisplayUserId(int account, long userId) {
        if (userId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalUserDisplayUserIdKey(account, userId));
    }

    public static void setLocalUserDisplayUserId(int account, long userId, String displayUserId) {
        if (userId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalUserDisplayUserIdKey(account, userId), displayUserId);
    }

    public static void clearLocalUserDisplayUserId(int account, long userId) {
        setLocalUserDisplayUserId(account, userId, null);
    }

    public static String getLocalDisplayUsername(int account) {
        return getLocalDisplayValue(getLocalDisplayUsernameKey(account));
    }

    public static void setLocalDisplayUsername(int account, String username) {
        setLocalDisplayValue(getLocalDisplayUsernameKey(account), username);
    }

    public static void clearLocalDisplayUsername(int account) {
        setLocalDisplayUsername(account, null);
    }

    public static String getDisplayUsername(int account, TLRPC.User user) {
        String localUsername = user != null && !UserObject.isUserSelf(user) ? getLocalUserDisplayUsername(account, user.id) : getLocalDisplayUsername(account);
        if (!TextUtils.isEmpty(localUsername)) {
            return localUsername;
        }
        String username = UserObject.getPublicUsername(user);
        return TextUtils.isEmpty(username) ? "" : "@" + username;
    }

    public static String getLocalUserDisplayUsername(int account, long userId) {
        if (userId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalUserDisplayUsernameKey(account, userId));
    }

    public static void setLocalUserDisplayUsername(int account, long userId, String username) {
        if (userId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalUserDisplayUsernameKey(account, userId), username);
    }

    public static void clearLocalUserDisplayUsername(int account, long userId) {
        setLocalUserDisplayUsername(account, userId, null);
    }

    public static String getLocalUserRemark(int account, long userId) {
        if (userId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalUserRemarkKey(account, userId));
    }

    public static void setLocalUserRemark(int account, long userId, String remark) {
        if (userId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalUserRemarkKey(account, userId), remark);
    }

    public static void clearLocalUserRemark(int account, long userId) {
        setLocalUserRemark(account, userId, null);
    }

    public static int getLocalUserNameColor(int account, long userId) {
        if (userId == 0) {
            return 0;
        }
        return MessagesController.getGlobalMainSettings().getInt(getLocalUserNameColorKey(account, userId), 0);
    }

    public static void setLocalUserNameColor(int account, long userId, int color) {
        if (userId == 0) {
            return;
        }
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        if (color == 0) {
            editor.remove(getLocalUserNameColorKey(account, userId));
        } else {
            editor.putInt(getLocalUserNameColorKey(account, userId), color);
        }
        editor.apply();
    }

    public static void clearLocalUserNameColor(int account, long userId) {
        setLocalUserNameColor(account, userId, 0);
    }

    public static String getLocalUserBio(int account, long userId) {
        if (userId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalUserBioKey(account, userId));
    }

    public static void setLocalUserBio(int account, long userId, String bio) {
        if (userId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalUserBioKey(account, userId), bio);
    }

    public static void clearLocalUserBio(int account, long userId) {
        setLocalUserBio(account, userId, null);
    }

    public static String getDisplayUserBio(int account, long userId, String bio) {
        String localBio = getLocalUserBio(account, userId);
        return !TextUtils.isEmpty(localBio) ? localBio : bio;
    }

    public static boolean isLocalUserPremiumStatusEnabled(int account, long userId) {
        if (userId == 0) {
            return false;
        }
        return MessagesController.getGlobalMainSettings().getBoolean(getLocalUserPremiumStatusKey(account, userId), false);
    }

    public static long getLocalUserEmojiStatusDocumentId(int account, long userId) {
        if (userId == 0) {
            return 0;
        }
        return MessagesController.getGlobalMainSettings().getLong(getLocalUserEmojiStatusKey(account, userId), 0);
    }

    public static TLRPC.EmojiStatus getLocalUserEmojiStatus(int account, long userId) {
        long documentId = getLocalUserEmojiStatusDocumentId(account, userId);
        if (documentId == 0) {
            return null;
        }
        TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
        status.document_id = documentId;
        return status;
    }

    public static TLRPC.EmojiStatus getDisplayUserEmojiStatus(int account, TLRPC.User user) {
        if (user == null) {
            return null;
        }
        TLRPC.EmojiStatus localGiftStatus = getLocalVirtualGiftEmojiStatus(account, user.id);
        if (localGiftStatus != null) {
            return localGiftStatus;
        }
        TLRPC.EmojiStatus localStatus = getLocalUserEmojiStatus(account, user.id);
        return localStatus != null ? localStatus : user.emoji_status;
    }

    public static TLRPC.EmojiStatus getDisplayChatEmojiStatus(int account, TLRPC.Chat chat) {
        if (chat == null) {
            return null;
        }
        TLRPC.EmojiStatus localGiftStatus = getLocalVirtualGiftEmojiStatus(account, -chat.id);
        return localGiftStatus != null ? localGiftStatus : chat.emoji_status;
    }

    public static long getDisplayProfileEmojiId(TLRPC.EmojiStatus emojiStatus, long fallback) {
        if (emojiStatus instanceof TLRPC.TL_emojiStatusCollectible) {
            return ((TLRPC.TL_emojiStatusCollectible) emojiStatus).pattern_document_id;
        }
        return fallback;
    }

    public static void setLocalUserPremiumStatus(int account, long userId, long emojiStatusDocumentId) {
        if (userId == 0) {
            return;
        }
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean(getLocalUserPremiumStatusKey(account, userId), true);
        if (emojiStatusDocumentId != 0) {
            editor.putLong(getLocalUserEmojiStatusKey(account, userId), emojiStatusDocumentId);
        } else {
            editor.remove(getLocalUserEmojiStatusKey(account, userId));
        }
        editor.apply();
    }

    public static void clearLocalUserPremiumStatus(int account, long userId) {
        if (userId == 0) {
            return;
        }
        MessagesController.getGlobalMainSettings()
                .edit()
                .remove(getLocalUserPremiumStatusKey(account, userId))
                .remove(getLocalUserEmojiStatusKey(account, userId))
                .apply();
    }

    public static String getDisplayUserName(int account, TLRPC.User user) {
        if (user == null) {
            return "";
        }
        String remark = getLocalUserRemark(account, user.id);
        if (!TextUtils.isEmpty(remark)) {
            return remark;
        }
        return UserObject.getUserName(user);
    }

    public static String getLocalChatBio(int account, long chatId) {
        if (chatId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalChatBioKey(account, chatId));
    }

    public static void setLocalChatBio(int account, long chatId, String bio) {
        if (chatId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalChatBioKey(account, chatId), bio);
    }

    public static void clearLocalChatBio(int account, long chatId) {
        setLocalChatBio(account, chatId, null);
    }

    public static String getDisplayChatBio(int account, long chatId, String bio) {
        String localBio = getLocalChatBio(account, chatId);
        return !TextUtils.isEmpty(localBio) ? localBio : bio;
    }

    public static String getLocalChannelDisplayUsername(int account, long chatId) {
        if (chatId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalChannelDisplayUsernameKey(account, chatId));
    }

    public static void setLocalChannelDisplayUsername(int account, long chatId, String username) {
        if (chatId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalChannelDisplayUsernameKey(account, chatId), username);
    }

    public static void clearLocalChannelDisplayUsername(int account, long chatId) {
        setLocalChannelDisplayUsername(account, chatId, null);
    }

    public static String getDisplayChatUsername(int account, TLRPC.Chat chat) {
        if (chat == null) {
            return "";
        }
        String localUsername = getLocalChannelDisplayUsername(account, chat.id);
        if (!TextUtils.isEmpty(localUsername)) {
            return localUsername.startsWith("@") || localUsername.startsWith("http://") || localUsername.startsWith("https://") ? localUsername : "@" + localUsername;
        }
        String username = ChatObject.getPublicUsername(chat);
        return TextUtils.isEmpty(username) ? "" : "@" + username;
    }

    public static String getLocalChannelDisplayMembers(int account, long chatId) {
        if (chatId == 0) {
            return "";
        }
        return getLocalDisplayValue(getLocalChannelDisplayMembersKey(account, chatId));
    }

    public static String getLocalChannelDisplayMembers(int account, TLRPC.Chat chat) {
        if (!ChatObject.isChannelAndNotMegaGroup(chat) || ChatObject.isMonoForum(chat)) {
            return "";
        }
        return getLocalChannelDisplayMembers(account, chat.id);
    }

    public static void setLocalChannelDisplayMembers(int account, long chatId, String members) {
        if (chatId == 0) {
            return;
        }
        setLocalDisplayValue(getLocalChannelDisplayMembersKey(account, chatId), members);
    }

    public static void clearLocalChannelDisplayMembers(int account, long chatId) {
        setLocalChannelDisplayMembers(account, chatId, null);
    }

    public static boolean hasLocalChannelDisplayOverrides(int account, long chatId) {
        return !TextUtils.isEmpty(getLocalChannelDisplayUsername(account, chatId))
                || !TextUtils.isEmpty(getLocalChannelDisplayMembers(account, chatId));
    }

    public static String getDisplayChatTitle(int account, TLRPC.Chat chat) {
        if (chat == null) {
            return "";
        }
        return TextUtils.isEmpty(chat.title) ? "" : chat.title;
    }

    public static boolean isVpnProxyAutoSwitchEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(PREF_VPN_PROXY_AUTO_SWITCH_ENABLED, true);
    }

    public static void setVpnProxyAutoSwitchEnabled(boolean enabled) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(PREF_VPN_PROXY_AUTO_SWITCH_ENABLED, enabled).apply();
    }

    public static boolean isBlockSponsoredMessagesFullEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(PREF_BLOCK_SPONSORED_MESSAGES_FULL, false);
    }

    public static void setBlockSponsoredMessagesFullEnabled(boolean enabled) {
        if (isBlockSponsoredMessagesFullEnabled() == enabled) {
            return;
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean(PREF_BLOCK_SPONSORED_MESSAGES_FULL, enabled).apply();
        AndroidUtilities.runOnUIThread(() -> {
            for (int account : UserConfig.getActivatedAccounts()) {
                MessagesController.getInstance(account).clearSponsoredMessagesCache();
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.keywordFilterSettingsUpdated);
            }
        });
    }

    private static int sanitizeSpeedMultiplier(int multiplier, int[] values) {
        for (int value : values) {
            if (value == multiplier) {
                return value;
            }
        }
        return 1;
    }

    public static int sanitizeDownloadSpeedMultiplier(int multiplier) {
        return sanitizeSpeedMultiplier(multiplier, DOWNLOAD_SPEED_MULTIPLIERS);
    }

    public static int getDownloadSpeedMultiplier() {
        return sanitizeDownloadSpeedMultiplier(downloadSpeedMultiplier);
    }

    public static void setDownloadSpeedMultiplier(int multiplier) {
        downloadSpeedMultiplier = sanitizeDownloadSpeedMultiplier(multiplier);
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_DOWNLOAD_SPEED_MULTIPLIER, downloadSpeedMultiplier)
                    .apply();
        }
    }

    public static String getDownloadSpeedMultiplierText() {
        int multiplier = getDownloadSpeedMultiplier();
        if (multiplier <= 1) {
            return LocaleController.getString(R.string.DownloadSpeedBoostNormal);
        }
        return LocaleController.formatString(R.string.DownloadSpeedBoostValue, multiplier);
    }

    public static int sanitizeUploadSpeedMultiplier(int multiplier) {
        return sanitizeSpeedMultiplier(multiplier, UPLOAD_SPEED_MULTIPLIERS);
    }

    public static int getUploadSpeedMultiplier() {
        return sanitizeUploadSpeedMultiplier(uploadSpeedMultiplier);
    }

    public static void setUploadSpeedMultiplier(int multiplier) {
        uploadSpeedMultiplier = sanitizeUploadSpeedMultiplier(multiplier);
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_UPLOAD_SPEED_MULTIPLIER, uploadSpeedMultiplier)
                    .apply();
        }
    }

    public static String getUploadSpeedMultiplierText() {
        int multiplier = getUploadSpeedMultiplier();
        if (multiplier <= 1) {
            return LocaleController.getString(R.string.DownloadSpeedBoostNormal);
        }
        return LocaleController.formatString(R.string.DownloadSpeedBoostValue, multiplier);
    }

    public static String getCustomDownloadPath() {
        return customDownloadPath;
    }

    public static String getCustomDownloadTreeUri() {
        return customDownloadTreeUri;
    }

    public static void setCustomDownloadPath(String path) {
        setCustomDownloadPath(path, null);
    }

    public static void setCustomDownloadPath(String path, String treeUri) {
        customDownloadPath = path == null ? "" : path.trim();
        customDownloadTreeUri = treeUri == null ? "" : treeUri.trim();
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_CUSTOM_DOWNLOAD_PATH, customDownloadPath)
                    .putString(PREF_CUSTOM_DOWNLOAD_TREE_URI, customDownloadTreeUri)
                    .apply();
        }
    }

	public static boolean hasCustomDownloadPath() {
	    return !TextUtils.isEmpty(customDownloadPath) || !TextUtils.isEmpty(customDownloadTreeUri);
	}

	public static String getCustomDownloadPathText() {
	    if (!hasCustomDownloadPath()) {
	        return LocaleController.getString(R.string.CustomDownloadPathDefault);
	    }
	    if (!TextUtils.isEmpty(customDownloadPath)) {
	        return customDownloadPath;
	    }
	    return customDownloadTreeUri;
	}

    public static boolean isHideChannelSendAsEnabled() {
        return hideChannelSendAs;
    }

    public static void setHideChannelSendAsEnabled(boolean enabled) {
        hideChannelSendAs = enabled;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_HIDE_CHANNEL_SEND_AS, enabled)
                    .apply();
        }
    }

    public static boolean isDefaultHighQualityMediaEnabled() {
        return defaultHighQualityMedia;
    }

    public static void setDefaultHighQualityMediaEnabled(boolean enabled) {
        defaultHighQualityMedia = enabled;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_DEFAULT_HIGH_QUALITY_MEDIA, enabled)
                    .apply();
        }
    }

    private static int sanitizeStickerMessageSizePercent(int percent) {
        for (int value : STICKER_MESSAGE_SIZE_PERCENTS) {
            if (value == percent) {
                return value;
            }
        }
        return 100;
    }

    private static int sanitizeTabletWidthRatioPercent(int percent) {
        if (percent < 25) {
            return 25;
        }
        if (percent > 55) {
            return 55;
        }
        return percent;
    }

    private static int sanitizeDialogRowHeightPercent(int percent) {
        return sanitizePercent(percent, DIALOG_ROW_HEIGHT_PERCENTS, 100);
    }

    private static int sanitizePercent(int percent, int[] values, int defaultValue) {
        for (int value : values) {
            if (value == percent) {
                return value;
            }
        }
        return defaultValue;
    }

    public static int getStickerMessageSizePercent() {
        return sanitizeStickerMessageSizePercent(stickerMessageSizePercent);
    }

    public static float getStickerMessageSizeScale() {
        return getStickerMessageSizePercent() / 100.0f;
    }

    public static String getStickerMessageSizeText() {
        return LocaleController.formatString(R.string.StickerMessageSizeValue, getStickerMessageSizePercent());
    }

    public static void setStickerMessageSizePercent(int percent) {
        stickerMessageSizePercent = sanitizeStickerMessageSizePercent(percent);
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_STICKER_MESSAGE_SIZE_PERCENT, stickerMessageSizePercent)
                    .apply();
        }
    }

    public static void setMessageTextSize(int size) {
        fontSize = Math.max(12, Math.min(30, size));
        fontSizeIsDefault = false;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                    .edit()
                    .putInt("fons_size", fontSize)
                    .apply();
        }
        Theme.createCommonMessageResources();
    }

    public static boolean isTabletFullWidthEnabled() {
        return tabletFullWidth;
    }

    public static void setTabletFullWidthEnabled(boolean enabled) {
        tabletFullWidth = enabled;
        saveExtensionPreference(PREF_TABLET_FULL_WIDTH, enabled);
    }

    public static int getTabletWidthRatioPercent() {
        return sanitizeTabletWidthRatioPercent(tabletWidthRatioPercent);
    }

    public static float getTabletWidthRatio() {
        return getTabletWidthRatioPercent() / 100.0f;
    }

    public static String getTabletWidthRatioText() {
        return LocaleController.formatString(R.string.TabletWidthRatioValue, getTabletWidthRatioPercent());
    }

    public static void setTabletWidthRatioPercent(int percent) {
        tabletWidthRatioPercent = sanitizeTabletWidthRatioPercent(percent);
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_TABLET_WIDTH_RATIO_PERCENT, tabletWidthRatioPercent)
                    .apply();
        }
    }

    public static boolean isAutoTranslateMessagesEnabled() {
        // Automatic translation is intentionally disabled in this custom
        // client. Manual translation remains available.
        return false;
    }

    public static void setAutoTranslateMessagesEnabled(boolean enabled) {
        autoTranslateMessages = false;
        saveExtensionPreference(PREF_AUTO_TRANSLATE_MESSAGES, false);
    }

    public static int getDialogRowHeightPercent() {
        return sanitizeDialogRowHeightPercent(dialogRowHeightPercent);
    }

    public static float getDialogRowHeightScale() {
        return getDialogRowHeightPercent() / 100.0f;
    }

    public static String getDialogRowHeightText() {
        return LocaleController.formatString(R.string.DialogRowHeightValue, getDialogRowHeightPercent());
    }

    public static void setDialogRowHeightPercent(int percent) {
        dialogRowHeightPercent = sanitizeDialogRowHeightPercent(percent);
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_DIALOG_ROW_HEIGHT_PERCENT, dialogRowHeightPercent)
                    .apply();
        }
    }

    public static int getTranslationDisplayMode() {
        return translationDisplayMode == TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED ? TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED : TRANSLATION_DISPLAY_TRANSLATED_ONLY;
    }

    public static boolean isTranslationOriginalAndTranslated() {
        return getTranslationDisplayMode() == TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED;
    }

    public static String getTranslationDisplayModeText() {
        return LocaleController.getString(isTranslationOriginalAndTranslated() ? R.string.TranslationDisplayOriginalAndTranslated : R.string.TranslationDisplayTranslatedOnly);
    }

    public static void setTranslationDisplayMode(int mode) {
        translationDisplayMode = mode == TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED ? TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED : TRANSLATION_DISPLAY_TRANSLATED_ONLY;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_TRANSLATION_DISPLAY_MODE, translationDisplayMode)
                    .apply();
        }
    }

    public static boolean isAutoReactionEnabled() {
        return autoReactionEnabled;
    }

    public static void setAutoReactionEnabled(boolean enabled) {
        autoReactionEnabled = enabled;
        saveExtensionPreference(PREF_AUTO_REACTION_ENABLED, enabled);
    }

    public static String getAutoReactionEmoji() {
        return TextUtils.isEmpty(autoReactionEmoji) ? "\uD83D\uDC4D" : autoReactionEmoji;
    }

    public static void setAutoReactionEmoji(String emoji) {
        autoReactionEmoji = TextUtils.isEmpty(emoji) ? "\uD83D\uDC4D" : emoji.trim();
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_AUTO_REACTION_EMOJI, autoReactionEmoji)
                    .apply();
        }
    }

    public static int getAutoReactionTarget() {
        if (autoReactionTarget < AUTO_REACTION_TARGET_INCOMING || autoReactionTarget > AUTO_REACTION_TARGET_BOTH) {
            return AUTO_REACTION_TARGET_INCOMING;
        }
        return autoReactionTarget;
    }

    public static void setAutoReactionTarget(int target) {
        if (target < AUTO_REACTION_TARGET_INCOMING || target > AUTO_REACTION_TARGET_BOTH) {
            target = AUTO_REACTION_TARGET_INCOMING;
        }
        autoReactionTarget = target;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_AUTO_REACTION_TARGET, autoReactionTarget)
                    .apply();
        }
    }

    public static HashSet<Long> getAutoReactionUserIds(int account) {
        HashSet<Long> result = new HashSet<>();
        if (ApplicationLoader.applicationContext == null) {
            return result;
        }
        Set<String> values = ApplicationLoader.applicationContext
                .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                .getStringSet(PREF_AUTO_REACTION_USERS_PREFIX + account, null);
        if (values == null) {
            return result;
        }
        for (String value : values) {
            try {
                long userId = Long.parseLong(value);
                if (userId > 0) {
                    result.add(userId);
                }
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    public static void setAutoReactionUserIds(int account, Collection<Long> userIds) {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        HashSet<String> values = new HashSet<>();
        if (userIds != null) {
            for (Long userId : userIds) {
                if (userId != null && userId > 0) {
                    values.add(String.valueOf(userId));
                }
            }
        }
        ApplicationLoader.applicationContext
                .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                .edit()
                .putStringSet(PREF_AUTO_REACTION_USERS_PREFIX + account, values)
                .apply();
    }

    public static boolean isAutoReactionUserAllowed(int account, long userId) {
        HashSet<Long> selectedUsers = getAutoReactionUserIds(account);
        return selectedUsers.isEmpty() || selectedUsers.contains(userId);
    }

    public static boolean isChannelFeedEnabled() {
        return channelFeedEnabled;
    }

    public static void setChannelFeedEnabled(boolean enabled) {
        channelFeedEnabled = enabled;
        saveExtensionPreference(PREF_CHANNEL_FEED_ENABLED, enabled);
    }

    public static ArrayList<String> getCustomChannelFeedEntries() {
        ArrayList<String> entries = new ArrayList<>();
        if (TextUtils.isEmpty(channelFeedCustomEntries)) {
            return entries;
        }
        String[] lines = channelFeedCustomEntries.split("\\n");
        HashSet<String> seen = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (TextUtils.isEmpty(line)) {
                continue;
            }
            String key = line.toLowerCase(Locale.US);
            if (seen.add(key)) {
                entries.add(line);
            }
        }
        return entries;
    }

    public static boolean addCustomChannelFeedEntry(String entry) {
        String value = sanitizeChannelFeedEntry(entry);
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        ArrayList<String> entries = getCustomChannelFeedEntries();
        String key = value.toLowerCase(Locale.US);
        for (int i = 0; i < entries.size(); i++) {
            if (key.equals(entries.get(i).toLowerCase(Locale.US))) {
                return false;
            }
        }
        entries.add(value);
        saveCustomChannelFeedEntries(entries);
        return true;
    }

    public static boolean removeCustomChannelFeedEntry(String entry) {
        String value = sanitizeChannelFeedEntry(entry);
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        ArrayList<String> entries = getCustomChannelFeedEntries();
        String key = value.toLowerCase(Locale.US);
        boolean removed = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (key.equals(entries.get(i).toLowerCase(Locale.US))) {
                entries.remove(i);
                removed = true;
            }
        }
        if (removed) {
            saveCustomChannelFeedEntries(entries);
        }
        return removed;
    }

    private static String sanitizeChannelFeedEntry(String entry) {
        if (entry == null) {
            return "";
        }
        return entry.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static void saveCustomChannelFeedEntries(ArrayList<String> entries) {
        StringBuilder builder = new StringBuilder();
        if (entries != null) {
            HashSet<String> seen = new HashSet<>();
            for (int i = 0; i < entries.size(); i++) {
                String entry = sanitizeChannelFeedEntry(entries.get(i));
                if (TextUtils.isEmpty(entry)) {
                    continue;
                }
                String key = entry.toLowerCase(Locale.US);
                if (!seen.add(key)) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(entry);
            }
        }
        channelFeedCustomEntries = builder.toString();
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_CHANNEL_FEED_CUSTOM_ENTRIES, channelFeedCustomEntries)
                    .apply();
        }
    }

    public static boolean isUnlimitedChannelJoinEnabled() {
        return unlimitedChannelJoinEnabled;
    }

    public static void setUnlimitedChannelJoinEnabled(boolean enabled) {
        unlimitedChannelJoinEnabled = enabled;
        saveExtensionPreference(PREF_UNLIMITED_CHANNEL_JOIN_ENABLED, enabled);
    }

    public static boolean isUnlimitedDialogFiltersEnabled() {
        return unlimitedDialogFiltersEnabled;
    }

    public static void setUnlimitedDialogFiltersEnabled(boolean enabled) {
        unlimitedDialogFiltersEnabled = enabled;
        saveExtensionPreference(PREF_UNLIMITED_DIALOG_FILTERS_ENABLED, enabled);
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (UserConfig.getInstance(account).isClientActivated()) {
                MessagesController.getInstance(account).reloadDialogFiltersLimits();
            }
        }
    }

    public static boolean isLocalHoldVerificationEnabled() {
        return localHoldVerificationEnabled;
    }

    public static void setLocalHoldVerificationEnabled(boolean enabled) {
        localHoldVerificationEnabled = enabled;
        saveExtensionPreference(PREF_LOCAL_HOLD_VERIFICATION_ENABLED, enabled);
        if (ApplicationLoader.applicationContext != null) {
            for (int account : UserConfig.getActivatedAccounts()) {
                if (UserConfig.getInstance(account).isClientActivated()) {
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, 0);
                }
            }
        }
    }

    public static boolean isSendMp3AsVoiceEnabled() {
        return sendMp3AsVoiceEnabled;
    }

    public static void setSendMp3AsVoiceEnabled(boolean enabled) {
        sendMp3AsVoiceEnabled = enabled;
        saveExtensionPreference(PREF_SEND_MP3_AS_VOICE_ENABLED, enabled);
    }

    public static String getCustomVideoWallpaperPath() {
        return customVideoWallpaperPath;
    }

    public static String getCustomVideoWallpaperUri() {
        return customVideoWallpaperUri;
    }

    public static boolean hasCustomVideoWallpaper() {
        return !TextUtils.isEmpty(customVideoWallpaperUri) || !TextUtils.isEmpty(customVideoWallpaperPath);
    }

    public static String getCustomVideoWallpaperSource() {
        return !TextUtils.isEmpty(customVideoWallpaperUri) ? customVideoWallpaperUri : customVideoWallpaperPath;
    }

    public static String getCustomVideoWallpaperText() {
        if (!hasCustomVideoWallpaper()) {
            return LocaleController.getString(R.string.CustomVideoWallpaperDefault);
        }
        return !TextUtils.isEmpty(customVideoWallpaperPath) ? customVideoWallpaperPath : customVideoWallpaperUri;
    }

    public static void setCustomVideoWallpaper(String path, String uri) {
        customVideoWallpaperPath = path == null ? "" : path.trim();
        customVideoWallpaperUri = uri == null ? "" : uri.trim();
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_CUSTOM_VIDEO_WALLPAPER_PATH, customVideoWallpaperPath)
                    .putString(PREF_CUSTOM_VIDEO_WALLPAPER_URI, customVideoWallpaperUri)
                    .apply();
        }
    }

    public static boolean hasCustomStarBalance() {
        return customStarBalance >= 0;
    }

    public static long getCustomStarBalance() {
        return customStarBalance;
    }

    public static void setCustomStarBalance(long balance) {
        customStarBalance = balance < 0 ? -1 : balance;
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_CUSTOM_STAR_BALANCE, customStarBalance)
                    .apply();
        }
    }

    public static String getInputBarText() {
        return inputBarText == null ? "" : inputBarText;
    }

    public static void setInputBarText(String text) {
        inputBarText = text == null ? "" : text.trim();
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_INPUT_BAR_TEXT, inputBarText)
                    .apply();
        }
    }

    public static boolean isLocalVirtualGiftBuyEnabled() {
        return localVirtualGiftBuyEnabled;
    }

    public static void setLocalVirtualGiftBuyEnabled(boolean enabled) {
        localVirtualGiftBuyEnabled = enabled;
        saveExtensionPreference(PREF_LOCAL_VIRTUAL_GIFT_BUY_ENABLED, enabled);
    }

    private static String getLocalVirtualGiftsKey(int account, long dialogId) {
        return PREF_LOCAL_VIRTUAL_GIFTS_PREFIX + account + "_" + dialogId;
    }

    private static String getLocalVirtualWornGiftKey(int account, long dialogId) {
        return PREF_LOCAL_VIRTUAL_WORN_GIFT_PREFIX + account + "_" + dialogId;
    }

    public static boolean hasLocalVirtualGifts(int account, long dialogId) {
        return getLocalVirtualGiftsCount(account, dialogId) > 0;
    }

    public static int getLocalVirtualGiftsCount(int account, long dialogId) {
        return getLocalVirtualGifts(account, dialogId).size();
    }

    public static int getLocalVirtualGiftsCount(int account) {
        if (ApplicationLoader.applicationContext == null) {
            return 0;
        }
        String prefix = PREF_LOCAL_VIRTUAL_GIFTS_PREFIX + account + "_";
        int count = 0;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith(prefix)) {
                count += getLocalVirtualGiftsFromKey(key).size();
            }
        }
        return count;
    }

    public static ArrayList<TL_stars.SavedStarGift> getLocalVirtualGifts(int account, long dialogId) {
        return getLocalVirtualGiftsFromKey(getLocalVirtualGiftsKey(account, dialogId));
    }

    private static ArrayList<TL_stars.SavedStarGift> getLocalVirtualGiftsFromKey(String key) {
        ArrayList<TL_stars.SavedStarGift> gifts = new ArrayList<>();
        if (ApplicationLoader.applicationContext == null) {
            return gifts;
        }
        String value = ApplicationLoader.applicationContext
                .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                .getString(key, "");
        if (TextUtils.isEmpty(value)) {
            return gifts;
        }
        SerializedData data = null;
        try {
            data = new SerializedData(Base64.decode(value, Base64.DEFAULT));
            int version = data.readInt32(false);
            if (version != 1) {
                return gifts;
            }
            int count = Math.max(0, Math.min(100, data.readInt32(false)));
            for (int i = 0; i < count; i++) {
                TL_stars.SavedStarGift gift = TL_stars.SavedStarGift.TLdeserialize(data, data.readInt32(false), false);
                if (gift != null && gift.gift != null) {
                    gifts.add(gift);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.cleanup();
            }
        }
        return gifts;
    }

    public static TL_stars.SavedStarGift addLocalVirtualGift(int account, long dialogId, TL_stars.StarGift gift, TLRPC.TL_textWithEntities message, boolean nameHidden) {
        if (gift == null || ApplicationLoader.applicationContext == null) {
            return null;
        }
        TL_stars.SavedStarGift savedGift = createLocalVirtualGift(account, dialogId, gift, message, nameHidden);
        if (savedGift == null) {
            return null;
        }
        ArrayList<TL_stars.SavedStarGift> gifts = getLocalVirtualGifts(account, dialogId);
        gifts.add(0, savedGift);
        while (gifts.size() > 50) {
            gifts.remove(gifts.size() - 1);
        }
        saveLocalVirtualGifts(account, dialogId, gifts);
        return savedGift;
    }

    public static void updateLocalVirtualGift(int account, long dialogId, TL_stars.SavedStarGift updatedGift) {
        if (!isLocalVirtualStarGift(updatedGift) || ApplicationLoader.applicationContext == null) {
            return;
        }
        ArrayList<TL_stars.SavedStarGift> gifts = getLocalVirtualGifts(account, dialogId);
        for (int i = 0; i < gifts.size(); i++) {
            TL_stars.SavedStarGift gift = gifts.get(i);
            if (isLocalVirtualStarGift(gift) && gift.saved_id == updatedGift.saved_id) {
                gifts.set(i, updatedGift);
                saveLocalVirtualGifts(account, dialogId, gifts);
                return;
            }
        }
    }

    public static boolean deleteLocalVirtualGift(int account, long dialogId, long savedId) {
        if (savedId >= 0 || ApplicationLoader.applicationContext == null) {
            return false;
        }
        ArrayList<TL_stars.SavedStarGift> gifts = getLocalVirtualGifts(account, dialogId);
        boolean removed = false;
        for (int i = gifts.size() - 1; i >= 0; i--) {
            TL_stars.SavedStarGift gift = gifts.get(i);
            if (isLocalVirtualStarGift(gift) && gift.saved_id == savedId) {
                gifts.remove(i);
                removed = true;
            }
        }
        if (!removed) {
            return false;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        String wornKey = getLocalVirtualWornGiftKey(account, dialogId);
        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.getLong(wornKey, 0) == savedId) {
            editor.remove(wornKey);
        }
        saveLocalVirtualGifts(account, dialogId, gifts, editor);
        editor.apply();
        return true;
    }

    public static void setLocalVirtualGiftWorn(int account, long dialogId, TL_stars.SavedStarGift gift, boolean worn) {
        if (!isLocalVirtualStarGift(gift) || ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String key = getLocalVirtualWornGiftKey(account, dialogId);
        if (worn) {
            editor.putLong(key, gift.saved_id);
        } else if (preferences.getLong(key, 0) == gift.saved_id) {
            editor.remove(key);
        }
        editor.apply();
    }

    public static boolean isLocalVirtualGiftWorn(int account, long dialogId, long giftId) {
        TL_stars.SavedStarGift gift = getLocalVirtualWornGift(account, dialogId);
        return gift != null && gift.gift != null && gift.gift.id == giftId;
    }

    public static TL_stars.SavedStarGift getLocalVirtualWornGift(int account, long dialogId) {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        String key = getLocalVirtualWornGiftKey(account, dialogId);
        long savedId = preferences.getLong(key, 0);
        if (savedId == 0) {
            return null;
        }
        ArrayList<TL_stars.SavedStarGift> gifts = getLocalVirtualGifts(account, dialogId);
        for (int i = 0; i < gifts.size(); i++) {
            TL_stars.SavedStarGift gift = gifts.get(i);
            if (isLocalVirtualStarGift(gift) && gift.saved_id == savedId) {
                return gift;
            }
        }
        preferences.edit().remove(key).apply();
        return null;
    }

    public static TLRPC.EmojiStatus getLocalVirtualGiftEmojiStatus(int account, long dialogId) {
        TL_stars.SavedStarGift gift = getLocalVirtualWornGift(account, dialogId);
        if (gift != null && gift.gift instanceof TL_stars.TL_starGiftUnique) {
            return MessagesController.emojiStatusCollectibleFromGift((TL_stars.TL_starGiftUnique) gift.gift);
        }
        return null;
    }

    public static void clearLocalVirtualGifts(int account) {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        String prefix = PREF_LOCAL_VIRTUAL_GIFTS_PREFIX + account + "_";
        String wornPrefix = PREF_LOCAL_VIRTUAL_WORN_GIFT_PREFIX + account + "_";
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith(prefix) || key.startsWith(wornPrefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public static boolean isLocalVirtualStarGift(TL_stars.SavedStarGift gift) {
        return gift != null && (gift.flags & 2048) != 0 && gift.saved_id < 0;
    }

    private static TL_stars.SavedStarGift createLocalVirtualGift(int account, long dialogId, TL_stars.StarGift gift, TLRPC.TL_textWithEntities message, boolean nameHidden) {
        TL_stars.StarGift giftCopy = cloneStarGift(gift);
        if (giftCopy == null) {
            return null;
        }
        if (giftCopy instanceof TL_stars.TL_starGiftUnique) {
            giftCopy.owner_id = getPeerForDialog(account, dialogId);
            giftCopy.flags |= 1;
            giftCopy.resell_amount = null;
            giftCopy.resale_ton_only = false;
        }

        TL_stars.TL_savedStarGift savedGift = new TL_stars.TL_savedStarGift();
        savedGift.flags = 2 | 2048;
        savedGift.from_id = getPeerForDialog(account, UserConfig.getInstance(account).getClientUserId());
        savedGift.date = ConnectionsManager.getInstance(account).getCurrentTime();
        savedGift.gift = giftCopy;
        savedGift.saved_id = -Math.max(1, System.currentTimeMillis());
        savedGift.convert_stars = 0;
        savedGift.name_hidden = nameHidden;
        savedGift.can_upgrade = false;
        savedGift.unsaved = false;
        savedGift.refunded = false;
        savedGift.pinned_to_top = false;
        savedGift.gift_num = gift.num;
        if (message != null && !TextUtils.isEmpty(message.text)) {
            savedGift.flags |= 4;
            savedGift.message = cloneTextWithEntities(message);
        }
        return savedGift;
    }

    private static TLRPC.Peer getPeerForDialog(int account, long dialogId) {
        TLRPC.InputPeer inputPeer = MessagesController.getInstance(account).getInputPeer(dialogId);
        TLRPC.Peer peer = MessagesController.getPeerFromInputPeer(inputPeer);
        if (peer == null || DialogObject.getPeerDialogId(peer) == 0) {
            TLRPC.TL_peerUser user = new TLRPC.TL_peerUser();
            user.user_id = dialogId > 0 ? dialogId : UserConfig.getInstance(account).getClientUserId();
            return user;
        }
        return peer;
    }

    private static TL_stars.StarGift cloneStarGift(TL_stars.StarGift gift) {
        SerializedData data = null;
        SerializedData input = null;
        try {
            data = new SerializedData(gift.getObjectSize());
            gift.serializeToStream(data);
            input = new SerializedData(data.toByteArray());
            return TL_stars.StarGift.TLdeserialize(input, input.readInt32(false), false);
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.cleanup();
            }
            if (input != null) {
                input.cleanup();
            }
        }
        return null;
    }

    private static TLRPC.TL_textWithEntities cloneTextWithEntities(TLRPC.TL_textWithEntities text) {
        SerializedData data = null;
        SerializedData input = null;
        try {
            data = new SerializedData(text.getObjectSize());
            text.serializeToStream(data);
            input = new SerializedData(data.toByteArray());
            return TLRPC.TL_textWithEntities.TLdeserialize(input, input.readInt32(false), false);
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.cleanup();
            }
            if (input != null) {
                input.cleanup();
            }
        }
        return text;
    }

    private static void saveLocalVirtualGifts(int account, long dialogId, ArrayList<TL_stars.SavedStarGift> gifts) {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext
                .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                .edit();
        saveLocalVirtualGifts(account, dialogId, gifts, editor);
        editor.apply();
    }

    private static void saveLocalVirtualGifts(int account, long dialogId, ArrayList<TL_stars.SavedStarGift> gifts, SharedPreferences.Editor editor) {
        if (ApplicationLoader.applicationContext == null || editor == null) {
            return;
        }
        SerializedData data = null;
        try {
            data = new SerializedData();
            data.writeInt32(1);
            data.writeInt32(gifts.size());
            for (int i = 0; i < gifts.size(); i++) {
                gifts.get(i).serializeToStream(data);
            }
            if (gifts.isEmpty()) {
                editor.remove(getLocalVirtualGiftsKey(account, dialogId));
            } else {
                editor.putString(getLocalVirtualGiftsKey(account, dialogId), Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP));
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.cleanup();
            }
        }
    }

    private static void saveExtensionPreference(String key, boolean value) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(key, value)
                    .apply();
        }
    }

    public static int getDownloadQueueMaxActiveOperations(int baseMax) {
        return DownloadRequestPolicy.getMaxActiveDownloads(baseMax);
    }

    public static int getUploadQueueMaxActiveOperations(int baseMax) {
        int multiplier = getUploadSpeedMultiplier();
        if (multiplier <= 1) {
            return baseMax;
        }
        int extra;
        if (multiplier >= 50) {
            extra = 3;
        } else if (multiplier >= 20) {
            extra = 2;
        } else if (multiplier >= 5) {
            extra = 1;
        } else {
            extra = 0;
        }
        return Math.max(baseMax, Math.min(baseMax + extra, 4));
    }

    public static boolean readOnlyStorageDirAlertShowed;

    public static void checkSdCard(File file) {
        if (file == null || SharedConfig.storageCacheDir == null || readOnlyStorageDirAlertShowed) {
            return;
        }
        if (file.getPath().startsWith(SharedConfig.storageCacheDir)) {
            AndroidUtilities.runOnUIThread(() -> {
                if (readOnlyStorageDirAlertShowed) {
                    return;
                }
                BaseFragment fragment = LaunchActivity.getLastFragment();
                if (fragment != null && fragment.getParentActivity() != null) {
                    SharedConfig.storageCacheDir = null;
                    SharedConfig.saveConfig();
                    ImageLoader.getInstance().checkMediaPaths(() -> {

                    });

                    readOnlyStorageDirAlertShowed = true;
                    AlertDialog.Builder dialog = new AlertDialog.Builder(fragment.getParentActivity());
                    dialog.setTitle(LocaleController.getString(R.string.SdCardError));
                    dialog.setSubtitle(LocaleController.getString(R.string.SdCardErrorDescription));
                    dialog.setPositiveButton(LocaleController.getString(R.string.DoNotUseSDCard), (dialog1, which) -> {

                    });
                    Dialog dialogFinal = dialog.create();
                    dialogFinal.setCanceledOnTouchOutside(false);
                    dialogFinal.show();
                }
            });
        }
    }

    static Boolean allowPreparingHevcPlayers;

    public static boolean allowPreparingHevcPlayers() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return false;
        }
        if (allowPreparingHevcPlayers == null) {
            int codecCount = MediaCodecList.getCodecCount();
            int maxInstances = 0;
            int capabilities = 0;

            for (int i = 0; i < codecCount; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (codecInfo.isEncoder()) {
                    continue;
                }

                boolean found = false;
                for (int k = 0; k < codecInfo.getSupportedTypes().length; k++) {
                    if (codecInfo.getSupportedTypes()[k].contains("video/hevc")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
                capabilities = codecInfo.getCapabilitiesForType("video/hevc").getMaxSupportedInstances();
                if (capabilities > maxInstances) {
                    maxInstances = capabilities;
                }
            }
            allowPreparingHevcPlayers = maxInstances >= 8;
        }
        return allowPreparingHevcPlayers;
    }

    public static void togglePaymentByInvoice() {
        payByInvoice = !payByInvoice;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("payByInvoice", payByInvoice)
                .apply();
    }

    public static void toggleSurfaceInStories() {
        useSurfaceInStories = !useSurfaceInStories;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("useSurfaceInStories", useSurfaceInStories)
                .apply();
    }

    public static void togglePhotoViewerBlur() {
        photoViewerBlur = !photoViewerBlur;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("photoViewerBlur", photoViewerBlur)
                .apply();
    }

    private static String goodHevcEncoder;
    private static HashSet<String> hevcEncoderWhitelist = new HashSet<>();
    static {
        hevcEncoderWhitelist.add("c2.exynos.hevc.encoder");
        hevcEncoderWhitelist.add("OMX.Exynos.HEVC.Encoder".toLowerCase());
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String findGoodHevcEncoder() {
        if (goodHevcEncoder == null) {
            int codecCount = MediaCodecList.getCodecCount();
            for (int i = 0; i < codecCount; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (!codecInfo.isEncoder()) {
                    continue;
                }

                for (int k = 0; k < codecInfo.getSupportedTypes().length; k++) {
                    if (codecInfo.getSupportedTypes()[k].contains("video/hevc") && codecInfo.isHardwareAccelerated() && isWhitelisted(codecInfo)) {
                        return goodHevcEncoder = codecInfo.getName();
                    }
                }
            }
            goodHevcEncoder = "";
        }
        return TextUtils.isEmpty(goodHevcEncoder) ? null : goodHevcEncoder;
    }

    private static boolean isWhitelisted(MediaCodecInfo codecInfo) {
        if (BuildVars.DEBUG_PRIVATE_VERSION) {
            return true;
        }
        return hevcEncoderWhitelist.contains(codecInfo.getName().toLowerCase());
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            PASSCODE_TYPE_PIN,
            PASSCODE_TYPE_PASSWORD
    })
    public @interface PasscodeType {}

    public final static int SAVE_TO_GALLERY_FLAG_PEER = 1;
    public final static int SAVE_TO_GALLERY_FLAG_GROUP = 2;
    public final static int SAVE_TO_GALLERY_FLAG_CHANNELS = 4;

    @PushListenerController.PushType
    public static int pushType = PushListenerController.PUSH_TYPE_FIREBASE;
    public static String pushString = "";
    public static String pushStringStatus = "";
    public static long pushStringGetTimeStart;
    public static long pushStringGetTimeEnd;
    public static boolean pushStatSent;
    public static byte[] pushAuthKey;
    public static byte[] pushAuthKeyId;
    public static boolean forceForumTabs;
    public static boolean fastWallpaperDisabled;
    public static boolean frameMetricsEnabled;

    public static String directShareHash;

    @PasscodeType
    public static int passcodeType;
    public static String passcodeHash = "";
    public static long passcodeRetryInMs;
    public static long lastUptimeMillis;
    public static int badPasscodeTries;
    public static byte[] passcodeSalt = new byte[0];
    public static boolean appLocked;
    public static int autoLockIn = 60 * 60;

    public static boolean saveIncomingPhotos;
    public static boolean allowScreenCapture;
    public static int lastPauseTime;
    public static boolean isWaitingForPasscodeEnter;
    public static boolean useFingerprintLock = true;
    public static boolean useFaceLock = true;
    public static int suggestStickers;
    public static boolean suggestAnimatedEmoji;
    public static int keepMedia = CacheByChatsController.KEEP_MEDIA_ONE_MONTH; //deprecated
    public static int lastKeepMediaCheckTime;
    public static int lastLogsCheckTime;
    public static int textSelectionHintShows;
    public static int scheduledOrNoSoundHintShows;
    public static long scheduledOrNoSoundHintSeenAt;
    public static int scheduledHintShows;
    public static long scheduledHintSeenAt;
    public static int lockRecordAudioVideoHint;
    public static boolean forwardingOptionsHintShown, replyingOptionsHintShown;
    public static boolean searchMessagesAsListUsed;
    public static boolean stickersReorderingHintUsed;
    public static int dayNightWallpaperSwitchHint;
    public static boolean storyReactionsLongPressHint;
    public static boolean storiesIntroShown;
    public static boolean disableVoiceAudioEffects;
    public static boolean forceDisableTabletMode;
    public static boolean updateStickersOrderOnSend = true;
    public static boolean bigCameraForRound;
    public static Boolean useCamera2Force;
    public static boolean useNewBlur;
    public static boolean useSurfaceInStories;
    public static boolean photoViewerBlur = true;
    public static boolean payByInvoice;
    public static int stealthModeSendMessageConfirm = 2;
    private static int lastLocalId = -210000;

    public static String storageCacheDir;

    private static String passportConfigJson = "";
    private static HashMap<String, String> passportConfigMap;
    public static int passportConfigHash;

    private static boolean configLoaded;
    private static final Object sync = new Object();
    private static final Object localIdSync = new Object();

//    public static int saveToGalleryFlags;
    public static int mapPreviewType = 2;
    public static int searchEngineType = 0;
    public static String searchEngineCustomURLQuery, searchEngineCustomURLAutocomplete;
    public static boolean chatBubbles = Build.VERSION.SDK_INT >= 30;
    public static boolean raiseToSpeak = false;
    public static boolean raiseToListen = true;
    public static boolean nextMediaTap = true;
    public static boolean recordViaSco = false;
    public static boolean customTabs = true;
    public static boolean inappBrowser = true;
    public static boolean adaptableColorInBrowser = true;
    public static boolean onlyLocalInstantView = false;
    public static boolean directShare = true;
    public static boolean inappCamera = true;
    public static boolean roundCamera16to9 = true;
    public static boolean noSoundHintShowed = false;
    public static boolean streamMedia = true;
    public static boolean streamAllVideo = false;
    public static boolean streamMkv = false;
    public static boolean saveStreamMedia = true;
    public static boolean pauseMusicOnRecord = false;
    public static boolean pauseMusicOnMedia = false;
    public static boolean noiseSupression;
    public static final boolean noStatusBar = true;
    public static boolean debugWebView;
    public static boolean sortContactsByName;
    public static boolean sortFilesByName;
    public static boolean shuffleMusic;
    public static boolean playOrderReversed;
    public static boolean hasCameraCache;
    public static boolean showNotificationsForAllAccounts = true;
    public static boolean debugVideoQualities = false;
    public static int repeatMode;
    public static boolean allowBigEmoji;
    public static boolean useSystemEmoji;
    public static boolean useSystemBoldFont;
    public static int fontSize = 16;
	public static boolean fontSizeIsDefault;
	public static int bubbleRadius = 17;
	public static int bubbleOpacity = 100;
	public static int ivFontSize = 16;
    public static boolean proxyRotationEnabled;
    public static int proxyRotationTimeout;
    public static int downloadSpeedMultiplier = 1;
    public static int uploadSpeedMultiplier = 1;
    public static String customDownloadPath = "";
    public static String customDownloadTreeUri = "";
    public static boolean hideChannelSendAs;
    public static boolean defaultHighQualityMedia = true;
    public static int stickerMessageSizePercent = 100;
    public static boolean tabletFullWidth = true;
    public static int tabletWidthRatioPercent = 32;
    public static boolean autoTranslateMessages = false;
    public static int dialogRowHeightPercent = 100;
    public static int translationDisplayMode = TRANSLATION_DISPLAY_TRANSLATED_ONLY;
    public static boolean autoReactionEnabled = true;
    public static String autoReactionEmoji = "\uD83D\uDC4D";
    public static int autoReactionTarget = AUTO_REACTION_TARGET_INCOMING;
    public static boolean channelFeedEnabled = true;
    public static String channelFeedCustomEntries = "";
    public static boolean unlimitedChannelJoinEnabled = true;
    public static boolean unlimitedDialogFiltersEnabled = true;
    public static boolean localHoldVerificationEnabled = true;
    public static long customStarBalance = -1;
    public static String inputBarText = "";
    public static boolean localVirtualGiftBuyEnabled = true;
    public static boolean sendMp3AsVoiceEnabled = true;
    public static String customVideoWallpaperPath = "";
    public static String customVideoWallpaperUri = "";
    public static int messageSeenHintCount;
    public static int emojiInteractionsHintCount;
    public static int dayNightThemeSwitchHintCount;
    public static int callEncryptionHintDisplayedCount;
    public static boolean shadowsInSections;

    public static TLRPC.TL_help_appUpdate pendingAppUpdate;
    public static int pendingAppUpdateBuildVersion;
    public static long lastUpdateCheckTime;

    public static boolean hasEmailLogin;

    @PerformanceClass
    private static int devicePerformanceClass;
    @PerformanceClass
    private static int overrideDevicePerformanceClass;

    public static boolean drawDialogIcons;
    public static boolean useThreeLinesLayout;
    public static boolean archiveHidden;

    private static int chatSwipeAction;

    public static int distanceSystemType;
    public static int mediaColumnsCount = 3;
    public static int storiesColumnsCount = 3;
    public static int fastScrollHintCount = 3;
    public static boolean dontAskManageStorage;
    public static boolean multipleReactionsPromoShowed;

    public static boolean translateChats = true;

    public static boolean isFloatingDebugActive;
    public static LiteMode liteMode;

    private static final int[] LOW_SOC = {
            -1775228513, // EXYNOS 850
            802464304,  // EXYNOS 7872
            802464333,  // EXYNOS 7880
            802464302,  // EXYNOS 7870
            2067362118, // MSM8953
            2067362060, // MSM8937
            2067362084, // MSM8940
            2067362241, // MSM8992
            2067362117, // MSM8952
            2067361998, // MSM8917
            -1853602818 // SDM439
    };

    static {
        loadConfig();
    }

    public static class ProxyInfo {

        public String address;
        public int port;
        public String username;
        public String password;
        public String secret;
        public String displayName;
        public boolean builtin;

        public long proxyCheckPingId;
        public long ping;
        public boolean checking;
        public boolean available;
        public long availableCheckTime;

        public ProxyInfo(String address, int port, String username, String password, String secret) {
            this(address, port, username, password, secret, "");
        }

        public ProxyInfo(String address, int port, String username, String password, String secret, String displayName) {
            this.address = address;
            this.port = port;
            this.username = username;
            this.password = password;
            this.secret = secret;
            this.displayName = displayName;
            if (this.address == null) {
                this.address = "";
            }
            if (this.password == null) {
                this.password = "";
            }
            if (this.username == null) {
                this.username = "";
            }
            if (this.secret == null) {
                this.secret = "";
            }
            if (this.displayName == null) {
                this.displayName = "";
            }
        }

        public String getLink() {
            StringBuilder url = new StringBuilder(!TextUtils.isEmpty(secret) ? "https://t.me/proxy?" : "https://t.me/socks?");
            try {
                url.append("server=").append(URLEncoder.encode(address, "UTF-8")).append("&").append("port=").append(port);
                if (!TextUtils.isEmpty(username)) {
                    url.append("&user=").append(URLEncoder.encode(username, "UTF-8"));
                }
                if (!TextUtils.isEmpty(password)) {
                    url.append("&pass=").append(URLEncoder.encode(password, "UTF-8"));
                }
                if (!TextUtils.isEmpty(secret)) {
                    url.append("&secret=").append(URLEncoder.encode(secret, "UTF-8"));
                }
            } catch (UnsupportedEncodingException ignored) {}
            return url.toString();
        }
    }

    public static ArrayList<ProxyInfo> proxyList = new ArrayList<>();
    private static boolean proxyListLoaded;
    public static ProxyInfo currentProxy;

    public static void saveConfig() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("saveIncomingPhotos", saveIncomingPhotos);
                editor.putString("passcodeHash1", passcodeHash);
                editor.putString("passcodeSalt", passcodeSalt.length > 0 ? Base64.encodeToString(passcodeSalt, Base64.DEFAULT) : "");
                editor.putBoolean("appLocked", appLocked);
                editor.putInt("passcodeType", passcodeType);
                editor.putLong("passcodeRetryInMs", passcodeRetryInMs);
                editor.putLong("lastUptimeMillis", lastUptimeMillis);
                editor.putInt("badPasscodeTries", badPasscodeTries);
                editor.putInt("autoLockIn", autoLockIn);
                editor.putInt("lastPauseTime", lastPauseTime);
                editor.putBoolean("useFingerprint", useFingerprintLock);
                editor.putBoolean("allowScreenCapture", allowScreenCapture);
                editor.putString("pushString2", pushString);
                editor.putInt("pushType", pushType);
                editor.putBoolean("pushStatSent", pushStatSent);
                editor.putString("pushAuthKey", pushAuthKey != null ? Base64.encodeToString(pushAuthKey, Base64.DEFAULT) : "");
                editor.putInt("lastLocalId", lastLocalId);
                editor.putString("passportConfigJson", passportConfigJson);
                editor.putInt("passportConfigHash", passportConfigHash);
                editor.putBoolean("sortContactsByName", sortContactsByName);
                editor.putBoolean("sortFilesByName", sortFilesByName);
                editor.putInt("textSelectionHintShows", textSelectionHintShows);
                editor.putInt("scheduledOrNoSoundHintShows", scheduledOrNoSoundHintShows);
                editor.putLong("scheduledOrNoSoundHintSeenAt", scheduledOrNoSoundHintSeenAt);
                editor.putInt("scheduledHintShows", scheduledHintShows);
                editor.putLong("scheduledHintSeenAt", scheduledHintSeenAt);
                editor.putBoolean("forwardingOptionsHintShown", forwardingOptionsHintShown);
                editor.putBoolean("replyingOptionsHintShown", replyingOptionsHintShown);
                editor.putInt("lockRecordAudioVideoHint", lockRecordAudioVideoHint);
                editor.putString("storageCacheDir", !TextUtils.isEmpty(storageCacheDir) ? storageCacheDir : "");
                editor.putBoolean("proxyRotationEnabled", proxyRotationEnabled);
                editor.putInt("proxyRotationTimeout", proxyRotationTimeout);
                editor.putInt(PREF_DOWNLOAD_SPEED_MULTIPLIER, getDownloadSpeedMultiplier());
                editor.putInt(PREF_UPLOAD_SPEED_MULTIPLIER, getUploadSpeedMultiplier());
                editor.putString(PREF_CUSTOM_DOWNLOAD_PATH, customDownloadPath == null ? "" : customDownloadPath);
                editor.putString(PREF_CUSTOM_DOWNLOAD_TREE_URI, customDownloadTreeUri == null ? "" : customDownloadTreeUri);
                editor.putBoolean(PREF_HIDE_CHANNEL_SEND_AS, hideChannelSendAs);
                editor.putBoolean(PREF_DEFAULT_HIGH_QUALITY_MEDIA, defaultHighQualityMedia);
                editor.putInt(PREF_STICKER_MESSAGE_SIZE_PERCENT, getStickerMessageSizePercent());
                editor.putBoolean(PREF_TABLET_FULL_WIDTH, tabletFullWidth);
                editor.putInt(PREF_TABLET_WIDTH_RATIO_PERCENT, getTabletWidthRatioPercent());
                editor.putBoolean(PREF_AUTO_TRANSLATE_MESSAGES, autoTranslateMessages);
                editor.putInt(PREF_DIALOG_ROW_HEIGHT_PERCENT, getDialogRowHeightPercent());
                editor.putInt(PREF_TRANSLATION_DISPLAY_MODE, getTranslationDisplayMode());
                editor.putBoolean(PREF_AUTO_REACTION_ENABLED, autoReactionEnabled);
                editor.putString(PREF_AUTO_REACTION_EMOJI, getAutoReactionEmoji());
                editor.putInt(PREF_AUTO_REACTION_TARGET, getAutoReactionTarget());
                editor.putBoolean(PREF_CHANNEL_FEED_ENABLED, channelFeedEnabled);
                editor.putString(PREF_CHANNEL_FEED_CUSTOM_ENTRIES, channelFeedCustomEntries == null ? "" : channelFeedCustomEntries);
                editor.putBoolean(PREF_UNLIMITED_CHANNEL_JOIN_ENABLED, unlimitedChannelJoinEnabled);
                editor.putBoolean(PREF_UNLIMITED_DIALOG_FILTERS_ENABLED, unlimitedDialogFiltersEnabled);
                editor.putBoolean(PREF_LOCAL_HOLD_VERIFICATION_ENABLED, localHoldVerificationEnabled);
                editor.putLong(PREF_CUSTOM_STAR_BALANCE, customStarBalance);
                editor.putString(PREF_INPUT_BAR_TEXT, getInputBarText());
                editor.putBoolean(PREF_LOCAL_VIRTUAL_GIFT_BUY_ENABLED, localVirtualGiftBuyEnabled);
                editor.putBoolean(PREF_SEND_MP3_AS_VOICE_ENABLED, sendMp3AsVoiceEnabled);
                editor.putString(PREF_CUSTOM_VIDEO_WALLPAPER_PATH, customVideoWallpaperPath == null ? "" : customVideoWallpaperPath);
                editor.putString(PREF_CUSTOM_VIDEO_WALLPAPER_URI, customVideoWallpaperUri == null ? "" : customVideoWallpaperUri);

                if (pendingAppUpdate != null) {
                    try {
                        SerializedData data = new SerializedData(pendingAppUpdate.getObjectSize());
                        pendingAppUpdate.serializeToStream(data);
                        String str = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
                        editor.putString("appUpdate", str);
                        editor.putInt("appUpdateBuild", pendingAppUpdateBuildVersion);
                        data.cleanup();
                    } catch (Exception ignore) {

                    }
                } else {
                    editor.remove("appUpdate");
                }
                editor.putLong("appUpdateCheckTime", lastUpdateCheckTime);

                editor.apply();

                editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE).edit();
                editor.putBoolean("hasEmailLogin", hasEmailLogin);
                editor.putBoolean("floatingDebugActive", isFloatingDebugActive);
                editor.putBoolean("record_via_sco", recordViaSco);
                editor.apply();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public static int getLastLocalId() {
        int value;
        synchronized (localIdSync) {
            value = lastLocalId--;
        }
        return value;
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded || ApplicationLoader.applicationContext == null) {
                return;
            }

            BackgroundActivityPrefs.prefs = ApplicationLoader.applicationContext.getSharedPreferences("background_activity", Context.MODE_PRIVATE);

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
            saveIncomingPhotos = preferences.getBoolean("saveIncomingPhotos", false);
            passcodeHash = preferences.getString("passcodeHash1", "");
            appLocked = preferences.getBoolean("appLocked", false);
            passcodeType = preferences.getInt("passcodeType", 0);
            passcodeRetryInMs = preferences.getLong("passcodeRetryInMs", 0);
            lastUptimeMillis = preferences.getLong("lastUptimeMillis", 0);
            badPasscodeTries = preferences.getInt("badPasscodeTries", 0);
            autoLockIn = preferences.getInt("autoLockIn", 60 * 60);
            lastPauseTime = preferences.getInt("lastPauseTime", 0);
            useFingerprintLock = preferences.getBoolean("useFingerprint", true);
            allowScreenCapture = preferences.getBoolean("allowScreenCapture", false);
            lastLocalId = preferences.getInt("lastLocalId", -210000);
            pushString = preferences.getString("pushString2", "");
            pushType = preferences.getInt("pushType", PushListenerController.PUSH_TYPE_FIREBASE);
            pushStatSent = preferences.getBoolean("pushStatSent", false);
            passportConfigJson = preferences.getString("passportConfigJson", "");
            passportConfigHash = preferences.getInt("passportConfigHash", 0);
            storageCacheDir = preferences.getString("storageCacheDir", null);
            proxyRotationEnabled = preferences.getBoolean("proxyRotationEnabled", false);
            proxyRotationTimeout = preferences.getInt("proxyRotationTimeout", ProxyRotationController.DEFAULT_TIMEOUT_INDEX);
            proxyRotationTimeout = Math.max(0, Math.min(proxyRotationTimeout, ProxyRotationController.ROTATION_TIMEOUTS.size() - 1));
            downloadSpeedMultiplier = sanitizeDownloadSpeedMultiplier(preferences.getInt(PREF_DOWNLOAD_SPEED_MULTIPLIER, 1));
            uploadSpeedMultiplier = sanitizeUploadSpeedMultiplier(preferences.getInt(PREF_UPLOAD_SPEED_MULTIPLIER, 1));
            customDownloadPath = preferences.getString(PREF_CUSTOM_DOWNLOAD_PATH, "");
            customDownloadTreeUri = preferences.getString(PREF_CUSTOM_DOWNLOAD_TREE_URI, "");
            hideChannelSendAs = preferences.getBoolean(PREF_HIDE_CHANNEL_SEND_AS, false);
            defaultHighQualityMedia = preferences.getBoolean(PREF_DEFAULT_HIGH_QUALITY_MEDIA, true);
            stickerMessageSizePercent = sanitizeStickerMessageSizePercent(preferences.getInt(PREF_STICKER_MESSAGE_SIZE_PERCENT, 100));
            tabletFullWidth = preferences.getBoolean(PREF_TABLET_FULL_WIDTH, true);
            tabletWidthRatioPercent = sanitizeTabletWidthRatioPercent(preferences.getInt(PREF_TABLET_WIDTH_RATIO_PERCENT, 32));
            autoTranslateMessages = false;
            dialogRowHeightPercent = sanitizeDialogRowHeightPercent(preferences.getInt(PREF_DIALOG_ROW_HEIGHT_PERCENT, 100));
            translationDisplayMode = preferences.getInt(PREF_TRANSLATION_DISPLAY_MODE, TRANSLATION_DISPLAY_TRANSLATED_ONLY) == TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED ? TRANSLATION_DISPLAY_ORIGINAL_AND_TRANSLATED : TRANSLATION_DISPLAY_TRANSLATED_ONLY;
            autoReactionEnabled = preferences.getBoolean(PREF_AUTO_REACTION_ENABLED, true);
            autoReactionEmoji = preferences.getString(PREF_AUTO_REACTION_EMOJI, "\uD83D\uDC4D");
            autoReactionTarget = preferences.getInt(PREF_AUTO_REACTION_TARGET, AUTO_REACTION_TARGET_INCOMING);
            channelFeedEnabled = preferences.getBoolean(PREF_CHANNEL_FEED_ENABLED, true);
            channelFeedCustomEntries = preferences.getString(PREF_CHANNEL_FEED_CUSTOM_ENTRIES, "");
            unlimitedChannelJoinEnabled = preferences.getBoolean(PREF_UNLIMITED_CHANNEL_JOIN_ENABLED, true);
            unlimitedDialogFiltersEnabled = preferences.getBoolean(PREF_UNLIMITED_DIALOG_FILTERS_ENABLED, true);
            localHoldVerificationEnabled = preferences.getBoolean(PREF_LOCAL_HOLD_VERIFICATION_ENABLED, true);
            customStarBalance = preferences.getLong(PREF_CUSTOM_STAR_BALANCE, -1);
            inputBarText = preferences.getString(PREF_INPUT_BAR_TEXT, "");
            localVirtualGiftBuyEnabled = preferences.getBoolean(PREF_LOCAL_VIRTUAL_GIFT_BUY_ENABLED, true);
            sendMp3AsVoiceEnabled = preferences.getBoolean(PREF_SEND_MP3_AS_VOICE_ENABLED, true);
            customVideoWallpaperPath = preferences.getString(PREF_CUSTOM_VIDEO_WALLPAPER_PATH, "");
            customVideoWallpaperUri = preferences.getString(PREF_CUSTOM_VIDEO_WALLPAPER_URI, "");
            String authKeyString = preferences.getString("pushAuthKey", null);
            if (!TextUtils.isEmpty(authKeyString)) {
                pushAuthKey = Base64.decode(authKeyString, Base64.DEFAULT);
            }

            if (passcodeHash.length() > 0 && lastPauseTime == 0) {
                lastPauseTime = (int) (SystemClock.elapsedRealtime() / 1000 - 60 * 10);
            }

            String passcodeSaltString = preferences.getString("passcodeSalt", "");
            if (passcodeSaltString.length() > 0) {
                passcodeSalt = Base64.decode(passcodeSaltString, Base64.DEFAULT);
            } else {
                passcodeSalt = new byte[0];
            }
            lastUpdateCheckTime = preferences.getLong("appUpdateCheckTime", System.currentTimeMillis());
            try {
                String update = preferences.getString("appUpdate", null);
                if (update != null) {
                    pendingAppUpdateBuildVersion = preferences.getInt("appUpdateBuild", buildVersion());
                    byte[] arr = Base64.decode(update, Base64.DEFAULT);
                    if (arr != null) {
                        SerializedData data = new SerializedData(arr);
                        pendingAppUpdate = (TLRPC.TL_help_appUpdate) TLRPC.help_AppUpdate.TLdeserialize(data, data.readInt32(false), false);
                        data.cleanup();
                    }
                }
                if (pendingAppUpdate != null) {
                    long updateTime = 0;
                    int updateVersion = 0;
                    String updateVersionString = null;
                    try {
                        PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                        updateVersion = packageInfo.versionCode;
                        updateVersionString = packageInfo.versionName;
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (updateVersion == 0) {
                        updateVersion = buildVersion();
                    }
                    if (updateVersionString == null) {
                        updateVersionString = BuildVars.BUILD_VERSION_STRING;
                    }
                    if (pendingAppUpdateBuildVersion != updateVersion || pendingAppUpdate.version == null || updateVersionString.compareTo(pendingAppUpdate.version) >= 0 || BuildVars.DEBUG_PRIVATE_VERSION) {
                        pendingAppUpdate = null;
                        AndroidUtilities.runOnUIThread(SharedConfig::saveConfig);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            SaveToGallerySettingsHelper.load(preferences);
            mapPreviewType = preferences.getInt("mapPreviewType", 2);
            searchEngineType = preferences.getInt("searchEngineType", 0);
            raiseToListen = preferences.getBoolean("raise_to_listen", true);
            raiseToSpeak = preferences.getBoolean("raise_to_speak", false);
            nextMediaTap = preferences.getBoolean("next_media_on_tap", true);
            recordViaSco = preferences.getBoolean("record_via_sco", false);
            customTabs = preferences.getBoolean("custom_tabs", true);
            inappBrowser = preferences.getBoolean("inapp_browser", true);
            adaptableColorInBrowser = preferences.getBoolean("adaptableBrowser", false);
            onlyLocalInstantView = preferences.getBoolean("onlyLocalInstantView", BuildVars.DEBUG_PRIVATE_VERSION);
            directShare = preferences.getBoolean("direct_share", true);
            shuffleMusic = preferences.getBoolean("shuffleMusic", false);
            playOrderReversed = !shuffleMusic && preferences.getBoolean("playOrderReversed", false);
            inappCamera = preferences.getBoolean("inappCamera", true);
            hasCameraCache = preferences.contains("cameraCache");
            roundCamera16to9 = true;
            repeatMode = preferences.getInt("repeatMode", 0);
	        fontSize = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
	        fontSizeIsDefault = !preferences.contains("fons_size");
	        bubbleRadius = preferences.getInt("bubbleRadius", 17);
	        bubbleOpacity = Math.max(30, Math.min(100, preferences.getInt("bubbleOpacity", 100)));
	        ivFontSize = preferences.getInt("iv_font_size", fontSize);
            allowBigEmoji = preferences.getBoolean("allowBigEmoji", true);
            useSystemEmoji = preferences.getBoolean("useSystemEmoji", false);
            useSystemBoldFont = preferences.getBoolean("useSystemBoldFont", false);
            forceForumTabs = preferences.getBoolean("forceForumTabs", false);
            fastWallpaperDisabled = preferences.getBoolean("fastWallpaperDisabled", false);
            frameMetricsEnabled = preferences.getBoolean("frameMetricsEnabled", false);
            if (useSystemBoldFont) {
                AndroidUtilities.mediumTypeface = null;
            }
            streamMedia = preferences.getBoolean("streamMedia", true);
            saveStreamMedia = preferences.getBoolean("saveStreamMedia", true);
            pauseMusicOnRecord = preferences.getBoolean("pauseMusicOnRecord", true);
            pauseMusicOnMedia = preferences.getBoolean("pauseMusicOnMedia", false);
            forceDisableTabletMode = preferences.getBoolean("forceDisableTabletMode", false);
            streamAllVideo = preferences.getBoolean("streamAllVideo", BuildVars.DEBUG_VERSION);
            streamMkv = preferences.getBoolean("streamMkv", false);
            suggestStickers = preferences.getInt("suggestStickers", 0);
            suggestAnimatedEmoji = preferences.getBoolean("suggestAnimatedEmoji", true);
            overrideDevicePerformanceClass = preferences.getInt("overrideDevicePerformanceClass", -1);
            devicePerformanceClass = preferences.getInt("devicePerformanceClass", -1);
            sortContactsByName = preferences.getBoolean("sortContactsByName", false);
            sortFilesByName = preferences.getBoolean("sortFilesByName", false);
            noSoundHintShowed = preferences.getBoolean("noSoundHintShowed", false);
            directShareHash = preferences.getString("directShareHash2", null);
            useThreeLinesLayout = preferences.getBoolean("useThreeLinesLayout", false);
            archiveHidden = preferences.getBoolean("archiveHidden", false);
            distanceSystemType = preferences.getInt("distanceSystemType", 0);
            keepMedia = preferences.getInt("keep_media", CacheByChatsController.KEEP_MEDIA_ONE_MONTH);
            debugWebView = preferences.getBoolean("debugWebView", false);
            lastKeepMediaCheckTime = preferences.getInt("lastKeepMediaCheckTime", 0);
            lastLogsCheckTime = preferences.getInt("lastLogsCheckTime", 0);
            searchMessagesAsListUsed = preferences.getBoolean("searchMessagesAsListUsed", false);
            stickersReorderingHintUsed = preferences.getBoolean("stickersReorderingHintUsed", false);
            storyReactionsLongPressHint = preferences.getBoolean("storyReactionsLongPressHint", false);
            storiesIntroShown = preferences.getBoolean("storiesIntroShown", false);
            textSelectionHintShows = preferences.getInt("textSelectionHintShows", 0);
            scheduledOrNoSoundHintShows = preferences.getInt("scheduledOrNoSoundHintShows", 0);
            scheduledOrNoSoundHintSeenAt = preferences.getLong("scheduledOrNoSoundHintSeenAt", 0);
            scheduledHintShows = preferences.getInt("scheduledHintShows", 0);
            scheduledHintSeenAt = preferences.getLong("scheduledHintSeenAt", 0);
            forwardingOptionsHintShown = preferences.getBoolean("forwardingOptionsHintShown", false);
            replyingOptionsHintShown = preferences.getBoolean("replyingOptionsHintShown", false);
            lockRecordAudioVideoHint = preferences.getInt("lockRecordAudioVideoHint", 0);
            disableVoiceAudioEffects = preferences.getBoolean("disableVoiceAudioEffects", false);
            noiseSupression = preferences.getBoolean("noiseSupression", false);
            chatSwipeAction = preferences.getInt("ChatSwipeAction", -1);
            messageSeenHintCount = preferences.getInt("messageSeenCount", 3);
            emojiInteractionsHintCount = preferences.getInt("emojiInteractionsHintCount", 3);
            dayNightThemeSwitchHintCount = preferences.getInt("dayNightThemeSwitchHintCount", 3);
            stealthModeSendMessageConfirm = preferences.getInt("stealthModeSendMessageConfirm", 2);
            mediaColumnsCount = preferences.getInt("mediaColumnsCount", 3);
            storiesColumnsCount = preferences.getInt("storiesColumnsCount", 3);
            fastScrollHintCount = preferences.getInt("fastScrollHintCount", 3);
            dontAskManageStorage = preferences.getBoolean("dontAskManageStorage", false);
            hasEmailLogin = preferences.getBoolean("hasEmailLogin", false);
            isFloatingDebugActive = preferences.getBoolean("floatingDebugActive", false);
            updateStickersOrderOnSend = preferences.getBoolean("updateStickersOrderOnSend", true);
            dayNightWallpaperSwitchHint = preferences.getInt("dayNightWallpaperSwitchHint", 0);
            bigCameraForRound = preferences.getBoolean("bigCameraForRound", false);
            useNewBlur = preferences.getBoolean("useNewBlur", true);
            useCamera2Force = !preferences.contains("useCamera2Force_2") ? null : preferences.getBoolean("useCamera2Force_2", false);
            useSurfaceInStories = preferences.getBoolean("useSurfaceInStories", Build.VERSION.SDK_INT >= 30);
            payByInvoice = preferences.getBoolean("payByInvoice", false);
            photoViewerBlur = preferences.getBoolean("photoViewerBlur", true);
            multipleReactionsPromoShowed = preferences.getBoolean("multipleReactionsPromoShowed", false);
            callEncryptionHintDisplayedCount = preferences.getInt("callEncryptionHintDisplayedCount", 0);
            debugVideoQualities = preferences.getBoolean("debugVideoQualities", false);
            shadowsInSections = preferences.getBoolean("shadowsInSections", false);

            loadDebugConfig(preferences);

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            showNotificationsForAllAccounts = preferences.getBoolean("AllAccounts", true);

            configLoaded = true;
        }
    }

    public static int buildVersion() {
        try {
            return ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            FileLog.e(e);
            return 0;
        }
    }

    public static void updateTabletConfig() {
        if (fontSizeIsDefault) {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            fontSize = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
            ivFontSize = preferences.getInt("iv_font_size", fontSize);
        }
    }

    public static void increaseBadPasscodeTries() {
        badPasscodeTries++;
        if (badPasscodeTries >= 3) {
            switch (badPasscodeTries) {
                case 3:
                    passcodeRetryInMs = 5000;
                    break;
                case 4:
                    passcodeRetryInMs = 10000;
                    break;
                case 5:
                    passcodeRetryInMs = 15000;
                    break;
                case 6:
                    passcodeRetryInMs = 20000;
                    break;
                case 7:
                    passcodeRetryInMs = 25000;
                    break;
                default:
                    passcodeRetryInMs = 30000;
                    break;
            }
            lastUptimeMillis = SystemClock.elapsedRealtime();
        }
        saveConfig();
    }

    public static boolean isAutoplayVideo() {
        return LiteMode.isEnabled(LiteMode.FLAG_AUTOPLAY_VIDEOS);
    }

    public static boolean isAutoplayGifs() {
        return LiteMode.isEnabled(LiteMode.FLAG_AUTOPLAY_GIFS);
    }

    public static boolean isPassportConfigLoaded() {
        return passportConfigMap != null;
    }

    public static void setPassportConfig(String json, int hash) {
        passportConfigMap = null;
        passportConfigJson = json;
        passportConfigHash = hash;
        saveConfig();
        getCountryLangs();
    }

    public static HashMap<String, String> getCountryLangs() {
        if (passportConfigMap == null) {
            passportConfigMap = new HashMap<>();
            try {
                JSONObject object = new JSONObject(passportConfigJson);
                Iterator<String> iter = object.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    passportConfigMap.put(key.toUpperCase(), object.getString(key).toUpperCase());
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return passportConfigMap;
    }

    public static boolean isAppUpdateAvailable() {
        if (pendingAppUpdate == null || pendingAppUpdate.document == null || !ApplicationLoader.isStandaloneBuild()) {
            return false;
        }
        int currentVersion;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            currentVersion = pInfo.versionCode;
        } catch (Exception e) {
            FileLog.e(e);
            currentVersion = buildVersion();
        }
        return pendingAppUpdateBuildVersion == currentVersion;
    }

    public static boolean setNewAppVersionAvailable(TLRPC.TL_help_appUpdate update) {
        String updateVersionString = null;
        int versionCode = 0;
        try {
            PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
            updateVersionString = packageInfo.versionName;
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (versionCode == 0) {
            versionCode = buildVersion();
        }
        if (updateVersionString == null) {
            updateVersionString = BuildVars.BUILD_VERSION_STRING;
        }
        if (update.version == null || versionBiggerOrEqual(updateVersionString, update.version)) {
            return false;
        }
        pendingAppUpdate = update;
        pendingAppUpdateBuildVersion = versionCode;
        saveConfig();
        return true;
    }

    // returns a >= b
    public static boolean versionBiggerOrEqual(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int count = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < count; ++i) {
            int numA = i < partsA.length ? Integer.parseInt(partsA[i]) : 0;
            int numB = i < partsB.length ? Integer.parseInt(partsB[i]) : 0;
            if (numA < numB) {
                return false;
            } else if (numA > numB) {
                return true;
            }
        }
        return true;
    }

    public static boolean checkPasscode(String passcode) {
        if (passcodeSalt.length == 0) {
            boolean result = Utilities.MD5(passcode).equals(passcodeHash);
            if (result) {
                try {
                    passcodeSalt = new byte[16];
                    Utilities.random.nextBytes(passcodeSalt);
                    byte[] passcodeBytes = passcode.getBytes("UTF-8");
                    byte[] bytes = new byte[32 + passcodeBytes.length];
                    System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                    System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                    System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                    passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                    saveConfig();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return result;
        } else {
            try {
                byte[] passcodeBytes = passcode.getBytes("UTF-8");
                byte[] bytes = new byte[32 + passcodeBytes.length];
                System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                String hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                return passcodeHash.equals(hash);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return false;
    }

    public static void clearConfig() {
        saveIncomingPhotos = false;
        appLocked = false;
        passcodeType = PASSCODE_TYPE_PIN;
        passcodeRetryInMs = 0;
        lastUptimeMillis = 0;
        badPasscodeTries = 0;
        passcodeHash = "";
        passcodeSalt = new byte[0];
        autoLockIn = 60 * 60;
        lastPauseTime = 0;
        useFingerprintLock = true;
        isWaitingForPasscodeEnter = false;
        allowScreenCapture = false;
        textSelectionHintShows = 0;
        scheduledOrNoSoundHintShows = 0;
        scheduledOrNoSoundHintSeenAt = 0;
        scheduledHintShows = 0;
        scheduledHintSeenAt = 0;
        lockRecordAudioVideoHint = 0;
        forwardingOptionsHintShown = false;
        replyingOptionsHintShown = false;
        messageSeenHintCount = 3;
        emojiInteractionsHintCount = 3;
        dayNightThemeSwitchHintCount = 3;
        stealthModeSendMessageConfirm = 2;
        dayNightWallpaperSwitchHint = 0;
        saveConfig();
    }

    public static void setMultipleReactionsPromoShowed(boolean val) {
        multipleReactionsPromoShowed = val;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("multipleReactionsPromoShowed", multipleReactionsPromoShowed);
        editor.apply();
    }

    public static void setSuggestStickers(int type) {
        suggestStickers = type;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("suggestStickers", suggestStickers);
        editor.apply();
    }

    public static void setSearchMessagesAsListUsed(boolean value) {
        searchMessagesAsListUsed = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("searchMessagesAsListUsed", searchMessagesAsListUsed);
        editor.apply();
    }

    public static void setStickersReorderingHintUsed(boolean value) {
        stickersReorderingHintUsed = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("stickersReorderingHintUsed", stickersReorderingHintUsed);
        editor.apply();
    }

    public static void setStoriesReactionsLongPressHintUsed(boolean value) {
        storyReactionsLongPressHint = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("storyReactionsLongPressHint", storyReactionsLongPressHint);
        editor.apply();
    }

    public static void setStoriesIntroShown(boolean isShown) {
        storiesIntroShown = isShown;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("storiesIntroShown", storiesIntroShown);
        editor.apply();
    }

    public static void increaseTextSelectionHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("textSelectionHintShows", ++textSelectionHintShows);
        editor.apply();
    }

    public static void increaseDayNightWallpaperSiwtchHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("dayNightWallpaperSwitchHint", ++dayNightWallpaperSwitchHint);
        editor.apply();
    }

    public static void removeTextSelectionHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("textSelectionHintShows", 3);
        editor.apply();
    }

    public static void increaseScheduledOrNoSoundHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        scheduledOrNoSoundHintSeenAt = System.currentTimeMillis();
        editor.putInt("scheduledOrNoSoundHintShows", ++scheduledOrNoSoundHintShows);
        editor.putLong("scheduledOrNoSoundHintSeenAt", scheduledOrNoSoundHintSeenAt);
        editor.apply();
    }

    public static void increaseScheduledHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        scheduledHintSeenAt = System.currentTimeMillis();
        editor.putInt("scheduledHintShows", ++scheduledHintShows);
        editor.putLong("scheduledHintSeenAt", scheduledHintSeenAt);
        editor.apply();
    }

    public static void forwardingOptionsHintHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        forwardingOptionsHintShown = true;
        editor.putBoolean("forwardingOptionsHintShown", forwardingOptionsHintShown);
        editor.apply();
    }

    public static void replyingOptionsHintHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        replyingOptionsHintShown = true;
        editor.putBoolean("replyingOptionsHintShown", replyingOptionsHintShown);
        editor.apply();
    }

    public static void removeScheduledOrNoSoundHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("scheduledOrNoSoundHintShows", 3);
        editor.apply();
    }

    public static void removeScheduledHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("scheduledHintShows", 3);
        editor.apply();
    }

    public static void increaseLockRecordAudioVideoHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("lockRecordAudioVideoHint", ++lockRecordAudioVideoHint);
        editor.apply();
    }

    public static void removeLockRecordAudioVideoHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("lockRecordAudioVideoHint", 3);
        editor.apply();
    }

    public static void setKeepMedia(int value) {
        keepMedia = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("keep_media", keepMedia);
        editor.apply();
    }

    public static void toggleUpdateStickersOrderOnSend() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("updateStickersOrderOnSend", updateStickersOrderOnSend = !updateStickersOrderOnSend);
        editor.apply();
    }

    public static void checkLogsToDelete() {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        int time = (int) (System.currentTimeMillis() / 1000);
        if (Math.abs(time - lastLogsCheckTime) < 60 * 60) {
            return;
        }
        lastLogsCheckTime = time;
        Utilities.cacheClearQueue.postRunnable(() -> {
            long currentTime = time - 60 * 60 * 24 * 10;
            try {
                File dir = AndroidUtilities.getLogsDir();
                if (dir == null) {
                    return;
                }
                Utilities.clearDir(dir.getAbsolutePath(), 0, currentTime, false);
            } catch (Throwable e) {
                FileLog.e(e);
            }
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("lastLogsCheckTime", lastLogsCheckTime);
            editor.apply();
        });
    }

    public static void toggleDisableVoiceAudioEffects() {
        disableVoiceAudioEffects = !disableVoiceAudioEffects;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("disableVoiceAudioEffects", disableVoiceAudioEffects);
        editor.apply();
    }

    public static void toggleNoiseSupression() {
        noiseSupression = !noiseSupression;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("noiseSupression", noiseSupression);
        editor.apply();
    }

    public static void toggleDebugWebView() {
        debugWebView = !debugWebView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(debugWebView);
        }
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("debugWebView", debugWebView);
        editor.apply();
    }

    public static void incrementCallEncryptionHintDisplayed(int count) {
        callEncryptionHintDisplayedCount += count;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("callEncryptionHintDisplayedCount", callEncryptionHintDisplayedCount);
        editor.apply();
    }

    public static void toggleLoopStickers() {
        LiteMode.toggleFlag(LiteMode.FLAG_ANIMATED_STICKERS_CHAT);
    }

    public static void toggleBigEmoji() {
        allowBigEmoji = !allowBigEmoji;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("allowBigEmoji", allowBigEmoji);
        editor.apply();
    }

    public static void toggleUseSystemBoldFont() {
        useSystemBoldFont = !useSystemBoldFont;
        AndroidUtilities.mediumTypeface = null;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("useSystemBoldFont", useSystemBoldFont);
        editor.apply();
    }

    public static void toggleForceForumTabs() {
        forceForumTabs = !forceForumTabs;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("forceForumTabs", forceForumTabs);
        editor.apply();
    }

    public static void toggleFastWallpaperDisabled() {
        fastWallpaperDisabled = !fastWallpaperDisabled;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("fastWallpaperDisabled", fastWallpaperDisabled);
        editor.apply();
    }

    public static void toggleFrameMetricsEnabled() {
        frameMetricsEnabled = !frameMetricsEnabled;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("frameMetricsEnabled", frameMetricsEnabled);
        editor.apply();
    }

    public static void toggleSuggestAnimatedEmoji() {
        suggestAnimatedEmoji = !suggestAnimatedEmoji;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("suggestAnimatedEmoji", suggestAnimatedEmoji);
        editor.apply();
    }

    public static void setPlaybackOrderType(int type) {
        if (type == 2) {
            shuffleMusic = true;
            playOrderReversed = false;
        } else if (type == 1) {
            playOrderReversed = true;
            shuffleMusic = false;
        } else {
            playOrderReversed = false;
            shuffleMusic = false;
        }
        MediaController.getInstance().checkIsNextMediaFileDownloaded();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shuffleMusic", shuffleMusic);
        editor.putBoolean("playOrderReversed", playOrderReversed);
        editor.apply();
    }

    public static void setRepeatMode(int mode) {
        repeatMode = mode;
        if (repeatMode < 0 || repeatMode > 2) {
            repeatMode = 0;
        }
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("repeatMode", repeatMode);
        editor.apply();
    }

    public static void overrideDevicePerformanceClass(int performanceClass) {
        MessagesController.getGlobalMainSettings().edit().putInt("overrideDevicePerformanceClass", overrideDevicePerformanceClass = performanceClass).remove("lite_mode").apply();
        if (liteMode != null) {
            liteMode.loadPreference();
        }
    }

    public static void toggleAutoplayGifs() {
        LiteMode.toggleFlag(LiteMode.FLAG_AUTOPLAY_GIFS);
    }

    public static void setUseThreeLinesLayout(boolean value) {
        useThreeLinesLayout = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("useThreeLinesLayout", useThreeLinesLayout);
        editor.apply();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload, true);
    }

    public static void toggleArchiveHidden() {
        archiveHidden = !archiveHidden;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("archiveHidden", archiveHidden);
        editor.apply();
    }

    public static void toggleAutoplayVideo() {
        LiteMode.toggleFlag(LiteMode.FLAG_AUTOPLAY_VIDEOS);
    }

    public static boolean isSecretMapPreviewSet() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        return preferences.contains("mapPreviewType");
    }

    public static void setSecretMapPreviewType(int value) {
        mapPreviewType = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("mapPreviewType", mapPreviewType);
        editor.apply();
    }

    public static void setSearchEngineType(int value) {
        searchEngineType = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("searchEngineType", searchEngineType);
        editor.apply();
    }

    public static void setNoSoundHintShowed(boolean value) {
        if (noSoundHintShowed == value) {
            return;
        }
        noSoundHintShowed = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("noSoundHintShowed", noSoundHintShowed);
        editor.apply();
    }

    public static void toggleRaiseToSpeak() {
        raiseToSpeak = !raiseToSpeak;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("raise_to_speak", raiseToSpeak);
        editor.apply();
    }

    public static void toggleRaiseToListen() {
        raiseToListen = !raiseToListen;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("raise_to_listen", raiseToListen);
        editor.apply();
    }

    public static void toggleNextMediaTap() {
        nextMediaTap = !nextMediaTap;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("next_media_on_tap", nextMediaTap);
        editor.apply();
    }

    public static boolean enabledRaiseTo(boolean speak) {
        return raiseToListen && (!speak || raiseToSpeak);
    }

    public static void toggleCustomTabs(boolean newValue) {
        customTabs = newValue;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("custom_tabs", customTabs);
        editor.apply();
    }

    public static void toggleInappBrowser() {
        inappBrowser = !inappBrowser;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("inapp_browser", inappBrowser);
        editor.apply();
    }

    public static void toggleBrowserAdaptableColors() {
        adaptableColorInBrowser = !adaptableColorInBrowser;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("adaptableBrowser", adaptableColorInBrowser);
        editor.apply();
    }

    public static void toggleDebugVideoQualities() {
        debugVideoQualities = !debugVideoQualities;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("debugVideoQualities", debugVideoQualities);
        editor.apply();
    }

    public static void toggleLocalInstantView() {
        onlyLocalInstantView = !onlyLocalInstantView;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("onlyLocalInstantView", onlyLocalInstantView);
        editor.apply();
    }

    public static void toggleDirectShare() {
        directShare = !directShare;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("direct_share", directShare);
        editor.apply();
        ShortcutManagerCompat.removeAllDynamicShortcuts(ApplicationLoader.applicationContext);
        MediaDataController.getInstance(UserConfig.selectedAccount).buildShortcuts();
    }

    public static void toggleStreamMedia() {
        streamMedia = !streamMedia;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamMedia", streamMedia);
        editor.apply();
    }

    public static void toggleSortContactsByName() {
        sortContactsByName = !sortContactsByName;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("sortContactsByName", sortContactsByName);
        editor.apply();
    }

    public static void toggleSortFilesByName() {
        sortFilesByName = !sortFilesByName;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("sortFilesByName", sortFilesByName);
        editor.apply();
    }

    public static void toggleStreamAllVideo() {
        streamAllVideo = !streamAllVideo;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamAllVideo", streamAllVideo);
        editor.apply();
    }

    public static void toggleStreamMkv() {
        streamMkv = !streamMkv;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamMkv", streamMkv);
        editor.apply();
    }

    public static void toggleSaveStreamMedia() {
        saveStreamMedia = !saveStreamMedia;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("saveStreamMedia", saveStreamMedia);
        editor.apply();
    }

    public static void togglePauseMusicOnRecord() {
        pauseMusicOnRecord = !pauseMusicOnRecord;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pauseMusicOnRecord", pauseMusicOnRecord);
        editor.apply();
    }

    public static void togglePauseMusicOnMedia() {
        pauseMusicOnMedia = !pauseMusicOnMedia;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pauseMusicOnMedia", pauseMusicOnMedia);
        editor.apply();
    }

    public static void toggleChatBlur() {
        LiteMode.toggleFlag(LiteMode.FLAG_CHAT_BLUR);
    }

    public static void toggleForceDisableTabletMode() {
        forceDisableTabletMode = !forceDisableTabletMode;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("forceDisableTabletMode", forceDisableTabletMode);
        editor.apply();
    }

    public static void toggleInappCamera() {
        inappCamera = !inappCamera;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("inappCamera", inappCamera);
        editor.apply();
    }

    public static void toggleRoundCamera16to9() {
        roundCamera16to9 = !roundCamera16to9;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("roundCamera16to9", roundCamera16to9);
        editor.apply();
    }

    public static void setDistanceSystemType(int type) {
        distanceSystemType = type;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("distanceSystemType", distanceSystemType);
        editor.apply();
        LocaleController.resetImperialSystemType();
    }

    public static void loadProxyList() {
        if (proxyListLoaded) {
            return;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        String proxyAddress = preferences.getString("proxy_ip", "");
        String proxyUsername = preferences.getString("proxy_user", "");
        String proxyPassword = preferences.getString("proxy_pass", "");
        String proxySecret = preferences.getString("proxy_secret", "");
        int proxyPort = preferences.getInt("proxy_port", 1080);

        proxyListLoaded = true;
        proxyList.clear();
        currentProxy = null;
        String list = preferences.getString("proxy_list", null);
        if (!TextUtils.isEmpty(list)) {
            byte[] bytes = Base64.decode(list, Base64.DEFAULT);
            SerializedData data = new SerializedData(bytes);
            int count = data.readInt32(false);
            if (count == -1) { // V2 or newer
                int version = data.readByte(false);

                if (version == PROXY_SCHEMA_V2 || version == PROXY_SCHEMA_V3 || version == PROXY_SCHEMA_V4) {
                    count = data.readInt32(false);

                    for (int i = 0; i < count; i++) {
                        ProxyInfo info = new ProxyInfo(
                                data.readString(false),
                                data.readInt32(false),
                                data.readString(false),
                                data.readString(false),
                                data.readString(false),
                                version >= PROXY_SCHEMA_V3 ? data.readString(false) : "");

                        if (version >= PROXY_SCHEMA_V4) {
                            info.builtin = data.readBool(false);
                        }

                        info.ping = data.readInt64(false);
                        info.availableCheckTime = data.readInt64(false);

                        proxyList.add(0, info);
                        if (currentProxy == null && !TextUtils.isEmpty(proxyAddress)) {
                            if (proxyAddress.equals(info.address) && proxyPort == info.port && proxyUsername.equals(info.username) && proxyPassword.equals(info.password)) {
                                currentProxy = info;
                            }
                        }
                    }
                } else {
                    FileLog.e("Unknown proxy schema version: " + version);
                }
            } else {
                for (int a = 0; a < count; a++) {
                    ProxyInfo info = new ProxyInfo(
                            data.readString(false),
                            data.readInt32(false),
                            data.readString(false),
                            data.readString(false),
                            data.readString(false));
                    proxyList.add(0, info);
                    if (currentProxy == null && !TextUtils.isEmpty(proxyAddress)) {
                        if (proxyAddress.equals(info.address) && proxyPort == info.port && proxyUsername.equals(info.username) && proxyPassword.equals(info.password)) {
                            currentProxy = info;
                        }
                    }
                }
            }
            data.cleanup();
        }
        if (currentProxy == null && !TextUtils.isEmpty(proxyAddress)) {
            ProxyInfo info = currentProxy = new ProxyInfo(proxyAddress, proxyPort, proxyUsername, proxyPassword, proxySecret);
            proxyList.add(0, info);
        }
    }

    public static void saveProxyList() {
        List<ProxyInfo> infoToSerialize = new ArrayList<>(proxyList);
        Collections.sort(infoToSerialize, (o1, o2) -> {
            long bias1 = SharedConfig.currentProxy == o1 ? -200000 : 0;
            if (!o1.available) {
                bias1 += 100000;
            }
            long bias2 = SharedConfig.currentProxy == o2 ? -200000 : 0;
            if (!o2.available) {
                bias2 += 100000;
            }
            return Long.compare(o1.ping + bias1, o2.ping + bias2);
        });
        SerializedData serializedData = new SerializedData();
        serializedData.writeInt32(-1);
        serializedData.writeByte(PROXY_CURRENT_SCHEMA_VERSION);
        int count = infoToSerialize.size();
        serializedData.writeInt32(count);
        for (int a = count - 1; a >= 0; a--) {
            ProxyInfo info = infoToSerialize.get(a);
            serializedData.writeString(info.address != null ? info.address : "");
            serializedData.writeInt32(info.port);
            serializedData.writeString(info.username != null ? info.username : "");
            serializedData.writeString(info.password != null ? info.password : "");
            serializedData.writeString(info.secret != null ? info.secret : "");
            serializedData.writeString(info.displayName != null ? info.displayName : "");
            serializedData.writeBool(info.builtin);

            serializedData.writeInt64(info.ping);
            serializedData.writeInt64(info.availableCheckTime);
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putString("proxy_list", Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP)).apply();
        serializedData.cleanup();
    }

    public static ProxyInfo addProxy(ProxyInfo proxyInfo) {
        return addProxy(proxyInfo, false);
    }

    public static ProxyInfo addProxy(ProxyInfo proxyInfo, boolean userOwned) {
        loadProxyList();
        int count = proxyList.size();
        for (int a = 0; a < count; a++) {
            ProxyInfo info = proxyList.get(a);
            if (proxyInfo.address.equals(info.address) && proxyInfo.port == info.port && proxyInfo.username.equals(info.username) && proxyInfo.password.equals(info.password) && proxyInfo.secret.equals(info.secret)) {
                if (userOwned && info.builtin) {
                    info.builtin = false;
                    saveProxyList();
                }
                if (!TextUtils.isEmpty(proxyInfo.displayName) && !TextUtils.equals(proxyInfo.displayName, info.displayName)) {
                    info.displayName = proxyInfo.displayName;
                    saveProxyList();
                }
                return info;
            }
        }
        proxyList.add(0, proxyInfo);
        saveProxyList();
        return proxyInfo;
    }

    public static boolean isProxyEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false) && currentProxy != null;
    }

    public static void deleteProxy(ProxyInfo proxyInfo) {
        if (currentProxy == proxyInfo) {
            currentProxy = null;
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            boolean enabled = preferences.getBoolean("proxy_enabled", false);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("proxy_ip", "");
            editor.putString("proxy_pass", "");
            editor.putString("proxy_user", "");
            editor.putString("proxy_secret", "");
            editor.putInt("proxy_port", 1080);
            editor.putBoolean("proxy_enabled", false);
            editor.putBoolean("proxy_enabled_calls", false);
            editor.apply();
            if (enabled) {
                ConnectionsManager.setProxySettings(false, "", 0, "", "", "");
            }
        }
        proxyList.remove(proxyInfo);
        saveProxyList();
    }

    public static void checkSaveToGalleryFiles() {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                File telegramPath = new File(Environment.getExternalStorageDirectory(), "Telegram");
                File imagePath = new File(telegramPath, "Telegram Images");
                imagePath.mkdir();
                File videoPath = new File(telegramPath, "Telegram Video");
                videoPath.mkdir();

                if (!BuildVars.NO_SCOPED_STORAGE) {
                    if (imagePath.isDirectory()) {
                        new File(imagePath, ".nomedia").delete();
                    }
                    if (videoPath.isDirectory()) {
                        new File(videoPath, ".nomedia").delete();
                    }
                } else {
                    if (imagePath.isDirectory()) {
                        AndroidUtilities.createEmptyFile(new File(imagePath, ".nomedia"));
                    }
                    if (videoPath.isDirectory()) {
                        AndroidUtilities.createEmptyFile(new File(videoPath, ".nomedia"));
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    public static int getChatSwipeAction(int currentAccount) {
        if (chatSwipeAction >= 0) {
            if (chatSwipeAction == SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS && MessagesController.getInstance(currentAccount).dialogFilters.isEmpty()) {
                return SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE;
            }
            return chatSwipeAction;
        } else if (!MessagesController.getInstance(currentAccount).dialogFilters.isEmpty()) {
            return SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS;

        }
        return SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE;
    }

    public static void updateChatListSwipeSetting(int newAction) {
        chatSwipeAction = newAction;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("ChatSwipeAction", chatSwipeAction).apply();
    }

    public static void updateMessageSeenHintCount(int count) {
        messageSeenHintCount = count;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("messageSeenCount", messageSeenHintCount).apply();
    }

    public static void updateEmojiInteractionsHintCount(int count) {
        emojiInteractionsHintCount = count;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("emojiInteractionsHintCount", emojiInteractionsHintCount).apply();
    }

    public static void updateDayNightThemeSwitchHintCount(int count) {
        dayNightThemeSwitchHintCount = count;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("dayNightThemeSwitchHintCount", dayNightThemeSwitchHintCount).apply();
    }

    public static void updateStealthModeSendMessageConfirm(int count) {
        stealthModeSendMessageConfirm = count;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("stealthModeSendMessageConfirm", stealthModeSendMessageConfirm).apply();
    }

    public final static int PERFORMANCE_CLASS_LOW = 0;
    public final static int PERFORMANCE_CLASS_AVERAGE = 1;
    public final static int PERFORMANCE_CLASS_HIGH = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            PERFORMANCE_CLASS_LOW,
            PERFORMANCE_CLASS_AVERAGE,
            PERFORMANCE_CLASS_HIGH
    })
    public @interface PerformanceClass {}

    @PerformanceClass
    public static int getDevicePerformanceClass() {
        if (overrideDevicePerformanceClass != -1) {
            return overrideDevicePerformanceClass;
        }
        if (devicePerformanceClass == -1) {
            devicePerformanceClass = measureDevicePerformanceClass();
        }
        return devicePerformanceClass;
    }

    public static int measureDevicePerformanceClass() {
        int androidVersion = Build.VERSION.SDK_INT;
        int cpuCount = ConnectionsManager.CPU_COUNT;
        int memoryClass = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null) {
            int hash = Build.SOC_MODEL.toUpperCase().hashCode();
            for (int i = 0; i < LOW_SOC.length; ++i) {
                if (LOW_SOC[i] == hash) {
                    return PERFORMANCE_CLASS_LOW;
                }
            }
        }

        int totalCpuFreq = 0;
        int freqResolved = 0;
        for (int i = 0; i < cpuCount; i++) {
            try {
                RandomAccessFile reader = new RandomAccessFile(String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i), "r");
                String line = reader.readLine();
                if (line != null) {
                    totalCpuFreq += Utilities.parseInt(line) / 1000;
                    freqResolved++;
                }
                reader.close();
            } catch (Throwable ignore) {}
        }
        int maxCpuFreq = freqResolved == 0 ? -1 : (int) Math.ceil(totalCpuFreq / (float) freqResolved);

        long ram = -1;
        try {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
            ram = memoryInfo.totalMem;
        } catch (Exception ignore) {}

        int performanceClass;
        if (
            androidVersion < 21 ||
            cpuCount <= 2 ||
            memoryClass <= 100 ||
            cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 ||
            cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 ||
            cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24 ||
            ram != -1 && ram < 2L * 1024L * 1024L * 1024L
        ) {
            performanceClass = PERFORMANCE_CLASS_LOW;
        } else if (
            cpuCount < 8 ||
            memoryClass <= 160 ||
            maxCpuFreq != -1 && maxCpuFreq <= 2055 ||
            maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23
        ) {
            performanceClass = PERFORMANCE_CLASS_AVERAGE;
        } else {
            performanceClass = PERFORMANCE_CLASS_HIGH;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("device performance info selected_class = " + performanceClass + " (cpu_count = " + cpuCount + ", freq = " + maxCpuFreq + ", memoryClass = " + memoryClass + ", android version " + androidVersion + ", manufacture " + Build.MANUFACTURER + ", screenRefreshRate=" + AndroidUtilities.screenRefreshRate + ", screenMaxRefreshRate=" + AndroidUtilities.screenMaxRefreshRate + ")");
        }

        return performanceClass;
    }

    public static String performanceClassName(int perfClass) {
        switch (perfClass) {
            case PERFORMANCE_CLASS_HIGH: return "HIGH";
            case PERFORMANCE_CLASS_AVERAGE: return "AVERAGE";
            case PERFORMANCE_CLASS_LOW: return "LOW";
            default: return "UNKNOWN";
        }
    }

    public static void setMediaColumnsCount(int count) {
        if (mediaColumnsCount != count) {
            mediaColumnsCount = count;
            ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().putInt("mediaColumnsCount", mediaColumnsCount).apply();
        }
    }

    public static void setStoriesColumnsCount(int count) {
        if (storiesColumnsCount != count) {
            storiesColumnsCount = count;
            ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().putInt("storiesColumnsCount", storiesColumnsCount).apply();
        }
    }

    public static void setFastScrollHintCount(int count) {
        if (fastScrollHintCount != count) {
            fastScrollHintCount = count;
            ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().putInt("fastScrollHintCount", fastScrollHintCount).apply();
        }
    }

    public static void setDontAskManageStorage(boolean b) {
        dontAskManageStorage = b;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().putBoolean("dontAskManageStorage", dontAskManageStorage).apply();
    }

    public static boolean canBlurChat() {
        return getDevicePerformanceClass() >= (Build.VERSION.SDK_INT >= 31 ? PERFORMANCE_CLASS_AVERAGE : PERFORMANCE_CLASS_HIGH) || BuildVars.DEBUG_PRIVATE_VERSION;
    }

    public static boolean chatBlurEnabled() {
        return canBlurChat() && LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR);
    }

    public static class BackgroundActivityPrefs {
        private static SharedPreferences prefs;

        public static long getLastCheckedBackgroundActivity() {
            return prefs.getLong("last_checked", 0);
        }

        public static void setLastCheckedBackgroundActivity(long l) {
            prefs.edit().putLong("last_checked", l).apply();
        }

        public static int getDismissedCount() {
            return prefs.getInt("dismissed_count", 0);
        }

        public static void increaseDismissedCount() {
            prefs.edit().putInt("dismissed_count", getDismissedCount() + 1).apply();
        }
    }

    private static Boolean animationsEnabled;

    public static void setAnimationsEnabled(boolean b) {
        animationsEnabled = b;
    }

    public static boolean animationsEnabled() {
        if (animationsEnabled == null) {
            animationsEnabled = MessagesController.getGlobalMainSettings().getBoolean("view_animations", true);
        }
        return animationsEnabled;
    }

    public static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
    }

    public static boolean deviceIsLow() {
        return getDevicePerformanceClass() == PERFORMANCE_CLASS_LOW;
    }

    public static boolean deviceIsAboveAverage() {
        return getDevicePerformanceClass() >= PERFORMANCE_CLASS_AVERAGE;
    }

    public static boolean deviceIsHigh() {
        return getDevicePerformanceClass() >= PERFORMANCE_CLASS_HIGH;
    }

    public static boolean deviceIsAverage() {
        return getDevicePerformanceClass() <= PERFORMANCE_CLASS_AVERAGE;
    }

    public static void toggleRoundCamera() {
        bigCameraForRound = !bigCameraForRound;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("bigCameraForRound", bigCameraForRound)
                .apply();
    }

    public static void toggleUseNewBlur() {
        useNewBlur = !useNewBlur;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("useNewBlur", useNewBlur)
                .apply();
    }

    public static boolean isUsingCamera2(int currentAccount) {
        return useCamera2Force == null ? !MessagesController.getInstance(currentAccount).androidDisableRoundCamera2 : useCamera2Force;
    }

    public static void toggleUseCamera2(int currentAccount) {
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("useCamera2Force_2", useCamera2Force = !isUsingCamera2(currentAccount))
                .apply();
    }


    @Deprecated
    public static int getLegacyDevicePerformanceClass() {
        if (legacyDevicePerformanceClass == -1) {
            int androidVersion = Build.VERSION.SDK_INT;
            int cpuCount = ConnectionsManager.CPU_COUNT;
            int memoryClass = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
            int totalCpuFreq = 0;
            int freqResolved = 0;
            for (int i = 0; i < cpuCount; i++) {
                try {
                    RandomAccessFile reader = new RandomAccessFile(String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i), "r");
                    String line = reader.readLine();
                    if (line != null) {
                        totalCpuFreq += Utilities.parseInt(line) / 1000;
                        freqResolved++;
                    }
                    reader.close();
                } catch (Throwable ignore) {}
            }
            int maxCpuFreq = freqResolved == 0 ? -1 : (int) Math.ceil(totalCpuFreq / (float) freqResolved);

            if (androidVersion < 21 || cpuCount <= 2 || memoryClass <= 100 || cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 || cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 || cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24) {
                legacyDevicePerformanceClass = PERFORMANCE_CLASS_LOW;
            } else if (cpuCount < 8 || memoryClass <= 160 || maxCpuFreq != -1 && maxCpuFreq <= 2050 || maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23) {
                legacyDevicePerformanceClass = PERFORMANCE_CLASS_AVERAGE;
            } else {
                legacyDevicePerformanceClass = PERFORMANCE_CLASS_HIGH;
            }
        }
        return legacyDevicePerformanceClass;
    }


    //DEBUG
    public static boolean drawActionBarShadow = true;

    private static void loadDebugConfig(SharedPreferences preferences) {
        drawActionBarShadow = preferences.getBoolean("drawActionBarShadow", true);
    }

    public static void saveDebugConfig() {
        SharedPreferences pref = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        pref.edit().putBoolean("drawActionBarShadow", drawActionBarShadow);
    }



}
