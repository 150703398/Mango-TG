#!/usr/bin/env python3
"""Exercise QR decoding and token-completion lifecycle from production scanner methods."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
java_sources = ROOT/'TMessagesProj/src/main/java'
source = (java_sources/'org/telegram/ui/CameraScanActivity.java').read_text()

def method(marker):
    start=source.index(marker);end=source.index('{',start)+1;depth=1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]

fixture=r'''
import java.util.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.encoder.*;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
public class QrGalleryTest {
 static final int TYPE_QR_LOGIN=2, TYPE_QR_WEB_BOT=3;
 int cameraGeneration=1;
 boolean cameraResultDelivered;
 int currentType=TYPE_QR_LOGIN, galleryGeneration=1, finished, resumed, processed, delivered;
 boolean scannerDestroyed, galleryResultDelivered, recognized, qrLoading, galleryBusy;
 String recognizedText;
 CameraView cameraView;
 Delegate delegate;
 static class CameraView { Object getCameraSession(){return null;} }
 static class CameraController {static CameraController getInstance(){return new CameraController();} void stopPreview(Object session){} }
 static class TextUtils {static boolean isEmpty(String value){return value==null||value.isEmpty();}}
 static class FileLog {static void e(Exception ignored){} }
 static class AndroidUtilities {
  static ArrayList<Runnable> queue=new ArrayList<>();
  static void runOnUIThread(Runnable action){queue.add(action);}
  static void flush(){while(!queue.isEmpty())queue.remove(0).run();}
 }
 interface Delegate {boolean processQr(String text,Runnable end);void didFindQr(String text);default Runnable getLoginAction(){return null;}}
 void updateRecognized(){}
 void finishFragment(){finished++;}
 void resumeAfterGallery(boolean error){resumed++;}
 static class Bitmap {
  final int width,height;final int[] pixels;
  Bitmap(int w,int h,int[] p){width=w;height=h;pixels=p;}
  int getWidth(){return width;}int getHeight(){return height;}
  void getPixels(int[] dest,int offset,int stride,int x,int y,int w,int h){System.arraycopy(pixels,0,dest,0,pixels.length);}
 }
 //METHODS
 static int checks;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 static Bitmap qr(String text,boolean invert) throws Exception {
  ByteMatrix data=Encoder.encode(text,ErrorCorrectionLevel.M).getMatrix();
  int scale=8,quiet=4,side=(data.getWidth()+quiet*2)*scale;
  int[] pixels=new int[side*side];Arrays.fill(pixels,invert?0xff000000:0xffffffff);
  for(int y=0;y<data.getHeight();y++)for(int x=0;x<data.getWidth();x++)if(data.get(x,y)==1)
   for(int dy=0;dy<scale;dy++)for(int dx=0;dx<scale;dx++)pixels[((y+quiet)*scale+dy)*side+(x+quiet)*scale+dx]=invert?0xffffffff:0xff000000;
  return new Bitmap(side,side,pixels);
 }
 public static void main(String[] args) throws Exception {
  String token="tg://login?token="+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
  QrGalleryTest t=new QrGalleryTest();
  check(t.isValidQrText(token),"valid login token");
  for(String bad:new String[]{"", "tg://login?token=", "tg://login?token=!", "tg://login?token=A", "https://example.org", "tg://login?token=QQ&other=x"})
   check(!t.isValidQrText(bad),"reject invalid login token");
  t.currentType=1;check(t.isValidQrText("https://example.org"),"normal scanner still accepts ordinary links");t.currentType=2;
  check(t.decodeGalleryQr(qr(token,false)).equals(token),"real QR decoder reads photo pixels");
  check(t.decodeGalleryQr(qr(token,true)).equals(token),"inverted QR photo decoded");
  check(t.decodeGalleryQr(qr("https://t.me/mango_test",false)).equals("https://t.me/mango_test"),"ordinary profile QR photo decoded");
  final Runnable[] done={null};
  t.delegate=new Delegate(){public boolean processQr(String text,Runnable end){t.processed++;done[0]=end;return true;}public void didFindQr(String text){t.delivered++;}};
  t.processGalleryQr(token,1);AndroidUtilities.flush();
  check(t.processed==1&&t.delivered==0&&t.finished==0,"wait for accept-token completion before didFindQr");
  done[0].run();done[0].run();AndroidUtilities.flush();
  check(t.delivered==1&&t.finished==1,"duplicate asynchronous callback delivered only once");
  QrGalleryTest sync=new QrGalleryTest();
  sync.delegate=new Delegate(){public boolean processQr(String text,Runnable end){end.run();return false;}public void didFindQr(String text){sync.delivered++;}};
  sync.processGalleryQr(token,1);AndroidUtilities.flush();check(sync.delivered==1&&sync.finished==1,"synchronous completion plus false return still only delivers once");
  QrGalleryTest destroyed=new QrGalleryTest();
  destroyed.delegate=new Delegate(){public boolean processQr(String text,Runnable end){done[0]=end;return true;}public void didFindQr(String text){destroyed.delivered++;}};
  destroyed.processGalleryQr(token,1);destroyed.scannerDestroyed=true;done[0].run();AndroidUtilities.flush();
  check(destroyed.delivered==0&&destroyed.finished==0,"dismissed scanner ignores pending token callback");
  QrGalleryTest changed=new QrGalleryTest();
  changed.delegate=new Delegate(){public boolean processQr(String text,Runnable end){done[0]=end;return true;}public void didFindQr(String text){changed.delivered++;}};
  changed.processGalleryQr(token,1);changed.galleryGeneration++;done[0].run();AndroidUtilities.flush();check(changed.delivered==0,"stale gallery attempt cannot deliver");
  QrGalleryTest shortcut=new QrGalleryTest();
  final int[] jumps={0};
  shortcut.delegate=new Delegate(){public boolean processQr(String text,Runnable end){throw new AssertionError("shortcut must not submit tokens");}public void didFindQr(String text){throw new AssertionError("shortcut must not report a QR");}public Runnable getLoginAction(){return () -> {check(shortcut.finished==1,"old scanner closes before navigation");jumps[0]++;};}};
  shortcut.galleryBusy=true;shortcut.openLoginScanner();check(jumps[0]==0,"gallery in progress blocks login shortcut");shortcut.galleryBusy=false;
  shortcut.qrLoading=true;shortcut.openLoginScanner();check(jumps[0]==0,"QR submission in progress blocks login shortcut");shortcut.qrLoading=false;
  shortcut.scannerDestroyed=true;shortcut.openLoginScanner();check(jumps[0]==0,"destroyed scanner cannot navigate");shortcut.scannerDestroyed=false;
  shortcut.openLoginScanner();shortcut.openLoginScanner();check(jumps[0]==1&&shortcut.finished==1,"login shortcut closes and navigates exactly once");
  QrGalleryTest unavailable=new QrGalleryTest();unavailable.openLoginScanner();check(unavailable.finished==0,"scanner without a login delegate stays open");
  unavailable.delegate=new Delegate(){public boolean processQr(String text,Runnable end){return false;}public void didFindQr(String text){}};
  unavailable.openLoginScanner();check(unavailable.finished==0,"scanner without a login action stays open");
  QrGalleryTest camera=new QrGalleryTest();
  camera.delegate=new Delegate(){public boolean processQr(String text,Runnable end){return false;}public void didFindQr(String text){camera.delivered++;check(text.equals(token),"camera callback retains captured token");}};
  camera.completeCameraQr(token,1);camera.completeCameraQr(token,1);AndroidUtilities.flush();
  check(camera.delivered==1&&camera.finished==1,"duplicate camera completion closes only once");
  QrGalleryTest closedCamera=new QrGalleryTest();closedCamera.delegate=camera.delegate;
  closedCamera.completeCameraQr(token,1);closedCamera.scannerDestroyed=true;AndroidUtilities.flush();
  check(closedCamera.finished==0&&camera.delivered==1,"queued camera completion ignored after close");
  QrGalleryTest staleCamera=new QrGalleryTest();staleCamera.delegate=camera.delegate;staleCamera.cameraGeneration=2;
  staleCamera.completeCameraQr(token,1);AndroidUtilities.flush();check(camera.delivered==1&&staleCamera.finished==0,"previous camera attempt ignored");
  staleCamera.galleryBusy=true;staleCamera.completeCameraQr(token,2);AndroidUtilities.flush();check(camera.delivered==1,"camera completion ignored during gallery attempt");
  QrGalleryTest web=new QrGalleryTest();web.currentType=TYPE_QR_WEB_BOT;
  web.delegate=new Delegate(){public boolean processQr(String text,Runnable end){return false;}public void didFindQr(String text){web.delivered++;}};
  web.completeCameraQr(token,1);AndroidUtilities.flush();check(web.delivered==1&&web.finished==0,"web scanner remains open after delivery");
  web.cameraGeneration=2;web.cameraResultDelivered=false;web.completeCameraQr(token,2);AndroidUtilities.flush();check(web.delivered==2,"new web camera attempt can deliver again");
  System.out.println("PASS: "+checks+" QR gallery/camera decode/lifecycle behavior checks");
 }
}
'''
fixture=fixture.replace('//METHODS','\n'.join([method('    private boolean isValidQrText('),method('    private String decodeGalleryQr('),method('    private void processGalleryQr('),method('    private void openLoginScanner('),method('    private void completeCameraQr(')]))
sessions_source = (java_sources/'org/telegram/ui/SessionsActivity.java').read_text()
start = sessions_source.index('    private void openCameraScanActivity(boolean openGalleryOnShow)')
end = sessions_source.index('\n    @Override\n    public ArrayList<ThemeDescription>', start)
sessions_method = sessions_source[start:end]
sessions_fixture = r"""
import java.util.*;
import android.util.Base64;
public class SessionsActivity {
 static int checks;static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static class TLObject {}
 static class TLRPC {
  static class TL_authorization extends TLObject {boolean password_pending;}
  static class TL_error {String text;}
  static class TL_auth_acceptLoginToken extends TLObject {byte[] token;}
 }
 static class CameraScanActivity {
  static final int TYPE_QR_LOGIN=2;static CameraScanActivityDelegate current;
  interface CameraScanActivityDelegate {void didFindQr(String text);boolean processQr(String text,Runnable done);default void onDismiss(){}}
  static void showAsSheet(Object a,boolean gallery,int type,CameraScanActivityDelegate delegate,boolean open){current=delegate;}
 }
 static class AndroidUtilities {
  static final ArrayList<Runnable> queue=new ArrayList<>(),delayed=new ArrayList<>();
  static void runOnUIThread(Runnable r){queue.add(r);}static void runOnUIThread(Runnable r,int ms){delayed.add(r);}
  static void cancelRunOnUIThread(Runnable r){queue.remove(r);delayed.remove(r);}
  static void flush(){while(!queue.isEmpty())queue.remove(0).run();}
  static void advance(){ArrayList<Runnable> work=new ArrayList<>(delayed);delayed.clear();for(Runnable r:work)r.run();flush();}
 }
 interface RequestCallback {void run(TLObject response,TLRPC.TL_error error);}
 static class ConnectionsManager {
  int sent,cancelled;RequestCallback callback;
  int sendRequest(TLObject request,RequestCallback cb){sent++;callback=cb;return sent;}
  void cancelRequest(int id,boolean notifyServer){check(!notifyServer,"cancellation is local only");cancelled++;}
  void reply(){callback.run(new TLRPC.TL_authorization(),null);AndroidUtilities.flush();}
 }
 static class R {static class string {static int AccountAlreadyLoggedIn=1,ErrorOccurred=2,AuthAnotherClient=3;}}
 static class LocaleController {static String getString(int n){return "text";}}
 static class FileLog {static void e(String s,Exception e){}}
 static class AlertsCreator {static int count;static void showSimpleAlert(Object a,String title,String body){count++;}}
 static class Adapter {void notifyDataSetChanged(){}}
 static class UndoView {static final int ACTION_QR_SESSION_ACCEPTED=1;int shown;void showWithAction(int a,int b,TLObject o){shown++;}}
 boolean isFinished;int repeatLoad;ArrayList<TLObject> sessions=new ArrayList<>(),passwordSessions=new ArrayList<>();
 ConnectionsManager connections=new ConnectionsManager();Adapter listAdapter=new Adapter();UndoView undoView=new UndoView();
 Object getParentActivity(){return this;}ConnectionsManager getConnectionsManager(){return connections;}
 void updateRows(){}void loadSessions(boolean b){}
 //METHOD
 static CameraScanActivity.CameraScanActivityDelegate open(SessionsActivity activity){activity.openCameraScanActivity(false);return CameraScanActivity.current;}
 public static void main(String[] args){
  String token="tg://login?token=QQ";int[] completed={0};
  SessionsActivity before=new SessionsActivity();var first=open(before);first.processQr(token,()->completed[0]++);
  Runnable raced=AndroidUtilities.delayed.get(0);first.onDismiss();
  check(AndroidUtilities.delayed.isEmpty(),"dismiss removes delayed login submission");raced.run();AndroidUtilities.flush();
  check(before.connections.sent==0&&completed[0]==0,"raced delayed task cannot submit after dismissal");
  check(!first.processQr(token,()->completed[0]++),"dismissed scanner cannot start another request");
  SessionsActivity gone=new SessionsActivity();var goneDelegate=open(gone);goneDelegate.processQr(token,()->completed[0]++);gone.isFinished=true;AndroidUtilities.advance();
  check(gone.connections.sent==0,"destroyed parent prevents delayed login submission");
  SessionsActivity pending=new SessionsActivity();var second=open(pending);second.processQr(token,()->completed[0]++);AndroidUtilities.advance();
  check(pending.connections.sent==1,"live scanner submits token once");second.onDismiss();check(pending.connections.cancelled==1,"dismiss cancels outstanding local request");
  pending.connections.reply();second.didFindQr(token);check(completed[0]==0&&pending.sessions.isEmpty()&&pending.undoView.shown==0,"cancelled response and direct delivery ignored");
  SessionsActivity finishedParent=new SessionsActivity();var fp=open(finishedParent);fp.processQr(token,()->completed[0]++);AndroidUtilities.advance();finishedParent.isFinished=true;finishedParent.connections.reply();
  check(completed[0]==0,"response cannot complete after parent closes");fp.onDismiss();
  SessionsActivity live=new SessionsActivity();var third=open(live);third.processQr(token,()->completed[0]++);third.processQr(token,()->completed[0]+=100);AndroidUtilities.advance();
  check(live.connections.sent==1,"duplicate processing does not enqueue another token");live.connections.reply();live.connections.reply();
  check(completed[0]==1,"duplicate response completes scanner once");third.didFindQr(token);third.didFindQr(token);
  check(live.sessions.size()==1&&live.undoView.shown==1,"successful authorization updates UI once");third.onDismiss();check(live.connections.cancelled==0,"completed request is not cancelled");
  SessionsActivity malformed=new SessionsActivity();var bad=open(malformed);bad.processQr("bad",()->completed[0]++);AndroidUtilities.advance();
  check(malformed.connections.sent==0&&completed[0]==2&&AlertsCreator.count==1,"malformed token exits once without submission");bad.onDismiss();
  System.out.println("PASS: "+checks+" QR login delayed-request cancellation checks");
 }
}
""".replace('//METHOD', sessions_method)
# The Android Base64 API is a tiny compatibility double; all QR encoder/decoder code is real repository code.
base64='''package android.util; public class Base64 { public static final int URL_SAFE=8; public static byte[] decode(String text,int flags){return java.util.Base64.getUrlDecoder().decode(text);} }'''
java=Path(os.environ.get('JAVA_HOME','/Applications/Android Studio.app/Contents/jbr/Contents/Home'))/'bin'
with tempfile.TemporaryDirectory(prefix='mango-qr-gallery-') as directory:
    d=Path(directory);(d/'QrGalleryTest.java').write_text(fixture)
    (d/'android/util').mkdir(parents=True);(d/'android/util/Base64.java').write_text(base64)
    subprocess.run([str(java/'javac'),'-d',directory,'-sourcepath',str(java_sources)+os.pathsep+directory,str(d/'QrGalleryTest.java'),str(d/'android/util/Base64.java')],check=True)
    subprocess.run([str(java/'java'),'-cp',directory,'QrGalleryTest'],check=True)
    (d/'SessionsActivity.java').write_text(sessions_fixture)
    subprocess.run([str(java/'javac'),'-cp',directory,'-d',directory,str(d/'SessionsActivity.java')],check=True)
    subprocess.run([str(java/'java'),'-cp',directory,'SessionsActivity'],check=True)
# Wiring and lifetime contracts not accessible without an Android Activity.
assert 'Intent.ACTION_OPEN_DOCUMENT' in source and 'READ_MEDIA_IMAGES' not in source
assert 'currentType == TYPE_QR_LOGIN && !text.startsWith' not in source
assert 'MAX_GALLERY_QR_SIZE = 2048' in source
assert 'if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();' in source
assert 'clearGalleryPicker();' in method('    public void onFragmentDestroy()')
sessions = (java_sources/'org/telegram/ui/SessionsActivity.java').read_text()
assert 'getParentActivity(), true, CameraScanActivity.TYPE_QR_LOGIN' in sessions
assert '.setNeutralButton(LocaleController.getString(R.string.MangoQrGallery)' in sessions
assert 'fragment.fragmentView.post(fragment::openGalleryPicker)' in source
assert source.count('needGalleryButton && currentType != TYPE_QR_LOGIN') == 2
qr = (java_sources/'org/telegram/ui/QrActivity.java').read_text()
intro = (java_sources/'org/telegram/ui/ActionIntroActivity.java').read_text()
assert 'fragment.getParentActivity(), true, CameraScanActivity.TYPE_QR' in qr
assert 'sessions.setCurrentAccount(currentAccount);' in qr
assert 'new SessionsActivity(SessionsActivity.TYPE_DEVICES).setOpenQrScanner()' in qr
assert '.setNeutralButton(LocaleController.getString(R.string.MangoQrGallery)' in qr
assert 'openQrScannerOnShow = false;\n                requestCameraScan();' in sessions
assert 'getParentActivity(), true, CameraScanActivity.TYPE_QR_LOGIN' in intro
assert '.setNeutralButton(LocaleController.getString(R.string.MangoQrGallery)' in intro
assert source.index('fragment.setDelegate(cameraDelegate);') < source.index('actionBarLayout[0].showLastFragment();')
assert 'textLayout == null || !(textLayout.getText() instanceof Spannable)' in source
print('PASS: 18 QR gallery, login shortcut, account, permission and cleanup wiring checks')

assert 'qrLoading = delegate.processQr(text, () -> completeCameraQr(text, generation));' in source
assert 'completeCameraQr(recognizedText, cameraGeneration);' in source
print('PASS: 2 camera completion routing checks')

assert 'fragment.scannerDestroyed = true;\n                super.dismiss();' in source
print('PASS: 1 immediate sheet-dismiss guard check')
