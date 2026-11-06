#!/usr/bin/env python3
"""Exercise real settings-account recoloring methods with lightweight Java views.

These are offline behavior/source regression tests, not Android renderer or
device tests. Production Java methods are extracted fresh on every invocation.
"""
from pathlib import Path
import os
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "TMessagesProj/src/main/java"
JAVA = Path(os.environ.get(
    "JAVA_HOME", "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
)) / "bin"


def block(source, marker):
    start = source.index(marker)
    end = source.index("{", start) + 1
    depth = 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


settings = (SOURCE / "org/telegram/ui/SettingsActivity.java").read_text()
section = block(settings, "    private class AccountSectionView extends")
account = block(settings, "    public static class AccountCell extends")
text = (SOURCE / "org/telegram/ui/Cells/TextCell.java").read_text()
gray = (SOURCE / "org/telegram/ui/Cells/GraySectionCell.java").read_text()
utilities = (SOURCE / "org/telegram/messenger/AndroidUtilities.java").read_text()
parent_colors = block(settings[settings.index("    public void setInfo() {"):],
                      "    public void updateColors()")
section_colors = block(section, "        public void updateColors()")
account_colors = block(account, "        public void updateColors()")
section_attach = block(section, "        protected void onAttachedToWindow()")
section_set = block(section, "        public void setAccounts(")
parent_resume = block(settings, "    public void onResume()")
restore_accounts = block(settings, "    private void restoreAccountsExpanded()")
toggle_accounts = block(settings, "    private void toggleAccountsExpanded()")
text_colors = block(text, "    public void updateColors()")
gray_colors = block(gray, "    public void updateColors()")
recycler_each = block(utilities, "    public static void forEachViews(RecyclerView recyclerView,")
view_each = block(utilities, "    public static void forEachViews(View view,")

source_checks = 0


def source_check(condition, reason):
    global source_checks
    source_checks += 1
    assert condition, reason


constructor = block(section, "        public AccountSectionView(")
source_check("accountsListView.adapter.setApplyBackground(false);" in constructor,
             "nested rows must not retain adapter-created day/night backgrounds")
source_check("resourcesProvider);" in constructor and
             "new UniversalRecyclerView(context, currentAccount, classGuid" in constructor,
             "nested rows must receive the section's resources provider")
source_check("headerView.setNoBackground(true);" in constructor,
             "header shares the rounded card surface")
source_check("implements Theme.Colorable" not in text.split("public class TextCell", 1)[1].split("{", 1)[0],
             "TextCell intentionally needs an explicit recolor branch")
source_check("child instanceof TextCell" in section_colors,
             "add/collapse action rows need their own non-Colorable refresh")
source_check("AndroidUtilities.forEachViews(accountsListView, child ->" in section_colors,
             "typed RecyclerView traversal must include retained rows")
source_check("child.setBackgroundColor(Color.TRANSPARENT)" in section_colors,
             "every retained row must stop covering the card surface")
source_check("setSelectorDrawableColor(Theme.getColor(Theme.key_listSelector, resourcesProvider))" in section_colors,
             "touch feedback must follow the same palette")
source_check("if (accountSectionView != null)" in parent_colors and
             "accountSectionView.updateColors();" in parent_colors,
             "theme transition explicitly reaches the nested section")
source_check("super.onAttachedToWindow();" in section_attach and
             "updateColors();" in section_attach,
             "first attachment refreshes the card even after an offscreen theme switch")
source_check("updateColors();" in section_set,
             "account expansion/list updates recolor the retained view")
source_check("Theme.getColor(Theme.key_featuredStickers_buttonText, resourcesProvider)" in account_colors,
             "unread badge text must be refreshed, not only its background")
source_check("botDrawable.setColor(" in account_colors and
             "emojiStatusDrawable.setColor(" in account_colors,
             "bot and emoji status decorations follow the theme")
content_colors = block(settings, "            public void updateColors()")
source_check("SettingsActivity.this.updateColors();" in content_colors,
             "theme-aware content view dispatches the settings refresh")
source_check("super.onResume();" in parent_resume and
             "if (contentView != null)" in parent_resume and "updateColors();" in parent_resume,
             "retained settings page refreshes when returning from a theme change")

fixture = r'''
import java.util.*;
import java.util.function.Consumer;

class Color {static final int TRANSPARENT=0;}
class Drawable {int color,radius;Drawable(int r,int c){radius=r;color=c;}}
class GradientDrawable extends Drawable {
 enum Orientation {TOP_BOTTOM}
 GradientDrawable(Orientation o,int[] colors){super(0,colors[colors.length-1]);}
}
class PorterDuff {enum Mode {MULTIPLY,SRC_IN}}
class PorterDuffColorFilter {
 int color;PorterDuff.Mode mode;
 PorterDuffColorFilter(int c,PorterDuff.Mode m){color=c;mode=m;}
}
class View {
 static final int VISIBLE=0,GONE=8;
 int backgroundColor=-987,invalidations,visibility=VISIBLE,attaches;
 Drawable background;
 FrameLayout.LayoutParams params=new FrameLayout.LayoutParams();
 void setBackground(Drawable b){background=b;backgroundColor=b.color;}
 void setBackgroundColor(int c){backgroundColor=c;}
 void invalidate(){invalidations++;}
 void setVisibility(int v){visibility=v;}
 Object getLayoutParams(){return params;}
 void setLayoutParams(FrameLayout.LayoutParams p){params=p;}
 protected void onAttachedToWindow(){attaches++;}
}
class ViewGroup extends View {
 ArrayList<View> children=new ArrayList<>();
 int getChildCount(){return children.size();}View getChildAt(int i){return children.get(i);}
}
class FrameLayout extends ViewGroup {static class LayoutParams {int height;}}
class RecyclerView extends ViewGroup {
 ArrayList<View> cached=new ArrayList<>(),hidden=new ArrayList<>(),scrap=new ArrayList<>();
 int getCachedChildCount(){return cached.size();}View getCachedChildAt(int i){return cached.get(i);}
 int getHiddenChildCount(){return hidden.size();}View getHiddenChildAt(int i){return hidden.get(i);}
 int getAttachedScrapChildCount(){return scrap.size();}View getAttachedScrapChildAt(int i){return scrap.get(i);}
}
class Adapter {int updates;void update(boolean animated){updates++;}}
class UniversalRecyclerView extends RecyclerView {
 final Adapter adapter=new Adapter();int selector=-987;
 void setSelectorDrawableColor(int c){selector=c;}
}
class Label extends View {
 int textColor=-987;Object tag;String text;
 void setTextColor(int c){textColor=c;}void setText(String s){text=s;}
 Object getTag(){return tag;}void setTag(Object t){tag=t;}
}
class ImageView extends View {
 PorterDuffColorFilter filter;
 Object tag;Object getTag(){return tag;}
 void setColorFilter(PorterDuffColorFilter f){filter=f;}
}
class Emoji {int color=-987;void setColor(int c){color=c;}}
class ActionBar {
 int title,items;void setTitleColor(int c){title=c;}void setItemsColor(int c,boolean actionMode){items=c;}
}
class Search {int updates;void updateColor(){updates++;}}
class R {static class string {static final int SettingsAccounts=1;}}
class Theme {
 interface Colorable {void updateColors();}
 static class ResourcesProvider {Map<Integer,Integer> colors=new HashMap<>();}
 static final int key_windowBackgroundWhiteBlackText=1,key_windowBackgroundWhiteGrayText=2,
  key_windowBackgroundWhite=3,key_listSelector=4,key_featuredStickers_buttonText=5,
  key_featuredStickers_addButton=6,key_windowBackgroundWhiteGrayIcon=7,
  key_profile_verifiedBackground=8,key_windowBackgroundGray=9,key_graySection=10,
  key_graySectionText=11,key_dialogTextBlack=12,key_dialogIcon=13,
  key_windowBackgroundWhiteValueText=14,key_windowBackgroundWhiteBlueText=15;
 static int palette;
 static int getColor(int key,ResourcesProvider p){return p!=null&&p.colors.containsKey(key)?p.colors.get(key):palette+key;}
 static Drawable createRoundRectDrawable(int radius,int color){return new Drawable(radius,color);}
 static int multAlpha(int color,float alpha){return alpha==0?0:color;}
}
class AndroidUtilities {
 /*RECYCLER_EACH*/
 /*VIEW_EACH*/
}
class TextCell extends FrameLayout {
 final Theme.ResourcesProvider resourcesProvider;
 Label textView=new Label(),subtitleView=new Label(),valueTextView=new Label(),valueSpoilersTextView=new Label();
 ImageView imageView=new ImageView();
 TextCell(Theme.ResourcesProvider p){resourcesProvider=p;textView.setTag(Theme.key_windowBackgroundWhiteBlueText);imageView.tag=Theme.key_windowBackgroundWhiteBlueText;}
 int processColor(int color){return color;}
 /*TEXT_COLORS*/
}
class GraySectionCell extends FrameLayout implements Theme.Colorable {
 final Theme.ResourcesProvider resourcesProvider;
 boolean noBackground=true;
 Label textView=new Label(),rightTextView=new Label();
 GraySectionCell(Theme.ResourcesProvider p){resourcesProvider=p;}
 void setText(String text){textView.setText(text);}
 int getThemedColor(int key){return Theme.getColor(key,resourcesProvider);}
 /*GRAY_COLORS*/
}
class AccountCell extends View implements Theme.Colorable {
 final Theme.ResourcesProvider resourcesProvider;
 Label textView=new Label(),counterView=new Label();
 ImageView arrowView=new ImageView();
 Emoji botDrawable=new Emoji(),emojiStatusDrawable=new Emoji();
 AccountCell(Theme.ResourcesProvider p){resourcesProvider=p;}
 static int dp(int x){return x;}
 /*ACCOUNT_COLORS*/
}
class OtherColorable extends View implements Theme.Colorable {
 int updates;public void updateColors(){updates++;}
}
class Fragment {int resumes;public void onResume(){resumes++;}}
class Preferences {
 final Map<String,Boolean> values=new HashMap<>();
 boolean getBoolean(String key,boolean fallback){return values.getOrDefault(key,fallback);}
 Preferences edit(){return this;}
 Preferences putBoolean(String key,boolean value){values.put(key,value);return this;}
 void apply(){}
}
class MessagesController {
 static final Preferences global=new Preferences();
 static Preferences getGlobalMainSettings(){return global;}
}
public class SettingsAccountThemeRegression extends Fragment {
 static final int ACCOUNT_ROW_HEIGHT_DP=48,ACCOUNT_ACTION_ROW_HEIGHT_DP=50;
 static final String PREF_ACCOUNTS_EXPANDED="mango_settings_accounts_expanded";
 boolean accountsExpanded;
 static int checks;
 static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static int dp(int x){return x;}
 static String getString(int id){return "Accounts";}
 Theme.ResourcesProvider resourcesProvider;
 ActionBar actionBar=new ActionBar();View contentView=new View();
 Label titleView=new Label(),subtitleView=new Label();Search searchItem=new Search();
 View navigationBar=new View(),actionBarBackground=new View();
 UniversalRecyclerView listView=new UniversalRecyclerView();
 AccountSectionView accountSectionView;
 int getThemedColor(int key){return Theme.getColor(key,resourcesProvider);}
 /*PARENT_COLORS*/
 /*PARENT_RESUME*/
 /*RESTORE_ACCOUNTS*/
 /*TOGGLE_ACCOUNTS*/
 static class AccountSectionView extends FrameLayout implements Theme.Colorable {
  final Theme.ResourcesProvider resourcesProvider;
  final FrameLayout cardContainer=new FrameLayout();
  final GraySectionCell headerView;
  final UniversalRecyclerView accountsListView=new UniversalRecyclerView();
  final ArrayList<Integer> accounts=new ArrayList<>();
  boolean canAddAccount,showAccounts;
  AccountSectionView(Theme.ResourcesProvider p){resourcesProvider=p;headerView=new GraySectionCell(p);}
  /*SECTION_SET*/
  /*SECTION_ATTACH*/
  /*SECTION_COLORS*/
 }
 static ArrayList<Integer> ids(Integer...ids){return new ArrayList<>(Arrays.asList(ids));}
 static void assertText(TextCell cell,Theme.ResourcesProvider provider,String state){
  check(cell.backgroundColor==0,state+": action row transparent");
  check(cell.textView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteBlueText,provider),state+": accent action text refreshed despite TextCell not implementing Colorable");
  check(cell.subtitleView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteGrayText,provider),state+": subtitle color refreshed");
  check(cell.valueTextView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteValueText,provider)&&cell.valueSpoilersTextView.textColor==cell.valueTextView.textColor,state+": both value colors refreshed");
  check(cell.imageView.filter.color==Theme.getColor(Theme.key_windowBackgroundWhiteBlueText,provider),state+": action icon refreshed");
  check(cell.invalidations>0,state+": action row invalidated");
 }
 static void assertAccount(AccountCell cell,Theme.ResourcesProvider provider,String state){
  check(cell.backgroundColor==0,state+": account row transparent");
  check(cell.textView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteBlackText,provider),state+": account name refreshed");
  check(cell.counterView.textColor==Theme.getColor(Theme.key_featuredStickers_buttonText,provider),state+": unread badge text refreshed");
  check(cell.counterView.background.color==Theme.getColor(Theme.key_featuredStickers_addButton,provider)&&cell.counterView.background.radius==10,state+": unread badge surface refreshed with radius preserved");
  check(cell.arrowView.filter.color==Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon,provider)&&cell.arrowView.filter.mode==PorterDuff.Mode.SRC_IN,state+": account arrow refreshed");
  check(cell.botDrawable.color==Theme.getColor(Theme.key_profile_verifiedBackground,provider)&&cell.emojiStatusDrawable.color==cell.botDrawable.color,state+": bot and emoji status accents refreshed");
 }
 static void assertCard(AccountSectionView section,Theme.ResourcesProvider provider,String state){
  check(section.cardContainer.background!=null,state+": card surface initialized");
  check(section.cardContainer.background.color==Theme.getColor(Theme.key_windowBackgroundWhite,provider),state+": card surface follows current palette");
  check(section.cardContainer.background.radius==18,state+": rounded card radius preserved");
  check(section.accountsListView.backgroundColor==0,state+": nested list transparent");
  check(section.accountsListView.selector==Theme.getColor(Theme.key_listSelector,provider),state+": selector follows palette");
  check(section.headerView.backgroundColor==0,state+": header transparent");
  check(section.headerView.textView.textColor==Theme.getColor(Theme.key_graySectionText,provider)&&section.headerView.rightTextView.textColor==section.headerView.textView.textColor,state+": header labels refreshed");
 }
 static void repeatedSwitches(){
  SettingsAccountThemeRegression parent=new SettingsAccountThemeRegression();
  AccountSectionView section=new AccountSectionView(null);parent.accountSectionView=section;
  TextCell add=new TextCell(null),expand=new TextCell(null),scrap=new TextCell(null);
  AccountCell visible=new AccountCell(null),cached=new AccountCell(null),hidden=new AccountCell(null);
  OtherColorable custom=new OtherColorable();View plain=new View();
  section.accountsListView.children.addAll(Arrays.asList(add,visible,custom,plain));
  section.accountsListView.cached.addAll(Arrays.asList(expand,cached));
  section.accountsListView.hidden.add(hidden);section.accountsListView.scrap.add(scrap);
  int expectedCustom=0;
  for(int palette:new int[]{1000,2000,1000,3000,2000,1000}){
   Theme.palette=palette;parent.updateColors();expectedCustom++;
   String state="palette "+palette+" iteration "+expectedCustom;
   assertCard(section,null,state);assertText(add,null,state+" visible");
   assertText(expand,null,state+" cached");assertText(scrap,null,state+" attached scrap");
   assertAccount(visible,null,state+" visible");assertAccount(cached,null,state+" cached");assertAccount(hidden,null,state+" hidden");
   check(custom.updates==expectedCustom&&custom.backgroundColor==0,state+": other Colorable rows keep receiving updates");
   check(plain.backgroundColor==0&&plain.invalidations==expectedCustom,state+": non-Colorable rows still clear stale backgrounds");
   check(parent.contentView.backgroundColor==Theme.getColor(Theme.key_windowBackgroundGray,null),state+": parent and card refresh together");
   check(parent.titleView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteBlackText,null)&&parent.subtitleView.textColor==Theme.getColor(Theme.key_windowBackgroundWhiteGrayText,null),state+": parent labels refresh together");
  }
  check(parent.searchItem.updates==6&&parent.listView.invalidations==6,"parent refresh dispatch still invalidates its own content");
 }
 static void collapseAndExpand(){
  AccountSectionView section=new AccountSectionView(null);
  TextCell expand=new TextCell(null);AccountCell account=new AccountCell(null);
  section.accountsListView.children.add(expand);section.accountsListView.cached.add(account);
  Theme.palette=1000;section.setAccounts(ids(1,3,8),false,true);
  check(!section.showAccounts&&section.accounts.equals(ids(1,3,8))&&section.canAddAccount,"collapsed state retains account identity/order");
  check(section.accountsListView.params.height==100&&section.accountsListView.visibility==View.VISIBLE,"collapsed state reserves only two action rows");
  Theme.palette=2000;section.updateColors();assertText(expand,null,"collapsed switch");assertAccount(account,null,"collapsed cache");
  section.accountsListView.cached.remove(account);section.accountsListView.children.add(account);
  section.setAccounts(ids(1,3,8),true,true);
  check(section.showAccounts&&section.accountsListView.params.height==244,"expanded state restores three account rows plus actions");
  assertAccount(account,null,"expanded retained row");assertCard(section,null,"expanded card");
  Theme.palette=3000;section.setAccounts(ids(8),false,false);
  check(section.accountsListView.params.height==50&&section.accounts.equals(ids(8)),"account update clears old account order and keeps collapse action");
  assertText(expand,null,"updated accounts");
  section.setAccounts(ids(),false,false);
  check(section.accountsListView.visibility==View.GONE&&section.accountsListView.params.height==0,"no accounts/add permission hides the empty list");
  section.setAccounts(ids(),true,true);
  check(section.accountsListView.visibility==View.VISIBLE&&section.accountsListView.params.height==50,"add-only account section remains usable");
  check(section.accountsListView.adapter.updates==5&&"Accounts".equals(section.headerView.textView.text),"each account change updates adapter and header");
 }
 static void resourceProvidersAndAttachment(){
  Theme.ResourcesProvider provider=new Theme.ResourcesProvider();
  for(int key=1;key<=15;key++)provider.colors.put(key,9000+key);
  Theme.palette=1000;AccountSectionView section=new AccountSectionView(provider);
  TextCell add=new TextCell(provider);AccountCell account=new AccountCell(provider);
  section.accountsListView.children.add(add);section.accountsListView.cached.add(account);
  section.onAttachedToWindow();
  check(section.attaches==1,"first attachment calls superclass lifecycle");
  assertCard(section,provider,"provider first attachment");assertText(add,provider,"provider first attachment");assertAccount(account,provider,"provider first attachment");
  check(section.cardContainer.background.color!=Theme.getColor(Theme.key_windowBackgroundWhite,null),"provider takes precedence over different global palette");
  for(int key=1;key<=15;key++)provider.colors.put(key,8000+key);
  Theme.palette=2000;section.onAttachedToWindow();
  check(section.attaches==2,"reattachment refreshes without rebuilding section");
  assertCard(section,provider,"provider reattachment");assertText(add,provider,"provider reattachment");assertAccount(account,provider,"provider reattachment");
  SettingsAccountThemeRegression parent=new SettingsAccountThemeRegression();parent.updateColors();
  check(parent.listView.invalidations==1,"parent recoloring before account-section creation is null-safe");
  AndroidUtilities.forEachViews((RecyclerView)null,child->{throw new AssertionError("null recycler callback");});
  check(true,"production recycler traversal accepts null");
  AccountSectionView empty=new AccountSectionView(null);empty.updateColors();assertCard(empty,null,"empty attached card");
 }
 static void retainedPageResume(){
  SettingsAccountThemeRegression parent=new SettingsAccountThemeRegression();
  parent.accountSectionView=new AccountSectionView(null);
  TextCell add=new TextCell(null);parent.accountSectionView.accountsListView.children.add(add);
  Theme.palette=1000;parent.onResume();
  assertCard(parent.accountSectionView,null,"first resume");assertText(add,null,"first resume");
  Theme.palette=2000;parent.onResume();
  assertCard(parent.accountSectionView,null,"return after theme change");assertText(add,null,"return after theme change");
  check(parent.resumes==2,"resume calls superclass lifecycle every time");
  parent.contentView=null;int previous=parent.listView.invalidations;parent.onResume();
  check(parent.resumes==3&&parent.listView.invalidations==previous,"resume before view creation is null-safe and preserves superclass lifecycle");
 }
 static void persistExpansion(){
  MessagesController.global.values.clear();
  SettingsAccountThemeRegression first=new SettingsAccountThemeRegression();
  first.restoreAccountsExpanded();
  check(!first.accountsExpanded,"first launch stays collapsed");
  first.toggleAccountsExpanded();
  check(first.accountsExpanded&&first.listView.adapter.updates==1,"expand immediately updates list");
  SettingsAccountThemeRegression otherAccount=new SettingsAccountThemeRegression();
  otherAccount.restoreAccountsExpanded();
  check(otherAccount.accountsExpanded,"new page and account switch restore expansion");
  otherAccount.toggleAccountsExpanded();
  check(!otherAccount.accountsExpanded,"collapse applies immediately");
  first.onResume();
  check(!first.accountsExpanded,"retained page restores latest collapsed preference");
  SettingsAccountThemeRegression recreated=new SettingsAccountThemeRegression();
  recreated.restoreAccountsExpanded();
  check(!recreated.accountsExpanded,"page recreation keeps explicit collapse");
  recreated.listView=null;recreated.toggleAccountsExpanded();
  check(MessagesController.global.getBoolean(PREF_ACCOUNTS_EXPANDED,false),"toggle persists without an attached list");
  recreated.restoreAccountsExpanded();
  MessagesController.global.values.clear();
 }
 public static void main(String[] args){
  repeatedSwitches();collapseAndExpand();resourceProvidersAndAttachment();retainedPageResume();
  persistExpansion();
  System.out.println("Settings accounts theme: "+checks+" production-method behavior assertions passed");
 }
}
'''

for placeholder, production in {
    "RECYCLER_EACH": recycler_each, "VIEW_EACH": view_each,
    "TEXT_COLORS": text_colors, "GRAY_COLORS": gray_colors,
    "ACCOUNT_COLORS": account_colors, "PARENT_COLORS": parent_colors,
    "PARENT_RESUME": parent_resume,
    "RESTORE_ACCOUNTS": restore_accounts, "TOGGLE_ACCOUNTS": toggle_accounts,
    "SECTION_SET": section_set, "SECTION_ATTACH": section_attach,
    "SECTION_COLORS": section_colors,
}.items():
    fixture = fixture.replace("/*" + placeholder + "*/", production)


def run(directory, source, expected_success):
    path = Path(directory) / "SettingsAccountThemeRegression.java"
    path.write_text(source)
    subprocess.run([str(JAVA / "javac"), "-d", directory, str(path)], check=True,
                   capture_output=True, text=True)
    result = subprocess.run([str(JAVA / "java"), "-cp", directory,
                             "SettingsAccountThemeRegression"], capture_output=True, text=True)
    if expected_success:
        if result.returncode:
            raise AssertionError(result.stderr)
        print(result.stdout.strip())
    else:
        assert result.returncode != 0 and "AssertionError" in result.stderr, \
            "regression mutation was not detected: " + result.stdout + result.stderr


with tempfile.TemporaryDirectory(prefix="settings-account-theme-") as directory:
    run(directory, fixture, True)
    # Ensure these are behavior tests, not tests that merely accept any stub.
    mutations = {
        "missing TextCell update": fixture.replace("((TextCell) child).updateColors();", "", 1),
        "missing cached-row traversal": fixture.replace(
            "i < recyclerView.getCachedChildCount()", "i < 0", 1),
        "stale row backgrounds": fixture.replace(
            "child.setBackgroundColor(Color.TRANSPARENT);", "", 1),
        "missing parent dispatch": fixture.replace("accountSectionView.updateColors();", "", 1),
        "missing first attachment refresh": fixture.replace(section_attach,
            section_attach.replace("updateColors();", "", 1), 1),
        "missing badge text refresh": fixture.replace(account_colors,
            account_colors.replace("counterView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, resourcesProvider));", "", 1), 1),
        "missing retained-page resume refresh": fixture.replace(parent_resume,
            parent_resume.replace("updateColors();", "", 1), 1),
    }
    for name, mutation in mutations.items():
        assert mutation != fixture, "mutation did not modify extracted production code: " + name
        run(directory, mutation, False)
    print(f"Settings accounts theme: {source_checks} source-wiring checks and {len(mutations)} regression mutations passed")
