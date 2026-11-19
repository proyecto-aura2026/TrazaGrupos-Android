package ar.trazagrupos.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceService extends AccessibilityService {
    private static final int DARK=0xe8192928, WHITE=0xffe9edef, GREEN=0xff13c79b;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String,Record> records=new LinkedHashMap<>();
    private WindowManager wm; private View widget,panel; private WindowManager.LayoutParams widgetParams,panelParams;
    private LinearLayout list; private TextView count; private EditText query,draft;
    private String selectedTopic="Todos",selectedPerson="Todos",currentChat="Chat activo"; private long lastScan;
    private final String[] topics={"Todos","Web e IA","API","Suscripciones","Coordinación","Otros"};
    private static class Record { String text,person,topic,chat; long seen; Record(String t,String p,String g,String c){text=t;person=p;topic=g;chat=c;seen=System.currentTimeMillis();} }

    @Override protected void onServiceConnected(){super.onServiceConnected();wm=(WindowManager)getSystemService(WINDOW_SERVICE);showWidget();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null||e.getPackageName()==null)return;
        String pkg=e.getPackageName().toString();if(!pkg.equals("com.whatsapp")&&!pkg.equals("com.whatsapp.w4b"))return;
        long now=System.currentTimeMillis();if(now-lastScan<300)return;lastScan=now;
        handler.postDelayed(this::scan,320);
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){remove(panel);remove(widget);super.onDestroy();}
    private void scan(){
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
        ArrayList<String> lines=new ArrayList<>();walk(root,lines,0);root.recycle();
        if(lines.size()>2)currentChat=lines.get(1).length()<100?lines.get(1):currentChat;
        String person="Autor no identificado";
        for(String line:lines){
            String s=line.trim();if(s.length()==0||s.equals("Mensaje")||s.equals("Enviar"))continue;
            Matcher number=Pattern.compile("\\+\\d[\\d ]{7,18}").matcher(s);
            if(number.find() && s.length()<75){person=number.group().trim();continue;}
            if(s.length()<13||s.length()>1800)continue;
            if(s.matches("(?i).*(escribe un mensaje|buscar|videollamada|más opciones).*"))continue;
            String key=currentChat+"|"+person+"|"+s;
            if(!records.containsKey(key)){records.put(key,new Record(s,person,classify(s),currentChat));if(records.size()>500)records.remove(records.keySet().iterator().next());}
        }
        if(panel!=null)renderList();
    }
    private void walk(AccessibilityNodeInfo n,ArrayList<String> out,int depth){
        if(n==null||depth>35||out.size()>300)return;
        CharSequence text=n.getText();if(text!=null){String s=text.toString().trim();if(!s.isEmpty()&&(out.isEmpty()||!out.get(out.size()-1).equals(s)))out.add(s);}
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){walk(c,out,depth+1);c.recycle();}}
    }
    private String classify(String s){String t=s.toLowerCase(Locale.ROOT);
        if(t.contains("api")||t.contains("deepseek")||t.contains("tokens"))return "API";
        if(t.contains("web")||t.contains("codex")||t.contains("ia")||t.contains("html")||t.contains("apk"))return "Web e IA";
        if(t.contains("suscrip")||t.contains("cuenta")||t.contains("venta"))return "Suscripciones";
        if(t.contains("horario")||t.contains("reunión")||t.contains("fecha"))return "Coordinación";
        return "Otros";
    }
    private WindowManager.LayoutParams params(int w,int h,int x,int y){WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;return p;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,0x66ffffff);return d;}
    private TextView label(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(WHITE);v.setTextSize(size);v.setPadding(10,8,10,8);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(WHITE);b.setAllCaps(false);b.setBackground(bg(0xff25433b,14));return b;}
    private void showWidget(){if(widget!=null)return;Button b=button("🗨 Trazar");widget=b;widgetParams=params(150,65,20,180);wm.addView(b,widgetParams);b.setOnClickListener(v->togglePanel());drag(b,widgetParams);}
    private void togglePanel(){if(panel!=null){remove(panel);panel=null;return;}showPanel();}
    private void showPanel(){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(7,7,7,7);shell.setBackground(bg(DARK,26));panel=shell;
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView title=label("🗨 Trazabilidad",15);title.setTypeface(null,Typeface.BOLD);head.addView(title,new LinearLayout.LayoutParams(0,55,1));Button close=button("×");head.addView(close,new LinearLayout.LayoutParams(55,50));close.setOnClickListener(v->togglePanel());shell.addView(head);drag(head,null);
        count=label("Mensajes visibles",11);shell.addView(count);
        LinearLayout controls=new LinearLayout(this);Button person=button("Número: Todos");Button topic=button("Tema: Todos");controls.addView(person,new LinearLayout.LayoutParams(0,50,1));controls.addView(topic,new LinearLayout.LayoutParams(0,50,1));shell.addView(controls);
        person.setOnClickListener(v->{ArrayList<String> all=new ArrayList<>();all.add("Todos");for(Record r:records.values())if(!all.contains(r.person))all.add(r.person);selectedPerson=all.get((all.indexOf(selectedPerson)+1)%all.size());person.setText("Número: "+selectedPerson);renderList();});
        topic.setOnClickListener(v->{int n=0;for(int i=0;i<topics.length;i++)if(topics[i].equals(selectedTopic))n=i;selectedTopic=topics[(n+1)%topics.length];topic.setText("Tema: "+selectedTopic);renderList();});
        query=new EditText(this);query.setSingleLine(true);query.setTextColor(WHITE);query.setHintTextColor(0xff9bb6ab);query.setHint("Buscar mensajes");shell.addView(query);query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){renderList();}public void afterTextChanged(Editable e){}});
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        draft=new EditText(this);draft.setTextColor(WHITE);draft.setHintTextColor(0xff9bb6ab);draft.setHint("Redactar respuesta y copiar para revisar en WhatsApp");draft.setMinLines(2);shell.addView(draft);
        Button copy=button("Copiar borrador · envío manual");shell.addView(copy);copy.setOnClickListener(v->{String s=draft.getText().toString();if(!s.trim().isEmpty()){ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Borrador TrazaGrupos",s));copy.setText("Copiado. Revisá antes de enviar");}});
        int width=getResources().getDisplayMetrics().widthPixels;int height=getResources().getDisplayMetrics().heightPixels;
        panelParams=params(Math.min(width-24,550),Math.min((int)(height*.66),1000),12,(int)(height*.22));panelParams.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;wm.addView(shell,panelParams);renderList();
    }
    private void renderList(){if(list==null||count==null)return;list.removeAllViews();String q=query==null?"":query.getText().toString().toLowerCase(Locale.ROOT);int n=0;
        for(Record r:records.values()){if(!selectedTopic.equals("Todos")&&!selectedTopic.equals(r.topic))continue;if(!selectedPerson.equals("Todos")&&!selectedPerson.equals(r.person))continue;if(!r.text.toLowerCase(Locale.ROOT).contains(q))continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(bg(0xff263b38,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,8);list.addView(card,lp);
            TextView meta=label(r.person+" · "+r.topic,11);meta.setTextColor(GREEN);card.addView(meta);TextView body=label(r.text,13);card.addView(body);n++;
        }count.setText(n+" mensajes encontrados · "+currentChat+" · texto visible. Desplazate en el chat para cargar más.");
    }
    private void drag(View handle,WindowManager.LayoutParams explicit){handle.setOnTouchListener(new View.OnTouchListener(){float x0,y0;int startX,startY;boolean moved;public boolean onTouch(View v,MotionEvent e){WindowManager.LayoutParams p=explicit!=null?explicit:panelParams;if(p==null)return false;switch(e.getAction()){case MotionEvent.ACTION_DOWN:x0=e.getRawX();y0=e.getRawY();startX=p.x;startY=p.y;moved=false;return true;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getRawX()-x0)+Math.abs(e.getRawY()-y0)>10)moved=true;if(moved){p.x=startX+(int)(e.getRawX()-x0);p.y=startY+(int)(e.getRawY()-y0);wm.updateViewLayout(explicit!=null?widget:panel,p);}return true;case MotionEvent.ACTION_UP:if(!moved)v.performClick();return true;}return false;}});}
    private void remove(View v){if(v!=null&&wm!=null){try{wm.removeView(v);}catch(Exception ignored){}}}
}
