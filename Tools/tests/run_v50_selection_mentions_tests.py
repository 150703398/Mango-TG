#!/usr/bin/env python3
"""Run production selection layout and mention merge methods against stale/repeated data."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin'
SOURCE = ROOT / 'TMessagesProj/src/main/java'

def method(source, marker):
    start = source.index(marker)
    end = source.index('{', start) + 1
    depth = 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]

selection = (SOURCE / 'org/telegram/ui/Cells/TextSelectionHelper.java').read_text()
chat = selection[selection.index('public static class ChatListTextSelectionHelper'):]
selection_methods = '\n'.join(method(chat, marker) for marker in (
    '        protected int getLineHeight()',
    '        protected int getCharOffsetFromCord(',
    '        private void fillLayoutForCoords(',
    '        protected void fillLayoutForOffset(int offset, LayoutBlock layoutBlock, boolean maybe)',
))
selection_methods += '\n' + method(chat, 'protected boolean canSelect(int newSelection)').replace('super.canSelect(newSelection)', 'newSelection != selectionStart && newSelection != selectionEnd')
selection_fixture = r'''
import java.util.*;
public class SelectionRegression {
 static int checks;
 static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
 static int dp(int n){return n;}
 static class Layout {
  String text; Layout(String t){text=t;}
  CharSequence getText(){return text;}
  int getLineCount(){return 1;} int getLineTop(int n){return 0;} int getLineBottom(int n){return 20;}
  int getOffsetForHorizontal(int line,int x){return Math.min(text.length(),Math.max(0,x));}
 }
 static class StaticLayout extends Layout {StaticLayout(String t){super(t);}}
 static class MessageObject {
  ArrayList<TextLayoutBlock> textLayoutBlocks=new ArrayList<>();
  float textXOffset;
  static class TextLayoutBlocks {ArrayList<TextLayoutBlock> textLayoutBlocks=new ArrayList<>();float textXOffset;}
  static class TextLayoutBlock {
   StaticLayout textLayout;int padTop,charactersOffset;boolean quote,code;float y;
   TextLayoutBlock(String text,int offset,float y){this.textLayout=new StaticLayout(text);charactersOffset=offset;this.y=y;}
   float textYOffset(ArrayList<TextLayoutBlock> blocks){return y;}
   float height(Object params){return 20;}
   boolean isRtl(){return false;}
  }
 }
 static class ChatMessageCell {
  MessageObject object=new MessageObject();MessageObject.TextLayoutBlocks caption;StaticLayout description,fact;Object transitionParams;int type;
  MessageObject getMessageObject(){return object;} MessageObject.TextLayoutBlocks getCaptionLayout(){return caption;}
  StaticLayout getDescriptionlayout(){return description;}StaticLayout getFactCheckLayout(){return fact;}
 }
 static class LayoutBlock {Layout layout;int charOffset;float xOffset,yOffset;}
 static final int TYPE_MESSAGE=0,TYPE_CAPTION=1,TYPE_DESCRIPTION=2,TYPE_FACTCHECK=3;
 ChatMessageCell selectedView,maybeSelectedView;int selectedTextType,selectionStart,selectionEnd;
 LayoutBlock layoutBlock=new LayoutBlock();CharSequence selectedText="word";
 int getSelectionTextType(ChatMessageCell c,boolean maybe){return maybe?c.type:selectedTextType;}
 CharSequence getText(ChatMessageCell c,boolean maybe){return c==null?null:selectedText;}
 //METHODS
 public static void main(String[] args){
  SelectionRegression h=new SelectionRegression();ChatMessageCell c=new ChatMessageCell();h.selectedView=h.maybeSelectedView=c;
  check(h.getLineHeight()==0,"empty message layout has no line height");
  check(h.getCharOffsetFromCord(0,0,0,0,c,true)==-1,"empty message rejected before last block lookup");
  c.object.textLayoutBlocks=null;
  check(h.getCharOffsetFromCord(0,0,0,0,c,true)==-1,"null message blocks rejected");
  h.layoutBlock.layout=new StaticLayout("old");h.layoutBlock.charOffset=99;
  h.fillLayoutForCoords(1,1,c,h.layoutBlock,true);
  check(h.layoutBlock.layout==null&&h.layoutBlock.charOffset==0,"absent layouts clear previous cell hit");
  c.type=h.selectedTextType=TYPE_CAPTION;
  check(h.getLineHeight()==0,"deleted caption height safe");
  check(h.getCharOffsetFromCord(0,0,0,0,c,true)==-1,"deleted caption lookup safe");
  h.fillLayoutForOffset(1,h.layoutBlock,false);check(h.layoutBlock.layout==null,"deleted caption offset safe");
  c.caption=new MessageObject.TextLayoutBlocks();
  check(h.getLineHeight()==0&&h.getCharOffsetFromCord(0,0,0,0,c,true)==-1,"empty caption blocks safe");
  c.caption.textLayoutBlocks.add(new MessageObject.TextLayoutBlock("first",0,0));
  c.caption.textLayoutBlocks.add(new MessageObject.TextLayoutBlock("second",6,40));
  h.fillLayoutForOffset(8,h.layoutBlock,false);
  check(h.layoutBlock.charOffset==6&&h.layoutBlock.layout.getText().toString().equals("second"),"caption offsets resolve second block");
  check(h.getCharOffsetFromCord(2,41,0,0,c,true)==8,"caption coordinate maps back to full text offset");
  h.fillLayoutForCoords(2,30,c,h.layoutBlock,true);
  check(h.layoutBlock.layout==null,"gap between caption blocks does not reuse previous hit");
  c.type=h.selectedTextType=TYPE_MESSAGE;c.object.textLayoutBlocks=new ArrayList<>();
  c.object.textLayoutBlocks.add(new MessageObject.TextLayoutBlock("first",0,0));
  c.object.textLayoutBlocks.add(new MessageObject.TextLayoutBlock("last",6,40));
  check(h.getCharOffsetFromCord(2,41,0,0,c,true)==8,"message second block maps to full text offset");
  h.fillLayoutForOffset(8,h.layoutBlock,false);check(h.layoutBlock.charOffset==6,"message selection selects second block");
  c.object=null;h.fillLayoutForOffset(8,h.layoutBlock,false);
  check(h.layoutBlock.layout==null,"rebound cell without message clears layout");
  check(h.getCharOffsetFromCord(2,41,0,0,c,true)==-1,"rebound cell pointer lookup safe");
  h.selectionStart=0;h.selectionEnd=4;check(h.canSelect(2),"valid handle movement allowed");
  h.selectedText="x";check(!h.canSelect(2),"edited shorter message rejects stale handles");
  h.selectedText=null;check(!h.canSelect(0),"removed text rejects handle movement");
  h.selectedText="word";h.selectionStart=3;h.selectionEnd=2;check(!h.canSelect(1),"inverted stale range rejected");
  h.selectionStart=0;h.selectionEnd=4;check(!h.canSelect(5)&&!h.canSelect(-1),"handle outside text rejected");
  System.out.println("PASS: "+checks+" selection layout and stale-handle behavior checks");
 }
}
'''.replace('//METHODS', selection_methods)

adapter = (SOURCE / 'org/telegram/ui/Adapters/MentionsAdapter.java').read_text()
mention_methods = '\n'.join(method(adapter, marker) for marker in (
    '    private boolean addMentionResult(', '    private void showUsersResult('))
mention_fixture = r'''
import java.util.*;
public class MentionRegression {
 static int checks;static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
 static class TLObject {}
 static class TLRPC {
  static class User extends TLObject {long id;boolean bot;User(long n){id=n;}}
  static class Chat extends TLObject {long id;Chat(long n){id=n;}}
 }
 static class LongSparseArray<T> {LinkedHashMap<Long,T> map=new LinkedHashMap<>();int indexOfKey(long id){return map.containsKey(id)?0:-1;}void put(long n,T o){map.put(n,o);}int size(){return map.size();}}
 static class UserObject {static boolean isService(long id){return id==777000;}}
 static class AndroidUtilities {static void cancelRunOnUIThread(Runnable r){}}
 static class Delegate {void needChangePanelVisibility(boolean b){}}
 Delegate delegate=new Delegate();boolean allowBots=true,allowChats=true;
 ArrayList<TLObject> searchResultUsernames;LongSparseArray<TLObject> searchResultUsernamesMap;
 Runnable cancelDelayRunnable;Object searchResultBotContext,stickers;void notifyDataSetChanged(){}
 //METHODS
 public static void main(String[] args){
  MentionRegression h=new MentionRegression();TLRPC.User first=new TLRPC.User(10),duplicate=new TLRPC.User(10),second=new TLRPC.User(20);TLRPC.Chat chat=new TLRPC.Chat(10);
  ArrayList<TLObject> local=new ArrayList<>(Arrays.asList(first,duplicate,chat,second,first));LongSparseArray<TLObject> partial=new LongSparseArray<>();partial.put(20,second);
  h.showUsersResult(local,partial,true);
  check(h.searchResultUsernames.size()==3,"local member/dialog duplicates removed");
  check(h.searchResultUsernames.get(0)==first&&h.searchResultUsernames.get(1)==chat&&h.searchResultUsernames.get(2)==second,"first local match and ordering preserved");
  check(h.searchResultUsernamesMap.size()==3,"all displayed peers indexed despite partial rank map");
  check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,duplicate),"server duplicate of local peer ignored");
  check(h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,new TLRPC.User(30)),"new server peer appended");
  check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,new TLRPC.User(30)),"duplicate within server response ignored");
  check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,null),"uncached server peer ignored without aborting merge");
  check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,new TLRPC.User(0)),"empty peer ignored");
  h.showUsersResult(local,partial,false);check(h.searchResultUsernames.size()==3&&local.size()==5,"repeat response resets cleanly without mutating local input");
  TLRPC.User bot=new TLRPC.User(40);bot.bot=true;h.allowBots=false;
  check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,bot),"bot restrictions also apply to server peers");
  h.allowChats=false;check(!h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,new TLRPC.Chat(50)),"chat restriction preserved");
  h.allowBots=true;check(h.addMentionResult(h.searchResultUsernames,h.searchResultUsernamesMap,bot),"disallowing chats alone does not remove allowed bots");
  System.out.println("PASS: "+checks+" mention merge behavior checks");
 }
}
'''.replace('//METHODS', mention_methods)

with tempfile.TemporaryDirectory(prefix='mango-v50-selection-mentions-') as directory:
    for name, fixture in [('SelectionRegression', selection_fixture), ('MentionRegression', mention_fixture)]:
        path = Path(directory) / (name + '.java')
        path.write_text(fixture)
        subprocess.run([str(JAVA / 'javac'), '-d', directory, str(path)], check=True)
        subprocess.run([str(JAVA / 'java'), '-cp', directory, name], check=True)

# The drawing/gesture entry points must use the currently resolved block's offset.
assert 'layout.isRtlCharAt(selectionStart)' not in selection[selection.index('protected void onDraw(Canvas canvas)'):selection.index('protected void jumpToLine')]
assert 'layout.isRtlCharAt(selectionEnd)' not in selection[selection.index('protected void onDraw(Canvas canvas)'):selection.index('protected void jumpToLine')]
assert 'layoutNew.isRtlCharAt(newSelection)' not in selection
assert 'CharSequence selectedText = getText(selectedView, false);\n                int len = selectedText == null ? -1' in selection
assert 'addMentionResult(searchResultUsernames, searchResultUsernamesMap, user)' in adapter
assert 'addMentionResult(searchResultUsernames, searchResultUsernamesMap, chat)' in adapter
print('PASS: 6 selection/mention entry-point checks')
