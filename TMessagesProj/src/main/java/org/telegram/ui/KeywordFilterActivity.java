package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.KeywordMessageFilterManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

public class KeywordFilterActivity extends BaseFragment {

    private static final int ITEM_ID_ENABLE = 1;
    private static final int ITEM_ID_ADD_KEYWORD = 2;
    private static final int ITEM_ID_KEYWORD = 3;
    private static final int ITEM_ID_ADD_WHITELIST = 4;
    private static final int ITEM_ID_WHITELIST = 5;
    private static final int ITEM_ID_CLEAR_LOGS = 6;
    private static final int ITEM_ID_LOG = 7;
    private static final int ITEM_ID_BLOCK_SPONSORED_MESSAGES_FULL = 8;
    private static final int ITEM_ID_REMOTE_UPDATES = 9;

    private static final int ACTION_MODE_DELETE = 100;

    private UniversalRecyclerView listView;
    private boolean keywordSelectionMode;
    private boolean whitelistSelectionMode;
    private final HashSet<String> selectedKeywords = new HashSet<>();
    private final HashSet<Long> selectedWhitelistDialogs = new HashSet<>();

    private NumberTextView selectedCountTextView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.KeywordFilterSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        hideSelectionActionMode(true);
                    } else {
                        finishFragment();
                    }
                } else if (id == ACTION_MODE_DELETE) {
                    if (keywordSelectionMode) {
                        deleteSelectedKeywords();
                    } else {
                        deleteSelectedWhitelistDialogs();
                    }
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, this::onItemLongClick);
        listView.setSections();
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (actionBar != null && actionBar.isActionModeShowed()) {
            hideSelectionActionMode(false);
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        KeywordMessageFilterManager manager = getFilterManager();
        ArrayList<String> keywords = manager.getKeywords();
        ArrayList<KeywordMessageFilterManager.WhitelistDialog> whitelistDialogs = manager.getWhitelistDialogs();
        ArrayList<KeywordMessageFilterManager.LogEntry> logs = manager.getLogs();

        if (keywordSelectionMode && keywords.isEmpty() || whitelistSelectionMode && whitelistDialogs.isEmpty()) {
            hideSelectionActionMode(false);
        }

        items.add(UItem.asSwitch(ITEM_ID_ENABLE, getString(R.string.KeywordFilterEnable)).setChecked(manager.isEnabled()));
        items.add(UItem.asSwitch(ITEM_ID_REMOTE_UPDATES, getString(R.string.KeywordFilterRemoteUpdates)).setChecked(manager.isRemoteUpdatesEnabled()));
        items.add(UItem.asSwitch(ITEM_ID_BLOCK_SPONSORED_MESSAGES_FULL, getString(R.string.BlockSponsoredMessagesFull)).setChecked(SharedConfig.isBlockSponsoredMessagesFullEnabled()));
        items.add(UItem.asShadow(getString(R.string.KeywordFilterEnableInfo)));

        items.add(UItem.asHeader(getString(R.string.KeywordFilterWhitelist)));
        items.add(UItem.asButton(ITEM_ID_ADD_WHITELIST, "+ " + getString(R.string.KeywordFilterAddWhitelist)).accent());
        if (whitelistDialogs.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.KeywordFilterNoWhitelist)));
        } else {
            for (int i = 0; i < whitelistDialogs.size(); i++) {
                KeywordMessageFilterManager.WhitelistDialog dialog = whitelistDialogs.get(i);
                UItem item;
                if (whitelistSelectionMode) {
                    item = UItem.asRoundCheckbox(ITEM_ID_WHITELIST, buildWhitelistTitle(dialog))
                            .setChecked(selectedWhitelistDialogs.contains(dialog.dialogId));
                } else {
                    item = UItem.asSettingsCell(ITEM_ID_WHITELIST, dialog.title, getString(dialog.user ? R.string.KeywordFilterUser : dialog.channel ? R.string.KeywordFilterChannel : R.string.KeywordFilterGroup));
                }
                item.longValue = dialog.dialogId;
                item.object = dialog;
                items.add(item);
            }
            items.add(UItem.asShadow(getString(R.string.KeywordFilterWhitelistInfo)));
        }

        items.add(UItem.asHeader(getString(R.string.KeywordFilterKeywords)));
        items.add(UItem.asButton(ITEM_ID_ADD_KEYWORD, "+ " + getString(R.string.KeywordFilterAddKeyword)).accent());
        if (keywords.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.KeywordFilterNoKeywords)));
        } else {
            for (int i = 0; i < keywords.size(); i++) {
                UItem item;
                if (keywordSelectionMode) {
                    item = UItem.asRoundCheckbox(ITEM_ID_KEYWORD, keywords.get(i)).setChecked(selectedKeywords.contains(keywords.get(i)));
                } else {
                    item = UItem.asSettingsCell(ITEM_ID_KEYWORD, keywords.get(i), getString(R.string.KeywordFilterKeywordRowHint));
                }
                item.object = keywords.get(i);
                items.add(item);
            }
        }

        items.add(UItem.asHeader(getString(R.string.KeywordFilterLogs)));
        if (!logs.isEmpty()) {
            items.add(UItem.asButton(ITEM_ID_CLEAR_LOGS, getString(R.string.KeywordFilterClearLogs)).red());
            for (int i = 0; i < logs.size(); i++) {
                KeywordMessageFilterManager.LogEntry log = logs.get(i);
                UItem item = UItem.asSettingsCell(ITEM_ID_LOG, getLogTitle(log), LocaleController.formatString(R.string.KeywordFilterMatchedKeywordLabel, log.keyword));
                item.object = log;
                items.add(item);
            }
        } else {
            items.add(UItem.asShadow(getString(R.string.KeywordFilterNoLogs)));
        }
        items.add(UItem.asShadow(getString(R.string.KeywordFilterLogsInfo)));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        KeywordMessageFilterManager manager = getFilterManager();
        if (actionBar.isActionModeShowed()
                && !(keywordSelectionMode && item.id == ITEM_ID_KEYWORD)
                && !(whitelistSelectionMode && item.id == ITEM_ID_WHITELIST)) {
            return;
        }

        switch (item.id) {
            case ITEM_ID_ENABLE: {
                boolean enabled = !manager.isEnabled();
                manager.setEnabled(enabled);
                if (view instanceof TextCheckCell2) {
                    ((TextCheckCell2) view).setChecked(enabled);
                }
                refreshList();
                break;
            }
            case ITEM_ID_REMOTE_UPDATES: {
                boolean enabled = !manager.isRemoteUpdatesEnabled();
                manager.setRemoteUpdatesEnabled(enabled);
                if (view instanceof TextCheckCell2) {
                    ((TextCheckCell2) view).setChecked(enabled);
                }
                refreshList();
                break;
            }
            case ITEM_ID_BLOCK_SPONSORED_MESSAGES_FULL: {
                boolean enabled = !SharedConfig.isBlockSponsoredMessagesFullEnabled();
                SharedConfig.setBlockSponsoredMessagesFullEnabled(enabled);
                if (view instanceof TextCheckCell2) {
                    ((TextCheckCell2) view).setChecked(enabled);
                }
                refreshList();
                break;
            }
            case ITEM_ID_ADD_KEYWORD:
                showKeywordDialog(null);
                break;
            case ITEM_ID_KEYWORD:
                if (keywordSelectionMode) {
                    toggleKeywordSelection((String) item.object);
                } else {
                    showKeywordDialog((String) item.object);
                }
                break;
            case ITEM_ID_ADD_WHITELIST:
                showWhitelistSelector();
                break;
            case ITEM_ID_WHITELIST:
                if (whitelistSelectionMode) {
                    toggleWhitelistSelection(item.longValue);
                }
                break;
            case ITEM_ID_CLEAR_LOGS:
                manager.clearLogs();
                refreshList();
                break;
            case ITEM_ID_LOG:
                showLogDetail((KeywordMessageFilterManager.LogEntry) item.object);
                break;
        }
    }

    private boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (actionBar.isActionModeShowed()) {
            return false;
        }
        if (item.id == ITEM_ID_KEYWORD) {
            keywordSelectionMode = true;
            selectedKeywords.clear();
            selectedKeywords.add((String) item.object);
        } else if (item.id == ITEM_ID_WHITELIST) {
            whitelistSelectionMode = true;
            selectedWhitelistDialogs.clear();
            selectedWhitelistDialogs.add(item.longValue);
        } else {
            return false;
        }
        showOrUpdateSelectionActionMode();
        refreshList();
        return true;
    }

    private void showKeywordDialog(String existingKeyword) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }

        FrameLayout container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setBackgroundDrawable(Theme.createEditTextDrawable(context, true));
        editText.setHintText(getString(R.string.KeywordFilterEditKeywordHint));
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});
        editText.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        if (!TextUtils.isEmpty(existingKeyword)) {
            editText.setText(existingKeyword);
            editText.setSelection(existingKeyword.length());
        }
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourceProvider);
        builder.setTitle(getString(TextUtils.isEmpty(existingKeyword) ? R.string.KeywordFilterAddKeyword : R.string.KeywordFilterEditKeyword));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.Save), (dialog, which) -> {
            if (TextUtils.isEmpty(existingKeyword)) {
                getFilterManager().addKeyword(editText.getText().toString());
            } else {
                getFilterManager().updateKeyword(existingKeyword, editText.getText().toString());
            }
            refreshList();
        });
        if (!TextUtils.isEmpty(existingKeyword)) {
            builder.setNeutralButton(getString(R.string.Delete), (dialog, which) -> {
                getFilterManager().removeKeyword(existingKeyword);
                refreshList();
            });
        }
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 120);
    }

    private void showWhitelistSelector() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        args.putBoolean("allowGroups", true);
        args.putBoolean("allowMegagroups", true);
        args.putBoolean("allowLegacyGroups", true);
        args.putBoolean("allowChannels", true);
        args.putBoolean("allowUsers", true);
        args.putBoolean("allowBots", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowGlobalSearch", false);
        args.putString("selectAlertString", LocaleController.getString(R.string.SendMessagesToText));
        args.putString("selectAlertStringGroup", LocaleController.getString(R.string.SendMessagesToGroupText));

        DialogsActivity fragment = new DialogsActivity(args) {
            @Override
            public ArrayList<TLRPC.Dialog> getDialogsArray(int currentAccount, int dialogsType, int folderId, boolean frozen) {
                ArrayList<TLRPC.Dialog> sourceDialogs = super.getDialogsArray(currentAccount, DialogsActivity.DIALOGS_TYPE_DEFAULT, folderId, frozen);
                ArrayList<TLRPC.Dialog> filteredDialogs = new ArrayList<>(sourceDialogs.size());
                for (int i = 0; i < sourceDialogs.size(); i++) {
                    TLRPC.Dialog dialog = sourceDialogs.get(i);
                    if (dialog == null || dialog.id == 0) {
                        continue;
                    }
                    if (dialog.id > 0) {
                        if (allowUsers) {
                            filteredDialogs.add(dialog);
                        }
                    } else {
                        TLRPC.Chat chat = getMessagesController().getChat(-dialog.id);
                        if (chat == null) {
                            continue;
                        }
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            if (allowChannels) {
                                filteredDialogs.add(dialog);
                            }
                        } else if (chat.megagroup) {
                            if (allowGroups && allowMegagroups) {
                                filteredDialogs.add(dialog);
                            }
                        } else if (allowGroups && allowLegacyGroups) {
                            filteredDialogs.add(dialog);
                        }
                    }
                }
                return filteredDialogs;
            }

            @Override
            public boolean shouldShowNextButton(DialogsActivity fragment, ArrayList<Long> dids, CharSequence message, boolean param) {
                return dids != null && !dids.isEmpty();
            }
        };
        fragment.setCurrentAccount(currentAccount);
        fragment.setDelegate((dialogsActivity, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            ArrayList<KeywordMessageFilterManager.WhitelistDialog> dialogs = new ArrayList<>();
            for (int i = 0; i < dids.size(); i++) {
                MessagesStorage.TopicKey key = dids.get(i);
                long dialogId = key.dialogId;
                if (dialogId == 0) {
                    continue;
                }
                if (dialogId > 0) {
                    TLRPC.User user = getMessagesController().getUser(dialogId);
                    if (user != null) {
                        dialogs.add(new KeywordMessageFilterManager.WhitelistDialog(
                                dialogId,
                                UserObject.getUserName(user),
                                false,
                                true
                        ));
                    }
                } else {
                    TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                    if (chat != null) {
                        dialogs.add(new KeywordMessageFilterManager.WhitelistDialog(
                                dialogId,
                                chat.title,
                                ChatObject.isChannel(chat) && !chat.megagroup,
                                false
                        ));
                    }
                }
            }
            getFilterManager().addWhitelistDialogs(dialogs);
            refreshList();
            dialogsActivity.finishFragment();
            return true;
        });
        presentFragment(fragment);
    }

    private void showLogDetail(KeywordMessageFilterManager.LogEntry log) {
        if (log == null || getParentActivity() == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append(LocaleController.formatString(R.string.KeywordFilterMatchedKeywordLabel, log.keyword));
        message.append("\n");
        message.append(LocaleController.formatString(R.string.KeywordFilterTimeLabel, LocaleController.formatDateTime(log.timestamp, true)));
        if (!TextUtils.isEmpty(log.snippet)) {
            message.append("\n");
            message.append(LocaleController.formatString(R.string.KeywordFilterContentLabel, log.snippet));
        }
        showDialog(new AlertDialog.Builder(getParentActivity(), resourceProvider)
                .setTitle(getLogTitle(log))
                .setMessage(message)
                .setPositiveButton(getString(R.string.OK), null)
                .create());
    }

    private void createActionMode() {
        if (actionBar.actionModeIsExist(null)) {
            return;
        }
        ActionBarMenu actionMode = actionBar.createActionMode();

        selectedCountTextView = new NumberTextView(actionMode.getContext());
        selectedCountTextView.setTextSize(18);
        selectedCountTextView.setTypeface(AndroidUtilities.bold());
        selectedCountTextView.setTextColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        selectedCountTextView.setOnTouchListener((v, event) -> true);

        actionMode.addItemWithWidth(ACTION_MODE_DELETE, R.drawable.msg_delete, AndroidUtilities.dp(54), getString(R.string.Delete));
    }

    private void showOrUpdateSelectionActionMode() {
        int selectedCount = keywordSelectionMode ? selectedKeywords.size() : selectedWhitelistDialogs.size();
        if (selectedCount == 0) {
            hideSelectionActionMode(true);
            return;
        }
        createActionMode();
        if (!actionBar.isActionModeShowed()) {
            actionBar.showActionMode();
        }
        if (selectedCountTextView != null) {
            selectedCountTextView.setNumber(selectedCount, true);
        }
    }

    private void hideSelectionActionMode(boolean animated) {
        keywordSelectionMode = false;
        whitelistSelectionMode = false;
        selectedKeywords.clear();
        selectedWhitelistDialogs.clear();
        if (actionBar != null) {
            actionBar.hideActionMode();
        }
        refreshList();
    }

    private void toggleWhitelistSelection(long dialogId) {
        if (selectedWhitelistDialogs.contains(dialogId)) {
            selectedWhitelistDialogs.remove(dialogId);
        } else {
            selectedWhitelistDialogs.add(dialogId);
        }
        showOrUpdateSelectionActionMode();
        refreshList();
    }

    private void toggleKeywordSelection(String keyword) {
        if (selectedKeywords.contains(keyword)) {
            selectedKeywords.remove(keyword);
        } else {
            selectedKeywords.add(keyword);
        }
        showOrUpdateSelectionActionMode();
        refreshList();
    }

    private void deleteSelectedKeywords() {
        if (selectedKeywords.isEmpty()) {
            return;
        }
        getFilterManager().removeKeywords(new ArrayList<>(selectedKeywords));
        hideSelectionActionMode(false);
        refreshList();
    }

    private void deleteSelectedWhitelistDialogs() {
        if (selectedWhitelistDialogs.isEmpty()) {
            return;
        }
        getFilterManager().removeWhitelistDialogs(new ArrayList<>(selectedWhitelistDialogs));
        hideSelectionActionMode(false);
        refreshList();
    }

    private void refreshList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    private KeywordMessageFilterManager getFilterManager() {
        return KeywordMessageFilterManager.getInstance(currentAccount);
    }

    private String buildWhitelistTitle(KeywordMessageFilterManager.WhitelistDialog dialog) {
        return getString(dialog.user ? R.string.KeywordFilterUser : dialog.channel ? R.string.KeywordFilterChannel : R.string.KeywordFilterGroup) + " · " + dialog.title;
    }

    private String getLogTitle(KeywordMessageFilterManager.LogEntry log) {
        if (TextUtils.isEmpty(log.dialogTitle)) {
            return getString(R.string.KeywordFilterSettings);
        }
        return log.dialogTitle;
    }
}
